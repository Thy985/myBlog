package com.xingchen.backend.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 * 
 * 统一管理缓存操作，支持序列化/反序列化
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 设置缓存
     */
    public <T> void set(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.error("缓存设置失败: key={}", key, e);
        }
    }

    /**
     * 获取缓存
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            T value = objectMapper.readValue(json, clazz);
            return Optional.of(value);
        } catch (Exception e) {
            log.error("缓存获取失败: key={}", key, e);
            return Optional.empty();
        }
    }

    /**
     * 获取缓存（支持泛型）
     */
    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            T value = objectMapper.readValue(json, typeRef);
            return Optional.of(value);
        } catch (Exception e) {
            log.error("缓存获取失败: key={}", key, e);
            return Optional.empty();
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除
     */
    public void deletePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 检查是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
    }

    /**
     * 获取剩余过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 自增
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自增（带过期时间）
     */
    public Long increment(String key, long delta, Duration ttl) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        if (value != null && value == delta) {
            // 第一次设置，添加过期时间
            redisTemplate.expire(key, ttl);
        }
        return value;
    }

    /**
     * 获取分布式锁
     */
    public boolean tryLock(String key, String value, Duration ttl) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 缓存空值（防止缓存穿透）
     */
    public void setNull(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, "NULL", ttl);
    }

    /**
     * 检查是否为空值
     */
    public boolean isNull(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return "NULL".equals(value);
    }
}