package com.xingchen.backend.workflow;

import com.xingchen.backend.workflow.nodes.ConditionNode;
import com.xingchen.backend.workflow.nodes.LLMCallNode;
import com.xingchen.backend.workflow.nodes.ParallelNode;
import com.xingchen.backend.workflow.nodes.ToolCallNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final LLMCallNode llmCallNode;
    private final ToolCallNode toolCallNode;
    private final ConditionNode conditionNode;
    private final ParallelNode parallelNode;
    private final MeterRegistry meterRegistry;

    private final Map<String, WorkflowDefinition> workflowRegistry = new ConcurrentHashMap<>();
    private final Map<String, WorkflowExecution> activeExecutions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final long DEFAULT_TIMEOUT_MS = 60000;
    private static final int DEFAULT_MAX_RETRIES = 3;

    public String registerWorkflow(WorkflowDefinition workflow) {
        if (workflow.getWorkflowId() == null || workflow.getWorkflowId().isEmpty()) {
            workflow.setWorkflowId(UUID.randomUUID().toString());
        }
        workflowRegistry.put(workflow.getWorkflowId(), workflow);
        log.info("工作流已注册: workflowId={}, name={}", workflow.getWorkflowId(), workflow.getName());
        return workflow.getWorkflowId();
    }

    public WorkflowExecution startWorkflow(String workflowId, Long userId, String sessionId,
                                            Map<String, Object> initialContext) {
        WorkflowDefinition workflow = workflowRegistry.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("工作流不存在: " + workflowId);
        }

        WorkflowExecution execution = WorkflowExecution.create(workflowId, userId, sessionId);
        if (initialContext != null) {
            initialContext.forEach(execution::putContext);
        }

        activeExecutions.put(execution.getExecutionId(), execution);
        execution.markRunning();

        log.info("工作流执行开始: executionId={}, workflowId={}, userId={}",
                execution.getExecutionId(), workflowId, userId);

        executorService.submit(() -> executeWorkflow(execution, workflow));

        return execution;
    }

    public void startWorkflowAsync(String workflowId, Long userId, String sessionId,
                                    Map<String, Object> initialContext,
                                    Consumer<WorkflowExecution> onComplete) {
        executorService.submit(() -> {
            try {
                WorkflowExecution execution = startWorkflow(workflowId, userId, sessionId, initialContext);
                if (onComplete != null) {
                    onComplete.accept(execution);
                }
            } catch (Exception e) {
                log.error("异步启动工作流失败: workflowId={}", workflowId, e);
            }
        });
    }

    private void executeWorkflow(WorkflowExecution execution, WorkflowDefinition workflow) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String currentNodeId = findStartNode(workflow);

        try {
            while (currentNodeId != null && execution.getStatus() == WorkflowExecution.ExecutionStatus.RUNNING) {
                WorkflowDefinition.WorkflowNode currentNode = findNode(workflow, currentNodeId);
                if (currentNode == null) {
                    execution.markFailed("节点不存在: " + currentNodeId);
                    break;
                }

                log.debug("执行节点: nodeId={}, type={}, executionId={}",
                        currentNodeId, currentNode.getType(), execution.getExecutionId());

                Map<String, Object> nodeInput = buildNodeInput(execution, currentNode);

                TaskNode taskNode = resolveTaskNode(currentNode.getType());
                if (taskNode == null) {
                    execution.markFailed("不支持的节点类型: " + currentNode.getType());
                    break;
                }

                TaskNode.NodeResult result = executeNodeWithRetry(taskNode, execution, nodeInput);

                updateExecutionState(execution, currentNodeId, result);

                if (!result.success()) {
                    log.warn("节点执行失败: nodeId={}, error={}, executionId={}",
                            currentNodeId, result.error(), execution.getExecutionId());
                    if (!handleError(execution, workflow, currentNode, result.error())) {
                        break;
                    }
                    currentNodeId = findNextNode(workflow, currentNodeId, true);
                    continue;
                }

                currentNodeId = findNextNode(workflow, currentNodeId, false);
            }

            if (execution.getStatus() == WorkflowExecution.ExecutionStatus.RUNNING) {
                execution.markCompleted();
                log.info("工作流执行完成: executionId={}, duration={}ms",
                        execution.getExecutionId(), execution.getDurationMs());
            }

        } catch (Exception e) {
            log.error("工作流执行异常: executionId={}", execution.getExecutionId(), e);
            execution.markFailed(e.getMessage());
        } finally {
            sample.stop(meterRegistry.timer("workflow.execution",
                    "workflow_id", workflow.getWorkflowId(),
                    "status", execution.getStatus().name()));
            activeExecutions.remove(execution.getExecutionId());
        }
    }

    private TaskNode.NodeResult executeNodeWithRetry(TaskNode taskNode, WorkflowExecution execution,
                                                      Map<String, Object> input) {
        int maxRetries = execution.getMetadata() != null &&
                execution.getMetadata().containsKey("maxRetries") ?
                (Integer) execution.getMetadata().get("maxRetries") : DEFAULT_MAX_RETRIES;

        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return taskNode.executeSync(execution, input);
            } catch (Exception e) {
                lastException = e;
                log.warn("节点执行重试: attempt={}, nodeId={}, error={}",
                        attempt + 1, taskNode.getNodeId(), e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return TaskNode.NodeResult.failure(taskNode.getNodeId(),
                lastException != null ? lastException.getMessage() : "执行失败");
    }

    private Map<String, Object> buildNodeInput(WorkflowExecution execution, WorkflowDefinition.WorkflowNode node) {
        Map<String, Object> input = new HashMap<>();

        if (node.getConfig() != null && node.getConfig().containsKey("input")) {
            Object inputConfig = node.getConfig().get("input");
            if (inputConfig instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configInput = (Map<String, Object>) inputConfig;
                input.putAll(configInput);
            }
        }

        Map<String, Object> sharedContext = execution.getSharedContext();
        for (Map.Entry<String, Object> entry : sharedContext.entrySet()) {
            if (!input.containsKey(entry.getKey())) {
                input.put(entry.getKey(), entry.getValue());
            }
        }

        return input;
    }

    private TaskNode resolveTaskNode(WorkflowDefinition.WorkflowNode.NodeType nodeType) {
        if (nodeType == null) {
            return null;
        }
        return switch (nodeType) {
            case LLM_CALL -> llmCallNode;
            case TOOL_CALL -> toolCallNode;
            case CONDITION -> conditionNode;
            case PARALLEL -> parallelNode;
            default -> null;
        };
    }

    private void updateExecutionState(WorkflowExecution execution, String nodeId, TaskNode.NodeResult result) {
        WorkflowExecution.NodeExecutionState state = WorkflowExecution.NodeExecutionState.builder()
                .nodeId(nodeId)
                .status(result.success() ? "COMPLETED" : "FAILED")
                .enteredAt(Instant.now())
                .completedAt(Instant.now())
                .output(result.output())
                .error(result.error())
                .attemptCount(1)
                .stateData(result.metadata() != null ? result.metadata() : Map.of())
                .build();

        execution.updateNodeState(nodeId, state);

        if (result.metadata() != null) {
            result.metadata().forEach(execution::putContext);
        }
    }

    private boolean handleError(WorkflowExecution execution, WorkflowDefinition workflow,
                                  WorkflowDefinition.WorkflowNode failedNode, String error) {
        List<WorkflowDefinition.WorkflowEdge> errorEdges = workflow.getEdges().stream()
                .filter(e -> e.getSourceNodeId().equals(failedNode.getNodeId()))
                .filter(e -> e.getType() == WorkflowDefinition.WorkflowEdge.EdgeType.ERROR_HANDLER)
                .toList();

        if (!errorEdges.isEmpty()) {
            String errorHandlerNodeId = errorEdges.get(0).getTargetNodeId();
            log.info("执行错误处理器: from={}, to={}, executionId={}",
                    failedNode.getNodeId(), errorHandlerNodeId, execution.getExecutionId());
            execution.putContext("_error", error);
            execution.putContext("_errorNode", failedNode.getNodeId());
            return true;
        }

        WorkflowDefinition.WorkflowConfig config = workflow.getConfig();
        if (config != null && "continue".equals(config.getOnError())) {
            log.info("配置为继续执行，忽略错误: nodeId={}, executionId={}",
                    failedNode.getNodeId(), execution.getExecutionId());
            return true;
        }

        return false;
    }

    private String findStartNode(WorkflowDefinition workflow) {
        return workflow.getNodes().stream()
                .filter(n -> n.getType() == WorkflowDefinition.WorkflowNode.NodeType.START)
                .findFirst()
                .map(WorkflowDefinition.WorkflowNode::getNodeId)
                .orElse(workflow.getNodes().isEmpty() ? null : workflow.getNodes().get(0).getNodeId());
    }

    private WorkflowDefinition.WorkflowNode findNode(WorkflowDefinition workflow, String nodeId) {
        return workflow.getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    private String findNextNode(WorkflowDefinition workflow, String currentNodeId, boolean fromError) {
        List<WorkflowDefinition.WorkflowEdge> candidateEdges = workflow.getEdges().stream()
                .filter(e -> e.getSourceNodeId().equals(currentNodeId))
                .filter(e -> fromError ?
                        e.getType() == WorkflowDefinition.WorkflowEdge.EdgeType.ERROR_HANDLER :
                        e.getType() == WorkflowDefinition.WorkflowEdge.EdgeType.SEQUENCE)
                .toList();

        if (candidateEdges.isEmpty()) {
            return null;
        }

        if (candidateEdges.size() == 1) {
            return candidateEdges.get(0).getTargetNodeId();
        }

        WorkflowDefinition.WorkflowNode currentNode = findNode(workflow, currentNodeId);
        if (currentNode != null && currentNode.getType() == WorkflowDefinition.WorkflowNode.NodeType.CONDITION) {
            WorkflowExecution execution = activeExecutions.values().stream()
                    .filter(e -> e.getWorkflowId().equals(workflow.getWorkflowId()))
                    .filter(e -> e.getStatus() == WorkflowExecution.ExecutionStatus.RUNNING)
                    .findFirst()
                    .orElse(null);

            if (execution != null) {
                Object conditionResult = execution.getContext("condition_result");
                boolean conditionMet = conditionResult != null && Boolean.parseBoolean(conditionResult.toString());

                return conditionMet ?
                        candidateEdges.stream()
                                .filter(e -> e.getMetadata() != null &&
                                        "true".equals(String.valueOf(e.getMetadata().getOrDefault("branch", "false"))))
                                .findFirst()
                                .map(WorkflowDefinition.WorkflowEdge::getTargetNodeId)
                                .orElse(candidateEdges.get(0).getTargetNodeId()) :
                        candidateEdges.stream()
                                .filter(e -> e.getMetadata() != null &&
                                        "false".equals(String.valueOf(e.getMetadata().getOrDefault("branch", "true"))))
                                .findFirst()
                                .map(WorkflowDefinition.WorkflowEdge::getTargetNodeId)
                                .orElse(candidateEdges.get(0).getTargetNodeId());
            }
        }

        return candidateEdges.get(0).getTargetNodeId();
    }

    public WorkflowExecution getExecution(String executionId) {
        return activeExecutions.get(executionId);
    }

    public List<WorkflowExecution> getActiveExecutions(Long userId) {
        return activeExecutions.values().stream()
                .filter(e -> e.getUserId().equals(userId))
                .toList();
    }

    public boolean pauseExecution(String executionId) {
        WorkflowExecution execution = activeExecutions.get(executionId);
        if (execution != null && execution.getStatus() == WorkflowExecution.ExecutionStatus.RUNNING) {
            execution.markPaused();
            return true;
        }
        return false;
    }

    public boolean cancelExecution(String executionId) {
        WorkflowExecution execution = activeExecutions.get(executionId);
        if (execution != null) {
            execution.markCancelled();
            activeExecutions.remove(executionId);
            return true;
        }
        return false;
    }

    public List<WorkflowDefinition> getRegisteredWorkflows() {
        return new ArrayList<>(workflowRegistry.values());
    }

    public WorkflowDefinition getWorkflow(String workflowId) {
        return workflowRegistry.get(workflowId);
    }
}