package com.xingchen.backend.workflow;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    private String executionId;
    private String workflowId;
    private Long userId;
    private String sessionId;
    private ExecutionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Map<String, NodeExecutionState> nodeStates;
    private Map<String, Object> sharedContext;
    private String currentNodeId;
    private String errorMessage;
    private int retryCount;
    private Map<String, Object> metadata;

    public enum ExecutionStatus {
        CREATED,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeExecutionState implements Serializable {
        private static final long serialVersionUID = 1L;

        private String nodeId;
        private String status;
        private Instant enteredAt;
        private Instant completedAt;
        private Object input;
        private Object output;
        private String error;
        private int attemptCount;
        private Map<String, Object> stateData;
    }

    public static WorkflowExecution create(String workflowId, Long userId, String sessionId) {
        return WorkflowExecution.builder()
                .executionId(java.util.UUID.randomUUID().toString())
                .workflowId(workflowId)
                .userId(userId)
                .sessionId(sessionId)
                .status(ExecutionStatus.CREATED)
                .startedAt(Instant.now())
                .nodeStates(new ConcurrentHashMap<>())
                .sharedContext(new ConcurrentHashMap<>())
                .retryCount(0)
                .metadata(new ConcurrentHashMap<>())
                .build();
    }

    public void updateNodeState(String nodeId, NodeExecutionState state) {
        this.nodeStates.put(nodeId, state);
        this.currentNodeId = nodeId;
    }

    public NodeExecutionState getNodeState(String nodeId) {
        return this.nodeStates.get(nodeId);
    }

    public void putContext(String key, Object value) {
        this.sharedContext.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        return (T) this.sharedContext.get(key);
    }

    public void markRunning() {
        this.status = ExecutionStatus.RUNNING;
    }

    public void markCompleted() {
        this.status = ExecutionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = Instant.now();
    }

    public void markPaused() {
        this.status = ExecutionStatus.PAUSED;
    }

    public void markCancelled() {
        this.status = ExecutionStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    public long getDurationMs() {
        if (startedAt == null) return 0;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return end.toEpochMilli() - startedAt.toEpochMilli();
    }
}