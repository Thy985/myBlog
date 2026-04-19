package com.xingchen.backend.meta;

import com.xingchen.backend.ai.tool.*;
import com.xingchen.backend.ai.tool.Tool.ToolResult;
import com.xingchen.backend.mcp.McpClientService;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.SearchService;
import com.xingchen.backend.service.WebSearchService;
import com.xingchen.backend.tool.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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
    private final KnowledgeBaseService knowledgeBaseService;
    private final SearchService searchService;
    private final WebSearchService webSearchService;
    private final McpClientService mcpClientService;
    private final CodeExecutionService codeExecutionService;
    private final ArticleGeneratorTool articleGeneratorTool;
    private final ArticlePublishTool articlePublishTool;
    private final ArticleUpdateTool articleUpdateTool;
    private final ArticleDeleteTool articleDeleteTool;
    private final ArticleQueryTool articleQueryTool;
    private final CategoryTool categoryTool;
    private final TagTool tagTool;

    private final ExpressionParser expressionParser = new SpelExpressionParser();

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
     * 使用 SpEL 表达式引擎进行动态求值
     * 支持的表达式格式如: "${count} > 5", "${stepResults.score} >= 0.8"
     */
    private boolean evaluateCondition(String condition, Map<String, Object> stepResults) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        try {
            String expressionStr = condition;
            if (expressionStr.contains("${") && expressionStr.contains("}")) {
                expressionStr = expressionStr.replaceAll("\\$\\{", "#").replaceAll("\\}", "");
            }

            EvaluationContext context = new StandardEvaluationContext();

            context.setVariable("stepResults", stepResults);
            for (Map.Entry<String, Object> entry : stepResults.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

            Expression expression = expressionParser.parseExpression(expressionStr);
            Object result = expression.getValue(context);

            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof Number) {
                return ((Number) result).doubleValue() != 0;
            } else if (result instanceof String) {
                return !((String) result).isEmpty();
            }

            return result != null;
        } catch (Exception e) {
            log.warn("条件表达式求值失败: condition={}, error={}", condition, e.getMessage());
            return false;
        }
    }
    
    /**
     * 构建提示词
     */
    private String buildPromptFromContext(Skill.SkillStep step, ExecutionContext context) {
        return "执行步骤: " + step.getName();
    }
    
    /**
     * 调用工具
     * 根据工具类型分发到不同的处理逻辑
     */
    private Object invokeTool(ToolOption tool, Map<String, Object> params,
                              ExecutionContext context) {
        String toolType = tool.getToolType();
        Long userId = context.getInputs().get("userId") != null
                ? ((Number) context.getInputs().get("userId")).longValue()
                : null;

        log.info("调用工具: toolId={}, toolType={}", tool.getToolId(), toolType);

        try {
            return switch (toolType) {
                case "llm" -> invokeLlmTool(tool, params, userId);
                case "search" -> invokeSearchTool(tool, params);
                case "knowledge" -> invokeKnowledgeTool(tool, params);
                case "websearch" -> invokeWebSearchTool(tool, params);
                case "code" -> invokeCodeTool(tool, params);
                case "database" -> invokeDatabaseTool(tool, params);
                case "cache" -> invokeCacheTool(tool, params);
                case "file" -> invokeFileTool(tool, params);
                case "mcp" -> invokeMcpTool(tool, params);
                case "article" -> invokeArticleTool(tool, params);
                case "category" -> invokeCategoryTool(tool, params);
                case "tag" -> invokeTagTool(tool, params);
                default -> throw new UnsupportedOperationException("不支持的工具类型: " + toolType);
            };
        } catch (Exception e) {
            log.error("工具调用失败: toolId={}, error={}", tool.getToolId(), e.getMessage());
            throw new RuntimeException("工具调用失败: " + tool.getToolId() + ", error: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 LLM 工具
     */
    private Object invokeLlmTool(ToolOption tool, Map<String, Object> params, Long userId) {
        String prompt = (String) params.get("prompt");
        if (prompt == null) {
            prompt = "请回复这条消息";
        }

        String response;
        if (userId != null) {
            response = aiService.chatWithUserApiKey(userId, prompt);
        } else {
            response = aiService.chat(prompt);
        }

        return Map.of(
                "success", true,
                "toolId", tool.getToolId(),
                "response", response,
                "type", "llm"
        );
    }

    /**
     * 调用搜索工具
     */
    private Object invokeSearchTool(ToolOption tool, Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null) {
            query = "";
        }
        int limit = params.get("limit") != null ? ((Number) params.get("limit")).intValue() : 10;

        try {
            var searchResult = searchService.globalSearch(query, null, 1, limit);
            return Map.of(
                    "success", true,
                    "toolId", tool.getToolId(),
                    "query", query,
                    "results", searchResult.getList(),
                    "total", searchResult.getTotal(),
                    "type", "search"
            );
        } catch (Exception e) {
            log.warn("搜索工具执行失败: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", e.getMessage(),
                    "type", "search"
            );
        }
    }

    /**
     * 调用知识库工具
     */
    private Object invokeKnowledgeTool(ToolOption tool, Map<String, Object> params) {
        String operation = (String) params.get("operation");
        if (operation == null) {
            operation = "search";
        }

        try {
            return switch (operation) {
                case "search" -> {
                    String query = (String) params.get("query");
                    int topK = params.get("topK") != null ? ((Number) params.get("topK")).intValue() : 5;
                    String result = knowledgeBaseService.search(query, topK);
                    yield Map.of(
                            "success", true,
                            "toolId", tool.getToolId(),
                            "query", query,
                            "result", result,
                            "type", "knowledge"
                    );
                }
                case "add" -> {
                    Long articleId = params.get("articleId") != null
                            ? ((Number) params.get("articleId")).longValue()
                            : null;
                    String title = (String) params.get("title");
                    String content = (String) params.get("content");
                    if (articleId != null && title != null && content != null) {
                        knowledgeBaseService.addDocument(articleId, title, content);
                    }
                    yield Map.of(
                            "success", true,
                            "toolId", tool.getToolId(),
                            "operation", "add",
                            "articleId", articleId,
                            "type", "knowledge"
                    );
                }
                default -> throw new IllegalArgumentException("未知操作: " + operation);
            };
        } catch (Exception e) {
            log.warn("知识库工具执行失败: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", e.getMessage(),
                    "type", "knowledge"
            );
        }
    }

    /**
     * 调用网络搜索工具
     */
    private Object invokeWebSearchTool(ToolOption tool, Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null) {
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", "query 参数不能为空",
                    "type", "websearch"
            );
        }

        int maxResults = params.get("maxResults") != null ? ((Number) params.get("maxResults")).intValue() : 5;

        try {
            var result = webSearchService.search(query, maxResults);
            return Map.of(
                    "success", true,
                    "toolId", tool.getToolId(),
                    "query", query,
                    "result", result,
                    "type", "websearch"
            );
        } catch (Exception e) {
            log.warn("网络搜索工具执行失败: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", e.getMessage(),
                    "type", "websearch"
            );
        }
    }

    /**
     * 调用代码执行工具
     */
    private Object invokeCodeTool(ToolOption tool, Map<String, Object> params) {
        String code = (String) params.get("code");
        String language = (String) params.get("language");

        log.info("代码执行工具收到请求: language={}, codeLength={}", language, code != null ? code.length() : 0);

        if (code == null || code.trim().isEmpty()) {
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", "代码不能为空",
                    "type", "code"
            );
        }

        CodeExecutionService.CodeExecutionResult result = codeExecutionService.execute(code, language);

        if (result.isSuccess()) {
            return Map.of(
                    "success", true,
                    "toolId", tool.getToolId(),
                    "language", result.getLanguage(),
                    "output", result.getOutput(),
                    "result", result.getResult(),
                    "executionTime", result.getExecutionTime(),
                    "type", "code"
            );
        } else {
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", result.getError(),
                    "language", result.getLanguage(),
                    "supportedLanguages", result.getSupportedLanguages(),
                    "executionTime", result.getExecutionTime(),
                    "type", "code"
            );
        }
    }

    /**
     * 调用数据库工具
     */
    private Object invokeDatabaseTool(ToolOption tool, Map<String, Object> params) {
        String operation = (String) params.get("operation");
        String sql = (String) params.get("sql");

        log.info("数据库工具收到请求: operation={}", operation);

        return Map.of(
                "success", true,
                "toolId", tool.getToolId(),
                "message", "数据库操作功能开发中",
                "operation", operation,
                "type", "database"
        );
    }

    /**
     * 调用缓存工具
     */
    private Object invokeCacheTool(ToolOption tool, Map<String, Object> params) {
        String operation = (String) params.get("operation");
        String key = (String) params.get("key");
        Object value = params.get("value");

        log.info("缓存工具收到请求: operation={}, key={}", operation, key);

        return Map.of(
                "success", true,
                "toolId", tool.getToolId(),
                "message", "缓存操作已记录（实际调用需要缓存服务支持）",
                "operation", operation,
                "key", key,
                "type", "cache"
        );
    }

    /**
     * 调用文件操作工具
     */
    private Object invokeFileTool(ToolOption tool, Map<String, Object> params) {
        String operation = (String) params.get("operation");
        String path = (String) params.get("path");
        String content = (String) params.get("content");

        log.info("文件工具收到请求: operation={}, path={}", operation, path);

        return Map.of(
                "success", true,
                "toolId", tool.getToolId(),
                "message", "文件操作功能开发中，请使用 LLM 工具进行文件内容处理",
                "operation", operation,
                "path", path,
                "type", "file"
        );
    }

    /**
     * 调用 MCP 服务工具
     */
    private Object invokeMcpTool(ToolOption tool, Map<String, Object> params) {
        String mcpMethod = (String) params.get("method");
        if (mcpMethod == null) {
            mcpMethod = "call";
        }

        log.info("MCP工具收到请求: toolId={}, method={}", tool.getToolId(), mcpMethod);

        if (!toolRegistry.isMcpConnected(tool.getToolId())) {
            ToolRegistry.McpConnectionStatus status = toolRegistry.getMcpConnectionStatus(tool.getToolId());
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", "MCP 服务未连接: " + status.message(),
                    "method", mcpMethod,
                    "type", "mcp"
            );
        }

        try {
            return switch (tool.getToolId()) {
                case "mcp-whisper" -> invokeWhisperService(params);
                case "mcp-vision" -> invokeVisionService(params);
                default -> Map.of(
                        "success", false,
                        "toolId", tool.getToolId(),
                        "error", "未知的 MCP 服务: " + tool.getToolId(),
                        "type", "mcp"
                );
            };
        } catch (Exception e) {
            log.error("MCP 服务调用失败: toolId={}, error={}", tool.getToolId(), e.getMessage());
            return Map.of(
                    "success", false,
                    "toolId", tool.getToolId(),
                    "error", "MCP 服务调用失败: " + e.getMessage(),
                    "type", "mcp"
            );
        }
    }

    /**
     * 调用 Whisper 语音识别服务
     */
    private Object invokeWhisperService(Map<String, Object> params) {
        String audioUrl = (String) params.get("audioUrl");
        String audioData = (String) params.get("audioData");
        String language = (String) params.get("language");

        log.info("调用 Whisper 服务: audioUrl={}", audioUrl);

        McpClientService.WhisperResult result;
        
        if (audioData != null && !audioData.isEmpty()) {
            // 直接传入 Base64 音频数据
            result = mcpClientService.transcribeAudio(audioData, language);
        } else if (audioUrl != null && !audioUrl.isEmpty()) {
            // 从 URL 下载音频
            result = mcpClientService.transcribeFromUrl(audioUrl, language);
        } else {
            return Map.of(
                    "success", false,
                    "toolId", "mcp-whisper",
                    "error", "缺少音频数据（audioData 或 audioUrl）",
                    "type", "mcp"
            );
        }

        if (result.isSuccess()) {
            return Map.of(
                    "success", true,
                    "toolId", "mcp-whisper",
                    "text", result.getText(),
                    "language", result.getLanguage(),
                    "confidence", result.getConfidence(),
                    "executionTime", result.getExecutionTime(),
                    "type", "mcp"
            );
        } else {
            return Map.of(
                    "success", false,
                    "toolId", "mcp-whisper",
                    "error", result.getError(),
                    "executionTime", result.getExecutionTime(),
                    "type", "mcp"
            );
        }
    }

    /**
     * 调用 Vision 图像识别服务
     */
    private Object invokeVisionService(Map<String, Object> params) {
        String imageUrl = (String) params.get("imageUrl");
        String imageData = (String) params.get("imageData");
        String operation = (String) params.get("operation");

        log.info("调用 Vision 服务: imageUrl={}, operation={}", imageUrl, operation);

        McpClientService.VisionResult result;
        
        if (imageData != null && !imageData.isEmpty()) {
            // 直接传入 Base64 图像数据
            result = mcpClientService.analyzeImage(imageData, operation);
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            // 从 URL 下载图像
            result = mcpClientService.analyzeImageFromUrl(imageUrl, operation);
        } else {
            return Map.of(
                    "success", false,
                    "toolId", "mcp-vision",
                    "error", "缺少图像数据（imageData 或 imageUrl）",
                    "type", "mcp"
            );
        }

        if (result.isSuccess()) {
            return Map.of(
                    "success", true,
                    "toolId", "mcp-vision",
                    "result", result.getResult(),
                    "operation", result.getOperation(),
                    "executionTime", result.getExecutionTime(),
                    "type", "mcp"
            );
        } else {
            return Map.of(
                    "success", false,
                    "toolId", "mcp-vision",
                    "error", result.getError(),
                    "executionTime", result.getExecutionTime(),
                    "type", "mcp"
            );
        }
    }

    /**
     * 调用文章相关工具
     */
    private Object invokeArticleTool(ToolOption tool, Map<String, Object> params) {
        String toolId = tool.getToolId();
        log.info("调用文章工具: toolId={}", toolId);

        try {
            ToolResult result = switch (toolId) {
                case "article_generator" -> articleGeneratorTool.execute(params);
                case "article_publish" -> articlePublishTool.execute(params);
                case "article_update" -> articleUpdateTool.execute(params);
                case "article_delete" -> articleDeleteTool.execute(params);
                case "article_query" -> articleQueryTool.execute(params);
                default -> throw new UnsupportedOperationException("未知的文章工具: " + toolId);
            };

            if (result.success()) {
                return Map.of(
                        "success", true,
                        "toolId", toolId,
                        "data", result.data(),
                        "message", result.message(),
                        "type", "article"
                );
            } else {
                return Map.of(
                        "success", false,
                        "toolId", toolId,
                        "error", result.message(),
                        "type", "article"
                );
            }
        } catch (Exception e) {
            log.error("文章工具调用失败: toolId={}, error={}", toolId, e.getMessage());
            return Map.of(
                    "success", false,
                    "toolId", toolId,
                    "error", "文章工具执行失败: " + e.getMessage(),
                    "type", "article"
            );
        }
    }

    /**
     * 调用分类管理工具
     */
    private Object invokeCategoryTool(ToolOption tool, Map<String, Object> params) {
        String toolId = tool.getToolId();
        log.info("调用分类工具: toolId={}", toolId);

        try {
            ToolResult result = categoryTool.execute(params);

            if (result.success()) {
                return Map.of(
                        "success", true,
                        "toolId", toolId,
                        "data", result.data(),
                        "message", result.message(),
                        "type", "category"
                );
            } else {
                return Map.of(
                        "success", false,
                        "toolId", toolId,
                        "error", result.message(),
                        "type", "category"
                );
            }
        } catch (Exception e) {
            log.error("分类工具调用失败: toolId={}, error={}", toolId, e.getMessage());
            return Map.of(
                    "success", false,
                    "toolId", toolId,
                    "error", "分类工具执行失败: " + e.getMessage(),
                    "type", "category"
            );
        }
    }

    /**
     * 调用标签管理工具
     */
    private Object invokeTagTool(ToolOption tool, Map<String, Object> params) {
        String toolId = tool.getToolId();
        log.info("调用标签工具: toolId={}", toolId);

        try {
            ToolResult result = tagTool.execute(params);

            if (result.success()) {
                return Map.of(
                        "success", true,
                        "toolId", toolId,
                        "data", result.data(),
                        "message", result.message(),
                        "type", "tag"
                );
            } else {
                return Map.of(
                        "success", false,
                        "toolId", toolId,
                        "error", result.message(),
                        "type", "tag"
                );
            }
        } catch (Exception e) {
            log.error("标签工具调用失败: toolId={}, error={}", toolId, e.getMessage());
            return Map.of(
                    "success", false,
                    "toolId", toolId,
                    "error", "标签工具执行失败: " + e.getMessage(),
                    "type", "tag"
            );
        }
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