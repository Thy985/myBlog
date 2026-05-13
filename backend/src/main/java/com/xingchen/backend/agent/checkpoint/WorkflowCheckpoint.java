package com.xingchen.backend.agent.checkpoint;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 工作流检查点，用于保存工作流的状态
 */
@Data
@Builder
@AllArgsConstructor//全参构造注解
@NoArgsConstructor//无参构造注解
public class WorkflowCheckpoint {

    private String checkpointId;// 检查点ID
    private String sessionId;
    private String workflowType;
    private Long userId;
    private String currentPhase;//当前任务阶段
    private List<String> completedPhases;//已完成的任务阶段
    private int stepNumber;//步骤计数器
    private Map<String, Object> stateData;//当前步骤的数据
    private Map<String, Object> metadata;//元数据，用于保存工作流执行过程中的临时数据，比如模型参数、模型配置等
    private Instant createdAt;
    private Instant expiresAt;
    private boolean isCompleted;
    private boolean isFailed;

    public static WorkflowCheckpoint create(
            String sessionId,
            String workflowType,
            Long userId,
            String currentPhase,
            List<String> completedPhases,
            int stepNumber,
            Map<String, Object> stateData) {

        Instant now = Instant.now();
        return WorkflowCheckpoint.builder()
                .checkpointId(java.util.UUID.randomUUID().toString())
                .sessionId(sessionId)
                .workflowType(workflowType)
                .userId(userId)
                .currentPhase(currentPhase)
                .completedPhases(completedPhases)
                .stepNumber(stepNumber)
                .stateData(stateData)
                .createdAt(now)
                .expiresAt(now.plusSeconds(7 * 24 * 3600))
                .isCompleted(false)
                .isFailed(false)
                .build();
    }

    public void markCompleted() {
        this.isCompleted = true;
    }

    public void markFailed(String reason) {
        this.isFailed = true;
        if (this.metadata == null) {
            this.metadata = Map.of();
        }
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean canResume() {
        return !isCompleted && !isFailed && !isExpired();
    }

    public String getNextPhase() {
        if (completedPhases == null || completedPhases.isEmpty()) {
            return currentPhase;
        }
        return completedPhases.get(completedPhases.size() - 1);
    }
}