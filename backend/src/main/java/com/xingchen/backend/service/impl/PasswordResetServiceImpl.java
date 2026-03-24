package com.xingchen.backend.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.xingchen.backend.common.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.PasswordResetService;
import com.xingchen.backend.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserMapper userMapper;
    private final VerificationCodeService verificationCodeService;

    @Override
    public void sendResetCode(String email) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            log.warn("密码重置请求: 邮箱不存在 - {}", email);
            return;
        }
        
        verificationCodeService.generateAndSendCode(email, "reset");
        
        log.info("密码重置验证码已发送 - 邮箱: {}", email);
    }

    @Override
    public void verifyCode(String email, String code) {
        verificationCodeService.verifyCode(email, code, "reset");
    }

    @Override
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        verificationCodeService.verifyCode(email, code, "reset");
        
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setPassword(BCrypt.hashpw(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);
        
        verificationCodeService.invalidateCode(email, "reset");
        
        log.info("密码重置成功 - 用户ID: {}, 邮箱: {}", user.getId(), email);
    }

    @Override
    public String getResetToken(String email, String code) {
        return null;
    }

    @Override
    @Transactional
    public void resetPasswordByToken(String token, String newPassword) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持Token重置方式");
    }
}
