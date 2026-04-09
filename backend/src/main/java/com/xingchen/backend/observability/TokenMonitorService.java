package com.xingchen.backend.observability;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 消耗监控服务
 * <p>
 * 按用户统计 Token 消耗，提供阈值告警和预算管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMonitorService {

    private final MetricsService metricsService;

    // 用户日 Token 消耗缓存
    private final Cache<Long, UserTokenStats> userTokenCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofHours(24))
            .build();

    // 告警阈值配置
    @Value("${ai.token.alert.daily-threshold:10000}")
    private int dailyAlertThreshold;

    @Value("${ai.token.alert.hourly-threshold:2000}")
    private int hourlyAlertThreshold;

    @Value("${ai.token.budget.daily:50000}")
    private int dailyBudget;

    // 已发送告警的用户（避免重复告警）
    private final Map<Long, AlertStatus> alertStatusMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Token 监控服务初始化完成，日阈值: {}, 小时阈值: {}", dailyAlertThreshold, hourlyAlertThreshold);
    }

    /**
     * 记录用户 Token 消耗
     *
     * @param userId      用户ID
     * @param inputTokens  输入 Token 数
     * @param outputTokens 输出 Token 数
     * @param model       模型名称
     */
    public void recordTokenConsumption(Long userId, int inputTokens, int outputTokens, String model) {
        if (userId == null) return;

        int totalTokens = inputTokens + outputTokens;

        // 获取或创建用户统计
        UserTokenStats stats = userTokenCache.get(userId, id -> new UserTokenStats(id, LocalDate.now()));
        stats.addTokens(totalTokens, inputTokens, outputTokens, model);

        // 记录到 Micrometer
        metricsService.recordAICall(model, true, 0, inputTokens, outputTokens);

        // 检查是否需要告警
        checkAlertThresholds(userId, stats);

        log.debug("用户 {} Token 消耗: {} (输入: {}, 输出: {}), 模型: {}",
                userId, totalTokens, inputTokens, outputTokens, model);
    }

    /**
     * 检查告警阈值
     */
    private void checkAlertThresholds(Long userId, UserTokenStats stats) {
        AlertStatus alertStatus = alertStatusMap.computeIfAbsent(userId, k -> new AlertStatus());

        // 检查小时阈值
        int hourlyTokens = stats.getHourlyTokens();
        if (hourlyTokens > hourlyAlertThreshold && !alertStatus.isHourlyAlertSent()) {
            sendAlert(userId, "HOURLY_THRESHOLD", hourlyTokens, hourlyAlertThreshold);
            alertStatus.setHourlyAlertSent(true);
        }

        // 检查日阈值
        int dailyTokens = stats.getDailyTokens();
        if (dailyTokens > dailyAlertThreshold && !alertStatus.isDailyAlertSent()) {
            sendAlert(userId, "DAILY_THRESHOLD", dailyTokens, dailyAlertThreshold);
            alertStatus.setDailyAlertSent(true);
        }

        // 检查预算（更严重的告警）
        if (dailyTokens > dailyBudget && !alertStatus.isBudgetAlertSent()) {
            sendAlert(userId, "BUDGET_EXCEEDED", dailyTokens, dailyBudget);
            alertStatus.setBudgetAlertSent(true);
        }
    }

    /**
     * 发送告警
     */
    private void sendAlert(Long userId, String alertType, int currentUsage, int threshold) {
        log.warn("🚨 Token 告警 - 用户: {}, 类型: {}, 当前: {}, 阈值: {}",
                userId, alertType, currentUsage, threshold);

        // 这里可以集成邮件、短信、钉钉等告警渠道

        // 记录安全事件
        metricsService.recordSecurityEvent("TOKEN_THRESHOLD_EXCEEDED", "WARNING");
    }

    /**
     * 获取用户 Token 统计
     */
    public UserTokenStats getUserStats(Long userId) {
        return userTokenCache.getIfPresent(userId);
    }

    /**
     * 检查用户是否超出预算
     */
    public boolean isOverBudget(Long userId) {
        UserTokenStats stats = userTokenCache.getIfPresent(userId);
        if (stats == null) return false;
        return stats.getDailyTokens() > dailyBudget;
    }

    /**
     * 获取用户剩余预算
     */
    public int getRemainingBudget(Long userId) {
        UserTokenStats stats = userTokenCache.getIfPresent(userId);
        if (stats == null) return dailyBudget;
        return Math.max(0, dailyBudget - stats.getDailyTokens());
    }

    /**
     * 重置用户告警状态（新的一天）
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨重置
    public void resetDailyStats() {
        log.info("重置每日 Token 统计和告警状态");
        userTokenCache.invalidateAll();
        alertStatusMap.clear();
    }

    /**
     * 每小时重置小时告警
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void resetHourlyAlerts() {
        alertStatusMap.values().forEach(status -> status.setHourlyAlertSent(false));
    }

    // ========== 内部类 ==========

    /**
     * 用户 Token 统计
     */
    @Data
    public static class UserTokenStats {
        private final Long userId;
        private final LocalDate date;
        private final AtomicLong dailyTokens = new AtomicLong(0);
        private final AtomicLong dailyInputTokens = new AtomicLong(0);
        private final AtomicLong dailyOutputTokens = new AtomicLong(0);
        private final Map<String, AtomicLong> modelUsage = new ConcurrentHashMap<>();
        private final Map<Integer, AtomicLong> hourlyUsage = new ConcurrentHashMap<>(); // 小时 -> Token 数

        public UserTokenStats(Long userId, LocalDate date) {
            this.userId = userId;
            this.date = date;
        }

        public void addTokens(int total, int input, int output, String model) {
            dailyTokens.addAndGet(total);
            dailyInputTokens.addAndGet(input);
            dailyOutputTokens.addAndGet(output);

            modelUsage.computeIfAbsent(model, k -> new AtomicLong(0)).addAndGet(total);

            int currentHour = java.time.LocalDateTime.now().getHour();
            hourlyUsage.computeIfAbsent(currentHour, k -> new AtomicLong(0)).addAndGet(total);
        }

        public int getDailyTokens() {
            return dailyTokens.intValue();
        }

        public int getHourlyTokens() {
            int currentHour = java.time.LocalDateTime.now().getHour();
            AtomicLong hourly = hourlyUsage.get(currentHour);
            return hourly != null ? hourly.intValue() : 0;
        }

        public Map<String, Long> getModelUsage() {
            Map<String, Long> result = new ConcurrentHashMap<>();
            modelUsage.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }
    }

    /**
     * 告警状态
     */
    @Data
    private static class AlertStatus {
        private volatile boolean hourlyAlertSent = false;
        private volatile boolean dailyAlertSent = false;
        private volatile boolean budgetAlertSent = false;
    }
}
