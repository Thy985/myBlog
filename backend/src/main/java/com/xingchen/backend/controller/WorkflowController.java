package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.workflow.WorkflowDefinition;
import com.xingchen.backend.workflow.WorkflowEngine;
import com.xingchen.backend.workflow.WorkflowExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflow")
@Slf4j
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    @PostMapping("/start")
    @SaCheckLogin
    public Result<WorkflowStartResponse> startWorkflow(@RequestBody WorkflowStartRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            Map<String, Object> context = request.context() != null ? request.context() : Map.of();
            context = new java.util.HashMap<>(context);
            context.put("userId", userId);

            WorkflowExecution execution = workflowEngine.startWorkflow(
                    request.workflowName(),
                    userId,
                    null,
                    context
            );

            return Result.success(new WorkflowStartResponse(
                    execution.getExecutionId(),
                    request.workflowName(),
                    execution.getStatus().name(),
                    "工作流已启动"
            ));
        } catch (IllegalArgumentException e) {
            log.error("Failed to start workflow: {}", request.workflowName(), e);
            return Result.fail(400, e.getMessage());
        }
    }

    @GetMapping("/{executionId}")
    @SaCheckLogin
    public Result<WorkflowStatusResponse> getWorkflowStatus(@PathVariable String executionId) {
        WorkflowExecution execution = workflowEngine.getExecution(executionId);

        if (execution == null) {
            return Result.fail(404, "Workflow execution not found");
        }

        return Result.success(WorkflowStatusResponse.fromExecution(execution));
    }

    @GetMapping("/active")
    @SaCheckLogin
    public Result<List<WorkflowStatusResponse>> getActiveWorkflows() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<WorkflowExecution> activeExecutions = workflowEngine.getActiveExecutions(userId);

        List<WorkflowStatusResponse> responses = activeExecutions.stream()
                .filter(e -> e.getStatus() == WorkflowExecution.ExecutionStatus.RUNNING ||
                        e.getStatus() == WorkflowExecution.ExecutionStatus.PAUSED)
                .map(WorkflowStatusResponse::fromExecution)
                .collect(Collectors.toList());

        return Result.success(responses);
    }

    @GetMapping("/list")
    @SaCheckLogin
    public Result<List<WorkflowListResponse>> listWorkflows() {
        List<WorkflowDefinition> definitions = workflowEngine.getRegisteredWorkflows();

        List<WorkflowListResponse> responses = definitions.stream()
                .map(d -> new WorkflowListResponse(
                        d.getWorkflowId(),
                        d.getName(),
                        d.getDescription() != null ? d.getDescription() : ""
                ))
                .collect(Collectors.toList());

        return Result.success(responses);
    }

    @PostMapping("/{executionId}/pause")
    @SaCheckLogin
    public Result<String> pauseWorkflow(@PathVariable String executionId) {
        boolean paused = workflowEngine.pauseExecution(executionId);
        if (paused) {
            return Result.success("Workflow paused");
        } else {
            return Result.fail(400, "Cannot pause workflow");
        }
    }

    @PostMapping("/{executionId}/resume")
    @SaCheckLogin
    public Result<String> resumeWorkflow(@PathVariable String executionId) {
        workflowEngine.startWorkflowAsync(executionId, StpUtil.getLoginIdAsLong(), null, null, null);
        return Result.success("Workflow resumed");
    }

    @PostMapping("/{executionId}/cancel")
    @SaCheckLogin
    public Result<String> cancelWorkflow(@PathVariable String executionId) {
        boolean cancelled = workflowEngine.cancelExecution(executionId);
        if (cancelled) {
            return Result.success("Workflow cancelled");
        } else {
            return Result.fail(400, "Cannot cancel workflow");
        }
    }

    public record WorkflowStartRequest(
            String workflowName,
            String niche,
            Long articleId,
            Map<String, Object> context
    ) {}

    public record WorkflowStartResponse(
            String executionId,
            String workflowName,
            String status,
            String message
    ) {}

    public record WorkflowStatusResponse(
            String executionId,
            String workflowId,
            String currentNodeId,
            String status,
            long startTime,
            long durationMs,
            Object result,
            String error
    ) {
        public static WorkflowStatusResponse fromExecution(WorkflowExecution execution) {
            return new WorkflowStatusResponse(
                    execution.getExecutionId(),
                    execution.getWorkflowId(),
                    execution.getCurrentNodeId(),
                    execution.getStatus().name(),
                    execution.getStartedAt() != null ? execution.getStartedAt().toEpochMilli() : 0,
                    execution.getCompletedAt() != null && execution.getStartedAt() != null ?
                            execution.getCompletedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli() : 0,
                    null,
                    execution.getErrorMessage()
            );
        }
    }

    public record WorkflowListResponse(
            String id,
            String name,
            String description
    ) {}
}
