package com.xingchen.backend.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流配置
 * 支持全局限流和用户级限流
 */
@Configuration
@Slf4j
public class RateLimitConfig {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final Map<Long, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    public RateLimitConfig() {
        this.rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return rateLimiterRegistry;
    }

    /**
     * 获取全局 AI 调用限流器
     */
    @Bean(name = "globalAiRateLimiter")
    public RateLimiter globalAiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)           // 每秒 100 次
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build();

        return rateLimiterRegistry.rateLimiter("global-ai", config);
    }

    /**
     * 获取用户级限流器
     */
    public RateLimiter getUserRateLimiter(Long userId) {
        return userRateLimiters.computeIfAbsent(userId, id -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(20)            // 每分钟 20 次
                    .limitRefreshPeriod(Duration.ofMinutes(1))
                    .timeoutDuration(Duration.ofMillis(500))
                    .build();

            return rateLimiterRegistry.rateLimiter("user-" + id, config);
        });
    }

    /**
     * 清理不活跃用户的限流器
     */
    public void cleanupInactiveUserLimiters() {
        // 可以定期清理，防止内存泄漏
        log.debug("清理用户限流器，当前数量: {}", userRateLimiters.size());
    }
}