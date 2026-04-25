package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "openai", name = "api-key")
@Slf4j
public class OpenAIProvider extends BaseOpenAICompatibleProvider {

    public OpenAIProvider(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.model:gpt-3.5-turbo}") String defaultModel,
            @Value("${ai.agent.llm.timeout-seconds:60}") int timeoutSeconds) {
        super(apiKey, baseUrl, defaultModel, timeoutSeconds);
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    protected String getDefaultBaseUrl() {
        return "https://api.openai.com/v1";
    }

    @Override
    protected String getDefaultModel() {
        return "gpt-3.5-turbo";
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