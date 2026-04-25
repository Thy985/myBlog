package com.xingchen.backend.ai.service;

import com.xingchen.backend.ai.llm.UserLLMProviderManager;
import com.xingchen.backend.ai.memory.UnifiedMemoryService;
import com.xingchen.backend.ai.util.AIUserContext;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.service.UserApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAIConfigService {

    private final UserApiKeyService userApiKeyService;
    private final UserLLMProviderManager userProviderManager;
    private final UnifiedMemoryService unifiedMemoryService;

    public AIUserContext getUserContext(Long userId) {
        if (userId == null) {
            return null;
        }

        UserApiKey apiKey = userApiKeyService.getByUserId(userId);
        if (apiKey == null) {
            return AIUserContext.defaultContext(userId);
        }

        return AIUserContext.builder()
                .userId(userId)
                .provider(apiKey.getProvider())
                .model(apiKey.getDefaultModel())
                .temperature(apiKey.getTemperature())
                .maxTokens(apiKey.getMaxTokens())
                .topP(apiKey.getTopP())
                .memoryEnabled(true)
                .ragEnabled(false)
                .quota(AIUserContext.UserQuota.builder()
                        .totalQuota(apiKey.getQuota())
                        .usedQuota(apiKey.getUsed())
                        .remainingQuota(apiKey.getQuota() != null && apiKey.getUsed() != null
                                ? apiKey.getQuota() - apiKey.getUsed() : null)
                        .build())
                .build();
    }

    public boolean isAIConfigured(Long userId) {
        return userProviderManager.hasUserProvider(userId);
    }

    public UnifiedMemoryService.MemoryStats getUserMemoryStats(Long userId) {
        return unifiedMemoryService.getMemoryStats(userId);
    }

    public void clearUserMemory(Long userId) {
        unifiedMemoryService.clearUserMemory(userId);
        log.info("清除用户 {} 的所有记忆", userId);
    }

    public void clearUserSession(Long userId, String sessionId) {
        unifiedMemoryService.clearSessionMemory(userId, sessionId);
        log.info("清除用户 {} 的会话 {}", userId, sessionId);
    }

    public List<String> getUserSessions(Long userId) {
        return unifiedMemoryService.getUserSessions(userId);
    }

    public void refreshUserConfig(Long userId) {
        userProviderManager.clearUserProviderCache(userId);
        log.info("刷新用户 {} 的配置缓存", userId);
    }

    public List<ProviderInfo> getSupportedProviders() {
        return List.of(
                new ProviderInfo("glm", "智谱AI", "国内访问快，有免费额度",
                        List.of("glm-4-plus", "glm-4", "glm-4-air", "glm-4-flash")),
                new ProviderInfo("openai", "OpenAI", "最强模型，需国际信用卡",
                        List.of("gpt-4o", "gpt-4", "gpt-3.5-turbo")),
                new ProviderInfo("baidu", "百度文心", "国内合规，企业友好",
                        List.of("ernie-bot-4", "ernie-bot", "ernie-bot-turbo")),
                new ProviderInfo("openrouter", "OpenRouter", "多模型聚合",
                        List.of("openai/gpt-4", "anthropic/claude-3-opus"))
        );
    }

    public record ProviderInfo(
            String code,
            String name,
            String description,
            List<String> models
    ) {}
}