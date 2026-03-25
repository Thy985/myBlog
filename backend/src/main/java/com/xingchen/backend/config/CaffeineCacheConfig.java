package com.xingchen.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CaffeineCacheConfig.class);

    @Value("${model.cache.ttl:30}")
    private int cacheTtlMinutes;

    @Value("${model.cache.max-size:100}")
    private int cacheMaxSize;

    @Bean
    public Cache<Long, ChatLanguageModel> userModelCache() {
        log.info("Initializing userModelCache with TTL={} minutes, maxSize={}", cacheTtlMinutes, cacheMaxSize);
        return Caffeine.newBuilder()
                .expireAfterAccess(cacheTtlMinutes, TimeUnit.MINUTES)
                .weakValues()
                .maximumSize(cacheMaxSize)
                .recordStats()
                .build();
    }
}
