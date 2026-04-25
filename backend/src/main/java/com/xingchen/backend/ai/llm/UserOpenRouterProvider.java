package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户级 OpenRouter Provider
 * 支持 DeepSeek 等多模型，使用用户自己的 API Key
 */
@Slf4j
public class UserOpenRouterProvider extends BaseOpenRouterProvider {

    public UserOpenRouterProvider(String apiKey, String baseUrl, String defaultModel) {
        super(apiKey, baseUrl, defaultModel, 120);
    }
    
    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "deepseek/deepseek-chat-v3",
                "deepseek/deepseek-coder",
                "openai/gpt-4",
                "openai/gpt-4-turbo",
                "openai/gpt-3.5-turbo",
                "anthropic/claude-3-opus",
                "anthropic/claude-3-sonnet"
        );
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
