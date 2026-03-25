package com.xingchen.backend.util;

import lombok.extern.slf4j.Slf4j;

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
 */
@Slf4j
public class AesUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 32; // 256 bits

    // 基于机器信息生成稳定的密钥
    private static final byte[] SECRET_KEY;

    static {
        try {
            String machineId = System.getProperty("user.name", "default")
                    + System.getProperty("os.name", "unknown")
                    + System.getProperty("user.home", "/tmp");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            SECRET_KEY = Arrays.copyOf(md.digest(machineId.getBytes(StandardCharsets.UTF_8)), KEY_LENGTH);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密初始化失败", e);
        }
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

        // 掩码处理：保留前 3 位和后 4 位
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
