package com.xingchen.backend.workflow;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface TaskNode {

    String getNodeId();

    String getName();

    WorkflowDefinition.WorkflowNode.NodeType getNodeType();

    CompletableFuture<NodeResult> execute(WorkflowExecution execution, Map<String, Object> input);

    default NodeResult executeSync(WorkflowExecution execution, Map<String, Object> input) {
        try {
            return execute(execution, input).join();
        } catch (Exception e) {
            return NodeResult.failure(getNodeId(), e.getMessage());
        }
    }

    default boolean validate(WorkflowDefinition.WorkflowNode nodeConfig) {
        return nodeConfig != null && nodeConfig.getNodeId() != null;
    }

    default void onError(WorkflowExecution execution, Exception error) {
    }

    record NodeResult(
            String nodeId,
            boolean success,
            Object output,
            String error,
            Map<String, Object> metadata
    ) {
        public static NodeResult success(String nodeId, Object output) {
            return new NodeResult(nodeId, true, output, null, Map.of());
        }

        public static NodeResult success(String nodeId, Object output, Map<String, Object> metadata) {
            return new NodeResult(nodeId, true, output, null, metadata);
        }

        public static NodeResult failure(String nodeId, String error) {
            return new NodeResult(nodeId, false, null, error, Map.of());
        }

        public static NodeResult failure(String nodeId, String error, Map<String, Object> metadata) {
            return new NodeResult(nodeId, false, null, error, metadata);
        }
    }
}