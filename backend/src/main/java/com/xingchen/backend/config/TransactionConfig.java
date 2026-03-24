package com.xingchen.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;

/**
 * 事务配置类
 * 统一配置事务行为和监控
 */
@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy(exposeProxy = true)
@Slf4j
public class TransactionConfig {

    /**
     * 事务执行时间阈值（毫秒）
     * 超过此时间将记录慢事务日志
     */
    private static final long SLOW_TRANSACTION_THRESHOLD = 1000;

    /**
     * 事务超时时间（秒）
     */
    public static final int DEFAULT_TIMEOUT = 30;

    /**
     * 事务监控切面
     * 记录慢事务和异常事务
     */
    @Aspect
    @Configuration
    @Slf4j
    public static class TransactionMonitorAspect {

        @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
        public Object monitorTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
            String methodName = joinPoint.getSignature().toShortString();
            Object[] args = joinPoint.getArgs();
            
            long startTime = System.currentTimeMillis();
            boolean success = false;
            
            try {
                log.debug("[事务开始] {} | 参数: {}", methodName, Arrays.toString(args));
                
                Object result = joinPoint.proceed();
                success = true;
                
                return result;
            } catch (Exception e) {
                log.error("[事务异常] {} | 异常: {} - {}", methodName, e.getClass().getSimpleName(), e.getMessage());
                throw e;
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                
                if (duration > SLOW_TRANSACTION_THRESHOLD) {
                    log.warn("[慢事务] {} | 耗时: {}ms | 状态: {}", 
                        methodName, duration, success ? "成功" : "失败");
                } else {
                    log.debug("[事务结束] {} | 耗时: {}ms | 状态: {}", 
                        methodName, duration, success ? "成功" : "失败");
                }
            }
        }
    }
}
