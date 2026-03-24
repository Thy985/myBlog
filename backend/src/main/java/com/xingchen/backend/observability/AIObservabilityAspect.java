package com.xingchen.backend.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AI 服务可观测性切面
 * 监控 AI 调用成功率、延迟、Token 消耗
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AIObservabilityAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(com.xingchen.backend.observability.Monitored)")
    public Object monitorAI(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String metricName = "ai." + className + "." + methodName;

        Timer.Sample sample = Timer.start(meterRegistry);
        Counter successCounter = Counter.builder(metricName + ".success")
                .description("AI 调用成功次数")
                .register(meterRegistry);
        Counter failureCounter = Counter.builder(metricName + ".failure")
                .description("AI 调用失败次数")
                .register(meterRegistry);

        try {
            Object result = joinPoint.proceed();
            successCounter.increment();
            return result;
        } catch (Exception e) {
            failureCounter.increment();
            log.error("AI 调用失败: {}.{}, 错误: {}", className, methodName, e.getMessage());
            throw e;
        } finally {
            sample.stop(Timer.builder(metricName + ".latency")
                    .description("AI 调用延迟")
                    .register(meterRegistry));
        }
    }

    @Around("execution(* com.xingchen.backend.service.AIService.*(..))")
    public Object monitorAIService(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorAI(joinPoint);
    }
}