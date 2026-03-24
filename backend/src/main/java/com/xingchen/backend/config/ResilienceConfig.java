package com.xingchen.backend.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 弹性配置
 * 统一配置熔断、重试、超时策略
 */
@Configuration
@Slf4j
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // 默认配置
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                    // 失败率阈值 50%
                .slowCallRateThreshold(80)                   // 慢调用阈值 80%
                .slowCallDurationThreshold(Duration.ofSeconds(10))  // 慢调用定义：>10s
                .permittedNumberOfCallsInHalfOpenState(3)    // 半开状态允许 3 次调用
                .slidingWindowSize(10)                       // 滑动窗口大小
                .minimumNumberOfCalls(5)                     // 最小调用次数
                .waitDurationInOpenState(Duration.ofSeconds(30))    // 熔断后等待 30s
                .automaticTransitionFromOpenToHalfOpenEnabled(true) // 自动转半开
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        
        // 为 AI 服务配置专门的熔断器
        registry.addConfiguration("aiChat", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build());

        registry.addConfiguration("aiStream", CircuitBreakerConfig.custom()
                .failureRateThreshold(60)  // 流式调用容错率更高
                .slowCallDurationThreshold(Duration.ofSeconds(30))
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .build());

        registry.addConfiguration("knowledgeBase", CircuitBreakerConfig.custom()
                .failureRateThreshold(70)  // 知识库容错率更高
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build());

        log.info("熔断器配置完成");
        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        // 默认重试配置
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .retryExceptions(
                        java.io.IOException.class,
                        java.net.SocketTimeoutException.class,
                        java.net.ConnectException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        SecurityException.class
                )
                .build();

        RetryRegistry registry = RetryRegistry.of(defaultConfig);

        // AI 调用重试配置
        registry.addConfiguration("aiCall", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnResult(result -> result == null || result.toString().isEmpty())
                .build());

        log.info("重试配置完成");
        return registry;
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .cancelRunningFuture(true)
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(defaultConfig);
        
        // AI 调用超时配置
        registry.addConfiguration("aiChat", TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(60))
                .build());

        registry.addConfiguration("aiStream", TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(120))
                .build());

        log.info("超时配置完成");
        return registry;
    }
}