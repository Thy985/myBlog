package com.xingchen.backend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.FeishuAppConfig;
import com.xingchen.backend.mapper.FeishuAppConfigMapper;
import com.xingchen.backend.service.FeishuAppConfigService;
import com.xingchen.backend.service.FeishuWebSocketManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeishuAppConfigServiceImpl implements FeishuAppConfigService {

    private final FeishuAppConfigMapper feishuAppConfigMapper;
    private final FeishuWebSocketManager feishuWebSocketManager;

    @Override
    public FeishuAppConfig getByUserId(Long userId) {
        FeishuAppConfig config = feishuAppConfigMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(FeishuAppConfig::getUserId).eq(userId)
        );

        // App Secret 保持加密状态，需要时通过 getDecryptedAppSecret() 解密

        return config;
    }

    @Override
    public FeishuAppConfig saveOrUpdate(FeishuAppConfig config) {
        FeishuAppConfig existing = getByUserId(config.getUserId());

        // 加密 App Secret（如果传入的是明文）
        if (config.getAppSecret() != null && !config.getAppSecret().isEmpty()
                && !config.getAppSecret().startsWith("ENC:")) {
            config.setAppSecretEncrypted(config.getAppSecret());
        }

        if (existing != null) {
            config.setId(existing.getId());
            config.setUpdateTime(LocalDateTime.now());
            // 如果没有传入新的 App Secret，保留原有的
            if (config.getAppSecret() == null || config.getAppSecret().isEmpty()) {
                config.setAppSecret(existing.getAppSecret());
            }
            feishuAppConfigMapper.update(config);
        } else {
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            config.setConnectionStatus("DISCONNECTED");
            feishuAppConfigMapper.insert(config);
        }

        // 动态启动或停止 WebSocket 连接
        Long userId = config.getUserId();
        if (config.getEnabled() != null && config.getEnabled() == 1) {
            // 启用状态，启动连接
            String appId = config.getAppId();
            String appSecret = config.getDecryptedAppSecret();
            feishuWebSocketManager.startConnection(userId, appId, appSecret);
            config.setConnectionStatus("CONNECTED");
            config.setLastConnectedAt(LocalDateTime.now());
            feishuAppConfigMapper.update(config);
        } else {
            // 禁用状态，停止连接
            feishuWebSocketManager.stopConnection(userId);
            config.setConnectionStatus("DISCONNECTED");
            feishuAppConfigMapper.update(config);
        }

        return config;
    }

    @Override
    public void deleteByUserId(Long userId) {
        feishuAppConfigMapper.deleteByQuery(
                QueryWrapper.create()
                        .where(FeishuAppConfig::getUserId).eq(userId)
        );
    }

    @Override
    public boolean testConnection(Long userId) {
        FeishuAppConfig config = getByUserId(userId);
        if (config == null || config.getEnabled() == 0) {
            return false;
        }

        // 实际的连接测试待实现
        // 这里可以调用飞书 API 测试凭证是否有效

        return config.getAppId() != null && !config.getAppId().isEmpty()
                && config.getAppSecret() != null && !config.getAppSecret().isEmpty();
    }
}
