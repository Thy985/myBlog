package com.xingchen.backend.ai.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * LLM服务实现
 * 支持多种模型提供商的切换和管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private final List<LLMProvider> providers;
    
    @Value("${ai.agent.llm.default-provider:OpenAI}")
    private String defaultProviderName;
    
    // 缓存提供商实例
    private final Map<String, LLMProvider> providerMap = new ConcurrentHashMap<>();
    
    @Override
    public AIResponse chat(AIRequest request) {
        LLMProvider provider = getProvider(request.getPreferredModel());
        if (provider == null) {
            return AIResponse.error("未找到可用的LLM提供商");
        }
        
        return provider.chat(request);
    }
    
    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        LLMProvider provider = getProvider(request.getPreferredModel());
        if (provider == null) {
            onChunk.accept(AIResponse.error("未找到可用的LLM提供商"));
            return;
        }
        
        provider.streamChat(request, onChunk);
    }
    
    @Override
    public List<String> getAvailableModels() {
        return getDefaultProvider().getAvailableModels();
    }
    
    @Override
    public boolean isAvailable() {
        return !providers.isEmpty() && getDefaultProvider().isAvailable();
    }
    
    /**
     * 获取指定提供商
     */
    private LLMProvider getProvider(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            return getDefaultProvider();
        }
        
        return providerMap.computeIfAbsent(providerName, name -> 
            providers.stream()
                .filter(p -> p.getProviderName().equals(name))
                .findFirst()
                .orElse(null)
        );
    }
    
    /**
     * 获取默认提供商
     */
    private LLMProvider getDefaultProvider() {
        return providerMap.computeIfAbsent(defaultProviderName, name -> 
            providers.stream()
                .filter(p -> p.getProviderName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    // 如果默认提供商不可用，返回第一个可用的
                    return providers.stream()
                        .filter(LLMProvider::isAvailable)
                        .findFirst()
                        .orElse(null);
                })
        );
    }
    
    /**
     * 获取所有可用的提供商
     */
    public List<LLMProvider> getAvailableProviders() {
        return providers.stream()
            .filter(LLMProvider::isAvailable)
            .toList();
    }
    
    /**
     * 切换默认提供商
     */
    public void setDefaultProvider(String providerName) {
        this.defaultProviderName = providerName;
        providerMap.clear(); // 清除缓存
    }
}
