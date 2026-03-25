package com.xingchen.backend.service;

import com.xingchen.backend.entity.UserApiKey;

import java.util.List;
import java.util.Map;

public interface UserApiKeyService {
    
    UserApiKey getByUserId(Long userId);
    
    UserApiKey saveOrUpdate(UserApiKey userApiKey);
    
    void deleteByUserId(Long userId);
    
    boolean isApiKeyValid(Long userId);
    
    String getEffectiveApiKey(Long userId);
    
    void incrementUsage(Long userId);
    
    List<String> getAvailableProviders();
    
    List<String> getAvailableModels(String provider);

    /**
     * 获取所有用户的 API Key 状态（管理员用，不含明文）
     */
    List<UserApiKey> getAllForAdmin();

    /**
     * 验证 API Key 是否有效（通过发送测试请求）
     */
    Map<String, Object> validateApiKey(String provider, String apiKey, String baseUrl, String model);
}
