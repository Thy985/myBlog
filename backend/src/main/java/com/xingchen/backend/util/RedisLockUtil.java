package com.xingchen.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class RedisLockUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisLockUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final long DEFAULT_LOCK_EXPIRE_SECONDS = 30;  // 默认30秒，覆盖大多数业务
    private static final long DEFAULT_RETRY_INTERVAL_MS = 100;
    private static final int DEFAULT_RETRY_TIMES = 3;
    private static final long COMPLETED_DELAY_SECONDS = 5;  // 完成后延迟5秒释放

    private static final String STATUS_LOCKED = "locked";
    private static final String STATUS_COMPLETED = "completed";

    // Lua脚本：原子性尝试获取锁
    private static final String TRY_LOCK_SCRIPT =
        "if redis.call('exists', KEYS[1]) == 0 then " +
        "    redis.call('setex', KEYS[1], ARGV[2], ARGV[1]); " +
        "    return 1; " +
        "end; " +
        "return 0;";

    // Lua脚本：原子性释放锁（仅当状态匹配时）
    private static final String RELEASE_LOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    redis.call('del', KEYS[1]); " +
        "    return 1; " +
        "end; " +
        "return 0;";

    // Lua脚本：原子性标记完成并设置延迟过期
    private static final String COMPLETE_LOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    redis.call('setex', KEYS[1], ARGV[3], ARGV[2]); " +
        "    return 1; " +
        "end; " +
        "return 0;";

    /**
     * 尝试获取锁
     */
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_LOCK_EXPIRE_SECONDS);
    }

    /**
     * 尝试获取锁（带过期时间）
     */
    public boolean tryLock(String key, long expireSeconds) {
        String lockKey = "lock:" + key;
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(TRY_LOCK_SCRIPT, Long.class),
            Collections.singletonList(lockKey),
            STATUS_LOCKED,
            String.valueOf(expireSeconds)
        );
        return result != null && result == 1;
    }

    /**
     * 带重试的获取锁
     */
    public boolean tryLockWithRetry(String key, long expireSeconds, int retryTimes, long retryIntervalMs) {
        for (int i = 0; i < retryTimes; i++) {
            if (tryLock(key, expireSeconds)) {
                return true;
            }
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取锁等待时被中断：{}", key);
                return false;
            }
        }
        return false;
    }

    /**
     * 释放锁（业务异常时调用）
     */
    public void unlock(String key) {
        String lockKey = "lock:" + key;
        try {
            redisTemplate.execute(
                new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                STATUS_LOCKED
            );
            log.debug("锁已释放：{}", key);
        } catch (Exception e) {
            log.error("释放锁失败：{}", key, e);
        }
    }

    /**
     * 标记锁完成（业务成功时调用，延迟释放）
     */
    public void completeLock(String key) {
        String lockKey = "lock:" + key;
        try {
            redisTemplate.execute(
                new DefaultRedisScript<>(COMPLETE_LOCK_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                STATUS_LOCKED,                                      // 当前期望值
                STATUS_COMPLETED,                                   // 新值
                String.valueOf(COMPLETED_DELAY_SECONDS)            // 延迟过期时间
            );
            log.debug("锁标记完成，延迟 {} 秒后释放：{}", COMPLETED_DELAY_SECONDS, key);
        } catch (Exception e) {
            log.error("标记锁完成失败：{}", key, e);
        }
    }

    /**
     * 带锁执行业务逻辑（简化版）
     * 业务成功后延迟释放锁，业务异常时立即释放
     */
    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        return executeWithLock(key, DEFAULT_LOCK_EXPIRE_SECONDS, DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_MS, supplier);
    }

    /**
     * 带锁执行业务逻辑（完整版）
     * 
     * 执行流程：
     * 1. 获取锁（设置30秒过期）
     * 2. 执行业务逻辑
     * 3. 业务成功：标记为完成状态，5秒后自动过期
     * 4. 业务异常：立即释放锁，允许重试
     */
    public <T> T executeWithLock(String key, long expireSeconds, int retryTimes, long retryIntervalMs, Supplier<T> supplier) {
        boolean locked = false;
        
        try {
            // 1. 获取锁
            locked = tryLockWithRetry(key, expireSeconds, retryTimes, retryIntervalMs);
            
            if (!locked) {
                log.warn("获取锁失败：{}", key);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            
            log.debug("获取锁成功：{}", key);
            
            // 2. 执行业务逻辑
            T result = supplier.get();
            
            // 3. 业务成功，标记完成（延迟释放）
            completeLock(key);
            
            return result;
        } catch (Exception e) {
            // 4. 业务异常，立即释放锁
            if (locked) {
                unlock(key);
            }
            throw e;
        }
    }

    /**
     * 带锁执行Runnable（无返回值）
     */
    public void executeWithLock(String key, Runnable runnable) {
        executeWithLock(key, DEFAULT_LOCK_EXPIRE_SECONDS, DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_MS, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 带锁执行Runnable（自定义参数）
     */
    public void executeWithLock(String key, long expireSeconds, int retryTimes, long retryIntervalMs, Runnable runnable) {
        executeWithLock(key, expireSeconds, retryTimes, retryIntervalMs, () -> {
            runnable.run();
            return null;
        });
    }
}
