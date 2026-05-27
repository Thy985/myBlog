package com.xingchen.backend.agent;

import com.xingchen.backend.ai.intent.IntentClassifierInterface;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.llm.UserLLMProviderManager;
import com.xingchen.backend.ai.memory.MemoryManager;
import com.xingchen.backend.ai.memory.UnifiedMemoryContext;
import com.xingchen.backend.ai.memory.UnifiedMemoryService;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.model.Intent;
import com.xingchen.backend.ai.security.SecurityFilterChain;
import com.xingchen.backend.ai.tool.AIToolRegistry;
import com.xingchen.backend.ai.tool.Tool;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.meta.lightweight.LightweightMetaOrchestrator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final SecurityFilterChain securityFilterChain;
    private final IntentClassifierInterface intentClassifier;
    private final UnifiedMemoryService unifiedMemoryService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final AIToolRegistry toolRegistry;
    private final UserLLMProviderManager userProviderManager;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;
    private final LightweightMetaOrchestrator metaOrchestrator;

    public AIResponse handle(AIRequest request) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            request = securityFilterChain.filter(request);

            Intent intent = intentClassifier.classify(request.getMessage());
            log.debug("意图分类: type={}, confidence={}", intent.getType(), intent.getConfidence());

            // 使用微型元能力增强 systemPrompt
            if (request.getUserId() != null) {
                String enhancedPrompt = metaOrchestrator.buildEnhancedPrompt(request.getUserId(), request.getMessage());
                if (!enhancedPrompt.isEmpty()) {
                    request.setSystemPrompt(enhancedPrompt);
                }
            }

            ExecutionPlan plan = buildExecutionPlan(request, intent);
            AIResponse response = executePlan(plan);

            if (response.isSuccess()) {
                saveMemoryWithAnalysis(request, response);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            sample.stop(meterRegistry.timer("ai.orchestrator.handle",
                    "intent", intent.getType().name(),
                    "success", String.valueOf(response.isSuccess())));

            log.info("请求处理完成: intent={}, elapsed={}ms", intent.getType(), elapsed);

            return response;

        } catch (SecurityFilterChain.SecurityException e) {
            sample.stop(meterRegistry.timer("ai.orchestrator.handle",
                    "intent", "SECURITY_BLOCKED",
                    "success", "false"));
            log.warn("安全拦截: {}", e.getMessage());
            return AIResponse.error("请求被拦截: " + e.getMessage());

        } catch (Exception e) {
            sample.stop(meterRegistry.timer("ai.orchestrator.handle",
                    "intent", "ERROR",
                    "success", "false"));
            log.error("请求处理失败", e);
            return AIResponse.error("处理失败: " + e.getMessage());
        }
    }

    public void handleStream(AIRequest request, Consumer<AIResponse> onChunk) {
        StringBuilder fullResponse = new StringBuilder();

        try {
            request = securityFilterChain.filter(request);

            Long userId = request.getUserId();
            if (userId == null) {
                onChunk.accept(AIResponse.error("用户未登录"));
                return;
            }

            Intent intent = intentClassifier.classify(request.getMessage());

            // 3. 构建增强 System Prompt（使用微型元能力层）
            String enhancedSystemPrompt = "";
            if (userId != null) {
                enhancedSystemPrompt = metaOrchestrator.buildEnhancedPrompt(userId, request.getMessage());
                log.debug("使用微型元能力构建增强 Prompt, userId={}", userId);
            }

            // 4. 如果有增强 Prompt，设置到请求中
            AIRequest enhancedRequest = request;
            if (!enhancedSystemPrompt.isEmpty()) {
                enhancedRequest = AIRequest.builder()
                        .userId(request.getUserId())
                        .sessionId(request.getSessionId())
                        .message(request.getMessage())
                        .systemPrompt(enhancedSystemPrompt)
                        .stream(request.isStream())
                        .history(request.getHistory())
                        .build();
            }

            LLMProvider userProvider = validateAndGetUserProvider(userId, onChunk);
            if (userProvider != null && userProvider.supportsStreaming()) {
                userProvider.streamChat(enhancedRequest, response -> {
                    if (response.isSuccess() && response.getContent() != null) {
                        fullResponse.append(response.getContent());
                    }
                    onChunk.accept(response);
                });

                if (fullResponse.length() > 0) {
                    UnifiedMemoryService.MemoryRequirements requirements = new UnifiedMemoryService.MemoryRequirements(
                        true, true, false, null, 10);
                    UnifiedMemoryContext context = unifiedMemoryService.loadContext(userId, request.getSessionId(), requirements);

                    UnifiedMemoryContext.MemoryItem userItem = UnifiedMemoryContext.MemoryItem.builder()
                        .id(UUID.randomUUID().toString())
                        .role("user")
                        .content(request.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .importance(0.6)
                        .type(UnifiedMemoryContext.MemoryType.CONVERSATION)
                        .category("CONVERSATION")
                        .build();

                    UnifiedMemoryContext.MemoryItem assistantItem = UnifiedMemoryContext.MemoryItem.builder()
                        .id(UUID.randomUUID().toString())
                        .role("assistant")
                        .content(fullResponse.toString())
                        .timestamp(System.currentTimeMillis())
                        .importance(0.5)
                        .type(UnifiedMemoryContext.MemoryType.CONVERSATION)
                        .category("CONVERSATION")
                        .build();

                    unifiedMemoryService.addWorkingMemory(userId, request.getSessionId(), userItem);
                    unifiedMemoryService.addWorkingMemory(userId, request.getSessionId(), assistantItem);

                    // 5. 记录反馈用于用户偏好学习（异步）
                    final Long feedbackUserId = userId;
                    final String userMessage = request.getMessage();
                    new Thread(() -> {
                        try {
                            metaOrchestrator.recordFeedback(feedbackUserId, userMessage, true);
                        } catch (Exception e) {
                            log.warn("记录反馈失败", e);
                        }
                    }).start();
                }
            }

        } catch (SecurityFilterChain.SecurityException e) {
            log.warn("安全拦截流式请求: {}", e.getMessage());
            onChunk.accept(AIResponse.error("请求被拦截: " + e.getMessage()));
        } catch (Exception e) {
            log.error("流式处理失败", e);
            onChunk.accept(AIResponse.error("流式处理失败: " + e.getMessage()));
        }
    }

    private LLMProvider validateAndGetUserProvider(Long userId) {
        LLMProvider userProvider = userProviderManager.getUserProvider(userId);
        if (userProvider == null) {
            log.warn("用户 {} 没有配置API Key", userId);
            throw new IllegalStateException("请先配置您的AI API Key,在设置->AI配置中添加");
        }
        log.debug("使用用户 {} 的Provider: {}", userId, userProvider.getProviderName());
        return userProvider;
    }

    private LLMProvider validateAndGetUserProvider(Long userId, Consumer<AIResponse> onChunk) {
        LLMProvider userProvider = userProviderManager.getUserProvider(userId);
        if (userProvider == null) {
            log.warn("用户 {} 没有配置API Key", userId);
            onChunk.accept(AIResponse.error("请先配置您的AI API Key,在设置->AI配置中添加"));
            return null;
        }
        log.debug("使用用户 {} 的Provider: {}", userId, userProvider.getProviderName());
        return userProvider;
    }

    private ExecutionPlan buildExecutionPlan(AIRequest request, Intent intent) {
        ExecutionPlan.ExecutionPlanBuilder planBuilder = ExecutionPlan.builder()
                .planId(UUID.randomUUID().toString())
                .originalRequest(request)
                .intent(intent)
                .requiresToolCall(intent.isRequiresTool())
                .requiresMemory(intent.isRequiresMemory())
                .requiresRag(request.needsRag());

        List<ExecutionPlan.ExecutionStep> steps = new ArrayList<>();
        int order = 1;

        steps.add(ExecutionPlan.ExecutionStep.builder()
                .order(order++)
                .type(ExecutionPlan.ExecutionStep.StepType.INTENT_CLASSIFICATION)
                .description("意图分类: " + intent.getType())
                .status(ExecutionPlan.ExecutionStep.StepStatus.COMPLETED)
                .build());

        if (intent.isRequiresMemory()) {
            steps.add(ExecutionPlan.ExecutionStep.builder()
                    .order(order++)
                    .type(ExecutionPlan.ExecutionStep.StepType.MEMORY_RETRIEVAL)
                    .description("检索相关记忆")
                    .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                    .build());
        }

        if (request.needsRag()) {
            steps.add(ExecutionPlan.ExecutionStep.builder()
                    .order(order++)
                    .type(ExecutionPlan.ExecutionStep.StepType.RAG_RETRIEVAL)
                    .description("检索知识库")
                    .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                    .build());
        }

        if (intent.isRequiresTool() && intent.getPossibleTools() != null) {
            for (String toolName : intent.getPossibleTools()) {
                steps.add(ExecutionPlan.ExecutionStep.builder()
                        .order(order++)
                        .type(ExecutionPlan.ExecutionStep.StepType.TOOL_CALL)
                        .description("调用工具: " + toolName)
                        .parameters(Map.of("toolName", toolName))
                        .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                        .build());
            }
        }

        steps.add(ExecutionPlan.ExecutionStep.builder()
                .order(order++)
                .type(ExecutionPlan.ExecutionStep.StepType.LLM_CALL)
                .description("生成回复")
                .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                .build());

        steps.add(ExecutionPlan.ExecutionStep.builder()
                .order(order++)
                .type(ExecutionPlan.ExecutionStep.StepType.RESPONSE_FORMAT)
                .description("格式化响应")
                .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                .build());

        return planBuilder.steps(steps).build();
    }

    private AIResponse executePlan(ExecutionPlan plan) {
        AIRequest request = plan.getOriginalRequest();
        Intent intent = plan.getIntent();
        UnifiedMemoryContext memoryContext = null;
        List<Tool.ToolResult> toolResults = new ArrayList<>();
        String ragContext = null;

        for (ExecutionPlan.ExecutionStep step : plan.getSteps()) {
            if (step.getStatus() == ExecutionPlan.ExecutionStep.StepStatus.COMPLETED) {
                continue;
            }

            long stepStart = System.currentTimeMillis();
            step.setStatus(ExecutionPlan.ExecutionStep.StepStatus.RUNNING);

            try {
                switch (step.getType()) {
                    case MEMORY_RETRIEVAL:
                        UnifiedMemoryService.MemoryRequirements requirements = new UnifiedMemoryService.MemoryRequirements(
                            intent.isRequiresMemory(), true, intent.isRequiresMemory(),
                            intent.isRequiresMemory() ? request.getMessage() : null, 10);
                        memoryContext = unifiedMemoryService.loadContext(
                                request.getUserId(),
                                request.getSessionId(),
                                requirements);
                        break;

                    case TOOL_CALL:
                        String toolName = (String) step.getParameters().get("toolName");
                        Tool.ToolResult result = executeTool(toolName, request, intent);
                        toolResults.add(result);
                        break;

                    case RAG_RETRIEVAL:
                        ragContext = knowledgeBaseService.search(request.getMessage(), 5);
                        break;

                    case LLM_CALL:
                        return executeLLMCall(request, intent, memoryContext, toolResults, ragContext);

                    default:
                        break;
                }

                step.setStatus(ExecutionPlan.ExecutionStep.StepStatus.COMPLETED);
                step.setExecutionTime(System.currentTimeMillis() - stepStart);

            } catch (Exception e) {
                step.setStatus(ExecutionPlan.ExecutionStep.StepStatus.FAILED);
                step.setExecutionTime(System.currentTimeMillis() - stepStart);
                log.error("步骤执行失败: {}", step.getDescription(), e);
                throw e;
            }
        }

        return AIResponse.error("执行计划异常");
    }

    private Tool.ToolResult executeTool(String toolName, AIRequest request, Intent intent) {
        Map<String, Object> params = new HashMap<>();
        params.put("message", request.getMessage());
        params.put("intent", intent.getType().name());

        if (intent.getEntities() != null) {
            for (Intent.Entity entity : intent.getEntities()) {
                params.put(entity.getName(), entity.getValue());
            }
        }

        return toolRegistry.execute(toolName, params);
    }

    private AIResponse executeLLMCall(AIRequest request, Intent intent,
                                       UnifiedMemoryContext memoryContext,
                                       List<Tool.ToolResult> toolResults,
                                       String ragContext) {
        Long userId = request.getUserId();
        if (userId == null) {
            return AIResponse.error("用户未登录");
        }

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个小星Agent,帮助用户完成各种任务。\n\n");

        if (ragContext != null && !ragContext.isBlank()) {
            systemPrompt.append("【知识库检索结果】\n").append(ragContext).append("\n");
        }

        if (memoryContext != null && !memoryContext.isEmpty()) {
            systemPrompt.append(memoryContext.toPromptText());
        }

        if (!toolResults.isEmpty()) {
            systemPrompt.append("【工具执行结果】\n");
            for (Tool.ToolResult result : toolResults) {
                systemPrompt.append("- 工具: ").append(result.success() ? "成功" : "失败").append("\n");
                systemPrompt.append("  消息: ").append(result.message()).append("\n");
                if (result.data() != null) {
                    systemPrompt.append("  数据: ").append(result.data().toString()).append("\n");
                }
            }
            systemPrompt.append("\n");
            systemPrompt.append("请基于上述工具执行结果和知识库检索结果回答用户的问题。\n");
        }

        AIRequest enhancedRequest = AIRequest.builder()
                .userId(userId)
                .sessionId(request.getSessionId())
                .message(request.getMessage())
                .preferredModel(request.getPreferredModel())
                .systemPrompt(systemPrompt.toString())
                .history(request.getHistory())
                .stream(request.isStream())
                .extraParams(request.getExtraParams())
                .build();

        try {
            LLMProvider userProvider = validateAndGetUserProvider(userId);
            return userProvider.chat(enhancedRequest);
        } catch (IllegalStateException e) {
            return AIResponse.error(e.getMessage());
        }
    }

    private void saveMemoryWithAnalysis(AIRequest request, AIResponse response) {
        if (request.getUserId() == null) {
            return;
        }

        if (request.getMessage() == null || request.getMessage().trim().length() < 3) {
            return;
        }

        UnifiedMemoryContext.MemoryItem userItem = UnifiedMemoryContext.MemoryItem.builder()
                .id(UUID.randomUUID().toString())
                .role("user")
                .content(request.getMessage())
                .timestamp(System.currentTimeMillis())
                .importance(assessMessageImportance(request.getMessage()))
                .type(classifyMessageType(request.getMessage()))
                .category(classifyMessageType(request.getMessage()).name())
                .build();

        unifiedMemoryService.addWorkingMemory(request.getUserId(), request.getSessionId(), userItem);

        if (response.isSuccess() && response.getContent() != null && response.getContent().trim().length() > 5) {
            UnifiedMemoryContext.MemoryItem assistantItem = UnifiedMemoryContext.MemoryItem.builder()
                    .id(UUID.randomUUID().toString())
                    .role("assistant")
                    .content(response.getContent())
                    .timestamp(System.currentTimeMillis())
                    .importance(Math.min(assessMessageImportance(request.getMessage()), 0.8))
                    .type(UnifiedMemoryContext.MemoryType.CONVERSATION)
                    .category("CONVERSATION")
                    .build();

            unifiedMemoryService.addWorkingMemory(request.getUserId(), request.getSessionId(), assistantItem);
        }

        UnifiedMemoryService.MemoryAnalysis analysis = unifiedMemoryService.analyzeAndSave(
                request.getUserId(), request.getMessage(), response.getContent());

        if (analysis.saved()) {
            log.info("记忆分析完成: {}", analysis.summary());
        }
    }

    private double assessMessageImportance(String message) {
        if (message == null || message.isBlank()) return 0.3;

        double importance = 0.5;
        int length = message.length();

        if (length > 100) importance += 0.1;
        if (length > 300) importance += 0.1;

        Set<String> importantTriggers = Set.of(
            "喜欢", "偏好", "习惯", "不要", "别用", "决定", "选择",
            "采用", "使用", "不用", "放弃", "我叫", "我的名字", "我在"
        );

        for (String trigger : importantTriggers) {
            if (message.contains(trigger)) {
                importance += 0.1;
                break;
            }
        }

        return Math.min(importance, 1.0);
    }

    private UnifiedMemoryContext.MemoryType classifyMessageType(String message) {
        if (message == null) return UnifiedMemoryContext.MemoryType.CONVERSATION;

        Set<String> prefTriggers = Set.of("喜欢", "偏好", "习惯", "不要", "别用", "讨厌", "想要", "希望");
        Set<String> decisionTriggers = Set.of("决定", "选择", "确定", "采用", "使用", "不用", "放弃");
        Set<String> factTriggers = Set.of("我叫", "我的名字", "我在", "我做", "我的工作", "我是");

        for (String trigger : prefTriggers) {
            if (message.contains(trigger)) return UnifiedMemoryContext.MemoryType.PREFERENCE;
        }
        for (String trigger : decisionTriggers) {
            if (message.contains(trigger)) return UnifiedMemoryContext.MemoryType.DECISION;
        }
        for (String trigger : factTriggers) {
            if (message.contains(trigger)) return UnifiedMemoryContext.MemoryType.FACT;
        }

        return UnifiedMemoryContext.MemoryType.CONVERSATION;
    }
}