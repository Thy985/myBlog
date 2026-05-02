package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户级 DeepSeek Provider
 * 每个用户独立的 Provider 实例，使用用户自己的 DeepSeek API Key
 */
@Slf4j
public class UserDeepSeekProvider extends BaseOpenAICompatibleProvider {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    public UserDeepSeekProvider(String apiKey, String baseUrl, String defaultModel) {
        super(apiKey,
              (baseUrl == null || baseUrl.isBlank()) ? null : baseUrl,
              (defaultModel == null || defaultModel.isBlank()) ? null : defaultModel,
              60);
        log.info("UserDeepSeekProvider 创建成功: apiKey={}, baseUrl={}, defaultModel={}",
                apiKey != null ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) : null,
                this.baseUrl, this.defaultModel);
    }

    @Override
    public String getProviderName() {
        return "DeepSeek";
    }

    @Override
    protected String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    protected String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "deepseek-v4-flash",
                "deepseek-v4-pro",
                "deepseek-chat",
                "deepseek-coder",
                "deepseek-math"
        );
    }
}
