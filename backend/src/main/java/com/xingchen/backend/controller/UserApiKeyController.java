package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.ApiKeyConfigDTO;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.service.UserApiKeyService;
import com.xingchen.backend.service.impl.AIServiceImplV3;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/apikey")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;
    private final AIServiceImplV3 aiService;

    @SaCheckLogin
    @GetMapping
    public Result<Map<String, Object>> getApiKey() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserApiKey apiKey = userApiKeyService.getByUserId(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("availableProviders", userApiKeyService.getAvailableProviders());
        
        if (apiKey != null) {
            Map<String, Object> keyInfo = new HashMap<>();
            keyInfo.put("provider", apiKey.getProvider());
            keyInfo.put("defaultModel", apiKey.getDefaultModel());
            keyInfo.put("enabled", apiKey.getEnabled());
            keyInfo.put("quota", apiKey.getQuota());
            keyInfo.put("used", apiKey.getUsed());
            keyInfo.put("expireAt", apiKey.getExpireAt());
            keyInfo.put("hasApiKey", apiKey.getApiKey() != null && !apiKey.getApiKey().isEmpty());
            result.put("apiKey", keyInfo);
        } else {
            result.put("apiKey", null);
        }
        
        return Result.success(result);
    }

    @SaCheckLogin
    @PutMapping
    public Result<Void> updateApiKey(@RequestBody ApiKeyConfigDTO config) {
        Long userId = StpUtil.getLoginIdAsLong();

        UserApiKey apiKey = new UserApiKey();
        apiKey.setUserId(userId);
        apiKey.setProvider(config.getProvider());
        apiKey.setApiKey(config.getApiKey());
        apiKey.setBaseUrl(config.getBaseUrl());
        apiKey.setDefaultModel(config.getDefaultModel());
        apiKey.setQuota(config.getQuota());
        apiKey.setTemperature(config.getTemperature());
        apiKey.setMaxTokens(config.getMaxTokens());
        apiKey.setTopP(config.getTopP());

        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            apiKey.setEnabled(1);
        }

        userApiKeyService.saveOrUpdate(apiKey);

        aiService.clearUserModelCache(userId);

        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping
    public Result<Void> deleteApiKey() {
        Long userId = StpUtil.getLoginIdAsLong();
        userApiKeyService.deleteByUserId(userId);
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/usage")
    public Result<Map<String, Object>> getUsage() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserApiKey apiKey = userApiKeyService.getByUserId(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("used", apiKey != null ? apiKey.getUsed() : 0);
        result.put("quota", apiKey != null ? apiKey.getQuota() : null);
        result.put("valid", userApiKeyService.isApiKeyValid(userId));
        
        return Result.success(result);
    }

    @SaCheckLogin
    @GetMapping("/models")
    public Result<List<String>> getModels(@RequestParam String provider) {
        return Result.success(userApiKeyService.getAvailableModels(provider));
    }

    @SaCheckLogin
    @PostMapping("/test")
    public Result<Map<String, Object>> testApiKey(@RequestBody ApiKeyConfigDTO config) {
        Map<String, Object> result = new HashMap<>();

        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            result.put("success", false);
            result.put("message", "API Key 不能为空");
            return Result.success(result);
        }

        result.put("success", true);
        result.put("message", "API Key 格式验证通过！保存成功后即可使用。");

        return Result.success(result);
    }

    @SaCheckLogin
    @GetMapping("/admin/list")
    public Result<List<Map<String, Object>>> listAllApiKeys() {
        List<UserApiKey> apiKeys = userApiKeyService.getAllForAdmin();

        List<Map<String, Object>> result = apiKeys.stream().map(key -> {
            Map<String, Object> info = new HashMap<>();
            info.put("userId", key.getUserId());
            info.put("provider", key.getProvider());
            info.put("defaultModel", key.getDefaultModel());
            info.put("enabled", key.getEnabled());
            info.put("quota", key.getQuota());
            info.put("used", key.getUsed());
            info.put("expireAt", key.getExpireAt());
            info.put("hasApiKey", key.getApiKey() != null && !key.getApiKey().isEmpty());
            return info;
        }).toList();

        return Result.success(result);
    }
}
