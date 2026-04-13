package com.xingchen.backend.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redisTemplate;

    // Lua脚本：原子性检查并设置状态
    private static final String CHECK_AND_SET_SCRIPT =
        "if redis.call('exists', KEYS[1]) == 0 then " +
        "    redis.call('setex', KEYS[1], tonumber(ARGV[2]), ARGV[1]); " +
        "    return 1; " +
        "elseif redis.call('get', KEYS[1]) == ARGV[3] then " +
        "    return 2; " +  // 已完成状态，拒绝重复请求
        "else " +
        "    return 0; " +  // 处理中状态
        "end";

    // Lua脚本：原子性完成并设置延迟过期
    private static final String COMPLETE_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    redis.call('setex', KEYS[1], tonumber(ARGV[3]), ARGV[2]); " +  // 设置为完成状态，延迟过期
        "    return 1; " +
        "end; " +
        "return 0;";

    // Lua脚本：原子性释放（异常时）
    private static final String RELEASE_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    redis.call('del', KEYS[1]); " +
        "    return 1; " +
        "end; " +
        "return 0;";

    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    
    // 处理中状态TTL：30秒（要大于最大预期业务耗时）
    private static final long PROCESSING_TTL_SECONDS = 30;
    // 完成状态延迟过期时间：5秒（防止事务提交前的并发问题）
    private static final long COMPLETED_DELAY_SECONDS = 5;

    @Around("@annotation(com.xingchen.backend.aspect.Idempotent)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        String key = buildKey(point, idempotent.key());
        String lockKey = "idempotent:" + key;

        // 1. 尝试获取锁（原子性操作）
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(CHECK_AND_SET_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            STATUS_PROCESSING,                                    // ARGV[1]: 要设置的值
            String.valueOf(PROCESSING_TTL_SECONDS),              // ARGV[2]: TTL
            STATUS_COMPLETED                                     // ARGV[3]: 检查的值
        );

        if (result == null || result == 0) {
            log.warn("接口幂等性拦截（正在处理中）: {}", key);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求正在处理中，请勿重复提交");
        }

        if (result == 2) {
            log.warn("接口幂等性拦截（已完成）: {}", key);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, idempotent.message());
        }

        // 成功获取锁，开始执行业务
        try {
            Object proceedResult = point.proceed();
            
            // 2. 业务成功，标记为完成状态（延迟过期）
            completeLock(lockKey);
            
            return proceedResult;
        } catch (Exception e) {
            // 3. 业务异常，立即释放锁，允许重试
            releaseLock(lockKey);
            throw e;
        }
    }

    /**
     * 标记任务完成，设置延迟过期
     */
    private void completeLock(String lockKey) {
        try {
            redisTemplate.execute(
                new DefaultRedisScript<>(COMPLETE_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                STATUS_PROCESSING,                                    // ARGV[1]: 当前期望值
                STATUS_COMPLETED,                                     // ARGV[2]: 新值
                String.valueOf(COMPLETED_DELAY_SECONDS)             // ARGV[3]: 延迟过期时间
            );
            log.debug("幂等任务完成，设置延迟过期: {}", lockKey);
        } catch (Exception e) {
            log.error("标记任务完成失败: {}", lockKey, e);
        }
    }

    /**
     * 业务异常时立即释放锁
     */
    private void releaseLock(String lockKey) {
        try {
            redisTemplate.execute(
                new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                STATUS_PROCESSING                                     // ARGV[1]: 当前期望值
            );
            log.debug("业务异常，立即释放锁: {}", lockKey);
        } catch (Exception e) {
            log.error("释放锁失败: {}", lockKey, e);
        }
    }

    private String buildKey(ProceedingJoinPoint point, String customKey) {
        if (customKey != null && !customKey.isEmpty()) {
            return customKey;
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        
        String userId = "anonymous";
        try {
            if (StpUtil.isLogin()) {
                userId = String.valueOf(StpUtil.getLoginId());
            }
        } catch (Exception ignored) {
        }

        Object[] args = point.getArgs();
        String argsHash = args.length > 0 ? String.valueOf(Arrays.hashCode(args)) : "0";

        return className + ":" + methodName + ":" + userId + ":" + argsHash;
    }
}
