package com.xingchen.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.mapper.UserApiKeyMapper;
import com.xingchen.backend.service.UserApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private final UserApiKeyMapper userApiKeyMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${agent.default-provider:BAIDU}")
    private String defaultProvider;

    @Value("${baidu.api.key:}")
    private String systemDefaultApiKey;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Map<String, List<String>> PROVIDER_MODELS = Map.of(
            "OPENAI", Arrays.asList("gpt-4o", "gpt-4o-mini", "gpt-4-turbo"),
            "ANTHROPIC", Arrays.asList("claude-3-5-sonnet", "claude-3-opus"),
            "ZHIPU", Arrays.asList("glm-4-flash", "glm-4-plus", "glm-4"),
            "BAIDU", Arrays.asList("qianfan-code-latest", "ernie-bot-4"),
            "AZURE", Arrays.asList("gpt-4o", "gpt-35-turbo"),
            "DEEPSEEK", Arrays.asList("deepseek-chat", "deepseek-reasoner"),
            "CUSTOM", Arrays.asList()
    );

    private static final Map<String, String> PROVIDER_BASE_URLS = Map.of(
            "OPENAI", "https://api.openai.com/v1",
            "ANTHROPIC", "https://api.anthropic.com",
            "ZHIPU", "https://open.bigmodel.cn/api/paas/v4",
            "BAIDU", "https://qianfan.baidubce.com/v2",
            "DEEPSEEK", "https://api.deepseek.com",
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
        UserApiKey existing = getByUserId(userApiKey.getUserId());

        if (existing != null) {
            if (userApiKey.getTemperature() != null) {
                existing.setTemperature(userApiKey.getTemperature());
            }
            if (userApiKey.getMaxTokens() != null) {
                existing.setMaxTokens(userApiKey.getMaxTokens());
            }
            if (userApiKey.getTopP() != null) {
                existing.setTopP(userApiKey.getTopP());
            }
            if (userApiKey.getProvider() != null) {
                existing.setProvider(userApiKey.getProvider());
            }
            if (userApiKey.getBaseUrl() != null) {
                existing.setBaseUrl(userApiKey.getBaseUrl());
            }
            if (userApiKey.getDefaultModel() != null) {
                existing.setDefaultModel(userApiKey.getDefaultModel());
            }
            if (userApiKey.getQuota() != null) {
                existing.setQuota(userApiKey.getQuota());
            }

            if (userApiKey.getApiKeyPlain() != null && !userApiKey.getApiKeyPlain().isBlank()) {
                existing.setApiKeyEncrypted(userApiKey.getApiKeyPlain());
            } else if (userApiKey.getApiKey() != null && !userApiKey.getApiKey().startsWith("ENC:")) {
                existing.setApiKeyEncrypted(userApiKey.getApiKey());
            }

            existing.setUpdateTime(LocalDateTime.now());
            userApiKeyMapper.update(existing);
            return existing;
        } else {
            if (userApiKey.getApiKeyPlain() != null && !userApiKey.getApiKeyPlain().isBlank()) {
                userApiKey.setApiKeyEncrypted(userApiKey.getApiKeyPlain());
            } else if (userApiKey.getApiKey() != null && !userApiKey.getApiKey().startsWith("ENC:")) {
                userApiKey.setApiKeyEncrypted(userApiKey.getApiKey());
            }
            userApiKey.setApiKeyPlain(null);
            userApiKey.setCreateTime(LocalDateTime.now());
            userApiKey.setUpdateTime(LocalDateTime.now());
            userApiKey.setUsed(0);
            userApiKey.setEnabled(1);
            userApiKeyMapper.insert(userApiKey);
            return userApiKey;
        }
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

        log.info(">>>>>>>>>>>>> getEffectiveApiKey called: userId={}, apiKey={}, enabled={}, apiKeyPlain={}",
                userId, 
                apiKey != null ? apiKey.getApiKeyForLog() : "null",
                apiKey != null ? apiKey.getEnabled() : "null",
                apiKey != null ? apiKey.getApiKeyPlain() : "null");

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

            String decrypted = apiKey.getDecryptedApiKey();
            log.debug("API Key 解密结果: {}", decrypted != null ? "成功" : "失败");
            return decrypted;
        }

        log.debug("用户 {} 未配置有效 API Key，使用系统默认. systemDefaultApiKey 配置: {}",
                userId, systemDefaultApiKey != null && !systemDefaultApiKey.isEmpty() ? "有值" : "空");
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
        return Arrays.asList("OPENAI", "ANTHROPIC", "ZHIPU", "BAIDU", "AZURE", "DEEPSEEK", "CUSTOM");
    }

    @Override
    public List<String> getAvailableModels(String provider) {
        return PROVIDER_MODELS.getOrDefault(provider, Arrays.asList());
    }

    @Override
    public List<UserApiKey> getAllForAdmin() {
        return userApiKeyMapper.selectAll();
    }

    public String getBaseUrl(String provider) {
        return PROVIDER_BASE_URLS.getOrDefault(provider, "");
    }
    
    public String getProvider() {
        UserApiKey apiKey = getByUserId(StpUtil.getLoginIdAsLong());
        return apiKey != null ? apiKey.getProvider() : defaultProvider;
    }

    @Override
    public Map<String, Object> validateApiKey(String provider, String apiKey, String baseUrl, String model) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("valid", false);

        try {
            String requestBody = buildValidationRequest(provider, model);
            if (requestBody == null) {
                result.put("message", "不支持的 provider: " + provider);
                return result;
            }

            String actualBaseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : PROVIDER_BASE_URLS.get(provider);
            if (actualBaseUrl == null || actualBaseUrl.isEmpty()) {
                result.put("message", "provider 或 baseUrl 未配置");
                return result;
            }

            Request request = buildValidationRequest(provider, apiKey, actualBaseUrl, model, requestBody);
            if (request == null) {
                result.put("message", "构建请求失败");
                return result;
            }

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    result.put("success", true);
                    result.put("valid", true);
                    result.put("message", "API Key 有效");
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    result.put("message", "验证失败: " + response.code() + " - " + errorBody);
                    result.put("valid", false);
                }
            }
        } catch (IOException e) {
            log.error("验证 API Key 失败", e);
            result.put("message", "连接失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("验证 API Key 异常", e);
            result.put("message", "验证异常: " + e.getMessage());
        }

        return result;
    }

    private String buildValidationRequest(String provider, String model) {
        try {
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", model != null ? model : getDefaultModel(provider));
            requestMap.put("messages", Arrays.asList(
                    Map.of("role", "user", "content", "Hi")
            ));
            requestMap.put("max_tokens", 5);
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            log.error("构建请求体失败", e);
            return null;
        }
    }

    private Request buildValidationRequest(String provider, String apiKey, String baseUrl, String model, String requestBody) {
        String chatEndpoint = getChatEndpoint(provider);
        String url = baseUrl + chatEndpoint;

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON));

        switch (provider.toUpperCase()) {
            case "OPENAI":
                requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
                break;
            case "ANTHROPIC":
                requestBuilder.addHeader("x-api-key", apiKey)
                        .addHeader("anthropic-version", "2023-06-01");
                break;
            case "ZHIPU":
                requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
                break;
            case "BAIDU":
                requestBuilder.addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json");
                break;
            case "AZURE":
                requestBuilder.addHeader("api-key", apiKey);
                break;
            case "DEEPSEEK":
            case "CUSTOM":
                requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
                break;
            default:
                return null;
        }

        return requestBuilder.build();
    }

    private String getChatEndpoint(String provider) {
        return switch (provider.toUpperCase()) {
            case "OPENAI", "ZHIPU", "AZURE", "DEEPSEEK" -> "/chat/completions";
            case "ANTHROPIC" -> "/v1/messages";
            case "BAIDU" -> "/chat/completions";
            default -> "/v1/chat/completions";
        };
    }

    private String getDefaultModel(String provider) {
        List<String> models = PROVIDER_MODELS.get(provider.toUpperCase());
        return models != null && !models.isEmpty() ? models.get(0) : "gpt-3.5-turbo";
    }
}
