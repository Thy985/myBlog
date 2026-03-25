package com.xingchen.backend.agent;

import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

/**
 * Agent 执行器
 *
 * 执行多步骤任务，支持超时控制
 * - 每个步骤执行超时：30 秒
 * - 整体任务超时：5 分钟
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentExecutor {

    private final AgentWorkflow workflow;
    private final AIService aiService;

    // 最大重试次数
    private static final int MAX_RETRIES = 2;
    // 最大步骤数
    private static final int MAX_STEPS = 10;

    // 超时配置
    private static final long STEP_TIMEOUT_SECONDS = 30;  // 每个步骤超时
    private static final long TASK_TIMEOUT_MINUTES = 5;   // 整体任务超时

    // 线程池用于执行带超时的任务
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("agent-executor-" + t.getId());
        t.setDaemon(true);
        return t;
    });

    /**
     * 执行任务
     */
    public AgentResult execute(String task, AgentState state) {
        log.info("开始执行任务: sessionId={}, task={}", state.getSessionId(), task);

        state.setCurrentTask(task);
        state.updateStatus(AgentState.TaskStatus.PLANNING);

        // 创建整体任务超时控制
        Future<AgentResult> taskFuture = executorService.submit(() -> executeInternal(task, state));

        try {
            return taskFuture.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("任务执行超时（{}分钟）: sessionId={}", TASK_TIMEOUT_MINUTES, state.getSessionId());
            taskFuture.cancel(true);
            cleanupResources(state);
            state.updateStatus(AgentState.TaskStatus.FAILED);
            return AgentResult.failure("任务执行超时，请稍后重试或简化任务描述");
        } catch (InterruptedException e) {
            log.error("任务执行被中断: sessionId={}", state.getSessionId());
            Thread.currentThread().interrupt();
            cleanupResources(state);
            state.updateStatus(AgentState.TaskStatus.FAILED);
            return AgentResult.failure("任务执行被中断");
        } catch (ExecutionException e) {
            log.error("任务执行异常: sessionId={}", state.getSessionId(), e.getCause());
            cleanupResources(state);
            state.updateStatus(AgentState.TaskStatus.FAILED);
            return AgentResult.failure("执行异常: " + e.getCause().getMessage());
        }
    }

    /**
     * 内部执行逻辑
     */
    private AgentResult executeInternal(String task, AgentState state) {
        try {
            // 1. 规划步骤
            List<AgentWorkflow.TaskStep> steps = executeWithStepTimeout(
                () -> workflow.planSteps(task, state),
                "任务规划"
            );

            if (steps == null || steps.isEmpty()) {
                return AgentResult.failure("任务规划失败，无法分解步骤");
            }

            state.setTotalSteps(steps.size());
            log.info("任务规划完成: {} 个步骤", steps.size());

            // 2. 执行步骤
            state.updateStatus(AgentState.TaskStatus.EXECUTING);

            for (AgentWorkflow.TaskStep step : steps) {
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("任务执行被中断");
                    state.updateStatus(AgentState.TaskStatus.FAILED);
                    return AgentResult.failure("任务执行被中断");
                }

                if (state.getCurrentStep() >= MAX_STEPS) {
                    log.warn("达到最大步骤数限制");
                    break;
                }

                AgentWorkflow.StepResult result = executeStepWithRetryAndTimeout(step, state);

                if (result == null) {
                    state.updateStatus(AgentState.TaskStatus.FAILED);
                    return AgentResult.failure("步骤 " + step.getIndex() + " 执行超时");
                }

                if (result.success()) {
                    state.setIntermediateResult("step_" + step.getIndex(), result.result());
                    state.addTurn("执行步骤 " + step.getIndex(), result.result());
                } else {
                    state.updateStatus(AgentState.TaskStatus.FAILED);
                    return AgentResult.failure("步骤 " + step.getIndex() + " 执行失败: " + result.error());
                }

                state.nextStep();
            }

            // 3. 反思结果
            state.updateStatus(AgentState.TaskStatus.COMPLETED);
            Boolean success = executeWithStepTimeout(
                () -> workflow.reflect(state),
                "结果反思"
            );

            if (success != null && success) {
                String finalResult = generateFinalResult(state);
                return AgentResult.success(finalResult, state);
            } else {
                return AgentResult.failure("任务执行结果不符合预期");
            }

        } catch (Exception e) {
            log.error("任务执行异常", e);
            state.updateStatus(AgentState.TaskStatus.FAILED);
            return AgentResult.failure("执行异常: " + e.getMessage());
        }
    }

    /**
     * 带超时的步骤执行
     *
     * @param callable 执行逻辑
     * @param stepName 步骤名称（用于日志）
     * @param <T> 返回类型
     * @return 执行结果，超时返回 null
     */
    private <T> T executeWithStepTimeout(Callable<T> callable, String stepName) {
        Future<T> future = executorService.submit(callable);
        try {
            return future.get(STEP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("步骤 [{}] 执行超时（{}秒）", stepName, STEP_TIMEOUT_SECONDS);
            future.cancel(true);
            return null;
        } catch (InterruptedException e) {
            log.error("步骤 [{}] 执行被中断", stepName);
            future.cancel(true);
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            log.error("步骤 [{}] 执行异常: {}", stepName, e.getCause().getMessage());
            return null;
        }
    }

    /**
     * 带重试和超时的步骤执行
     */
    private AgentWorkflow.StepResult executeStepWithRetryAndTimeout(AgentWorkflow.TaskStep step, AgentState state) {
        int retries = 0;

        while (retries <= MAX_RETRIES) {
            final int currentRetry = retries;

            AgentWorkflow.StepResult result = executeWithStepTimeout(
                () -> workflow.executeStep(step, state),
                "步骤 " + step.getIndex() + (currentRetry > 0 ? " (重试 " + currentRetry + ")" : "")
            );

            if (result == null) {
                // 超时，尝试重试
                retries++;
                if (retries <= MAX_RETRIES) {
                    log.warn("步骤 {} 执行超时，第 {} 次重试", step.getIndex(), retries);
                    try {
                        Thread.sleep(1000); // 重试前等待 1 秒
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
                continue;
            }

            if (result.success()) {
                return result;
            }

            retries++;
            if (retries <= MAX_RETRIES) {
                log.warn("步骤 {} 执行失败，第 {} 次重试: {}", step.getIndex(), retries, result.error());
                try {
                    Thread.sleep(1000); // 重试前等待 1 秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return result;
                }
            }
        }

        return new AgentWorkflow.StepResult(false, null, "达到最大重试次数");
    }

    /**
     * 清理资源
     */
    private void cleanupResources(AgentState state) {
        log.info("清理任务资源: sessionId={}", state.getSessionId());
        try {
            // 清理中间结果，释放内存
            if (state.getIntermediateResults() != null) {
                state.getIntermediateResults().clear();
            }
            // 清理历史记录
            if (state.getDialogueHistory() != null) {
                state.getDialogueHistory().clear();
            }
        } catch (Exception e) {
            log.warn("清理资源时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 生成最终结果
     */
    private String generateFinalResult(AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 任务执行结果\n\n");
        sb.append("任务：").append(state.getCurrentTask()).append("\n\n");

        sb.append("### 执行步骤\n");
        for (int i = 1; i <= state.getCurrentStep(); i++) {
            Object result = state.getIntermediateResult("step_" + i);
            if (result != null) {
                sb.append("**步骤 ").append(i).append("**\n");
                sb.append(result.toString()).append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 关闭执行器（应用关闭时调用）
     */
    public void shutdown() {
        log.info("关闭 AgentExecutor 线程池");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 执行结果
     */
    public record AgentResult(boolean success, String result, String error, AgentState finalState) {
        public static AgentResult success(String result, AgentState state) {
            return new AgentResult(true, result, null, state);
        }

        public static AgentResult failure(String error) {
            return new AgentResult(false, null, error, null);
        }
    }
}
