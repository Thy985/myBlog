package com.xingchen.backend.service;

import com.xingchen.backend.entity.FeishuAppConfig;

public interface FeishuAppConfigService {

    FeishuAppConfig getByUserId(Long userId);

    FeishuAppConfig saveOrUpdate(FeishuAppConfig config);

    void deleteByUserId(Long userId);

    boolean testConnection(Long userId);
}
