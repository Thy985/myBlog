package com.xingchen.backend.service;

import com.xingchen.backend.entity.UserApiKey;

import java.util.List;

public interface UserApiKeyService {
    
    UserApiKey getByUserId(Long userId);
    
    UserApiKey saveOrUpdate(UserApiKey userApiKey);
    
    void deleteByUserId(Long userId);
    
    boolean isApiKeyValid(Long userId);
    
    String getEffectiveApiKey(Long userId);
    
    void incrementUsage(Long userId);
    
    List<String> getAvailableProviders();
    
    List<String> getAvailableModels(String provider);
}
