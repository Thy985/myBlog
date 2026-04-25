package com.xingchen.backend.workflow;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service("workflowTaskScheduler")
@RequiredArgsConstructor
@Slf4j
public class TaskScheduler {

    private final WorkflowEngine workflowEngine;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private final Map<String, ScheduledTaskWrapper> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, CronTask> cronTasks = new ConcurrentHashMap<>();
    private final Map<String, Long> taskExecutionCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private static final String SCHEDULE_LOCK_PREFIX = "workflow:schedule:lock:";
    private static final String TASK_RESULT_PREFIX = "workflow:task:result:";
    private static final String TASK_META_PREFIX = "workflow:task:meta:";

    @PostConstruct
    public void init() {
        log.info("TaskScheduler 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        scheduledTasks.values().forEach(ScheduledTaskWrapper::cancel);
        cronTasks.values().forEach(CronTask::cancel);
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("调度线程池未能在5秒内终止");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduledTasks.clear();
        cronTasks.clear();
        log.info("TaskScheduler 已清理");
    }

    public String scheduleOnce(String workflowId, Long userId, String sessionId,
                               Map<String, Object> context, long delayMs) {
        String taskId = UUID.randomUUID().toString();

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                executeTask(taskId, workflowId, userId, sessionId, context);
            } catch (Exception e) {
                log.error("延迟任务执行失败: taskId={}", taskId, e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);

        ScheduledTaskWrapper task = new ScheduledTaskWrapper(taskId, workflowId, userId, sessionId,
                TaskType.ONCE, System.currentTimeMillis() + delayMs, future);
        scheduledTasks.put(taskId, task);

        log.info("一次性任务已调度: taskId={}, workflowId={}, delay={}ms",
                taskId, workflowId, delayMs);

        meterRegistry.counter("workflow.task.scheduled", "type", "once").increment();
        return taskId;
    }

    public String scheduleRepeating(String workflowId, Long userId, String sessionId,
                                    Map<String, Object> context, long intervalMs) {
        String taskId = UUID.randomUUID().toString();

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                executeTask(taskId, workflowId, userId, sessionId, context);
            } catch (Exception e) {
                log.error("重复任务执行失败: taskId={}", taskId, e);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        ScheduledTaskWrapper task = new ScheduledTaskWrapper(taskId, workflowId, userId, sessionId,
                TaskType.REPEATING, System.currentTimeMillis() + intervalMs, future);
        scheduledTasks.put(taskId, task);

        log.info("重复任务已调度: taskId={}, workflowId={}, interval={}ms",
                taskId, workflowId, intervalMs);

        meterRegistry.counter("workflow.task.scheduled", "type", "repeating").increment();
        return taskId;
    }

    public String scheduleCron(String workflowId, Long userId, String sessionId,
                               Map<String, Object> context, String cronExpression) {
        String taskId = UUID.randomUUID().toString();

        CronTask cronTask = new CronTask(taskId, workflowId, userId, sessionId,
                context, cronExpression);
        cronTasks.put(taskId, cronTask);

        log.info("Cron任务已调度: taskId={}, workflowId={}, cron={}",
                taskId, workflowId, cronExpression);

        meterRegistry.counter("workflow.task.scheduled", "type", "cron").increment();
        return taskId;
    }

    public boolean cancelTask(String taskId) {
        ScheduledTaskWrapper scheduledTask = scheduledTasks.remove(taskId);
        if (scheduledTask != null) {
            scheduledTask.cancel();
            log.info("任务已取消: taskId={}", taskId);
            meterRegistry.counter("workflow.task.cancelled").increment();
            return true;
        }

        CronTask cronTask = cronTasks.remove(taskId);
        if (cronTask != null) {
            cronTask.cancel();
            log.info("Cron任务已取消: taskId={}", taskId);
            meterRegistry.counter("workflow.task.cancelled").increment();
            return true;
        }

        return false;
    }

    public List<ScheduledTaskInfo> getScheduledTasks(Long userId) {
        List<ScheduledTaskInfo> result = new ArrayList<>();

        scheduledTasks.values().stream()
                .filter(t -> t.userId.equals(userId))
                .forEach(t -> result.add(convertToInfo(t)));

        cronTasks.values().stream()
                .filter(t -> t.userId.equals(userId))
                .forEach(t -> result.add(convertCronToInfo(t)));

        return result;
    }

    public TaskExecutionResult getTaskResult(String taskId) {
        String resultJson = redisTemplate.opsForValue().get(TASK_RESULT_PREFIX + taskId);
        if (resultJson != null) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        resultJson, TaskExecutionResult.class);
            } catch (Exception e) {
                log.error("解析任务结果失败: taskId={}", taskId, e);
            }
        }
        return null;
    }

    private void executeTask(String taskId, String workflowId, Long userId,
                             String sessionId, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        String lockKey = SCHEDULE_LOCK_PREFIX + taskId;

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofMinutes(5));
            if (Boolean.FALSE.equals(acquired)) {
                log.warn("任务正在执行中，跳过: taskId={}", taskId);
                return;
            }

            log.info("开始执行调度任务: taskId={}, workflowId={}", taskId, workflowId);

            workflowEngine.startWorkflow(workflowId, userId, sessionId, context);

            taskExecutionCounts.merge(taskId, 1L, Long::sum);

            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("workflow.task.execution").record(Duration.ofMillis(duration));
            meterRegistry.counter("workflow.task.executed").increment();

            log.info("调度任务执行完成: taskId={}, duration={}ms", taskId, duration);

        } catch (Exception e) {
            log.error("调度任务执行失败: taskId={}", taskId, e);
            meterRegistry.counter("workflow.task.failed").increment();

            saveTaskResult(taskId, false, e.getMessage(), null);

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void saveTaskResult(String taskId, boolean success, String error, Map<String, Object> result) {
        try {
            TaskExecutionResult executionResult = new TaskExecutionResult();
            executionResult.setTaskId(taskId);
            executionResult.setSuccess(success);
            executionResult.setError(error);
            executionResult.setResult(result);
            executionResult.setExecutedAt(Instant.now());

            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(executionResult);
            redisTemplate.opsForValue().set(TASK_RESULT_PREFIX + taskId, json, Duration.ofHours(24));
        } catch (Exception e) {
            log.error("保存任务结果失败: taskId={}", taskId, e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredTasks() {
        long now = System.currentTimeMillis();

        var expiredTaskIds = scheduledTasks.entrySet().stream()
                .filter(e -> e.getValue().type == TaskType.ONCE &&
                        e.getValue().scheduledTime < now - 60000)
                .map(Map.Entry::getKey)
                .toList();

        expiredTaskIds.forEach(scheduledTasks::remove);

        if (!expiredTaskIds.isEmpty()) {
            log.info("清理过期任务: count={}", expiredTaskIds.size());
        }
    }

    private ScheduledTaskInfo convertToInfo(ScheduledTaskWrapper task) {
        ScheduledTaskInfo info = new ScheduledTaskInfo();
        info.setTaskId(task.taskId);
        info.setWorkflowId(task.workflowId);
        info.setUserId(task.userId);
        info.setTaskType(task.type.name());
        info.setScheduledTime(task.scheduledTime);
        info.setNextExecutionTime(task.scheduledTime);
        info.setExecutionCount(taskExecutionCounts.getOrDefault(task.taskId, 0L));
        info.setStatus(task.future.isDone() ? "COMPLETED" : "RUNNING");
        return info;
    }

    private ScheduledTaskInfo convertCronToInfo(CronTask task) {
        ScheduledTaskInfo info = new ScheduledTaskInfo();
        info.setTaskId(task.taskId);
        info.setWorkflowId(task.workflowId);
        info.setUserId(task.userId);
        info.setTaskType("CRON");
        info.setCronExpression(task.cronExpression);
        info.setNextExecutionTime(calculateNextCronExecution(task.cronExpression));
        info.setExecutionCount(taskExecutionCounts.getOrDefault(task.taskId, 0L));
        info.setStatus("SCHEDULED");
        return info;
    }

    private long calculateNextCronExecution(String cronExpression) {
        return System.currentTimeMillis() + 60000;
    }

    public static class ScheduledTaskWrapper {
        private final String taskId;
        private final String workflowId;
        private final Long userId;
        private final String sessionId;
        private final TaskType type;
        private final long scheduledTime;
        private final ScheduledFuture<?> future;

        public ScheduledTaskWrapper(String taskId, String workflowId, Long userId, String sessionId,
                             TaskType type, long scheduledTime, ScheduledFuture<?> future) {
            this.taskId = taskId;
            this.workflowId = workflowId;
            this.userId = userId;
            this.sessionId = sessionId;
            this.type = type;
            this.scheduledTime = scheduledTime;
            this.future = future;
        }

        public void cancel() {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        }
    }

    public enum TaskType {
        ONCE,
        REPEATING,
        CRON
    }

    public static class CronTask {
        private final String taskId;
        private final String workflowId;
        private final Long userId;
        private final String sessionId;
        private final Map<String, Object> context;
        private final String cronExpression;
        private volatile boolean cancelled = false;

        public CronTask(String taskId, String workflowId, Long userId, String sessionId,
                        Map<String, Object> context, String cronExpression) {
            this.taskId = taskId;
            this.workflowId = workflowId;
            this.userId = userId;
            this.sessionId = sessionId;
            this.context = context;
            this.cronExpression = cronExpression;
        }

        public void cancel() {
            this.cancelled = true;
        }
    }

    @lombok.Data
    public static class ScheduledTaskInfo {
        private String taskId;
        private String workflowId;
        private Long userId;
        private String taskType;
        private long scheduledTime;
        private long nextExecutionTime;
        private String cronExpression;
        private long executionCount;
        private String status;
    }

    @lombok.Data
    public static class TaskExecutionResult {
        private String taskId;
        private boolean success;
        private String error;
        private Map<String, Object> result;
        private Instant executedAt;
    }
}