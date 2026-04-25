package com.xingchen.backend.ai.llm;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户级 GLM Provider
 * 每个用户独立的 Provider 实例，使用用户自己的 API Key
 */
@Slf4j
public class UserGLMProvider extends BaseGLMProvider {
    
    public UserGLMProvider(String apiKey, String baseUrl, String defaultModel) {
        super(apiKey, baseUrl, defaultModel, 60);
    }
}