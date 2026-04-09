package com.xingchen.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256 加密工具类
 * 用于 API Key 等敏感数据的加密存储
 * <p>
 * 安全说明：
 * 1. 使用配置的主密钥（encryption.master-key）派生加密密钥
 * 2. 支持密钥轮换（通过更新配置并重新加密数据）
 * 3. 每台服务器应配置独立的 master-key
 */
@Slf4j
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 32; // 256 bits

    // 从配置读取主密钥
    @Value("${encryption.master-key:${random.uuid}}")
    private String masterKeyConfig;

    // 派生后的加密密钥
    private static byte[] SECRET_KEY;

    // 单例实例（用于静态方法调用）
    private static AesUtil instance;

    @PostConstruct
    public void init() {
        try {
            log.debug("===========================================");
            log.debug("AesUtil.init() 被调用!");
            log.debug("masterKeyConfig = {}", masterKeyConfig);
            log.debug("===========================================");

            String masterKey = masterKeyConfig != null && !masterKeyConfig.isBlank()
                    ? masterKeyConfig
                    : System.getProperty("user.name", "default") + System.currentTimeMillis();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            SECRET_KEY = Arrays.copyOf(md.digest(masterKey.getBytes(StandardCharsets.UTF_8)), KEY_LENGTH);

            instance = this;
            
            String keySource = masterKeyConfig != null && !masterKeyConfig.contains("${") 
                    ? "配置文件" : "系统属性/随机生成";
            log.info("===========================================");
            log.info("AES-256-GCM 加密工具初始化完成");
            log.info("密钥来源: {}", keySource);
            log.info("主密钥: {}", masterKey.length() > 20 ? masterKey.substring(0, 10) + "..." : masterKey);
            log.info("===========================================");
        } catch (Exception e) {
            throw new RuntimeException("AES 加密初始化失败", e);
        }
    }

    /**
     * 获取实例（用于非 Spring 管理的类）
     */
    public static AesUtil getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AesUtil 尚未初始化");
        }
        return instance;
    }

    /**
     * 加密明文
     *
     * @param plainText 明文
     * @return 加密后的字符串 (格式: ENC:base64)
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        // 已加密则不再加密
        if (plainText.startsWith("ENC:")) {
            return plainText;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 密文 拼接后 Base64
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return "ENC:" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES 加密失败: {}", e.getMessage());
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密密文
     *
     * @param encryptedText 密文 (格式: ENC:base64)
     * @return 明文
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }
        // 未加密则直接返回
        if (!encryptedText.startsWith("ENC:")) {
            return encryptedText;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText.substring(4));

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败: {}", e.getMessage());
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 检查文本是否已加密
     *
     * @param text 文本
     * @return true 如果已加密
     */
    public static boolean isEncrypted(String text) {
        return text != null && text.startsWith("ENC:");
    }

    /**
     * 安全地获取 API Key 的掩码形式（用于日志）
     * 绝不返回明文
     *
     * @param apiKey API Key（加密或明文）
     * @return 掩码形式，如 "sk-***xxxx"
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[EMPTY]";
        }

        String plainText;
        if (apiKey.startsWith("ENC:")) {
            try {
                plainText = decrypt(apiKey);
            } catch (Exception e) {
                log.warn("无法解密 API Key 进行掩码处理");
                return "[ENCRYPTED]";
            }
        } else {
            plainText = apiKey;
        }

        if (plainText.length() <= 10) {
            return "***";
        }
        return plainText.substring(0, 3) + "***" + plainText.substring(plainText.length() - 4);
    }

    /**
     * 安全地记录 API Key 信息（绝不打印明文）
     *
     * @param apiKey API Key
     * @return 用于日志的安全字符串
     */
    public static String toLogSafeString(String apiKey) {
        if (apiKey == null) {
            return "null";
        }
        if (apiKey.startsWith("ENC:")) {
            return "[ENCRYPTED:" + maskApiKey(apiKey) + "]";
        }
        return "[PLAIN:" + maskApiKey(apiKey) + "]";
    }
}
