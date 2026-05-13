package com.xingchen.backend.service;

import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.impl.MfaServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MfaServiceImpl Tests")
class MfaServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MfaServiceImpl mfaService;

    // ==================== Helper Methods ====================

    private User buildUser(Long id, Integer mfaEnabled, String mfaSecret) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser");
        user.setNickname("Test User");
        user.setMfaEnabled(mfaEnabled);
        user.setMfaSecret(mfaSecret);
        return user;
    }

    // ==================== setupMfa Tests ====================

    @Nested
    @DisplayName("setupMfa")
    class SetupMfaTests {

        @Test
        @DisplayName("should setup MFA successfully for valid user")
        void setupMfa_Success() {
            // Arrange
            User user = buildUser(1L, 0, null);
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            MfaService.MfaSetupResult result = mfaService.setupMfa(1L, "testuser");

            // Assert
            assertNotNull(result);
            assertNotNull(result.getSecret());
            assertEquals(16, result.getSecret().length());
            assertTrue(result.getQrCodeUrl().contains("otpauth://totp/testuser"));
            assertTrue(result.getQrCodeUrl().contains("issuer=MyBlog"));
            assertEquals(result.getSecret(), result.getManualEntryKey());

            // Verify user was updated with secret
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).update(userCaptor.capture());
            assertNotNull(userCaptor.getValue().getMfaSecret());
        }

        @Test
        @DisplayName("should setup MFA when mfaEnabled is null")
        void setupMfa_MfaEnabledNull() {
            // Arrange
            User user = buildUser(1L, null, null);
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            MfaService.MfaSetupResult result = mfaService.setupMfa(1L, "testuser");

            // Assert
            assertNotNull(result);
            assertNotNull(result.getSecret());
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user does not exist")
        void setupMfa_UserNotFound() {
            // Arrange
            when(userMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.setupMfa(999L, "testuser"));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw MFA_ALREADY_ENABLED when MFA is already enabled")
        void setupMfa_AlreadyEnabled() {
            // Arrange
            User user = buildUser(1L, 1, "EXISTINGSECRET");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.setupMfa(1L, "testuser"));
            assertEquals(ErrorCode.MFA_ALREADY_ENABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should generate secret with valid Base32 characters")
        void setupMfa_SecretHasValidChars() {
            // Arrange
            User user = buildUser(1L, 0, null);
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            MfaService.MfaSetupResult result = mfaService.setupMfa(1L, "testuser");

            // Assert
            String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
            for (char c : result.getSecret().toCharArray()) {
                assertTrue(validChars.indexOf(c) >= 0,
                        "Secret contains invalid Base32 character: " + c);
            }
        }
    }

    // ==================== verifyCode Tests ====================

    @Nested
    @DisplayName("verifyCode")
    class VerifyCodeTests {

        @Test
        @DisplayName("should throw MFA_NOT_ENABLED when user is null")
        void verifyCode_UserNull() {
            // Arrange
            when(userMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.verifyCode(999L, "123456"));
            assertEquals(ErrorCode.MFA_NOT_ENABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw MFA_NOT_ENABLED when mfaSecret is null")
        void verifyCode_SecretNull() {
            // Arrange
            User user = buildUser(1L, 0, null);
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.verifyCode(1L, "123456"));
            assertEquals(ErrorCode.MFA_NOT_ENABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw PARAMS_ERROR when code is invalid")
        void verifyCode_InvalidCode() {
            // Arrange
            User user = buildUser(1L, 1, "JBSWY3DPEHPK3PXP");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.verifyCode(1L, "000000"));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw PARAMS_ERROR when code is wrong length")
        void verifyCode_WrongLength() {
            // Arrange
            User user = buildUser(1L, 1, "JBSWY3DPEHPK3PXP");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.verifyCode(1L, "12345"));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw PARAMS_ERROR when code is null")
        void verifyCode_NullCode() {
            // Arrange
            User user = buildUser(1L, 1, "JBSWY3DPEHPK3PXP");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.verifyCode(1L, null));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw PARAMS_ERROR when code is non-numeric")
        void verifyCode_NonNumeric() {
            // Arrange
            User user = buildUser(1L, 1, "JBSWY3DPEHPK3PXP");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.verifyCode(1L, "abcdef"));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        }
    }

    // ==================== enableMfa Tests ====================

    @Nested
    @DisplayName("enableMfa")
    class EnableMfaTests {

        @Test
        @DisplayName("should enable MFA successfully")
        void enableMfa_Success() {
            // Arrange
            User user = buildUser(1L, 0, "JBSWY3DPEHPK3PXP");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            mfaService.enableMfa(1L);

            // Assert
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).update(captor.capture());
            User updated = captor.getValue();
            assertEquals(1, updated.getMfaEnabled());
            assertEquals("TOTP", updated.getMfaType());
            assertNotNull(updated.getUpdatedTime());
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user does not exist")
        void enableMfa_UserNotFound() {
            // Arrange
            when(userMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.enableMfa(999L));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // ==================== disableMfa Tests ====================

    @Nested
    @DisplayName("disableMfa")
    class DisableMfaTests {

        @Test
        @DisplayName("should disable MFA and clear secret")
        void disableMfa_Success() {
            // Arrange
            User user = buildUser(1L, 1, "JBSWY3DPEHPK3PXP");
            user.setMfaType("TOTP");
            user.setBackupCodes("code1,code2");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            mfaService.disableMfa(1L);

            // Assert
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).update(captor.capture());
            User updated = captor.getValue();
            assertEquals(0, updated.getMfaEnabled());
            assertNull(updated.getMfaSecret());
            assertNull(updated.getMfaType());
            assertNull(updated.getBackupCodes());
            assertNotNull(updated.getUpdatedTime());
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user does not exist")
        void disableMfa_UserNotFound() {
            // Arrange
            when(userMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> mfaService.disableMfa(999L));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // ==================== isMfaEnabled Tests ====================

    @Nested
    @DisplayName("isMfaEnabled")
    class IsMfaEnabledTests {

        @Test
        @DisplayName("should return true when MFA is enabled")
        void isMfaEnabled_True() {
            // Arrange
            User user = buildUser(1L, 1, "JBSWY3DPEHPK3PXP");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            boolean result = mfaService.isMfaEnabled(1L);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("should return false when MFA is disabled")
        void isMfaEnabled_False() {
            // Arrange
            User user = buildUser(1L, 0, null);
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            boolean result = mfaService.isMfaEnabled(1L);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("should return false when user not found")
        void isMfaEnabled_UserNotFound() {
            // Arrange
            when(userMapper.selectOneById(999L)).thenReturn(null);

            // Act
            boolean result = mfaService.isMfaEnabled(999L);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("should return false when mfaEnabled is null")
        void isMfaEnabled_Null() {
            // Arrange
            User user = buildUser(1L, null, null);
            when(userMapper.selectOneById(1L)).thenReturn(user);

            // Act
            boolean result = mfaService.isMfaEnabled(1L);

            // Assert
            assertFalse(result);
        }
    }
}
