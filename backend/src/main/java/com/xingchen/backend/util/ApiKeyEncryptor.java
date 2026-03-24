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
 * API Key 加密工具
 * 使用 AES-GCM 加密，密钥基于机器指纹生成
 */
@Slf4j
public class ApiKeyEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    // 基于机器信息生成稳定的密钥
    private static final byte[] SECRET_KEY;

    static {
        try {
            String machineId = System.getProperty("user.name", "default")
                    + System.getProperty("os.name", "unknown")
                    + System.getProperty("user.home", "/tmp");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            SECRET_KEY = Arrays.copyOf(md.digest(machineId.getBytes(StandardCharsets.UTF_8)), 16);
        } catch (Exception e) {
            throw new RuntimeException("加密初始化失败", e);
        }
    }

    /**
     * 加密 API Key
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        if (plainText.startsWith("ENC:")) return plainText; // 已加密

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
            log.error("加密失败: {}", e.getMessage());
            return plainText;
        }
    }

    /**
     * 解密 API Key
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) return encryptedText;
        if (!encryptedText.startsWith("ENC:")) return encryptedText; // 未加密的明文

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
            log.error("解密失败: {}", e.getMessage());
            return encryptedText;
        }
    }
}
