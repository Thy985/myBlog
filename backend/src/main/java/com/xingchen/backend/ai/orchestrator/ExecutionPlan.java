package com.xingchen.backend.ai.orchestrator;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.model.Intent;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 执行计划
 * 定义AI任务的执行步骤
 */
@Data
@Builder
public class ExecutionPlan {
    
    /** 计划ID */
    private String planId;
    
    /** 原始请求 */
    private AIRequest originalRequest;
    
    /** 识别到的意图 */
    private Intent intent;
    
    /** 执行步骤 */
    private List<ExecutionStep> steps;
    
    /** 是否需要工具调用 */
    private boolean requiresToolCall;
    
    /** 需要调用的工具 */
    private List<String> requiredTools;
    
    /** 是否需要记忆 */
    private boolean requiresMemory;
    
    /** 是否需要RAG */
    private boolean requiresRag;
    
    /** 系统提示词 */
    private String systemPrompt;
    
    /** 上下文数据 */
    private Map<String, Object> context;
    
    /**
     * 执行步骤
     */
    @Data
    @Builder
    public static class ExecutionStep {
        private int order;
        private StepType type;
        private String description;
        private Map<String, Object> parameters;
        private StepStatus status;
        private AIResponse result;
        private long executionTime;
        
        public enum StepType {
            INTENT_CLASSIFICATION,  // 意图分类
            MEMORY_RETRIEVAL,       // 记忆检索
            RAG_RETRIEVAL,          // RAG检索
            TOOL_CALL,              // 工具调用
            LLM_CALL,               // LLM调用
            RESPONSE_FORMAT         // 响应格式化
        }
        
        public enum StepStatus {
            PENDING,    // 待执行
            RUNNING,    // 执行中
            COMPLETED,  // 完成
            FAILED      // 失败
        }
    }
    
    /**
     * 创建简单对话计划
     */
    public static ExecutionPlan simpleChat(AIRequest request) {
        return ExecutionPlan.builder()
                .planId(java.util.UUID.randomUUID().toString())
                .originalRequest(request)
                .intent(Intent.chat(request.getMessage()))
                .requiresToolCall(false)
                .requiresMemory(false)
                .requiresRag(false)
                .steps(List.of(
                        ExecutionStep.builder()
                                .order(1)
                                .type(ExecutionStep.StepType.INTENT_CLASSIFICATION)
                                .description("意图分类")
                                .status(ExecutionStep.StepStatus.PENDING)
                                .build(),
                        ExecutionStep.builder()
                                .order(2)
                                .type(ExecutionStep.StepType.LLM_CALL)
                                .description("LLM对话")
                                .status(ExecutionStep.StepStatus.PENDING)
                                .build()
                ))
                .build();
    }
}