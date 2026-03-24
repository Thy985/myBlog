package com.xingchen.backend.observability;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指标收集服务
 * 统一收集 AI 调用相关指标
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;
    
    // 缓存计数器，避免重复创建
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> gauges = new ConcurrentHashMap<>();

    /**
     * 记录 AI 调用
     */
    public void recordAICall(String model, boolean success, long latencyMs, int inputTokens, int outputTokens) {
        // 调用次数
        getCounter("ai.calls", "model", model, "status", success ? "success" : "failure").increment();
        
        // 延迟
        getTimer("ai.latency", "model", model).record(latencyMs, TimeUnit.MILLISECONDS);
        
        // Token 消耗
        getCounter("ai.tokens.input", "model", model).increment(inputTokens);
        getCounter("ai.tokens.output", "model", model).increment(outputTokens);
        getCounter("ai.tokens.total", "model", model).increment(inputTokens + outputTokens);
    }

    /**
     * 记录知识库检索
     */
    public void recordKnowledgeSearch(String method, boolean success, long latencyMs, int resultCount) {
        getCounter("kb.search", "method", method, "status", success ? "success" : "failure").increment();
        getTimer("kb.latency", "method", method).record(latencyMs, TimeUnit.MILLISECONDS);
        
        if (success) {
            // 记录结果数量分布
            meterRegistry.summary("kb.results", "method", method).record(resultCount);
        }
    }

    /**
     * 记录记忆操作
     */
    public void recordMemoryOperation(String operation, boolean success) {
        getCounter("memory.operations", "operation", operation, "status", success ? "success" : "failure").increment();
    }

    /**
     * 记录流式响应
     */
    public void recordStreaming(String model, long firstTokenLatencyMs, long totalTokens, long durationMs) {
        getTimer("ai.streaming.first_token", "model", model).record(firstTokenLatencyMs, TimeUnit.MILLISECONDS);
        getTimer("ai.streaming.duration", "model", model).record(durationMs, TimeUnit.MILLISECONDS);
        meterRegistry.summary("ai.streaming.tokens_per_second", "model", model)
                .record(totalTokens * 1000.0 / durationMs);
    }

    /**
     * 记录安全事件
     */
    public void recordSecurityEvent(String eventType, String severity) {
        getCounter("security.events", "type", eventType, "severity", severity).increment();
    }

    /**
     * 记录限流事件
     */
    public void recordRateLimit(String type, Long userId) {
        getCounter("ratelimit.triggered", "type", type, "user", userId != null ? "authenticated" : "anonymous").increment();
    }

    /**
     * 记录熔断事件
     */
    public void recordCircuitBreaker(String name, String state) {
        getCounter("circuitbreaker.state_change", "name", name, "state", state).increment();
    }

    // ========== 私有方法 ==========

    private Counter getCounter(String name, String... tags) {
        String key = name + String.join("", tags);
        return counters.computeIfAbsent(key, k -> Counter.builder(name)
                .tags(tags)
                .register(meterRegistry));
    }

    private Timer getTimer(String name, String... tags) {
        String key = name + String.join("", tags);
        return timers.computeIfAbsent(key, k -> Timer.builder(name)
                .tags(tags)
                .register(meterRegistry));
    }

    /**
     * 获取当前活跃对话数
     */
    public void setActiveConversations(int count) {
        meterRegistry.gauge("ai.conversations.active", count);
    }

    /**
     * 获取队列等待数
     */
    public void setQueueSize(int size) {
        meterRegistry.gauge("ai.queue.size", size);
    }
}