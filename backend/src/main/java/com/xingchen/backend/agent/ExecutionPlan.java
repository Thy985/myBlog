package com.xingchen.backend.agent;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.model.Intent;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ExecutionPlan {

    private String planId;
    private AIRequest originalRequest;
    private Intent intent;
    private List<ExecutionStep> steps;
    private boolean requiresToolCall;
    private List<String> requiredTools;
    private boolean requiresMemory;
    private boolean requiresRag;
    private String systemPrompt;
    private Map<String, Object> context;

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
            INTENT_CLASSIFICATION,
            MEMORY_RETRIEVAL,
            RAG_RETRIEVAL,
            TOOL_CALL,
            LLM_CALL,
            RESPONSE_FORMAT
        }

        public enum StepStatus {
            PENDING,
            RUNNING,
            COMPLETED,
            FAILED
        }
    }

    public static ExecutionPlan simpleChat(AIRequest request) {
        return ExecutionPlan.builder()
                .planId(UUID.randomUUID().toString())
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