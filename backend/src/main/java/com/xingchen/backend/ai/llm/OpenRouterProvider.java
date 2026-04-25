package com.xingchen.backend.ai.llm;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenRouter提供商实现
 * 支持多模型路由
 */
@Component
@Slf4j
public class OpenRouterProvider extends BaseOpenAICompatibleProvider {

    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final int timeoutSeconds;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public OpenRouterProvider(
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${openrouter.default-model:openai/gpt-3.5-turbo}") String defaultModel,
            @Value("${openrouter.timeout:60}") int timeoutSeconds,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        super(apiKey, baseUrl, defaultModel, timeoutSeconds);
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.timeoutSeconds = timeoutSeconds;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public String getProviderName() {
        return "OpenRouter";
    }

    @Override
    protected String getDefaultBaseUrl() {
        return baseUrl;
    }

    @Override
    protected String getDefaultModel() {
        return defaultModel;
    }
}