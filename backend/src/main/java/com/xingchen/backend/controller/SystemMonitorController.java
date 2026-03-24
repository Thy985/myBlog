package com.xingchen.backend.controller;

import com.xingchen.backend.common.Result;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统监控控制器
 * 提供运行时状态和指标查询
 */
@RestController
@RequestMapping("/api/system")
@Slf4j
@RequiredArgsConstructor
public class SystemMonitorController {

    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * 获取系统指标概览
     */
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // AI 调用指标
        metrics.put("aiCalls", getCounter("ai.calls"));
        metrics.put("aiLatency", getTimer("ai.latency"));
        metrics.put("aiTokens", getCounter("ai.tokens.total"));
        
        // 知识库指标
        metrics.put("kbSearches", getCounter("kb.search"));
        
        // 安全指标
        metrics.put("securityEvents", getCounter("security.events"));
        metrics.put("rateLimitTriggered", getCounter("ratelimit.triggered"));
        
        return Result.success(metrics);
    }

    /**
     * 获取熔断器状态
     */
    @GetMapping("/circuit-breakers")
    public Result<Map<String, Object>> getCircuitBreakers() {
        Map<String, Object> result = new HashMap<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbInfo = new HashMap<>();
            cbInfo.put("state", cb.getState());
            cbInfo.put("failureRate", cb.getMetrics().getFailureRate());
            cbInfo.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            cbInfo.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbInfo.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            
            result.put(cb.getName(), cbInfo);
        });
        
        return Result.success(result);
    }

    /**
     * 获取限流器状态
     */
    @GetMapping("/rate-limiters")
    public Result<Map<String, Object>> getRateLimiters() {
        Map<String, Object> result = new HashMap<>();
        
        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            Map<String, Object> rlInfo = new HashMap<>();
            rlInfo.put("availablePermissions", rl.getMetrics().getAvailablePermissions());
            rlInfo.put("numberOfWaitingThreads", rl.getMetrics().getNumberOfWaitingThreads());
            
            result.put(rl.getName(), rlInfo);
        });
        
        return Result.success(result);
    }

    /**
     * 健康检查详情
     */
    @GetMapping("/health")
    public Result<Map<String, String>> getHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", java.time.Instant.now().toString());
        return Result.success(health);
    }

    // ========== 私有方法 ==========

    private double getCounter(String name) {
        return meterRegistry.find(name).counter() != null 
            ? meterRegistry.find(name).counter().count() 
            : 0;
    }

    private Map<String, Double> getTimer(String name) {
        Map<String, Double> result = new HashMap<>();
        var timer = meterRegistry.find(name).timer();
        if (timer != null) {
            result.put("mean", timer.mean(TimeUnit.MILLISECONDS));
            result.put("max", timer.max(TimeUnit.MILLISECONDS));
            result.put("count", (double) timer.count());
        }
        return result;
    }
}