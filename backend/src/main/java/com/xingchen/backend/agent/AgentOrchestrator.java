package com.xingchen.backend.agent;

import com.xingchen.backend.ai.intent.IntentClassifierInterface;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.llm.UserLLMProviderManager;
import com.xingchen.backend.ai.memory.MemoryContext;
import com.xingchen.backend.ai.memory.MemoryManager;
import com.xingchen.backend.ai.memory.UserMemoryManager;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.model.Intent;
import com.xingchen.backend.ai.security.SecurityFilterChain;
import com.xingchen.backend.ai.tool.AIToolRegistry;
import com.xingchen.backend.ai.tool.Tool;
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
/**
 * 智能助手执行器
 */
public class AgentOrchestrator {

    private final SecurityFilterChain securityFilterChain;//安全过滤器
    private final IntentClassifierInterface intentClassifier;//意图分类器
    private final UserMemoryManager userMemoryManager;//用户记忆管理器
    private final AIToolRegistry toolRegistry;//工具注册器
    private final UserLLMProviderManager userProviderManager;//用户LLM提供器管理器
    private final CircuitBreakerRegistry circuitBreakerRegistry;//熔断器注册器
    private final MeterRegistry meterRegistry;//仪表盘

    /*
    * 处理请求
     */
    public AIResponse handle(AIRequest request) {
        long startTime = System.currentTimeMillis();//开始时间
        Timer.Sample sample = Timer.start(meterRegistry);//创建计时器

        try {
            request = securityFilterChain.filter(request);//过滤器

            Intent intent = intentClassifier.classify(request.getMessage());//意图分类
            log.debug("意图分类: type={}, confidence={}", intent.getType(), intent.getConfidence());

            ExecutionPlan plan = buildExecutionPlan(request, intent);//构建执行计划
            AIResponse response = executePlan(plan);//执行计划

            saveMemory(request, response);//保存记忆

            long elapsed = System.currentTimeMillis() - startTime;//计算耗时
            sample.stop(meterRegistry.timer("ai.orchestrator.handle",
                    "intent", intent.getType().name(),
                    "success", String.valueOf(response.isSuccess())));//停止计时器

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

    /*
    * 处理流式请求
     */
    public void handleStream(AIRequest request, Consumer<AIResponse> onChunk) {
        try {
            request = securityFilterChain.filter(request);

            Long userId = request.getUserId();
            if (userId == null) {
                onChunk.accept(AIResponse.error("用户未登录"));
                return;
            }

            Intent intent = intentClassifier.classify(request.getMessage()); //意图分类

            LLMProvider userProvider = validateAndGetUserProvider(userId, onChunk);//获取用户LLM提供器
            if (userProvider != null && userProvider.supportsStreaming()) {
                userProvider.streamChat(request, onChunk);
            }

        } catch (SecurityFilterChain.SecurityException e) {
            log.warn("安全拦截流式请求: {}", e.getMessage());
            onChunk.accept(AIResponse.error("请求被拦截: " + e.getMessage()));
        } catch (Exception e) {
            log.error("流式处理失败", e);
            onChunk.accept(AIResponse.error("流式处理失败: " + e.getMessage()));
        }
    }

    /**
     * 验证并获取用户的LLM Provider
     * @return LLMProvider,如果验证失败则返回null并通过onChunk发送错误消息(仅流式场景)
     */
    private LLMProvider validateAndGetUserProvider(Long userId) {
        LLMProvider userProvider = userProviderManager.getUserProvider(userId);
        if (userProvider == null) {
            log.warn("用户 {} 没有配置API Key", userId);
            throw new IllegalStateException("请先配置您的AI API Key,在设置->AI配置中添加");
        }
        log.debug("使用用户 {} 的Provider: {}", userId, userProvider.getProviderName());
        return userProvider;
    }

    /**
     * 验证并获取用户的LLM Provider(流式场景)
     */
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
                .status(ExecutionPlan.ExecutionStep.StepStatus.COMPLETED)//步骤状态设为COMPLETED
                .build());

        if (intent.isRequiresMemory()) {
            steps.add(ExecutionPlan.ExecutionStep.builder()
                    .order(order++)
                    .type(ExecutionPlan.ExecutionStep.StepType.MEMORY_RETRIEVAL)//步骤类型设为MEMORY_RETRIEVAL
                    .description("检索相关记忆")
                    .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)//步骤状态设为PENDING
                    .build());//添加步骤
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
        MemoryContext memoryContext = null;
        List<Tool.ToolResult> toolResults = new ArrayList<>();

        for (ExecutionPlan.ExecutionStep step : plan.getSteps()) {
            if (step.getStatus() == ExecutionPlan.ExecutionStep.StepStatus.COMPLETED) {
                continue;
            }

            long stepStart = System.currentTimeMillis();
            step.setStatus(ExecutionPlan.ExecutionStep.StepStatus.RUNNING);

            try {
                switch (step.getType()) {
                    case MEMORY_RETRIEVAL:
                        memoryContext = userMemoryManager.load(
                                request.getUserId(),
                                request.getSessionId(),
                                MemoryManager.MemoryRequirements.fromIntent(intent)
                        );
                        break;

                    case TOOL_CALL:
                        String toolName = (String) step.getParameters().get("toolName");
                        Tool.ToolResult result = executeTool(toolName, request, intent);
                        toolResults.add(result);
                        break;

                    case LLM_CALL:
                        return executeLLMCall(request, intent, memoryContext, toolResults);

                    default:
                        break;
                }

                step.setStatus(ExecutionPlan.ExecutionStep.StepStatus.COMPLETED);
                step.setExecutionTime(System.currentTimeMillis() - stepStart);

            } catch (Exception e) {
                step.setStatus(ExecutionPlan.ExecutionStep.StepStatus.FAILED);//步骤状态设为FAILED
                step.setExecutionTime(System.currentTimeMillis() - stepStart);//执行时间
                log.error("步骤执行失败: {}", step.getDescription(), e);
                throw e;
            }
        }

        return AIResponse.error("执行计划异常");
    }
    /**
     * 执行工具调用
     *
     * @param toolName 工具名称
     * @param request  请求
     * @param intent   意图
     * @return 工具调用结果
     */
    private Tool.ToolResult executeTool(String toolName, AIRequest request, Intent intent) {
        Map<String, Object> params = new HashMap<>();
        params.put("message", request.getMessage());//消息
        params.put("intent", intent.getType().name());//意图

        // 将意图识别出的实体参数添加到工具调用参数中
        if (intent.getEntities() != null) {
            for (Intent.Entity entity : intent.getEntities()) {
                params.put(entity.getName(), entity.getValue());
            }
        }

        return toolRegistry.execute(toolName, params);//执行工具调用
    }
    /**
     * 执行 LLM 调用
     *
     * @param request           请求
     * @param intent            意图
     * @param memoryContext     内存上下文
     * @param toolResults       工具调用结果
     * @return 响应
     */
    private AIResponse executeLLMCall(AIRequest request, Intent intent,
                                       MemoryContext memoryContext,
                                       List<Tool.ToolResult> toolResults) {
        Long userId = request.getUserId();
        if (userId == null) {
            return AIResponse.error("用户未登录");
        }
    
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个小星Agent,帮助用户完成各种任务。\n\n");
    
        if (memoryContext != null && !memoryContext.isEmpty()) {//内存不为空
            systemPrompt.append(memoryContext.toPromptText());//添加上下文
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
            systemPrompt.append("请基于上述工具执行结果回答用户的问题。\n");
        }
        //创建增强后的请求
        AIRequest enhancedRequest = AIRequest.builder()
                .userId(userId)
                .sessionId(request.getSessionId())
                .message(request.getMessage())
                .preferredModel(request.getPreferredModel())//使用 preferredModel
                .systemPrompt(systemPrompt.toString())
                .history(request.getHistory())
                .stream(request.isStream())
                .extraParams(request.getExtraParams())//额外参数
                .build();
    
        try {
            LLMProvider userProvider = validateAndGetUserProvider(userId);//获取用户提供者
            return userProvider.chat(enhancedRequest);
        } catch (IllegalStateException e) {
            return AIResponse.error(e.getMessage());
        }
    }



    private void saveMemory(AIRequest request, AIResponse response) {
        if (request.getUserId() == null) {
            return;
        }

        List<MemoryContext.MemoryItem> memories = new ArrayList<>();

        memories.add(MemoryContext.MemoryItem.builder()
                .id(UUID.randomUUID().toString())
                .role("user")
                .content(request.getMessage())
                .timestamp(System.currentTimeMillis())
                .type(MemoryContext.MemoryType.FACT)
                .build());

        if (response.isSuccess()) {
            memories.add(MemoryContext.MemoryItem.builder()
                    .id(UUID.randomUUID().toString())
                    .role("assistant")
                    .content(response.getContent())
                    .timestamp(System.currentTimeMillis())
                    .type(MemoryContext.MemoryType.FACT)
                    .build());
        }

        userMemoryManager.save(request.getUserId(), memories);
    }
}