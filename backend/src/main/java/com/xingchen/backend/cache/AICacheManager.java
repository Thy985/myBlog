package com.xingchen.backend.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

/**
 * AI 调用缓存管理器
 * 
 * 缓存 AI 调用结果，减少重复调用成本
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AICacheManager {

    private final CacheService cacheService;

    // 缓存前缀
    private static final String CACHE_PREFIX = "ai:response:";
    // 缓存过期时间（默认 1 小时）
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    /**
     * 获取缓存的 AI 响应
     */
    public Optional<String> getCachedResponse(String prompt, String model) {
        String cacheKey = generateCacheKey(prompt, model);
        
        Optional<String> cached = cacheService.get(cacheKey, String.class);
        
        if (cached.isPresent()) {
            log.debug("AI 缓存命中: key={}", cacheKey);
        }
        
        return cached;
    }

    /**
     * 缓存 AI 响应
     */
    public void cacheResponse(String prompt, String model, String response, Duration ttl) {
        String cacheKey = generateCacheKey(prompt, model);
        cacheService.set(cacheKey, response, ttl);
        log.debug("AI 响应已缓存: key={}", cacheKey);
    }

    public void cacheResponse(String prompt, String model, String response) {
        cacheResponse(prompt, model, response, DEFAULT_TTL);
    }

    /**
     * 清除特定模型的缓存
     */
    public void clearModelCache(String model) {
        String pattern = CACHE_PREFIX + model + ":*";
        cacheService.deletePattern(pattern);
        log.info("已清除模型缓存: {}", model);
    }

    /**
     * 清除所有 AI 缓存
     */
    public void clearAllCache() {
        cacheService.deletePattern(CACHE_PREFIX + "*");
        log.info("已清除所有 AI 缓存");
    }

    /**
     * 生成缓存 Key
     */
    private String generateCacheKey(String prompt, String model) {
        // 使用 MD5 哈希生成固定长度的 key
        String content = model + ":" + prompt;
        String hash = md5(content);
        return CACHE_PREFIX + model + ":" + hash;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 降级为简单哈希
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * 判断是否应该缓存
     */
    public boolean shouldCache(String prompt) {
        // 不缓存包含敏感信息的请求
        if (containsSensitiveInfo(prompt)) {
            return false;
        }
        
        // 不缓存过长的请求
        if (prompt.length() > 5000) {
            return false;
        }
        
        return true;
    }

    private boolean containsSensitiveInfo(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("password") || 
               lower.contains("secret") || 
               lower.contains("token") ||
               lower.contains("api key");
    }
}