package com.xingchen.backend.ai.orchestrator;

import com.xingchen.backend.ai.intent.IntentClassifierInterface;
import com.xingchen.backend.ai.llm.*;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * 智能体编排器（用户级）
 * 每个用户使用独立的Provider和记忆
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {
    
    private final SecurityFilterChain securityFilterChain;
    private final IntentClassifierInterface intentClassifier;
    private final UserMemoryManager userMemoryManager;
    private final AIToolRegistry toolRegistry;
    private final UserLLMProviderManager userProviderManager;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;
    
    /**
     * 处理请求
     */
    public AIResponse handle(AIRequest request) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // 1. 安全检查
            request = securityFilterChain.filter(request);
            
            // 2. 意图分类
            Intent intent = intentClassifier.classify(request.getMessage());
            log.debug("意图分类: type={}, confidence={}", intent.getType(), intent.getConfidence());
            
            // 3. 构建执行计划
            ExecutionPlan plan = buildExecutionPlan(request, intent);
            
            // 4. 执行计划
            AIResponse response = executePlan(plan);
            
            // 5. 保存记忆
            saveMemory(request, response);
            
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
    
    /**
     * 流式处理
     */
    public void handleStream(AIRequest request, Consumer<AIResponse> onChunk) {
        try {
            // 安全检查
            request = securityFilterChain.filter(request);

            // 检查用户ID
            Long userId = request.getUserId();
            if (userId == null) {
                onChunk.accept(AIResponse.error("用户未登录"));
                return;
            }

            // 意图分类（简化处理）
            Intent intent = intentClassifier.classify(request.getMessage());

            // 流式输出 - 优先使用用户自己的Provider
            LLMProvider userProvider = userProviderManager.getUserProvider(userId);
            if (userProvider != null && userProvider.supportsStreaming()) {
                userProvider.streamChat(request, onChunk);
            } else {
                onChunk.accept(AIResponse.error("请先配置您的AI API Key"));
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
     * 构建执行计划
     */
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
        
        // 步骤1: 意图分类（已完成）
        steps.add(ExecutionPlan.ExecutionStep.builder()
                .order(order++)
                .type(ExecutionPlan.ExecutionStep.StepType.INTENT_CLASSIFICATION)
                .description("意图分类: " + intent.getType())
                .status(ExecutionPlan.ExecutionStep.StepStatus.COMPLETED)
                .build());
        
        // 步骤2: 记忆检索（如果需要）
        if (intent.isRequiresMemory()) {
            steps.add(ExecutionPlan.ExecutionStep.builder()
                    .order(order++)
                    .type(ExecutionPlan.ExecutionStep.StepType.MEMORY_RETRIEVAL)
                    .description("检索相关记忆")
                    .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                    .build());
        }
        
        // 步骤3: RAG检索（如果需要）
        if (request.needsRag()) {
            steps.add(ExecutionPlan.ExecutionStep.builder()
                    .order(order++)
                    .type(ExecutionPlan.ExecutionStep.StepType.RAG_RETRIEVAL)
                    .description("检索知识库")
                    .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                    .build());
        }
        
        // 步骤4: 工具调用（如果需要）
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
        
        // 步骤5: LLM调用
        steps.add(ExecutionPlan.ExecutionStep.builder()
                .order(order++)
                .type(ExecutionPlan.ExecutionStep.StepType.LLM_CALL)
                .description("生成回复")
                .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                .build());
        
        // 步骤6: 格式化
        steps.add(ExecutionPlan.ExecutionStep.builder()
                .order(order++)
                .type(ExecutionPlan.ExecutionStep.StepType.RESPONSE_FORMAT)
                .description("格式化响应")
                .status(ExecutionPlan.ExecutionStep.StepStatus.PENDING)
                .build());
        
        return planBuilder.steps(steps).build();
    }
    
    /**
     * 执行计划
     */
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
                step.setStatus(ExecutionPlan.ExecutionStep.StepStatus.FAILED);
                step.setExecutionTime(System.currentTimeMillis() - stepStart);
                log.error("步骤执行失败: {}", step.getDescription(), e);
                throw e;
            }
        }
        
        return AIResponse.error("执行计划异常");
    }
    
    /**
     * 执行工具
     */
    private Tool.ToolResult executeTool(String toolName, AIRequest request, Intent intent) {
        // 构建工具参数
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
    
    /**
     * 执行LLM调用（用户级）
     * 优先使用用户自己的API Key
     */
    private AIResponse executeLLMCall(AIRequest request, Intent intent,
                                       MemoryContext memoryContext,
                                       List<Tool.ToolResult> toolResults) {
        Long userId = request.getUserId();
        if (userId == null) {
            return AIResponse.error("用户未登录");
        }

        // 构建系统提示词
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个智能助手，帮助用户完成各种任务。\n\n");

        // 添加记忆上下文
        if (memoryContext != null && !memoryContext.isEmpty()) {
            systemPrompt.append(memoryContext.toPromptText());
        }

        // 添加工具结果
        if (!toolResults.isEmpty()) {
            systemPrompt.append("【工具执行结果】\n");
            for (Tool.ToolResult result : toolResults) {
                systemPrompt.append("- ").append(result.message()).append("\n");
            }
            systemPrompt.append("\n");
        }

        // 构建增强请求
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

        // 优先使用用户自己的Provider
        LLMProvider userProvider = userProviderManager.getUserProvider(userId);
        if (userProvider != null) {
            log.debug("使用用户 {} 的Provider: {}", userId, userProvider.getProviderName());
            return userProvider.chat(enhancedRequest);
        }

        // 用户没有配置，使用系统默认
        log.warn("用户 {} 没有配置API Key，使用系统默认Provider", userId);
        return AIResponse.error("请先配置您的AI API Key，在设置->AI配置中添加");
    }
    
    /**
     * 流式LLM调用（用户级）
     */
    private void executeStreamLLMCall(AIRequest request, Consumer<AIResponse> onChunk) {
        Long userId = request.getUserId();
        if (userId == null) {
            onChunk.accept(AIResponse.error("用户未登录"));
            return;
        }

        LLMProvider userProvider = userProviderManager.getUserProvider(userId);
        if (userProvider != null && userProvider.supportsStreaming()) {
            userProvider.streamChat(request, onChunk);
        } else {
            onChunk.accept(AIResponse.error("请先配置您的AI API Key"));
        }
    }
    
    /**
     * 保存记忆
     */
    private void saveMemory(AIRequest request, AIResponse response) {
        if (request.getUserId() == null) {
            return;
        }
        
        List<MemoryContext.MemoryItem> memories = new ArrayList<>();
        
        // 用户消息
        memories.add(MemoryContext.MemoryItem.builder()
                .id(UUID.randomUUID().toString())
                .role("user")
                .content(request.getMessage())
                .timestamp(System.currentTimeMillis())
                .type(MemoryContext.MemoryType.FACT)
                .build());
        
        // AI回复
        if (response.isSuccess()) {
            memories.add(MemoryContext.MemoryItem.builder()
                    .id(UUID.randomUUID().toString())
                    .role("assistant")
                    .content(response.getContent())
                    .timestamp(System.currentTimeMillis())
                    .type(MemoryContext.MemoryType.FACT)
                    .build());
        }
        
        // 异步保存
        userMemoryManager.save(request.getUserId(), memories);
    }
}