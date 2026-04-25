package com.xingchen.backend.ai.llm;

import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.service.UserApiKeyService;
import com.xingchen.backend.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Slf4j
public class UserLLMProviderManager {

    private final UserApiKeyService userApiKeyService;
    private final Map<Long, ProviderCacheEntry> userProviderCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    private static final long CACHE_TTL_SECONDS = 3600;
    private static final long CACHE_CLEANUP_INTERVAL_SECONDS = 300;

    public UserLLMProviderManager(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
        startCacheCleanup();
    }

    private void startCacheCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredCache();
            } catch (Exception e) {
                log.error("清理过期缓存失败", e);
            }
        }, CACHE_CLEANUP_INTERVAL_SECONDS, CACHE_CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void cleanupExpiredCache() {
        long now = Instant.now().getEpochSecond();
        int beforeSize = userProviderCache.size();

        userProviderCache.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().createdAt) > CACHE_TTL_SECONDS;
            if (expired) {
                log.debug("清理过期Provider缓存: userId={}", entry.getKey());
            }
            return expired;
        });

        int removed = beforeSize - userProviderCache.size();
        if (removed > 0) {
            log.info("清理过期Provider缓存: removed={}, remaining={}", removed, userProviderCache.size());
        }
    }

    public LLMProvider getUserProvider(Long userId) {
        ProviderCacheEntry cached = userProviderCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return cached.provider;
        }

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        try {
            cached = userProviderCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                return cached.provider;
            }

            LLMProvider provider = createUserProvider(userId);
            if (provider != null) {
                ProviderCacheEntry entry = new ProviderCacheEntry(provider, Instant.now().getEpochSecond());
                userProviderCache.put(userId, entry);
            }
            return provider;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private LLMProvider createUserProvider(Long userId) {
        try {
            UserApiKey apiKey = userApiKeyService.getByUserId(userId);
            if (apiKey == null) {
                log.debug("用户未配置API Key: userId={}", userId);
                return null;
            }

            String decryptedKey = apiKey.getDecryptedApiKey();
            if (decryptedKey == null || decryptedKey.isEmpty()) {
                log.warn("用户API Key解密失败或为空: userId={}", userId);
                return null;
            }

            String providerType = apiKey.getProvider();
            String baseUrl = apiKey.getBaseUrl();
            String model = apiKey.getDefaultModel();

            return createProviderByType(providerType, decryptedKey, baseUrl, model);

        } catch (Exception e) {
            log.error("创建用户Provider失败: userId={}", userId, e);
            return null;
        }
    }

    private LLMProvider createProviderByType(String providerType, String apiKey, String baseUrl, String model) {
        String type = providerType != null ? providerType.toUpperCase() : "";

        if ("OPENROUTER".equals(type)) {
            return new UserOpenRouterProvider(apiKey, baseUrl, model);
        } else if ("ZHIPU".equals(type) || "GLM".equals(type)) {
            return new UserGLMProvider(apiKey, baseUrl, model);
        } else if ("OPENAI".equals(type)) {
            return new UserOpenAIProvider(apiKey, baseUrl, model);
        } else {
            log.warn("不支持的Provider类型: {}", providerType);
            return null;
        }
    }

    public void clearUserProviderCache(Long userId) {
        userProviderCache.remove(userId);
        log.info("清理用户Provider缓存: userId={}", userId);
    }

    public void clearAllCache() {
        userProviderCache.clear();
        log.info("清理所有Provider缓存");
    }

    public int getCacheSize() {
        return userProviderCache.size();
    }

    public boolean hasUserProvider(Long userId) {
        return userProviderCache.containsKey(userId);
    }

    public record ProviderCacheEntry(LLMProvider provider, long createdAt) {
        public boolean isExpired() {
            return (Instant.now().getEpochSecond() - createdAt) > CACHE_TTL_SECONDS;
        }
    }
}