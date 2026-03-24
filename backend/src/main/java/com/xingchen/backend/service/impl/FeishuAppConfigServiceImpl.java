package com.xingchen.backend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.FeishuAppConfig;
import com.xingchen.backend.mapper.FeishuAppConfigMapper;
import com.xingchen.backend.service.FeishuAppConfigService;
import com.xingchen.backend.service.FeishuWebSocketManager;
import com.xingchen.backend.util.ApiKeyEncryptor;
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

        // 解密 App Secret
        if (config != null && config.getAppSecret() != null && !config.getAppSecret().isEmpty()) {
            try {
                config.setAppSecret(ApiKeyEncryptor.decrypt(config.getAppSecret()));
            } catch (Exception e) {
                log.warn("解密飞书 App Secret 失败: {}", e.getMessage());
            }
        }

        return config;
    }

    @Override
    public FeishuAppConfig saveOrUpdate(FeishuAppConfig config) {
        FeishuAppConfig existing = getByUserId(config.getUserId());

        // 加密 App Secret
        if (config.getAppSecret() != null && !config.getAppSecret().isEmpty()) {
            config.setAppSecret(ApiKeyEncryptor.encrypt(config.getAppSecret()));
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
            String appSecret = config.getAppSecret();
            // 解密 App Secret 用于连接
            if (appSecret != null && !appSecret.isEmpty()) {
                try {
                    appSecret = ApiKeyEncryptor.decrypt(appSecret);
                } catch (Exception e) {
                    log.error("解密 App Secret 失败: {}", e.getMessage());
                }
            }
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

        // TODO: 实现实际的连接测试
        // 这里可以调用飞书 API 测试凭证是否有效

        return config.getAppId() != null && !config.getAppId().isEmpty()
                && config.getAppSecret() != null && !config.getAppSecret().isEmpty();
    }
}
