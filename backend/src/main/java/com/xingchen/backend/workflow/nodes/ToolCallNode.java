package com.xingchen.backend.workflow.nodes;

import com.xingchen.backend.ai.tool.AIToolRegistry;
import com.xingchen.backend.ai.tool.Tool;
import com.xingchen.backend.workflow.TaskNode;
import com.xingchen.backend.workflow.WorkflowDefinition;
import com.xingchen.backend.workflow.WorkflowExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ToolCallNode implements TaskNode {

    private final AIToolRegistry toolRegistry;

    private static final String NODE_ID = "tool_call";
    private static final String NODE_NAME = "工具调用节点";

    @Override
    public String getNodeId() {
        return NODE_ID;
    }

    @Override
    public String getName() {
        return NODE_NAME;
    }

    @Override
    public WorkflowDefinition.WorkflowNode.NodeType getNodeType() {
        return WorkflowDefinition.WorkflowNode.NodeType.TOOL_CALL;
    }

    @Override
    public CompletableFuture<NodeResult> execute(WorkflowExecution execution, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String toolName = extractString(input, "toolName");
                if (toolName == null || toolName.isEmpty()) {
                    return NodeResult.failure(getNodeId(), "工具名称为空");
                }

                log.info("执行工具调用: tool={}, executionId={}", toolName, execution.getExecutionId());

                Map<String, Object> params = buildToolParams(input, execution);

                Tool.ToolResult result = toolRegistry.execute(toolName, params);

                if (result.success()) {
                    execution.putContext("tool_result_" + toolName, result.message());
                    return NodeResult.success(getNodeId(), Map.of(
                            "toolName", toolName,
                            "message", result.message(),
                            "data", result.data() != null ? result.data() : Map.of()
                    ));
                } else {
                    return NodeResult.failure(getNodeId(), result.message());
                }

            } catch (Exception e) {
                log.error("工具调用失败", e);
                return NodeResult.failure(getNodeId(), e.getMessage());
            }
        });
    }

    private Map<String, Object> buildToolParams(Map<String, Object> input, WorkflowExecution execution) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("userId", execution.getUserId());
        params.put("sessionId", execution.getSessionId());

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (!entry.getKey().equals("toolName")) {
                params.put(entry.getKey(), entry.getValue());
            }
        }

        return params;
    }

    private String extractString(Map<String, Object> input, String key) {
        Object value = input.get(key);
        return value != null ? value.toString() : null;
    }
}