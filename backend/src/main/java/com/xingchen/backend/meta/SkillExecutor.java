package com.xingchen.backend.meta;

import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 执行器
 * 
 * 执行 Skill 并支持自我验证与修复
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SkillExecutor {

    private final AIService aiService;
    private final ToolRegistry toolRegistry;
    private final SkillRepository skillRepository;
    
    // 执行中的 Skill
    private final Map<String, ExecutionContext> runningExecutions = new ConcurrentHashMap<>();
    
    /**
     * 执行 Skill
     */
    public ExecutionResult execute(Skill skill, Map<String, Object> inputs) {
        String executionId = generateExecutionId();
        log.info("开始执行 Skill: skillId={}, executionId={}", skill.getId(), executionId);
        
        ExecutionContext context = new ExecutionContext(executionId, skill, inputs);
        runningExecutions.put(executionId, context);
        
        Instant start = Instant.now();
        boolean success = false;
        String error = null;
        Object result = null;
        
        try {
            // 1. 验证输入
            validateInputs(skill, inputs);
            
            // 2. 执行步骤
            result = executeSteps(skill, context);
            
            // 3. 验证输出
            success = validateOutput(skill, result);
            
            if (!success) {
                error = "输出验证失败";
            }
            
        } catch (Exception e) {
            log.error("Skill 执行失败: skillId={}", skill.getId(), e);
            error = e.getMessage();
            success = false;
            
            // 尝试修复
            RepairResult repair = attemptRepair(skill, context, e);
            if (repair.isSuccess()) {
                log.info("Skill 修复成功，重新执行");
                return execute(repair.getRepairedSkill(), inputs);
            }
        } finally {
            runningExecutions.remove(executionId);
            
            // 记录执行统计
            long latency = Duration.between(start, Instant.now()).toMillis();
            skill.recordExecution(success, latency, error);
            skillRepository.save(skill);
        }
        
        return ExecutionResult.builder()
                .executionId(executionId)
                .skillId(skill.getId())
                .success(success)
                .result(result)
                .error(error)
                .executionTime(Duration.between(start, Instant.now()).toMillis())
                .build();
    }
    
    /**
     * 异步执行 Skill
     */
    @Async("taskExecutor")
    public CompletableFuture<ExecutionResult> executeAsync(Skill skill, 
                                                            Map<String, Object> inputs) {
        return CompletableFuture.completedFuture(execute(skill, inputs));
    }
    
    /**
     * 执行步骤
     */
    private Object executeSteps(Skill skill, ExecutionContext context) {
        Map<String, Object> stepResults = new HashMap<>();
        
        for (Skill.SkillStep step : skill.getSteps()) {
            log.debug("执行步骤 {}: {}", step.getOrder(), step.getName());
            
            // 检查条件
            if (step.getCondition() != null && !evaluateCondition(step.getCondition(), stepResults)) {
                log.debug("步骤 {} 条件不满足，跳过", step.getName());
                continue;
            }
            
            // 检查依赖
            if (step.getDependencies() != null) {
                for (String dep : step.getDependencies()) {
                    if (!stepResults.containsKey(dep)) {
                        throw new RuntimeException("依赖步骤未执行: " + dep);
                    }
                }
            }
            
            // 执行步骤
            Object stepResult = executeStep(step, context, stepResults);
            stepResults.put(step.getName(), stepResult);
            
            context.getStepResults().put(step.getName(), stepResult);
        }
        
        // 返回最终结果
        return stepResults.get("output_result");
    }
    
    /**
     * 执行单个步骤
     */
    private Object executeStep(Skill.SkillStep step, ExecutionContext context,
                               Map<String, Object> stepResults) {
        int retryCount = 0;
        int maxRetries = step.getRetryCount() > 0 ? step.getRetryCount() : 1;
        
        while (retryCount < maxRetries) {
            try {
                return doExecuteStep(step, context, stepResults);
            } catch (Exception e) {
                retryCount++;
                log.warn("步骤 {} 执行失败 (尝试 {}/{}): {}", 
                        step.getName(), retryCount, maxRetries, e.getMessage());
                
                if (retryCount >= maxRetries) {
                    // 尝试 fallback
                    if (step.getFallbackStep() != null) {
                        log.info("执行 fallback 步骤: {}", step.getFallbackStep());
                        return executeFallback(step, context, stepResults);
                    }
                    throw e;
                }
                
                // 指数退避
                try {
                    Thread.sleep((long) (Math.pow(2, retryCount) * 1000));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("执行中断", ie);
                }
            }
        }
        
        throw new RuntimeException("步骤执行失败，已达到最大重试次数");
    }
    
    /**
     * 实际执行步骤
     */
    private Object doExecuteStep(Skill.SkillStep step, ExecutionContext context,
                                  Map<String, Object> stepResults) {
        return switch (step.getType()) {
            case LLM_CALL -> executeLLMStep(step, context);
            case TOOL_CALL -> executeToolStep(step, context);
            case CODE_EXEC -> executeCodeStep(step, context, stepResults);
            case CONDITION -> evaluateCondition(step.getCondition(), stepResults);
            case OUTPUT -> stepResults;
            default -> throw new UnsupportedOperationException("不支持的步骤类型: " + step.getType());
        };
    }
    
    /**
     * 执行 LLM 调用步骤
     */
    private Object executeLLMStep(Skill.SkillStep step, ExecutionContext context) {
        String prompt = (String) step.getParameters().get("prompt");
        if (prompt == null) {
            prompt = buildPromptFromContext(step, context);
        }
        
        return aiService.chat(prompt);
    }
    
    /**
     * 执行工具调用步骤
     */
    private Object executeToolStep(Skill.SkillStep step, ExecutionContext context) {
        String toolId = step.getToolId();
        ToolOption tool = toolRegistry.getTool(toolId);
        
        if (tool == null) {
            throw new RuntimeException("工具不存在: " + toolId);
        }
        
        // 调用工具
        Map<String, Object> params = step.getParameters();
        return invokeTool(tool, params, context);
    }
    
    /**
     * 执行代码步骤
     */
    private Object executeCodeStep(Skill.SkillStep step, ExecutionContext context,
                                    Map<String, Object> stepResults) {
        String code = (String) step.getParameters().get("code");
        if (code == null) {
            code = context.getSkill().getCode();
        }
        
        // 简化实现：实际应该使用脚本引擎执行
        log.debug("执行代码步骤，代码长度: {}", code != null ? code.length() : 0);
        return Map.of("executed", true, "code", code);
    }
    
    /**
     * 执行 fallback
     */
    private Object executeFallback(Skill.SkillStep step, ExecutionContext context,
                                    Map<String, Object> stepResults) {
        // 简化实现
        return Map.of("fallback", true, "originalStep", step.getName());
    }
    
    /**
     * 验证输入
     */
    private void validateInputs(Skill skill, Map<String, Object> inputs) {
        // 简化实现
        if (inputs == null) {
            throw new IllegalArgumentException("输入不能为空");
        }
    }
    
    /**
     * 验证输出
     */
    private boolean validateOutput(Skill skill, Object result) {
        // 简化实现
        return result != null;
    }
    
    /**
     * 评估条件
     */
    private boolean evaluateCondition(String condition, Map<String, Object> stepResults) {
        // 简化实现，实际应该使用表达式引擎
        return true;
    }
    
    /**
     * 构建提示词
     */
    private String buildPromptFromContext(Skill.SkillStep step, ExecutionContext context) {
        return "执行步骤: " + step.getName();
    }
    
    /**
     * 调用工具
     */
    private Object invokeTool(ToolOption tool, Map<String, Object> params, 
                              ExecutionContext context) {
        // 简化实现，实际应该调用具体的工具实现
        return Map.of(
                "toolId", tool.getToolId(),
                "executed", true,
                "params", params
        );
    }
    
    /**
     * 尝试修复
     */
    private RepairResult attemptRepair(Skill skill, ExecutionContext context, Exception error) {
        log.info("尝试修复 Skill: skillId={}", skill.getId());
        
        // 1. 分析错误
        ErrorAnalysis analysis = analyzeError(error, context);
        
        // 2. 选择修复策略
        for (MetaCapability.RepairStrategy strategy : getRepairStrategies()) {
            if (matchesStrategy(analysis, strategy)) {
                log.info("应用修复策略: {}", strategy.getName());
                
                // 3. 执行修复
                Skill repairedSkill = applyRepair(skill, context, strategy, analysis);
                
                return RepairResult.builder()
                        .success(true)
                        .repairedSkill(repairedSkill)
                        .strategy(strategy.getName())
                        .build();
            }
        }
        
        return RepairResult.builder()
                .success(false)
                .message("无法自动修复")
                .build();
    }
    
    /**
     * 分析错误
     */
    private ErrorAnalysis analyzeError(Exception error, ExecutionContext context) {
        String message = error.getMessage();
        
        ErrorAnalysis analysis = new ErrorAnalysis();
        analysis.setErrorMessage(message);
        analysis.setErrorType(classifyError(message));
        analysis.setFailedStep(context.getCurrentStep());
        analysis.setStackTrace(getStackTrace(error));
        
        return analysis;
    }
    
    /**
     * 分类错误
     */
    private String classifyError(String message) {
        if (message == null) return "UNKNOWN";
        
        String lower = message.toLowerCase();
        if (lower.contains("timeout") || lower.contains("connection")) {
            return "NETWORK";
        } else if (lower.contains("null") || lower.contains("npe")) {
            return "NULL_POINTER";
        } else if (lower.contains("permission") || lower.contains("access")) {
            return "PERMISSION";
        } else if (lower.contains("parse") || lower.contains("format")) {
            return "PARSE";
        }
        return "UNKNOWN";
    }
    
    /**
     * 获取修复策略
     */
    private List<MetaCapability.RepairStrategy> getRepairStrategies() {
        return List.of(
                MetaCapability.RepairStrategy.builder()
                        .name("retry_with_backoff")
                        .errorPatterns(List.of("timeout", "connection"))
                        .fixAction("retry")
                        .maxRetries(3)
                        .build(),
                MetaCapability.RepairStrategy.builder()
                        .name("switch_tool")
                        .errorPatterns(List.of("not found", "unavailable"))
                        .fixAction("switch")
                        .fallbackToAlternative(true)
                        .build(),
                MetaCapability.RepairStrategy.builder()
                        .name("fix_code")
                        .errorPatterns(List.of("null", "parse", "format"))
                        .fixAction("regenerate")
                        .maxRetries(2)
                        .build()
        );
    }
    
    /**
     * 匹配修复策略
     */
    private boolean matchesStrategy(ErrorAnalysis analysis, MetaCapability.RepairStrategy strategy) {
        for (String pattern : strategy.getErrorPatterns()) {
            if (analysis.getErrorMessage().toLowerCase().contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 应用修复
     */
    private Skill applyRepair(Skill skill, ExecutionContext context,
                              MetaCapability.RepairStrategy strategy,
                              ErrorAnalysis analysis) {
        // 创建修复后的 Skill 副本
        Skill repaired = Skill.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription() + " [已修复: " + strategy.getName() + "]")
                .trigger(skill.getTrigger())
                .steps(skill.getSteps())
                .tools(skill.getTools())
                .code(skill.getCode())
                .version(incrementVersion(skill.getVersion()))
                .author(skill.getAuthor())
                .source(skill.getSource())
                .status(Skill.SkillStatus.TESTING)
                .tags(skill.getTags())
                .createTime(skill.getCreateTime())
                .updateTime(java.time.LocalDateTime.now())
                .metadata(Map.of(
                        "originalSkill", skill.getId(),
                        "repairStrategy", strategy.getName(),
                        "errorAnalysis", analysis
                ))
                .build();
        
        // 根据策略修改步骤
        if ("switch".equals(strategy.getFixAction())) {
            // 切换工具
            repaired.setTools(switchToAlternativeTools(skill));
        } else if ("regenerate".equals(strategy.getFixAction())) {
            // 重新生成代码
            repaired.setCode(regenerateCode(skill, analysis));
        }
        
        return repaired;
    }
    
    /**
     * 切换到替代工具
     */
    private List<String> switchToAlternativeTools(Skill skill) {
        // 简化实现
        return skill.getTools();
    }
    
    /**
     * 重新生成代码
     */
    private String regenerateCode(Skill skill, ErrorAnalysis analysis) {
        String prompt = String.format("""
            修复以下代码中的错误：
            
            错误信息：%s
            错误类型：%s
            
            原代码：
            %s
            
            请生成修复后的代码。
            """, analysis.getErrorMessage(), analysis.getErrorType(), skill.getCode());
        
        return aiService.chat(prompt);
    }
    
    /**
     * 递增版本号
     */
    private String incrementVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            int patch = Integer.parseInt(parts[2]) + 1;
            return parts[0] + "." + parts[1] + "." + patch;
        }
        return version + ".1";
    }
    
    /**
     * 生成执行ID
     */
    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    // ========== 内部类 ==========
    
    /**
     * 执行上下文
     */
    @lombok.Data
    public static class ExecutionContext {
        private final String executionId;
        private final Skill skill;
        private final Map<String, Object> inputs;
        private final Map<String, Object> stepResults = new HashMap<>();
        private String currentStep;
        
        public ExecutionContext(String executionId, Skill skill, Map<String, Object> inputs) {
            this.executionId = executionId;
            this.skill = skill;
            this.inputs = inputs;
        }
    }
    
    /**
     * 执行结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ExecutionResult {
        private String executionId;
        private String skillId;
        private boolean success;
        private Object result;
        private String error;
        private long executionTime;
    }
    
    /**
     * 错误分析
     */
    @lombok.Data
    public static class ErrorAnalysis {
        private String errorMessage;
        private String errorType;
        private String failedStep;
        private String stackTrace;
    }
    
    /**
     * 修复结果
     */
    @lombok.Data
    @lombok.Builder
    public static class RepairResult {
        private boolean success;
        private Skill repairedSkill;
        private String strategy;
        private String message;
    }
}