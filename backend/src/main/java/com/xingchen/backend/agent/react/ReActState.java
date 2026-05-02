package com.xingchen.backend.agent.react;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReActState {

    private String sessionId;
    private Long userId;

    @Builder.Default
    private List<ReActStep> steps = new ArrayList<>();

    @Builder.Default
    private int maxIterations = 10;

    @Builder.Default
    private int currentStep = 0;

    @Builder.Default
    private int totalTokens = 0;

    @Builder.Default
    private Instant startedAt = Instant.now();

    private String finalAnswer;
    private boolean terminated;
    private TerminationReason terminationReason;

    public void addStep(ReActStep step) {
        steps.add(step);
        currentStep++;
    }

    public boolean hasMoreSteps() {
        return currentStep < maxIterations && !terminated;
    }

    public void terminate(TerminationReason reason, String answer) {
        this.terminated = true;
        this.terminationReason = reason;
        this.finalAnswer = answer;
    }

    public enum TerminationReason {
        MAX_ITERATIONS_REACHED,
        FINAL_ANSWER_GENERATED,
        TOOL_EXECUTION_FAILED,
        LLM_ERROR,
        USER_INTERRUPT
    }

    @Data
    @Builder
    public static class ReActStep {
        private int stepNumber;

        private StepType type;

        private String thought;

        private Action action;

        private String observation;

        private ActionResult result;

        private long executionTimeMs;

        private Instant timestamp;

        private boolean success;

        private String errorMessage;

        public enum StepType {
            REASONING,
            ACTION,
            OBSERVATION,
            FINAL_ANSWER
        }
    }

    @Data
    @Builder
    public static class Action {
        private String toolName;
        private Map<String, Object> parameters;
        private String rawResponse;
    }

    @Data
    @Builder
    public static class ActionResult {
        private boolean success;
        private Object data;
        private String message;
        private long executionTimeMs;
    }

    public String toDetailedTrace() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ReAct 推理轨迹 (共 ").append(steps.size()).append(" 步) ===\n\n");

        for (ReActStep step : steps) {
            sb.append("【步骤 ").append(step.getStepNumber()).append("】\n");

            if (step.getThought() != null) {
                sb.append("思考: ").append(step.getThought()).append("\n");
            }

            if (step.getAction() != null) {
                sb.append("行动: ").append(step.getAction().getToolName())
                  .append("(").append(step.getAction().getParameters()).append(")\n");
            }

            if (step.getObservation() != null) {
                sb.append("观察: ").append(step.getObservation()).append("\n");
            }

            if (step.getResult() != null) {
                sb.append("结果: ").append(step.getResult().isSuccess() ? "成功" : "失败")
                  .append(" - ").append(step.getResult().getMessage()).append("\n");
            }

            if (step.getErrorMessage() != null) {
                sb.append("错误: ").append(step.getErrorMessage()).append("\n");
            }

            sb.append("\n");
        }

        if (finalAnswer != null) {
            sb.append("最终答案: ").append(finalAnswer).append("\n");
        }

        if (terminationReason != null) {
            sb.append("终止原因: ").append(terminationReason).append("\n");
        }

        return sb.toString();
    }
}
