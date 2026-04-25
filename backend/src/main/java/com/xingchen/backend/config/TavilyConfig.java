package com.xingchen.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TavilyConfig {

    @Value("${tavily.api.key:}")
    private String apiKey;

    @Value("${tavily.api.base-url:https://api.tavily.com}")
    private String baseUrl;

    @Value("${tavily.search.max-results:10}")
    private int maxResults;

    @Value("${tavily.search.timeout:30}")
    private int timeout;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${");
    }
}
