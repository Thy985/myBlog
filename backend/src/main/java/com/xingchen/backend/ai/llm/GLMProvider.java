package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "glm.api", name = "key")
@Slf4j
public class GLMProvider extends BaseOpenAICompatibleProvider {

    public GLMProvider(
            @Value("${glm.api.key}") String apiKey,
            @Value("${glm.api.base-url:https://open.bigmodel.cn/api/paas/v4}") String baseUrl,
            @Value("${glm.api.model:glm-4-flash}") String defaultModel,
            @Value("${ai.agent.llm.timeout-seconds:60}") int timeoutSeconds) {
        super(apiKey, baseUrl, defaultModel, timeoutSeconds);
    }

    @Override
    public String getProviderName() {
        return "GLM";
    }

    @Override
    protected String getDefaultBaseUrl() {
        return "https://open.bigmodel.cn/api/paas/v4";
    }

    @Override
    protected String getDefaultModel() {
        return "glm-4-flash";
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "glm-4-plus",
                "glm-4",
                "glm-4-air",
                "glm-4-airx",
                "glm-4-flash"
        );
    }
}