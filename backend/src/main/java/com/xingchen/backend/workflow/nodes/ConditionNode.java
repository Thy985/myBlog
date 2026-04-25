package com.xingchen.backend.workflow.nodes;

import com.xingchen.backend.workflow.TaskNode;
import com.xingchen.backend.workflow.WorkflowDefinition;
import com.xingchen.backend.workflow.WorkflowExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ConditionNode implements TaskNode {

    private static final String NODE_ID = "condition";
    private static final String NODE_NAME = "条件判断节点";

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
        return WorkflowDefinition.WorkflowNode.NodeType.CONDITION;
    }

    @Override
    public CompletableFuture<NodeResult> execute(WorkflowExecution execution, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String conditionExpression = extractString(input, "condition");
                if (conditionExpression == null || conditionExpression.isEmpty()) {
                    return NodeResult.failure(getNodeId(), "条件表达式为空");
                }

                log.info("评估条件表达式: {}, executionId={}", conditionExpression, execution.getExecutionId());

                boolean result = evaluateCondition(conditionExpression, execution);

                return NodeResult.success(getNodeId(), Map.of(
                        "condition", conditionExpression,
                        "result", result,
                        "branch", result ? "true" : "false"
                ));

            } catch (Exception e) {
                log.error("条件判断失败", e);
                return NodeResult.failure(getNodeId(), e.getMessage());
            }
        });
    }

    private boolean evaluateCondition(String expression, WorkflowExecution execution) {
        expression = expression.trim();

        if (expression.startsWith("${context.")) {
            int closeIdx = expression.lastIndexOf("}");
            if (closeIdx > 0) {
                String contextKey = expression.substring(9, closeIdx);
                Object value = execution.getContext(contextKey);
                return value != null && !value.toString().isEmpty();
            }
        }

        if (expression.contains("==")) {
            String[] parts = expression.split("==");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("\"", "").replace("'", "");
                Object leftValue = resolveValue(left, execution);
                return right.equals(String.valueOf(leftValue));
            }
        }

        if (expression.contains("!=")) {
            String[] parts = expression.split("!=");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("\"", "").replace("'", "");
                Object leftValue = resolveValue(left, execution);
                return !right.equals(String.valueOf(leftValue));
            }
        }

        if (expression.contains(">") || expression.contains("<") ||
                expression.contains(">=") || expression.contains("<=")) {

            String op = null;
            if (expression.contains(">=")) op = ">=";
            else if (expression.contains("<=")) op = "<=";
            else if (expression.contains("!=")) op = "!=";
            else if (expression.contains("==")) op = "==";
            else if (expression.contains(">")) op = ">";
            else if (expression.contains("<")) op = "<";

            if (op != null) {
                String[] parts = expression.split(op);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim().replace("\"", "").replace("'", "");
                    Object leftValue = resolveValue(left, execution);
                    Object rightValue = resolveValue(right, execution);

                    try {
                        double leftNum = Double.parseDouble(String.valueOf(leftValue));
                        double rightNum = Double.parseDouble(String.valueOf(rightValue));

                        return switch (op) {
                            case ">" -> leftNum > rightNum;
                            case "<" -> leftNum < rightNum;
                            case ">=" -> leftNum >= rightNum;
                            case "<=" -> leftNum <= rightNum;
                            default -> false;
                        };
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
        }

        Object value = resolveValue(expression, execution);
        return value != null && !value.toString().isEmpty() &&
                !"false".equalsIgnoreCase(String.valueOf(value)) &&
                !"0".equals(String.valueOf(value));
    }

    private Object resolveValue(String expression, WorkflowExecution execution) {
        expression = expression.trim();

        if (expression.startsWith("${context.")) {
            int closeIdx = expression.lastIndexOf("}");
            if (closeIdx > 0) {
                String contextKey = expression.substring(9, closeIdx);
                return execution.getContext(contextKey);
            }
        }

        if (expression.startsWith("\"") && expression.endsWith("\"") ||
                expression.startsWith("'") && expression.endsWith("'")) {
            return expression.substring(1, expression.length() - 1);
        }

        try {
            if (expression.contains(".")) {
                return Double.parseDouble(expression);
            }
            return Integer.parseInt(expression);
        } catch (NumberFormatException e) {
            return expression;
        }
    }

    private String extractString(Map<String, Object> input, String key) {
        Object value = input.get(key);
        return value != null ? value.toString() : null;
    }
}