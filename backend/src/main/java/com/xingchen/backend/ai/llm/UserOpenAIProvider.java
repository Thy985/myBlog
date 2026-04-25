package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户级 OpenAI Provider
 * 每个用户独立的 Provider 实例，使用用户自己的 API Key
 */
@Slf4j
public class UserOpenAIProvider extends BaseOpenAICompatibleProvider {

    public UserOpenAIProvider(String apiKey, String baseUrl, String defaultModel) {
        super(apiKey, baseUrl, defaultModel, 60);
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    protected String getDefaultModel() {
        return "gpt-3.5-turbo";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo",
                "gpt-4",
                "gpt-3.5-turbo"
        );
    }
}