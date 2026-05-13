package com.xingchen.backend.config;

import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.llm.UserLLMProviderManager;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component("agentLLMProvider")
@Slf4j
public class AgentLLMProviderAdapter implements LLMProvider {

    private final UserLLMProviderManager userLLMProviderManager;
    private Long currentUserId;

    public AgentLLMProviderAdapter(UserLLMProviderManager userLLMProviderManager) {
        this.userLLMProviderManager = userLLMProviderManager;
    }

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
        log.debug("设置当前用户ID: userId={}", userId);
    }

    public void clearCurrentUserId() {
        this.currentUserId = null;
    }

    private LLMProvider getProvider() {
        if (currentUserId != null) {
            com.xingchen.backend.ai.llm.LLMProvider userProvider = userLLMProviderManager.getUserProvider(currentUserId);
            if (userProvider != null) {
                log.debug("使用用户Provider: userId={}", currentUserId);
                return wrapAiProvider(userProvider);
            }
            log.warn("用户未配置API Key: userId={}", currentUserId);
        }
        return null;
    }

    private LLMProvider wrapAiProvider(com.xingchen.backend.ai.llm.LLMProvider aiProvider) {
        if (aiProvider == null) {
            return null;
        }
        return new LLMProvider() {
            @Override
            public String getProviderName() {
                return "User_" + aiProvider.getProviderName();
            }

            @Override
            public String getModelName() {
                return aiProvider.getModelName();
            }

            @Override
            public AIResponse chat(AIRequest request) {
                return aiProvider.chat(request);
            }

            @Override
            public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
                aiProvider.streamChat(request, onChunk);
            }

            @Override
            public boolean isAvailable() {
                return aiProvider.isAvailable();
            }

            @Override
            public boolean supportsStreaming() {
                return aiProvider.supportsStreaming();
            }

            @Override
            public boolean supportsToolCalling() {
                return aiProvider.supportsToolCalling();
            }
        };
    }

    @Override
    public String getProviderName() {
        LLMProvider provider = getProvider();
        if (provider != null) {
            return "User_" + provider.getProviderName();
        }
        return "NoProvider";
    }

    @Override
    public String getModelName() {
        LLMProvider provider = getProvider();
        return provider != null ? provider.getModelName() : "unknown";
    }

    @Override
    public AIResponse chat(AIRequest request) {
        LLMProvider provider = getProvider();
        if (provider == null) {
            return AIResponse.builder()
                    .type(AIResponse.ResponseType.ERROR)
                    .content("AI服务调用失败: 用户未配置API Key，请前往个人设置添加API Key")
                    .build();
        }
        return provider.chat(request);
    }

    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        LLMProvider provider = getProvider();
        if (provider == null) {
            onChunk.accept(AIResponse.builder()
                    .content("AI服务调用失败: 用户未配置API Key，请前往个人设置添加API Key")
                    .build());
            return;
        }
        provider.streamChat(request, onChunk);
    }

    @Override
    public boolean isAvailable() {
        LLMProvider provider = getProvider();
        return provider != null && provider.isAvailable();
    }

    @Override
    public boolean supportsStreaming() {
        LLMProvider provider = getProvider();
        return provider != null && provider.supportsStreaming();
    }

    @Override
    public boolean supportsToolCalling() {
        LLMProvider provider = getProvider();
        return provider != null && provider.supportsToolCalling();
    }
}
