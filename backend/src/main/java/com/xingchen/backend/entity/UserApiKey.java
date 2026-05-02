package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.xingchen.backend.security.ApiKeyEncryptionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 用户 API Key 实体
 * <p>
 * 安全设计：
 * 1. apiKey 字段存储 AES-256 加密后的密文
 * 2. apiKeyPlain 为 @Transient 字段，仅用于临时明文传输，不存入数据库
 * 3. 日志中绝不打印 apiKey 明文
 */
@Data
@Table(value = "user_api_key")
@Slf4j
public class UserApiKey {

    private static ApiKeyEncryptionService encryptionService;

    public static void setEncryptionService(ApiKeyEncryptionService service) {
        encryptionService = service;
    }

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("provider")
    private String provider;

    /**
     * 加密后的 API Key（AES-256）
     * 存储格式: base64(iv+ciphertext)
     */
    @Column("api_key")
    private String apiKey;

    /**
     * 临时明文 API Key（不存入数据库）
     * 仅用于接收前端传入的新 API Key 或解密后临时使用
     */
    @Column(ignore = true)
    private String apiKeyPlain;

    @Column("base_url")
    private String baseUrl;

    @Column("default_model")
    private String defaultModel;

    @Column("enabled")
    private Integer enabled;

    @Column("quota")
    private Integer quota;

    @Column("used")
    private Integer used;

    @Column("temperature")
    private Double temperature;

    @Column("max_tokens")
    private Integer maxTokens;

    @Column("top_p")
    private Double topP;

    @Column("expire_at")
    private LocalDateTime expireAt;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    /**
     * 安全地获取 API Key 的日志表示（掩码形式）
     */
    public String getApiKeyForLog() {
        if (apiKey == null || apiKey.isBlank()) {
            return "[EMPTY]";
        }
        if (apiKey.length() > 20) {
            return "[ENCRYPTED]";
        }
        return "***";
    }

    /**
     * 获取解密后的 API Key
     */
    public String getDecryptedApiKey() {
        if (apiKeyPlain != null && !apiKeyPlain.isBlank()) {
            return apiKeyPlain;
        }
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException("ApiKeyEncryptionService 未注入");
        }
        return encryptionService.decrypt(apiKey);
    }

    /**
     * 设置 API Key（自动加密存储）
     */
    public void setApiKeyEncrypted(String plainApiKey) {
        if (plainApiKey == null || plainApiKey.isBlank()) {
            this.apiKey = null;
            this.apiKeyPlain = null;
            return;
        }
        this.apiKeyPlain = plainApiKey;
        if (encryptionService == null) {
            throw new IllegalStateException("ApiKeyEncryptionService 未注入");
        }
        this.apiKey = encryptionService.encrypt(plainApiKey);
    }

    @Override
    public String toString() {
        return "UserApiKey{" +
                "id=" + id +
                ", userId=" + userId +
                ", provider='" + provider + '\'' +
                ", apiKey=" + getApiKeyForLog() +
                ", baseUrl='" + baseUrl + '\'' +
                ", defaultModel='" + defaultModel + '\'' +
                ", enabled=" + enabled +
                ", quota=" + quota +
                ", used=" + used +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", topP=" + topP +
                ", expireAt=" + expireAt +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
