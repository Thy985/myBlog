package com.xingchen.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.mapper.UserApiKeyMapper;
import com.xingchen.backend.service.UserApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private final UserApiKeyMapper userApiKeyMapper;

    @Value("${agent.default-provider:BAIDU}")
    private String defaultProvider;

    @Value("${baidu.api.key:}")
    private String systemDefaultApiKey;

    private static final Map<String, List<String>> PROVIDER_MODELS = Map.of(
            "OPENAI", Arrays.asList("gpt-4o", "gpt-4o-mini", "gpt-4-turbo"),
            "ANTHROPIC", Arrays.asList("claude-3-5-sonnet", "claude-3-opus"),
            "ZHIPU", Arrays.asList("glm-4-flash", "glm-4-plus", "glm-4"),
            "BAIDU", Arrays.asList("qianfan-code-latest", "ernie-bot-4"),
            "AZURE", Arrays.asList("gpt-4o", "gpt-35-turbo"),
            "CUSTOM", Arrays.asList()
    );

    private static final Map<String, String> PROVIDER_BASE_URLS = Map.of(
            "OPENAI", "https://api.openai.com/v1",
            "ANTHROPIC", "https://api.anthropic.com",
            "ZHIPU", "https://open.bigmodel.cn/api/paas/v4",
            "BAIDU", "https://qianfan.baidubce.com/v2",
            "CUSTOM", ""
    );

    @Override
    public UserApiKey getByUserId(Long userId) {
        return userApiKeyMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(UserApiKey::getUserId).eq(userId)
        );
    }

    @Override
    public UserApiKey saveOrUpdate(UserApiKey userApiKey) {
        // 保存前加密 API Key
        if (userApiKey.getApiKey() != null && !userApiKey.getApiKey().startsWith("ENC:")) {
            userApiKey.setApiKey(com.xingchen.backend.util.ApiKeyEncryptor.encrypt(userApiKey.getApiKey()));
        }

        UserApiKey existing = getByUserId(userApiKey.getUserId());
        
        if (existing != null) {
            userApiKey.setId(existing.getId());
            userApiKey.setUpdateTime(LocalDateTime.now());
            userApiKeyMapper.update(userApiKey);
        } else {
            userApiKey.setCreateTime(LocalDateTime.now());
            userApiKey.setUpdateTime(LocalDateTime.now());
            userApiKey.setUsed(0);
            userApiKey.setEnabled(1);
            userApiKeyMapper.insert(userApiKey);
        }
        return userApiKey;
    }

    @Override
    public void deleteByUserId(Long userId) {
        userApiKeyMapper.deleteByQuery(
                QueryWrapper.create()
                        .where(UserApiKey::getUserId).eq(userId)
        );
    }

    @Override
    public boolean isApiKeyValid(Long userId) {
        UserApiKey apiKey = getByUserId(userId);
        
        if (apiKey == null || apiKey.getEnabled() == 0) {
            return systemDefaultApiKey != null && !systemDefaultApiKey.isEmpty();
        }
        
        if (apiKey.getExpireAt() != null && apiKey.getExpireAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        if (apiKey.getQuota() != null && apiKey.getUsed() >= apiKey.getQuota()) {
            return false;
        }
        
        return apiKey.getApiKey() != null && !apiKey.getApiKey().isEmpty();
    }

    @Override
    public String getEffectiveApiKey(Long userId) {
        UserApiKey apiKey = getByUserId(userId);
        
        if (apiKey != null && apiKey.getEnabled() == 1 && 
            apiKey.getApiKey() != null && !apiKey.getApiKey().isEmpty()) {
            
            if (apiKey.getExpireAt() != null && apiKey.getExpireAt().isBefore(LocalDateTime.now())) {
                log.warn("用户 {} 的 API Key 已过期", userId);
                return systemDefaultApiKey;
            }
            
            if (apiKey.getQuota() != null && apiKey.getUsed() >= apiKey.getQuota()) {
                log.warn("用户 {} 的 API Key 配额已用完", userId);
                return systemDefaultApiKey;
            }
            
            // 解密后返回
            return com.xingchen.backend.util.ApiKeyEncryptor.decrypt(apiKey.getApiKey());
        }
        
        return systemDefaultApiKey;
    }

    @Override
    public void incrementUsage(Long userId) {
        UserApiKey apiKey = getByUserId(userId);
        if (apiKey != null) {
            apiKey.setUsed(apiKey.getUsed() + 1);
            apiKey.setUpdateTime(LocalDateTime.now());
            userApiKeyMapper.update(apiKey);
        }
    }

    @Override
    public List<String> getAvailableProviders() {
        return Arrays.asList("OPENAI", "ANTHROPIC", "ZHIPU", "BAIDU", "AZURE", "CUSTOM");
    }

    @Override
    public List<String> getAvailableModels(String provider) {
        return PROVIDER_MODELS.getOrDefault(provider, Arrays.asList());
    }
    
    public String getBaseUrl(String provider) {
        return PROVIDER_BASE_URLS.getOrDefault(provider, "");
    }
    
    public String getProvider() {
        UserApiKey apiKey = getByUserId(StpUtil.getLoginIdAsLong());
        return apiKey != null ? apiKey.getProvider() : defaultProvider;
    }
}
