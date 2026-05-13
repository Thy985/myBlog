package com.xingchen.backend.agent.scheduler;

import com.xingchen.backend.agent.growth.GrowthWorkflowOrchestrator;
import com.xingchen.backend.service.GrowthOrchestrator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service("growthScheduler")
@RequiredArgsConstructor
@Slf4j
public class GrowthScheduler {

    private final GrowthOrchestrator growthOrchestrator;
    private final GrowthWorkflowOrchestrator growthWorkflowOrchestrator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    private ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, GrowthTaskContext> activeTasks = new ConcurrentHashMap<>();
    private final Map<String, GrowthScheduleConfig> scheduleConfigs = new ConcurrentHashMap<>();
    private final AtomicLong taskSequence = new AtomicLong(0);

    private static final String GROWTH_SCHEDULE_LOCK = "growth:scheduler:lock:";
    private static final String GROWTH_SCHEDULE_PREFIX = "growth:schedule:";
    private static final String GROWTH_LAST_RUN_PREFIX = "growth:lastrun:";
    private static final String GROWTH_TASK_HISTORY = "growth:task:history:";
    private static final String ACTIVE_USERS_KEY = "active:users";

    public static final String CYCLE_DAILY = "daily";
    public static final String CYCLE_WEEKLY = "weekly";
    public static final String CYCLE_MONTHLY = "monthly";

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(4);
        taskScheduler.setThreadNamePrefix("growth-scheduler-");
        taskScheduler.setErrorHandler(t -> log.error("Growth scheduler error", t));
        taskScheduler.initialize();

        initializeDefaultSchedules();
        restorePendingTasks();

        log.info("GrowthScheduler 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        if (taskScheduler != null) {
            taskScheduler.destroy();
        }
        activeTasks.clear();
        scheduleConfigs.clear();
        log.info("GrowthScheduler 已清理");
    }

    private void initializeDefaultSchedules() {
        scheduleConfigs.put(CYCLE_DAILY, new GrowthScheduleConfig(
                CYCLE_DAILY,
                "0 0 2 * * ?",
                "每日增长任务",
                true,
                24 * 60 * 60 * 1000L
        ));

        scheduleConfigs.put(CYCLE_WEEKLY, new GrowthScheduleConfig(
                CYCLE_WEEKLY,
                "0 0 3 ? * MON",
                "每周增长任务",
                true,
                7 * 24 * 60 * 60 * 1000L
        ));

        scheduleConfigs.put(CYCLE_MONTHLY, new GrowthScheduleConfig(
                CYCLE_MONTHLY,
                "0 0 4 1 * ?",
                "每月增长任务",
                true,
                30 * 24 * 60 * 60 * 1000L
        ));
    }

    private void restorePendingTasks() {
        try {
            Set<String> keys = redisTemplate.keys(GROWTH_SCHEDULE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                log.info("恢复 {} 个待执行任务", keys.size());
            }
        } catch (Exception e) {
            log.warn("恢复待执行任务失败", e);
        }
    }

    public String scheduleGrowthTask(Long userId, String cycle, Map<String, Object> options) {
        GrowthScheduleConfig config = scheduleConfigs.get(cycle);
        if (config == null) {
            throw new IllegalArgumentException("未知的增长周期: " + cycle);
        }

        String taskId = UUID.randomUUID().toString();
        GrowthTaskContext context = new GrowthTaskContext(
                taskId,
                userId,
                cycle,
                config,
                System.currentTimeMillis(),
                options
        );

        activeTasks.put(taskId, context);
        persistTask(taskId, context);

        Date nextRun = calculateNextRunFromCron(context.getCycle());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeGrowthTask(context),
                nextRun
        );

        context.setScheduledFuture(future);
        context.setNextExecutionTime(nextRun.getTime());

        log.info("增长任务已调度: taskId={}, userId={}, cycle={}, nextRun={}",
                taskId, userId, cycle, nextRun);

        meterRegistry.counter("growth.task.scheduled", "cycle", cycle).increment();

        return taskId;
    }

    public String scheduleOneTimeGrowthTask(Long userId, long delayMs, Map<String, Object> options) {
        String taskId = UUID.randomUUID().toString();

        GrowthScheduleConfig oneTimeConfig = new GrowthScheduleConfig(
                "onetime",
                null,
                "一次性增长任务",
                false,
                delayMs
        );

        GrowthTaskContext context = new GrowthTaskContext(
                taskId,
                userId,
                "onetime",
                oneTimeConfig,
                System.currentTimeMillis(),
                options
        );

        activeTasks.put(taskId, context);
        persistTask(taskId, context);

        Date nextRun = new Date(System.currentTimeMillis() + delayMs);
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeGrowthTask(context),
                nextRun
        );

        context.setScheduledFuture(future);
        context.setNextExecutionTime(nextRun.getTime());

        log.info("一次性增长任务已调度: taskId={}, userId={}, delay={}ms, nextRun={}",
                taskId, userId, delayMs, nextRun);

        return taskId;
    }

    public boolean cancelTask(String taskId) {
        GrowthTaskContext context = activeTasks.remove(taskId);
        if (context != null) {
            if (context.getScheduledFuture() != null) {
                boolean cancelled = context.getScheduledFuture().cancel(false);
                if (cancelled) {
                    log.info("增长任务已取消: taskId={}", taskId);
                    meterRegistry.counter("growth.task.cancelled").increment();
                    removePersistedTask(taskId);
                }
                return cancelled;
            }
            removePersistedTask(taskId);
            return true;
        }
        return false;
    }

    public List<GrowthTaskInfo> getUserTasks(Long userId) {
        return activeTasks.values().stream()
                .filter(t -> t.getUserId().equals(userId))
                .map(this::convertToInfo)
                .toList();
    }

    public GrowthTaskResult getTaskResult(String taskId) {
        GrowthTaskContext context = activeTasks.get(taskId);
        if (context != null) {
            return context.getLastResult();
        }
        return null;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledDailyGrowth() {
        executeScheduledGrowth(CYCLE_DAILY);
    }

    @Scheduled(cron = "0 0 3 ? * MON")
    public void scheduledWeeklyGrowth() {
        executeScheduledGrowth(CYCLE_WEEKLY);
    }

    @Scheduled(cron = "0 0 4 1 * ?")
    public void scheduledMonthlyGrowth() {
        executeScheduledGrowth(CYCLE_MONTHLY);
    }

    private void executeScheduledGrowth(String cycle) {
        log.info("开始执行定时增长任务: cycle={}", cycle);

        try {
            Set<Object> userIds = redisTemplate.opsForSet().members(ACTIVE_USERS_KEY);
            if (userIds == null || userIds.isEmpty()) {
                log.info("没有活跃用户，跳过增长任务");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (Object userIdObj : userIds) {
                try {
                    Long userId = userIdObj instanceof Long ? (Long) userIdObj :
                            Long.parseLong(userIdObj.toString());

                    if (canRunForUser(userId, cycle)) {
                        try {
                            GrowthWorkflowOrchestrator.GrowthResult result =
                                    growthWorkflowOrchestrator.executeGrowth闭环(userId, cycle);
                            if (result.isSuccess()) {
                                successCount++;
                            } else {
                                failCount++;
                            }
                        } catch (Exception e) {
                            log.error("用户 {} 增长闭环执行失败", userId, e);
                            failCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("用户 {} 定时任务执行失败", userIdObj, e);
                    failCount++;
                }
            }

            log.info("定时增长任务完成: cycle={}, success={}, failed={}",
                    cycle, successCount, failCount);

            meterRegistry.counter("growth.task.executed", "cycle", cycle, "status", "success")
                    .increment(successCount);
            meterRegistry.counter("growth.task.executed", "cycle", cycle, "status", "failed")
                    .increment(failCount);

        } catch (Exception e) {
            log.error("定时增长任务执行失败: cycle={}", cycle, e);
            meterRegistry.counter("growth.task.failed", "cycle", cycle).increment();
        }
    }

    private boolean canRunForUser(Long userId, String cycle) {
        String lastRunKey = GROWTH_LAST_RUN_PREFIX + cycle + ":" + userId;
        Object lastRun = redisTemplate.opsForValue().get(lastRunKey);

        if (lastRun == null) {
            return true;
        }

        GrowthScheduleConfig config = scheduleConfigs.get(cycle);
        if (config == null) {
            return true;
        }

        long lastRunTime;
        if (lastRun instanceof Long) {
            lastRunTime = (Long) lastRun;
        } else {
            lastRunTime = Long.parseLong(lastRun.toString());
        }

        long timeSinceLastRun = System.currentTimeMillis() - lastRunTime;
        return timeSinceLastRun >= config.getMinIntervalMs();
    }

    private void executeGrowthTask(GrowthTaskContext context) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String lockKey = GROWTH_SCHEDULE_LOCK + context.getTaskId();
        AtomicBoolean acquired = new AtomicBoolean(false);

        try {
            acquired.set(Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofMinutes(30))));

            if (!acquired.get()) {
                log.warn("增长任务正在执行中，跳过: taskId={}", context.getTaskId());
                return;
            }

            log.info("开始执行增长任务: taskId={}, userId={}, cycle={}",
                    context.getTaskId(), context.getUserId(), context.getCycle());

            GrowthWorkflowOrchestrator.GrowthResult result =
                    growthWorkflowOrchestrator.executeGrowth闭环(context.getUserId(), context.getCycle());

            context.setLastResult(new GrowthTaskResult(
                    result.isSuccess(),
                    result.getMessage(),
                    result.getPhase() != null ? result.getPhase().name() : "UNKNOWN",
                    Instant.now()
            ));

            if (result.isSuccess()) {
                String lastRunKey = GROWTH_LAST_RUN_PREFIX + context.getCycle() + ":" + context.getUserId();
                redisTemplate.opsForValue().set(lastRunKey, System.currentTimeMillis());
            }

            saveTaskHistory(context);

            log.info("增长任务执行完成: taskId={}, success={}, duration={}ms",
                    context.getTaskId(), result.isSuccess(),
                    context.getLastResult() != null ?
                            System.currentTimeMillis() - context.getCreateTime() : 0);

            if (!"onetime".equals(context.getCycle())) {
                scheduleNextRun(context);
            } else {
                activeTasks.remove(context.getTaskId());
                removePersistedTask(context.getTaskId());
            }

        } catch (Exception e) {
            log.error("增长任务执行失败: taskId={}", context.getTaskId(), e);

            context.setLastResult(new GrowthTaskResult(
                    false,
                    "执行失败: " + e.getMessage(),
                    "FAILED",
                    Instant.now()
            ));

            meterRegistry.counter("growth.task.failed").increment();

            if (!"onetime".equals(context.getCycle())) {
                scheduleNextRun(context);
            }

        } finally {
            if (acquired.get()) {
                redisTemplate.delete(lockKey);
            }
            sample.stop(meterRegistry.timer("growth.task.execution"));
        }
    }

    private void scheduleNextRun(GrowthTaskContext context) {
        GrowthScheduleConfig config = context.getConfig();
        if (config == null) {
            return;
        }

        Date nextRun = calculateNextRunFromCron(context.getCycle());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeGrowthTask(context),
                nextRun
        );

        context.setScheduledFuture(future);
        context.setNextExecutionTime(nextRun.getTime());

        log.info("任务下次执行时间: taskId={}, nextRun={}", context.getTaskId(), nextRun);
    }

    private Date calculateNextRunFromCron(String cycle) {
        try {
            GrowthScheduleConfig config = scheduleConfigs.get(cycle);
            if (config != null) {
                return new Date(System.currentTimeMillis() + config.getMinIntervalMs());
            }
        } catch (Exception e) {
            log.warn("计算下次执行时间失败: cycle={}", cycle, e);
        }
        return new Date(System.currentTimeMillis() + 60 * 1000);
    }

    private void persistTask(String taskId, GrowthTaskContext context) {
        try {
            String key = GROWTH_SCHEDULE_PREFIX + taskId;
            Map<String, Object> taskData = Map.of(
                    "taskId", context.getTaskId(),
                    "userId", context.getUserId(),
                    "cycle", context.getCycle(),
                    "createTime", context.getCreateTime(),
                    "options", context.getOptions() != null ? context.getOptions() : Map.of()
            );
            redisTemplate.opsForValue().set(key, taskData, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("任务持久化失败: taskId={}", taskId, e);
        }
    }

    private void removePersistedTask(String taskId) {
        try {
            redisTemplate.delete(GROWTH_SCHEDULE_PREFIX + taskId);
        } catch (Exception e) {
            log.warn("删除持久化任务失败: taskId={}", taskId, e);
        }
    }

    private void saveTaskHistory(GrowthTaskContext context) {
        try {
            String historyKey = GROWTH_TASK_HISTORY + context.getUserId();
            List<Object> history = redisTemplate.opsForList().range(historyKey, 0, -1);
            int maxHistory = 100;

            if (history == null) {
                history = new java.util.ArrayList<>();
            }

            Map<String, Object> record = Map.of(
                    "taskId", context.getTaskId(),
                    "cycle", context.getCycle(),
                    "executeTime", System.currentTimeMillis(),
                    "success", context.getLastResult() != null && context.getLastResult().isSuccess(),
                    "message", context.getLastResult() != null ? context.getLastResult().getMessage() : ""
            );

            history.add(record);

            while (history.size() > maxHistory) {
                history.remove(0);
            }

            redisTemplate.opsForList().rightPush(historyKey, history);
            redisTemplate.expire(historyKey, Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("保存任务历史失败: taskId={}", context.getTaskId(), e);
        }
    }

    private GrowthTaskInfo convertToInfo(GrowthTaskContext context) {
        GrowthTaskInfo info = new GrowthTaskInfo();
        info.setTaskId(context.getTaskId());
        info.setUserId(context.getUserId());
        info.setCycle(context.getCycle());
        info.setScheduleName(context.getConfig().getScheduleName());
        info.setCronExpression(context.getConfig().getCronExpression());
        info.setScheduledTime(context.getCreateTime());
        info.setNextExecutionTime(context.getNextExecutionTime());
        info.setStatus(determineStatus(context));
        info.setLastResult(context.getLastResult());
        return info;
    }

    private String determineStatus(GrowthTaskContext context) {
        if (context.getLastResult() != null) {
            return context.getLastResult().isSuccess() ? "COMPLETED" : "FAILED";
        }
        if (context.getScheduledFuture() != null) {
            if (context.getScheduledFuture().isCancelled()) {
                return "CANCELLED";
            }
            if (context.getScheduledFuture().isDone()) {
                return "DONE";
            }
            return "SCHEDULED";
        }
        return "PENDING";
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class GrowthScheduleConfig {
        private String cycle;
        private String cronExpression;
        private String scheduleName;
        private boolean enabled;
        private long minIntervalMs;
    }

    @lombok.Data
    private static class GrowthTaskContext {
        private String taskId;
        private Long userId;
        private String cycle;
        private GrowthScheduleConfig config;
        private long createTime;
        private Map<String, Object> options;
        private ScheduledFuture<?> scheduledFuture;
        private GrowthTaskResult lastResult;
        private long nextExecutionTime;

        public GrowthTaskContext(String taskId, Long userId, String cycle,
                                  GrowthScheduleConfig config, long createTime,
                                  Map<String, Object> options) {
            this.taskId = taskId;
            this.userId = userId;
            this.cycle = cycle;
            this.config = config;
            this.createTime = createTime;
            this.options = options;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class GrowthTaskResult {
        private boolean success;
        private String message;
        private String phase;
        private Instant timestamp;
    }

    @lombok.Data
    public static class GrowthTaskInfo {
        private String taskId;
        private Long userId;
        private String cycle;
        private String scheduleName;
        private String cronExpression;
        private long scheduledTime;
        private long nextExecutionTime;
        private String status;
        private GrowthTaskResult lastResult;
    }
}