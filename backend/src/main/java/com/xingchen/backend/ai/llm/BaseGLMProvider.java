package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class BaseGLMProvider extends BaseOpenAICompatibleProvider {

    protected BaseGLMProvider(String apiKey, String baseUrl, String defaultModel, int timeoutSeconds) {
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