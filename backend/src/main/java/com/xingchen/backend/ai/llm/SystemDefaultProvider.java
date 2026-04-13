package com.xingchen.backend.ai.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 系统默认Provider
 * 当用户没有配置自己的API Key时使用
 * 使用系统配置的共享API Key
 */
@Component
@Slf4j
public class SystemDefaultProvider implements LLMProvider {
    
    @Value("${ai.system.default-provider:glm}")
    private String defaultProviderType;
    
    private final GLMProvider glmProvider;
    private final OpenAIProvider openAIProvider;
    private final BaiduProvider baiduProvider;
    private final OpenRouterProvider openRouterProvider;
    
    public SystemDefaultProvider(GLMProvider glmProvider, 
                                  OpenAIProvider openAIProvider,
                                  BaiduProvider baiduProvider,
                                  OpenRouterProvider openRouterProvider) {
        this.glmProvider = glmProvider;
        this.openAIProvider = openAIProvider;
        this.baiduProvider = baiduProvider;
        this.openRouterProvider = openRouterProvider;
    }
    
    @Override
    public String getProviderName() {
        return "SystemDefault";
    }
    
    @Override
    public String getModelName() {
        return getActiveProvider().getModelName();
    }
    
    @Override
    public AIResponse chat(AIRequest request) {
        LLMProvider provider = getActiveProvider();
        if (provider == null || !provider.isAvailable()) {
            return AIResponse.error("系统AI服务暂时不可用，请稍后重试或配置自己的API Key");
        }
        
        log.debug("使用系统默认Provider: {}", provider.getProviderName());
        return provider.chat(request);
    }
    
    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        LLMProvider provider = getActiveProvider();
        if (provider == null || !provider.isAvailable()) {
            onChunk.accept(AIResponse.error("系统AI服务暂时不可用"));
            return;
        }
        
        provider.streamChat(request, onChunk);
    }
    
    @Override
    public boolean isAvailable() {
        return getActiveProvider() != null && getActiveProvider().isAvailable();
    }
    
    @Override
    public boolean supportsStreaming() {
        LLMProvider provider = getActiveProvider();
        return provider != null && provider.supportsStreaming();
    }
    
    @Override
    public boolean supportsToolCalling() {
        LLMProvider provider = getActiveProvider();
        return provider != null && provider.supportsToolCalling();
    }
    
    @Override
    public List<String> getAvailableModels() {
        LLMProvider provider = getActiveProvider();
        return provider != null ? provider.getAvailableModels() : List.of();
    }
    
    /**
     * 获取当前激活的Provider
     * 按优先级尝试：GLM > OpenAI > Baidu > OpenRouter
     */
    private LLMProvider getActiveProvider() {
        // 根据配置选择
        switch (defaultProviderType.toLowerCase()) {
            case "glm", "zhipu":
                if (glmProvider.isAvailable()) return glmProvider;
                break;
            case "openai":
                if (openAIProvider.isAvailable()) return openAIProvider;
                break;
            case "baidu", "wenxin":
                if (baiduProvider.isAvailable()) return baiduProvider;
                break;
            case "openrouter":
                if (openRouterProvider.isAvailable()) return openRouterProvider;
                break;
        }
        
        // 自动选择第一个可用的
        if (glmProvider.isAvailable()) return glmProvider;
        if (openAIProvider.isAvailable()) return openAIProvider;
        if (baiduProvider.isAvailable()) return baiduProvider;
        if (openRouterProvider.isAvailable()) return openRouterProvider;
        
        return null;
    }
    
    /**
     * 获取系统默认模型
     */
    public String getSystemDefaultModel() {
        LLMProvider provider = getActiveProvider();
        return provider != null ? provider.getModelName() : "none";
    }
}