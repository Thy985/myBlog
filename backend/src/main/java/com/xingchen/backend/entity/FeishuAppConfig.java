package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 飞书应用配置表（WebSocket 长连接模式）
 */
@Data
@Table(value = "feishu_app_config")
@Slf4j
public class FeishuAppConfig {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("app_id")
    private String appId;

    /**
     * 加密存储的 App Secret
     * 格式: ENC:base64(iv+ciphertext)
     */
    @Column("app_secret")
    private String appSecret;

    /**
     * 临时明文 App Secret（不存入数据库）
     * 仅用于接收前端传入或解密后临时使用
     */
    @Column(ignore = true)
    private String appSecretPlain;

    @Column("enabled")
    private Integer enabled;

    @Column("connection_status")
    private String connectionStatus;

    @Column("last_connected_at")
    private LocalDateTime lastConnectedAt;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    /**
     * 安全地获取 App Secret 的日志表示（掩码形式）
     */
    public String getAppSecretForLog() {
        if (appSecret == null || appSecret.isBlank()) {
            return "[EMPTY]";
        }
        if (appSecret.startsWith("ENC:")) {
            return "[ENCRYPTED]";
        }
        // 明文情况（不应该发生）- 返回掩码
        if (appSecret.length() <= 10) {
            return "***";
        }
        return appSecret.substring(0, 3) + "***" + appSecret.substring(appSecret.length() - 4);
    }

    /**
     * 获取解密后的 App Secret
     */
    public String getDecryptedAppSecret() {
        if (appSecretPlain != null && !appSecretPlain.isBlank()) {
            return appSecretPlain;
        }
        if (appSecret == null || appSecret.isBlank()) {
            return null;
        }
        return com.xingchen.backend.util.AesUtil.getInstance().decrypt(appSecret);
    }

    /**
     * 设置 App Secret（自动加密）
     */
    public void setAppSecretEncrypted(String plainAppSecret) {
        if (plainAppSecret == null || plainAppSecret.isBlank()) {
            this.appSecret = null;
            this.appSecretPlain = null;
            return;
        }
        this.appSecretPlain = plainAppSecret;
        this.appSecret = com.xingchen.backend.util.AesUtil.getInstance().encrypt(plainAppSecret);
    }

    @Override
    public String toString() {
        return "FeishuAppConfig{" +
                "id=" + id +
                ", userId=" + userId +
                ", appId='" + appId + '\'' +
                ", appSecret=" + getAppSecretForLog() +
                ", enabled=" + enabled +
                ", connectionStatus='" + connectionStatus + '\'' +
                '}';
    }
}
