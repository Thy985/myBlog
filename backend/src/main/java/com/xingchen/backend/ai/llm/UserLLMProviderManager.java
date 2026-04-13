package com.xingchen.backend.ai.llm;

import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.security.ApiKeyEncryptionService;
import com.xingchen.backend.service.UserApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户级LLM Provider管理器
 * 为每个用户创建独立的Provider实例
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserLLMProviderManager {
    
    private final UserApiKeyService userApiKeyService;
    private final ApiKeyEncryptionService encryptionService;
    
    // 用户Provider缓存
    private final Map<Long, LLMProvider> userProviderCache = new ConcurrentHashMap<>();
    
    /**
     * 获取用户的LLM Provider
     * @param userId 用户ID
     * @return 用户的Provider，如果没有配置则返回null
     */
    public LLMProvider getUserProvider(Long userId) {
        // 先检查缓存
        LLMProvider cached = userProviderCache.get(userId);
        if (cached != null) {
            return cached;
        }

        // 从数据库获取用户的API Key配置
        UserApiKey userApiKey = userApiKeyService.getByUserId(userId);
        if (userApiKey == null) {
            log.debug("用户 {} 没有配置API Key", userId);
            return null;
        }

        // 解密API Key（检查是否是加密格式）
        String apiKeyValue = userApiKey.getApiKey();
        if (apiKeyValue == null || apiKeyValue.isEmpty()) {
            log.warn("用户 {} 的API Key为空", userId);
            return null;
        }

        String plainApiKey;
        if (encryptionService.isEncrypted(apiKeyValue)) {
            // 是加密格式，需要解密
            plainApiKey = encryptionService.decrypt(apiKeyValue);
        } else {
            // 是明文格式，直接使用
            plainApiKey = apiKeyValue;
        }

        if (plainApiKey == null || plainApiKey.isEmpty()) {
            log.warn("用户 {} 的API Key解密失败", userId);
            return null;
        }

        // 创建Provider
        LLMProvider provider = createProvider(userApiKey.getProvider(), plainApiKey,
                userApiKey.getBaseUrl(), userApiKey.getDefaultModel());

        if (provider != null) {
            // 缓存Provider
            userProviderCache.put(userId, provider);
            log.info("为用户 {} 创建 {} Provider", userId, provider.getProviderName());
        }

        return provider;
    }

    /**
     * 获取用户的默认模型
     */
    public String getUserDefaultModel(Long userId) {
        UserApiKey userApiKey = userApiKeyService.getByUserId(userId);
        if (userApiKey != null && userApiKey.getDefaultModel() != null) {
            return userApiKey.getDefaultModel();
        }
        return "glm-4-flash"; // 默认模型
    }

    /**
     * 获取用户的模型参数
     */
    public UserModelConfig getUserModelConfig(Long userId) {
        UserApiKey userApiKey = userApiKeyService.getByUserId(userId);
        if (userApiKey == null) {
            return UserModelConfig.defaultConfig();
        }

        return new UserModelConfig(
                userApiKey.getProvider(),
                userApiKey.getDefaultModel(),
                userApiKey.getTemperature() != null ? userApiKey.getTemperature() : 0.7,
                userApiKey.getMaxTokens() != null ? userApiKey.getMaxTokens() : 4096,
                userApiKey.getTopP() != null ? userApiKey.getTopP() : 0.9
        );
    }

    /**
     * 检查用户是否配置了API Key
     */
    public boolean hasUserProvider(Long userId) {
        return userProviderCache.containsKey(userId) ||
               userApiKeyService.getByUserId(userId) != null;
    }
    
    /**
     * 清除用户的Provider缓存（配置变更时调用）
     */
    public void clearUserCache(Long userId) {
        userProviderCache.remove(userId);
        log.info("清除用户 {} 的Provider缓存", userId);
    }
    
    /**
     * 创建Provider实例
     */
    private LLMProvider createProvider(String providerType, String apiKey,
                                       String baseUrl, String model) {
        if (providerType == null) {
            return null;
        }
        return switch (providerType.toLowerCase()) {
            case "glm", "zhipu" -> new UserGLMProvider(apiKey, baseUrl, model);
            case "openai", "openrouter", "deepseek" -> new UserOpenRouterProvider(apiKey, baseUrl, model);
            default -> {
                log.warn("未知的Provider类型: {}", providerType);
                yield null;
            }
        };
    }

    /**
     * 用户模型配置
     */
    public record UserModelConfig(
            String provider,
            String model,
            double temperature,
            int maxTokens,
            double topP
    ) {
        public static UserModelConfig defaultConfig() {
            return new UserModelConfig("glm", "glm-4-flash", 0.7, 4096, 0.9);
        }
    }
}