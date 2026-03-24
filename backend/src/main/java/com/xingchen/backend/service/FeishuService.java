package com.xingchen.backend.service;

import com.xingchen.backend.entity.FeishuConfig;

public interface FeishuService {
    
    FeishuConfig getByUserId(Long userId);
    
    FeishuConfig saveOrUpdate(FeishuConfig config);
    
    void sendMessage(Long userId, String message);
    
    void sendArticleNotification(Long userId, String articleTitle, Long articleId);
    
    void sendInteractiveCard(Long userId, String title, String content, String articleId);
}
