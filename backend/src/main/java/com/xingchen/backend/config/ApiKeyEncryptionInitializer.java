package com.xingchen.backend.config;

import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.security.ApiKeyEncryptionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 初始化 API Key 加密服务的静态引用
 * 由于 UserApiKey 是 MyBatis 实体（非 Spring Bean），
 * 需要通过此组件将加密服务注入到实体的静态字段中
 */
@Component
@RequiredArgsConstructor
public class ApiKeyEncryptionInitializer {

    private final ApiKeyEncryptionService apiKeyEncryptionService;

    @PostConstruct
    public void init() {
        UserApiKey.setEncryptionService(apiKeyEncryptionService);
    }
}
