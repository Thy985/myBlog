package com.xingchen.backend.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 熔断降级配置
 * <p>
 * 为 AI 服务调用提供熔断保护和降级策略
 */
@Configuration
public class ResilienceConfig {

    /**
     * AI 服务熔断器配置
     * <p>
     * 当 AI 服务连续失败时，自动熔断并切换到备用策略
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // 失败率阈值：50% 失败即触发熔断
                .failureRateThreshold(50)
                // 慢调用阈值：超过 10 秒视为慢调用
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                // 慢调用比例阈值：50% 慢调用触发熔断
                .slowCallRateThreshold(50)
                // 熔断持续时间：30 秒后尝试恢复
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // 半开状态允许的请求数：5 个
                .permittedNumberOfCallsInHalfOpenState(5)
                // 滑动窗口大小：100 个请求
                .slidingWindowSize(100)
                // 最小调用次数：10 次才开始统计
                .minimumNumberOfCalls(10)
                // 自动从半开转为关闭
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // 忽略的异常（不计入失败）
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * AI 服务重试配置
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                // 最大重试次数：3 次
                .maxAttempts(3)
                // 重试间隔：1 秒
                .waitDuration(Duration.ofSeconds(1))
                // 只对特定异常重试
                .retryExceptions(RuntimeException.class, java.net.SocketTimeoutException.class)
                // 忽略的异常（不重试）
                .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * 获取 AI 服务熔断器实例
     */
    @Bean(name = "aiServiceCircuitBreaker")
    public io.github.resilience4j.circuitbreaker.CircuitBreaker aiServiceCircuitBreaker(
            CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("aiService");
    }

    /**
     * 获取 AI 服务重试器实例
     */
    @Bean(name = "aiServiceRetry")
    public io.github.resilience4j.retry.Retry aiServiceRetry(RetryRegistry registry) {
        return registry.retry("aiService");
    }
}
