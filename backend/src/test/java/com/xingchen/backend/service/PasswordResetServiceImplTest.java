package com.xingchen.backend.service;

import cn.hutool.crypto.digest.BCrypt;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.impl.PasswordResetServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl Tests")
class PasswordResetServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private VerificationCodeService verificationCodeService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User createTestUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername("testuser");
        user.setPassword(BCrypt.hashpw("oldpassword"));
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        return user;
    }

    @Nested
    @DisplayName("sendResetCode()")
    class SendResetCodeTests {

        @Test
        @DisplayName("should send reset code for existing user")
        void sendResetCode_existingUser_sendsCode() {
            User user = createTestUser(1L, "test@example.com");
            when(userMapper.selectByEmail("test@example.com")).thenReturn(user);

            passwordResetService.sendResetCode("test@example.com");

            verify(verificationCodeService).generateAndSendCode("test@example.com", "reset");
        }

        @Test
        @DisplayName("should not throw exception for non-existing user")
        void sendResetCode_nonExistingUser_noException() {
            when(userMapper.selectByEmail("nonexistent@example.com")).thenReturn(null);

            assertDoesNotThrow(() ->
                    passwordResetService.sendResetCode("nonexistent@example.com"));

            verify(verificationCodeService, never()).generateAndSendCode(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("verifyCode()")
    class VerifyCodeTests {

        @Test
        @DisplayName("should verify code successfully")
        void verifyCode_success() {
            doNothing().when(verificationCodeService).verifyCode("test@example.com", "123456", "reset");

            assertDoesNotThrow(() ->
                    passwordResetService.verifyCode("test@example.com", "123456"));

            verify(verificationCodeService).verifyCode("test@example.com", "123456", "reset");
        }

        @Test
        @DisplayName("should propagate exception when code is invalid")
        void verifyCode_invalidCode_throwsException() {
            doThrow(new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误"))
                    .when(verificationCodeService).verifyCode("test@example.com", "000000", "reset");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> passwordResetService.verifyCode("test@example.com", "000000"));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        @Test
        @DisplayName("should reset password successfully")
        void resetPassword_success() {
            User user = createTestUser(1L, "test@example.com");
            when(userMapper.selectByEmail("test@example.com")).thenReturn(user);
            doNothing().when(verificationCodeService).verifyCode("test@example.com", "123456", "reset");
            when(userMapper.update(any(User.class))).thenReturn(1);

            passwordResetService.resetPassword("test@example.com", "123456", "newpassword123");

            verify(verificationCodeService).verifyCode("test@example.com", "123456", "reset");
            verify(userMapper).update(argThat(u ->
                    u.getPassword() != null && !u.getPassword().equals(BCrypt.hashpw("oldpassword"))
            ));
            verify(verificationCodeService).invalidateCode("test@example.com", "reset");
        }

        @Test
        @DisplayName("should throw exception when user not found after code verification")
        void resetPassword_userNotFound_throwsException() {
            doNothing().when(verificationCodeService).verifyCode("test@example.com", "123456", "reset");
            when(userMapper.selectByEmail("test@example.com")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> passwordResetService.resetPassword("test@example.com", "123456", "newpassword123"));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should invalidate code after successful password reset")
        void resetPassword_success_invalidatesCode() {
            User user = createTestUser(1L, "test@example.com");
            when(userMapper.selectByEmail("test@example.com")).thenReturn(user);
            doNothing().when(verificationCodeService).verifyCode("test@example.com", "123456", "reset");
            when(userMapper.update(any(User.class))).thenReturn(1);

            passwordResetService.resetPassword("test@example.com", "123456", "newpassword123");

            verify(verificationCodeService).invalidateCode("test@example.com", "reset");
        }
    }

    @Nested
    @DisplayName("getResetToken()")
    class GetResetTokenTests {

        @Test
        @DisplayName("should return null - token method not supported")
        void getResetToken_returnsNull() {
            String result = passwordResetService.getResetToken("test@example.com", "123456");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("resetPasswordByToken()")
    class ResetPasswordByTokenTests {

        @Test
        @DisplayName("should throw exception - token method not supported")
        void resetPasswordByToken_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> passwordResetService.resetPasswordByToken("some-token", "newpassword"));

            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), ex.getCode());
            assertEquals("不支持Token重置方式", ex.getMessage());
        }
    }
}
