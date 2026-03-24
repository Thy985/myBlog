package com.xingchen.backend.meta;

import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skill 生成器
 * 
 * 根据用户需求自动生成 Skill
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SkillGenerator {

    private final AIService aiService;
    private final ToolRegistry toolRegistry;
    
    // Skill ID 生成计数器
    private int skillCounter = 0;
    
    /**
     * 分析用户需求，生成 Skill 草稿
     */
    public Skill generateSkill(String userRequest) {
        log.info("开始生成 Skill: {}", userRequest);
        
        try {
            // 1. 需求分析
            RequirementAnalysis analysis = analyzeRequirement(userRequest);
            
            // 2. 工具选择
            List<ToolOption> selectedTools = selectTools(analysis);
            
            // 3. 生成步骤
            List<Skill.SkillStep> steps = generateSteps(analysis, selectedTools);
            
            // 4. 生成代码（如果需要）
            String code = generateCode(analysis, steps);
            
            // 5. 构建 Skill
            Skill skill = Skill.builder()
                    .id(generateSkillId())
                    .name(analysis.getSkillName())
                    .description(analysis.getDescription())
                    .trigger(buildTrigger(analysis))
                    .steps(steps)
                    .tools(selectedTools.stream().map(ToolOption::getToolId).toList())
                    .code(code)
                    .codeLanguage(detectLanguage(code))
                    .version("1.0.0")
                    .author("AI_Generated")
                    .source(Skill.SkillSource.AUTO_GENERATED)
                    .status(Skill.SkillStatus.DRAFT)
                    .tags(analysis.getTags())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .metadata(Map.of(
                            "userRequest", userRequest,
                            "analysis", analysis
                    ))
                    .build();
            
            log.info("Skill 生成成功: {}", skill.getId());
            return skill;
            
        } catch (Exception e) {
            log.error("Skill 生成失败", e);
            throw new RuntimeException("Skill 生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 需求分析
     */
    private RequirementAnalysis analyzeRequirement(String userRequest) {
        String prompt = String.format("""
            分析以下用户需求，提取关键信息：
            
            需求：%s
            
            请分析并输出以下信息（JSON格式）：
            {
                "skillName": "Skill名称",
                "description": "Skill描述",
                "taskType": "任务类型",
                "inputs": [{"name": "参数名", "type": "参数类型", "description": "描述"}],
                "outputs": [{"name": "输出名", "type": "输出类型", "description": "描述"}],
                "requiredCapabilities": ["需要的能力"],
                "keywords": ["关键词"],
                "tags": ["标签"]
            }
            """, userRequest);
        
        String response = aiService.chat(prompt);
        
        // 解析 JSON 响应
        return parseAnalysis(response);
    }
    
    /**
     * 工具选择
     */
    private List<ToolOption> selectTools(RequirementAnalysis analysis) {
        List<ToolOption> availableTools = toolRegistry.getAvailableTools();
        List<ToolOption> selected = new ArrayList<>();
        
        // 基于需求匹配合适的工具
        for (String capability : analysis.getRequiredCapabilities()) {
            ToolOption bestTool = availableTools.stream()
                    .filter(t -> t.getCapabilities().containsKey(capability))
                    .max(Comparator.comparingDouble(t -> 
                            calculateToolScore(t, capability)))
                    .orElse(null);
            
            if (bestTool != null && !selected.contains(bestTool)) {
                selected.add(bestTool);
            }
        }
        
        // 如果没有匹配到，使用通用 LLM 工具
        if (selected.isEmpty()) {
            selected.add(toolRegistry.getDefaultTool());
        }
        
        return selected;
    }
    
    /**
     * 生成执行步骤
     */
    private List<Skill.SkillStep> generateSteps(RequirementAnalysis analysis, 
                                                 List<ToolOption> tools) {
        List<Skill.SkillStep> steps = new ArrayList<>();
        int order = 1;
        
        // 输入验证步骤
        steps.add(Skill.SkillStep.builder()
                .order(order++)
                .name("validate_input")
                .description("验证输入参数")
                .type(Skill.SkillStep.StepType.CODE_EXEC)
                .parameters(Map.of("validation", analysis.getInputs()))
                .build());
        
        // 为每个工具生成调用步骤
        for (ToolOption tool : tools) {
            steps.add(Skill.SkillStep.builder()
                    .order(order++)
                    .name("call_" + tool.getToolId())
                    .description("调用 " + tool.getDescription())
                    .type(detectStepType(tool))
                    .toolId(tool.getToolId())
                    .parameters(Map.of())
                    .retryCount(3)
                    .build());
        }
        
        // 结果处理步骤
        steps.add(Skill.SkillStep.builder()
                .order(order++)
                .name("process_result")
                .description("处理并格式化结果")
                .type(Skill.SkillStep.StepType.CODE_EXEC)
                .parameters(Map.of("outputFormat", analysis.getOutputs()))
                .build());
        
        // 输出步骤
        steps.add(Skill.SkillStep.builder()
                .order(order++)
                .name("output_result")
                .description("输出最终结果")
                .type(Skill.SkillStep.StepType.OUTPUT)
                .build());
        
        return steps;
    }
    
    /**
     * 生成代码实现
     */
    private String generateCode(RequirementAnalysis analysis, List<Skill.SkillStep> steps) {
        String prompt = String.format("""
            根据以下 Skill 定义，生成 Java 代码实现：
            
            Skill名称：%s
            描述：%s
            输入：%s
            输出：%s
            步骤：%s
            
            要求：
            1. 生成完整的 Java 类代码
            2. 包含输入验证逻辑
            3. 包含错误处理
            4. 使用 try-catch-finally
            5. 返回格式化的结果
            
            只输出代码，不要解释。
            """, 
            analysis.getSkillName(),
            analysis.getDescription(),
            analysis.getInputs(),
            analysis.getOutputs(),
            steps
        );
        
        return aiService.chat(prompt);
    }
    
    /**
     * 构建触发条件
     */
    private Skill.TriggerCondition buildTrigger(RequirementAnalysis analysis) {
        return Skill.TriggerCondition.builder()
                .type(Skill.TriggerCondition.TriggerType.SEMANTIC)
                .keywords(analysis.getKeywords())
                .intent(analysis.getTaskType())
                .confidenceThreshold(0.7)
                .build();
    }
    
    /**
     * 计算工具匹配分数
     */
    private double calculateToolScore(ToolOption tool, String capability) {
        double score = 0;
        
        // 能力匹配
        if (tool.getCapabilities().containsKey(capability)) {
            score += 0.4;
        }
        
        // 准确率
        score += tool.getAccuracy() * 0.3;
        
        // 延迟（越低越好）
        score += (1 - tool.getLatency() / 1000) * 0.2;
        
        // 成本（越低越好）
        score += (1 - tool.getCost() / 100) * 0.1;
        
        return score;
    }
    
    /**
     * 检测步骤类型
     */
    private Skill.SkillStep.StepType detectStepType(ToolOption tool) {
        String toolType = tool.getToolType();
        
        return switch (toolType.toLowerCase()) {
            case "llm", "ai" -> Skill.SkillStep.StepType.LLM_CALL;
            case "code", "script" -> Skill.SkillStep.StepType.CODE_EXEC;
            case "api", "service" -> Skill.SkillStep.StepType.TOOL_CALL;
            default -> Skill.SkillStep.StepType.TOOL_CALL;
        };
    }
    
    /**
     * 检测代码语言
     */
    private String detectLanguage(String code) {
        if (code.contains("public class") || code.contains("private class")) {
            return "java";
        } else if (code.contains("def ") && code.contains(":")) {
            return "python";
        } else if (code.contains("function") && code.contains("{")) {
            return "javascript";
        }
        return "unknown";
    }
    
    /**
     * 生成 Skill ID
     */
    private synchronized String generateSkillId() {
        return "skill_" + System.currentTimeMillis() + "_" + (++skillCounter);
    }
    
    /**
     * 解析需求分析结果
     */
    private RequirementAnalysis parseAnalysis(String response) {
        // 简化实现，实际应该解析 JSON
        return RequirementAnalysis.builder()
                .skillName("AutoGeneratedSkill")
                .description("自动生成的 Skill")
                .taskType("general")
                .inputs(new ArrayList<>())
                .outputs(new ArrayList<>())
                .requiredCapabilities(List.of("llm"))
                .keywords(new ArrayList<>())
                .tags(List.of("auto-generated"))
                .build();
    }
    
    /**
     * 需求分析结果
     */
    @lombok.Data
    @lombok.Builder
    public static class RequirementAnalysis {
        private String skillName;
        private String description;
        private String taskType;
        private List<Map<String, String>> inputs;
        private List<Map<String, String>> outputs;
        private List<String> requiredCapabilities;
        private List<String> keywords;
        private List<String> tags;
    }
}