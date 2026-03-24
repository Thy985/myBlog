package com.xingchen.backend.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * AI 调用日志切面
 * 记录详细的调用日志用于审计和调试
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AILoggingAspect {

    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    @Around("execution(* com.xingchen.backend.service.AIService.*(..))")
    public Object logAICall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // 脱敏处理参数
        String argsStr = maskSensitiveArgs(args);
        
        log.info("[AI-CALL-START] method={}, args={}", methodName, argsStr);
        
        Instant start = Instant.now();
        boolean success = false;
        String errorMsg = null;
        
        try {
            Object result = joinPoint.proceed();
            success = true;
            
            // 脱敏处理结果
            String resultStr = maskSensitiveResult(result);
            log.info("[AI-CALL-END] method={}, duration={}ms, result={}", 
                    methodName, 
                    Duration.between(start, Instant.now()).toMillis(),
                    resultStr);
            
            return result;
        } catch (Exception e) {
            success = false;
            errorMsg = e.getMessage();
            log.error("[AI-CALL-ERROR] method={}, duration={}ms, error={}", 
                    methodName,
                    Duration.between(start, Instant.now()).toMillis(),
                    errorMsg);
            throw e;
        } finally {
            // 记录指标
            long latency = Duration.between(start, Instant.now()).toMillis();
            metricsService.recordAICall(methodName, success, latency, 0, 0);
        }
    }

    /**
     * 脱敏处理参数
     */
    private String maskSensitiveArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            
            Object arg = args[i];
            if (arg instanceof String str) {
                // 对长文本进行截断
                if (str.length() > 200) {
                    sb.append("\"").append(str.substring(0, 200)).append("...(truncated)").append("\"");
                } else {
                    sb.append("\"").append(maskSensitive(str)).append("\"");
                }
            } else {
                sb.append(arg != null ? arg.getClass().getSimpleName() : "null");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 脱敏处理结果
     */
    private String maskSensitiveResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        String resultStr = result.toString();
        if (resultStr.length() > 500) {
            resultStr = resultStr.substring(0, 500) + "...(truncated)";
        }
        return maskSensitive(resultStr);
    }

    /**
     * 简单脱敏
     */
    private String maskSensitive(String input) {
        if (input == null) return null;
        
        // 脱敏 API Key 等敏感信息
        return input.replaceAll("(?i)(api[_-]?key|token|secret|password)\\s*[:=]\\s*['\"]?[a-zA-Z0-9_-]{10,}['\"]?", 
                "$1: ****");
    }
}