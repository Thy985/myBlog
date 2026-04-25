package com.xingchen.backend.workflow.nodes;

import com.xingchen.backend.workflow.TaskNode;
import com.xingchen.backend.workflow.WorkflowDefinition;
import com.xingchen.backend.workflow.WorkflowExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class ParallelNode implements TaskNode {

    private static final String NODE_ID = "parallel";
    private static final String NODE_NAME = "并行执行节点";

    private final ExecutorService executor = Executors.newCachedThreadPool();

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
        return WorkflowDefinition.WorkflowNode.NodeType.PARALLEL;
    }

    @Override
    public CompletableFuture<NodeResult> execute(WorkflowExecution execution, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> branches = (List<Map<String, Object>>) input.get("branches");
                if (branches == null || branches.isEmpty()) {
                    return NodeResult.failure(getNodeId(), "并行分支为空");
                }

                log.info("开始并行执行 {} 个分支, executionId={}", branches.size(), execution.getExecutionId());

                List<CompletableFuture<NodeResult>> futures = new ArrayList<>();
                for (int i = 0; i < branches.size(); i++) {
                    final int branchIndex = i;
                    final Map<String, Object> branchInput = branches.get(i);
                    CompletableFuture<NodeResult> future = CompletableFuture.supplyAsync(() -> {
                        String branchName = extractString(branchInput, "name", "Branch-" + branchIndex);
                        log.info("执行并行分支: {}, executionId={}", branchName, execution.getExecutionId());
                        return NodeResult.success(getNodeId() + "_branch_" + branchIndex, Map.of(
                                "branchIndex", branchIndex,
                                "branchName", branchName,
                                "result", "executed"
                        ));
                    }, executor);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                List<Map<String, Object>> results = new ArrayList<>();
                int successCount = 0;
                int failureCount = 0;

                for (CompletableFuture<NodeResult> future : futures) {
                    NodeResult result = future.join();
                    results.add(Map.of(
                            "success", result.success(),
                            "output", result.output() != null ? result.output() : Map.of()
                    ));
                    if (result.success()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }

                log.info("并行执行完成: success={}, failed={}, executionId={}",
                        successCount, failureCount, execution.getExecutionId());

                return NodeResult.success(getNodeId(), Map.of(
                        "totalBranches", branches.size(),
                        "successCount", successCount,
                        "failureCount", failureCount,
                        "results", results
                ));

            } catch (Exception e) {
                log.error("并行执行失败", e);
                return NodeResult.failure(getNodeId(), e.getMessage());
            }
        });
    }

    private String extractString(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}