package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class BaseOpenRouterProvider extends BaseOpenAICompatibleProvider {

    protected BaseOpenRouterProvider(String apiKey, String baseUrl, String defaultModel, int timeoutSeconds) {
        super(apiKey, baseUrl, defaultModel, timeoutSeconds);
    }

    @Override
    public String getProviderName() {
        return "OpenRouter";
    }

    @Override
    protected String getDefaultBaseUrl() {
        return "https://openrouter.ai/api/v1";
    }

    @Override
    protected String getDefaultModel() {
        return "openai/gpt-3.5-turbo";
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
}