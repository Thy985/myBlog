package com.xingchen.backend.service.impl;

import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.service.MailService;
import com.xingchen.backend.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;
    
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 15;
    private static final int CODE_EXPIRE_RANDOM_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final String CODE_KEY_PREFIX = "verification:code:";
    private static final String SEND_TIME_KEY_PREFIX = "verification:sendtime:";
    
    private static final java.util.Random RANDOM = new java.util.Random();

    @Override
    public String generateAndSendCode(String email, String type) {
        String codeKey = CODE_KEY_PREFIX + type + ":" + email;
        String sendTimeKey = SEND_TIME_KEY_PREFIX + type + ":" + email;
        
        String lastSendTimeStr = redisTemplate.opsForValue().get(sendTimeKey);
        if (lastSendTimeStr != null) {
            try {
                long lastSendTime = Long.parseLong(lastSendTimeStr);
                long elapsedSeconds = (System.currentTimeMillis() - lastSendTime) / 1000;
                if (elapsedSeconds < SEND_INTERVAL_SECONDS) {
                    long remainingSeconds = SEND_INTERVAL_SECONDS - elapsedSeconds;
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "发送过于频繁，请" + remainingSeconds + "秒后再试");
                }
            } catch (NumberFormatException e) {
                log.warn("解析上次发送时间失败: {}", lastSendTimeStr);
            }
        }
        
        String code = generateSecureCode();
        
        int expireTime = CODE_EXPIRE_MINUTES + RANDOM.nextInt(CODE_EXPIRE_RANDOM_MINUTES);
        
        redisTemplate.opsForValue().set(codeKey, code, expireTime, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(sendTimeKey, String.valueOf(System.currentTimeMillis()), expireTime, TimeUnit.MINUTES);
        
        log.info("验证码已生成 - 邮箱：{}, 类型：{}, 过期时间：{}分钟", email, type, expireTime);
        
        log.info("准备发送验证码邮件 - 邮箱: {}, 类型: {}, 验证码: {}", email, type, code);
        mailService.sendVerificationCode(email, code, type);
        
        log.info("验证码发送任务已提交 - 邮箱: {}, 类型: {}", email, type);
        
        return code;
    }

    @Override
    public void verifyCode(String email, String code, String type) {
        log.info("验证验证码 - 邮箱: {}, 类型: {}", email, type);
        
        if (email == null || code == null || type == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        
        String codeKey = CODE_KEY_PREFIX + type + ":" + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        
        if (storedCode != null && storedCode.equals(code)) {
            invalidateCode(email, type);
            log.info("验证码验证成功 - 邮箱: {}", email);
            return;
        }
        
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
    }

    @Override
    public void invalidateCode(String email, String type) {
        String codeKey = CODE_KEY_PREFIX + type + ":" + email;
        String sendTimeKey = SEND_TIME_KEY_PREFIX + type + ":" + email;
        redisTemplate.delete(codeKey);
        redisTemplate.delete(sendTimeKey);
    }

    private String generateSecureCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
