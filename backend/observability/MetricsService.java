package com.xingchen.backend.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监控服务
 * 收集和报告智能体的执行指标
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;
    
    // 计数器
    private final Counter agentTaskTotal = Counter.builder("agent.task.total")
            .description("智能体任务总数")
            .register(meterRegistry);
    
    private final Counter agentTaskSuccess = Counter.builder("agent.task.success")
            .description("智能体任务成功数")
            .register(meterRegistry);
    
    private final Counter agentTaskFailure = Counter.builder("agent.task.failure")
            .description("智能体任务失败数")
            .register(meterRegistry);
    
    private final Counter llmCallTotal = Counter.builder("llm.call.total")
            .description("LLM调用总数")
            .register(meterRegistry);
    
    private final Counter toolCallTotal = Counter.builder("tool.call.total")
            .description("工具调用总数")
            .register(meterRegistry);
    
    // 计时器
    private final Timer agentTaskTimer = Timer.builder("agent.task.duration")
            .description("智能体任务执行时间")
            .register(meterRegistry);
    
    private final Timer llmCallTimer = Timer.builder("llm.call.duration")
            .description("LLM调用执行时间")
            .register(meterRegistry);
    
    private final Timer toolCallTimer = Timer.builder("tool.call.duration")
            .description("工具调用执行时间")
            .register(meterRegistry);
    
    //  gauge
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 注册gauge
        Gauge.builder("agent.task.active", activeTasks, AtomicInteger::get)
                .description("当前活跃的智能体任务数")
                .register(meterRegistry);
    }
    
    /**
     * 记录任务开始
     */
    public void recordTaskStart() {
        agentTaskTotal.increment();
        activeTasks.incrementAndGet();
        log.debug("任务开始，当前活跃任务数: {}", activeTasks.get());
    }
    
    /**
     * 记录任务完成
     */
    public void recordTaskComplete(boolean success, long durationMs) {
        if (success) {
            agentTaskSuccess.increment();
        } else {
            agentTaskFailure.increment();
        }
        activeTasks.decrementAndGet();
        agentTaskTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("任务完成，成功: {}, 执行时间: {}ms, 当前活跃任务数: {}", 
                success, durationMs, activeTasks.get());
    }
    
    /**
     * 记录LLM调用
     */
    public void recordLLMCall(long durationMs) {
        llmCallTotal.increment();
        llmCallTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("LLM调用完成，执行时间: {}ms", durationMs);
    }
    
    /**
     * 记录工具调用
     */
    public void recordToolCall(String toolName, long durationMs) {
        toolCallTotal.increment();
        toolCallTimer.tag("tool", toolName)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("工具调用完成: {}, 执行时间: {}ms", toolName, durationMs);
    }
    
    /**
     * 记录错误
     */
    public void recordError(String errorType) {
        Counter.builder("agent.error.total")
                .tag("type", errorType)
                .description("智能体错误总数")
                .register(meterRegistry)
                .increment();
        log.debug("记录错误: {}", errorType);
    }
    
    /**
     * 记录RAG查询
     */
    public void recordRAGQuery(int documentCount, long durationMs) {
        Counter.builder("rag.query.total")
                .description("RAG查询总数")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("rag.query.duration")
                .description("RAG查询执行时间")
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        Counter.builder("rag.document.retrieved")
                .description("RAG检索到的文档数")
                .register(meterRegistry)
                .increment(documentCount);
        
        log.debug("RAG查询完成，检索到 {} 个文档，执行时间: {}ms", documentCount, durationMs);
    }
    
    /**
     * 获取当前指标
     */
    public Metrics getMetrics() {
        return new Metrics(
            (long) agentTaskTotal.count(),
            (long) agentTaskSuccess.count(),
            (long) agentTaskFailure.count(),
            (long) llmCallTotal.count(),
            (long) toolCallTotal.count(),
            activeTasks.get()
        );
    }
    
    /**
     * 指标类
     */
    public record Metrics(
        long totalTasks,
        long successfulTasks,
        long failedTasks,
        long totalLLMCalls,
        long totalToolCalls,
        int activeTasks
    ) {
        public double getSuccessRate() {
            return totalTasks > 0 ? (double) successfulTasks / totalTasks : 0;
        }
    }
}
