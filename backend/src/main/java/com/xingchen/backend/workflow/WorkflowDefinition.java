package com.xingchen.backend.workflow;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String workflowId;
    private String name;
    private String description;
    private String version;
    private WorkflowStatus status;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<WorkflowNode> nodes;
    private List<WorkflowEdge> edges;
    private WorkflowConfig config;

    public enum WorkflowStatus {
        DRAFT,
        ACTIVE,
        PAUSED,
        ARCHIVED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowNode implements Serializable {
        private static final long serialVersionUID = 1L;

        private String nodeId;
        private String name;
        private NodeType type;
        private NodeStatus status;
        private Map<String, Object> config;
        private Map<String, Object> inputSchema;
        private Map<String, Object> outputSchema;
        private List<String> inputNodes;
        private List<String> outputNodes;
        private int retryCount;
        private long timeoutMs;
        private boolean isAsync;

        public enum NodeType {
            START,
            END,
            LLM_CALL,
            TOOL_CALL,
            CONDITION,
            PARALLEL,
            SEQUENCE,
            LOOP,
            WAIT,
            NOTIFY,
            SCRIPT
        }

        public enum NodeStatus {
            IDLE,
            READY,
            RUNNING,
            COMPLETED,
            FAILED,
            SKIPPED
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowEdge implements Serializable {
        private static final long serialVersionUID = 1L;

        private String edgeId;
        private String sourceNodeId;
        private String targetNodeId;
        private String condition;
        private EdgeType type;
        private Map<String, Object> metadata;

        public enum EdgeType {
            SEQUENCE,
            CONDITIONAL,
            PARALLEL_BRANCH,
            ERROR_HANDLER,
            TIMEOUT
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        private long defaultTimeoutMs;
        private int maxRetries;
        private boolean enableLogging;
        private boolean enableMetrics;
        private String onError;
        private Map<String, String> environment;
    }
}