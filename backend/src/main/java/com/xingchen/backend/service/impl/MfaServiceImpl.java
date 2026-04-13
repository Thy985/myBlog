package com.xingchen.backend.service.impl;

import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaServiceImpl implements MfaService {

    private final UserMapper userMapper;
    
    private static final int TIME_STEP = 30;
    private static final int CODE_DIGITS = 6;
    private static final int WINDOW_SIZE = 1;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Override
    public MfaSetupResult setupMfa(Long userId, String username) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getMfaEnabled() != null && user.getMfaEnabled() == 1) {
            throw new BusinessException(ErrorCode.MFA_ALREADY_ENABLED);
        }

        String secret = generateSecret();

        MfaSetupResult result = new MfaSetupResult();
        result.setSecret(secret);
        result.setQrCodeUrl("otpauth://totp/" + username + "?secret=" + secret + "&issuer=MyBlog");
        result.setManualEntryKey(secret);

        user.setMfaSecret(secret);
        userMapper.update(user);

        return result;
    }

    @Override
    public void verifyCode(Long userId, String code) {
        User user = userMapper.selectOneById(userId);
        if (user == null || user.getMfaSecret() == null) {
            throw new BusinessException(ErrorCode.MFA_NOT_ENABLED, "MFA未启用");
        }

        if (!verifyTotpCode(user.getMfaSecret(), code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
    }

    @Override
    public void enableMfa(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setMfaEnabled(1);
        user.setMfaType("TOTP");
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public void disableMfa(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setMfaEnabled(0);
        user.setMfaSecret(null);
        user.setMfaType(null);
        user.setBackupCodes(null);
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public boolean isMfaEnabled(Long userId) {
        User user = userMapper.selectOneById(userId);
        return user != null && user.getMfaEnabled() != null && user.getMfaEnabled() == 1;
    }

    private String generateSecret() {
        SecureRandom random = new SecureRandom();
        StringBuilder secret = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            secret.append(BASE32_CHARS.charAt(random.nextInt(BASE32_CHARS.length())));
        }
        return secret.toString();
    }

    private boolean verifyTotpCode(String secret, String code) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        
        try {
            long currentTimeStep = getCurrentTimeStep();
            byte[] secretBytes = base32Decode(secret);
            
            for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
                long timeStep = currentTimeStep + i;
                String expectedCode = generateTotpCode(secretBytes, timeStep);
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("TOTP验证失败: {}", e.getMessage());
            return false;
        }
    }

    private long getCurrentTimeStep() {
        return System.currentTimeMillis() / 1000 / TIME_STEP;
    }

    private String generateTotpCode(byte[] secret, long timeStep) throws Exception {
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();
        
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secret, HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(timeBytes);
        
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                   | ((hash[offset + 1] & 0xFF) << 16)
                   | ((hash[offset + 2] & 0xFF) << 8)
                   | (hash[offset + 3] & 0xFF);
        
        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    private byte[] base32Decode(String base32) {
        base32 = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        
        int length = base32.length();
        byte[] result = new byte[length * 5 / 8];
        int buffer = 0;
        int bufferBits = 0;
        int index = 0;
        
        for (int i = 0; i < length; i++) {
            int value = BASE32_CHARS.indexOf(base32.charAt(i));
            if (value < 0) continue;
            
            buffer = (buffer << 5) | value;
            bufferBits += 5;
            
            if (bufferBits >= 8) {
                bufferBits -= 8;
                result[index++] = (byte) (buffer >> bufferBits);
            }
        }
        
        return Arrays.copyOf(result, index);
    }
}
