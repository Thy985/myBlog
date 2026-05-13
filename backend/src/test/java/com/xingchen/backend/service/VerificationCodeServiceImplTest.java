package com.xingchen.backend.service;

import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.service.impl.VerificationCodeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationCodeServiceImpl Tests")
class VerificationCodeServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MailService mailService;

    @InjectMocks
    private VerificationCodeServiceImpl verificationCodeService;

    private static final String CODE_KEY_PREFIX = "verification:code:";
    private static final String SEND_TIME_KEY_PREFIX = "verification:sendtime:";
    private static final int SEND_INTERVAL_SECONDS = 60;

    @Nested
    @DisplayName("generateAndSendCode()")
    class GenerateAndSendCodeTests {

        @Test
        @DisplayName("should generate and send code successfully")
        void generateAndSendCode_success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(startsWith(SEND_TIME_KEY_PREFIX))).thenReturn(null);

            String code = verificationCodeService.generateAndSendCode("test@example.com", "register");

            assertNotNull(code);
            assertEquals(6, code.length());
            assertTrue(code.matches("\\d{6}"));

            verify(valueOperations).set(
                    eq(CODE_KEY_PREFIX + "register:test@example.com"),
                    eq(code),
                    anyLong(),
                    eq(TimeUnit.MINUTES)
            );
            verify(mailService).sendVerificationCode("test@example.com", code, "register");
        }

        @Test
        @DisplayName("should throw exception when sending too frequently")
        void generateAndSendCode_tooFrequent_throwsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(startsWith(SEND_TIME_KEY_PREFIX)))
                    .thenReturn(String.valueOf(System.currentTimeMillis() - 1000));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.generateAndSendCode("test@example.com", "register"));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("发送过于频繁"));
        }

        @Test
        @DisplayName("should allow sending when interval has passed")
        void generateAndSendCode_intervalPassed_allowsSending() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(startsWith(SEND_TIME_KEY_PREFIX)))
                    .thenReturn(String.valueOf(System.currentTimeMillis() - 120000));

            String code = verificationCodeService.generateAndSendCode("test@example.com", "register");

            assertNotNull(code);
            verify(mailService).sendVerificationCode("test@example.com", code, "register");
        }

        @Test
        @DisplayName("should handle invalid last send time format")
        void generateAndSendCode_invalidSendTimeFormat_generatesCode() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(startsWith(SEND_TIME_KEY_PREFIX))).thenReturn("invalid-time");

            String code = verificationCodeService.generateAndSendCode("test@example.com", "register");

            assertNotNull(code);
            assertEquals(6, code.length());
        }

        @Test
        @DisplayName("should generate different codes for same email")
        void generateAndSendCode_multipleCalls_differentCodes() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(startsWith(SEND_TIME_KEY_PREFIX))).thenReturn(null);

            String code1 = verificationCodeService.generateAndSendCode("test@example.com", "register");
            when(valueOperations.get(startsWith(SEND_TIME_KEY_PREFIX)))
                    .thenReturn(String.valueOf(System.currentTimeMillis() - 120000));
            String code2 = verificationCodeService.generateAndSendCode("test@example.com", "register");

            assertNotNull(code1);
            assertNotNull(code2);
        }
    }

    @Nested
    @DisplayName("verifyCode()")
    class VerifyCodeTests {

        @Test
        @DisplayName("should verify code successfully")
        void verifyCode_success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CODE_KEY_PREFIX + "register:test@example.com")).thenReturn("123456");
            when(redisTemplate.delete(anyString())).thenReturn(true);

            assertDoesNotThrow(() ->
                    verificationCodeService.verifyCode("test@example.com", "123456", "register"));

            verify(redisTemplate).delete(CODE_KEY_PREFIX + "register:test@example.com");
            verify(redisTemplate).delete(SEND_TIME_KEY_PREFIX + "register:test@example.com");
        }

        @Test
        @DisplayName("should throw exception when code is null")
        void verifyCode_nullCode_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verifyCode("test@example.com", null, "register"));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
            assertEquals("参数不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when code is empty")
        void verifyCode_emptyCode_throwsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verifyCode("test@example.com", "", "register"));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
            assertEquals("验证码错误或已过期", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when email is null")
        void verifyCode_nullEmail_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verifyCode(null, "123456", "register"));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
            assertEquals("参数不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when type is null")
        void verifyCode_nullType_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verifyCode("test@example.com", "123456", null));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
            assertEquals("参数不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when code does not match")
        void verifyCode_wrongCode_throwsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CODE_KEY_PREFIX + "register:test@example.com")).thenReturn("123456");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verifyCode("test@example.com", "654321", "register"));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
            assertEquals("验证码错误或已过期", ex.getMessage());
        }

        @Test
        @DisplayName("should throw exception when code has expired")
        void verifyCode_expiredCode_throwsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CODE_KEY_PREFIX + "register:test@example.com")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verifyCode("test@example.com", "123456", "register"));

            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
            assertEquals("验证码错误或已过期", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("invalidateCode()")
    class InvalidateCodeTests {

        @Test
        @DisplayName("should delete code keys successfully")
        void invalidateCode_success() {
            when(redisTemplate.delete(anyString())).thenReturn(true);

            verificationCodeService.invalidateCode("test@example.com", "register");

            verify(redisTemplate).delete(CODE_KEY_PREFIX + "register:test@example.com");
            verify(redisTemplate).delete(SEND_TIME_KEY_PREFIX + "register:test@example.com");
        }

        @Test
        @DisplayName("should handle delete failure gracefully")
        void invalidateCode_deleteFailure_noException() {
            when(redisTemplate.delete(anyString())).thenReturn(false);

            assertDoesNotThrow(() ->
                    verificationCodeService.invalidateCode("test@example.com", "register"));

            verify(redisTemplate).delete(CODE_KEY_PREFIX + "register:test@example.com");
            verify(redisTemplate).delete(SEND_TIME_KEY_PREFIX + "register:test@example.com");
        }
    }
}
