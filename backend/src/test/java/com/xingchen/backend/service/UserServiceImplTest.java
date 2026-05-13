package com.xingchen.backend.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.LoginDTO;
import com.xingchen.backend.dto.PasswordUpdateDTO;
import com.xingchen.backend.dto.UserRegisterDTO;
import com.xingchen.backend.dto.UserUpdateDTO;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleLike;
import com.xingchen.backend.entity.Comment;
import com.xingchen.backend.entity.LoginHistory;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.impl.UserServiceImpl;
import com.xingchen.backend.vo.UserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private LoginHistoryMapper loginHistoryMapper;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ArticleLikeMapper articleLikeMapper;

    @Mock
    private MfaService mfaService;

    @InjectMocks
    private UserServiceImpl userService;

    // ==================== Helper Methods ====================

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword(BCrypt.hashpw("password123"));
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setNickname("Test User");
        user.setStatus(1);
        user.setLoginCount(5);
        user.setIsDeleted(0);
        user.setAvatar("https://example.com/avatar.jpg");
        user.setBio("Test bio");
        user.setCreatedTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        user.setRegisterTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        return user;
    }

    private User createDisabledUser() {
        User user = createTestUser();
        user.setStatus(0);
        return user;
    }

    private LoginDTO createLoginDTO() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");
        return dto;
    }

    private UserRegisterDTO createRegisterDTO() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setEmail("new@example.com");
        dto.setPhone("13900139000");
        dto.setNickname("New User");
        dto.setCode("123456");
        return dto;
    }

    private UserUpdateDTO createUserUpdateDTO() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("Updated Nickname");
        dto.setAvatar("https://example.com/new-avatar.jpg");
        dto.setBio("Updated bio");
        return dto;
    }

    private PasswordUpdateDTO createPasswordUpdateDTO() {
        PasswordUpdateDTO dto = new PasswordUpdateDTO();
        dto.setOldPassword("password123");
        dto.setNewPassword("newpassword456");
        return dto;
    }

    // ==================== login Tests ====================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with username")
        void login_withUsername_success() {
            LoginDTO dto = createLoginDTO();
            User user = createTestUser();
            when(userMapper.selectByUsername("testuser")).thenReturn(user);
            when(mfaService.isMfaEnabled(1L)).thenReturn(false);

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.login(1L)).then(invocation -> null);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn("mock-token");
                when(userMapper.updateLoginInfo(1L, "127.0.0.1")).thenReturn(1);

                Map<String, Object> result = userService.login(dto, "127.0.0.1", "Chrome");

                assertNotNull(result);
                assertEquals("mock-token", result.get("token"));
                assertEquals(1L, result.get("userId"));
                assertEquals("testuser", result.get("username"));
                verify(loginHistoryMapper).insert(any(LoginHistory.class));
            }
        }

        @Test
        @DisplayName("should login with email when username contains @")
        void login_withEmail_success() {
            LoginDTO dto = createLoginDTO();
            dto.setUsername("test@example.com");
            User user = createTestUser();
            when(userMapper.selectByUsername("test@example.com")).thenReturn(null);
            when(userMapper.selectByEmail("test@example.com")).thenReturn(user);
            when(mfaService.isMfaEnabled(1L)).thenReturn(false);

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.login(1L)).then(invocation -> null);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn("mock-token");

                Map<String, Object> result = userService.login(dto, "127.0.0.1", "Chrome");

                assertNotNull(result);
                assertEquals("mock-token", result.get("token"));
                verify(userMapper).selectByEmail("test@example.com");
            }
        }

        @Test
        @DisplayName("should login with phone when username not found and no @")
        void login_withPhone_success() {
            LoginDTO dto = createLoginDTO();
            dto.setUsername("13800138000");
            User user = createTestUser();
            when(userMapper.selectByUsername("13800138000")).thenReturn(null);
            when(userMapper.selectByPhone("13800138000")).thenReturn(user);
            when(mfaService.isMfaEnabled(1L)).thenReturn(false);

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.login(1L)).then(invocation -> null);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn("mock-token");

                Map<String, Object> result = userService.login(dto, "127.0.0.1", "Chrome");

                assertNotNull(result);
                verify(userMapper).selectByPhone("13800138000");
            }
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user not found by any method")
        void login_userNotFound_throwsException() {
            LoginDTO dto = createLoginDTO();
            when(userMapper.selectByUsername("testuser")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.login(dto, "127.0.0.1", "Chrome"));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw INVALID_PASSWORD when password is wrong")
        void login_invalidPassword_throwsException() {
            LoginDTO dto = createLoginDTO();
            dto.setPassword("wrongpassword");
            User user = createTestUser();
            when(userMapper.selectByUsername("testuser")).thenReturn(user);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.login(dto, "127.0.0.1", "Chrome"));
            assertEquals(ErrorCode.INVALID_PASSWORD.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw USER_DISABLED when user is disabled")
        void login_userDisabled_throwsException() {
            LoginDTO dto = createLoginDTO();
            User user = createDisabledUser();
            when(userMapper.selectByUsername("testuser")).thenReturn(user);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.login(dto, "127.0.0.1", "Chrome"));
            assertEquals(ErrorCode.USER_DISABLED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should login successfully with MFA code")
        void login_withMfaCode_success() {
            LoginDTO dto = createLoginDTO();
            dto.setMfaCode("654321");
            User user = createTestUser();
            when(userMapper.selectByUsername("testuser")).thenReturn(user);
            when(mfaService.isMfaEnabled(1L)).thenReturn(true);
            doNothing().when(mfaService).verifyCode(1L, "654321");

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.login(1L)).then(invocation -> null);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn("mock-token");

                Map<String, Object> result = userService.login(dto, "127.0.0.1", "Chrome");

                assertNotNull(result);
                verify(mfaService).verifyCode(1L, "654321");
            }
        }

        @Test
        @DisplayName("should throw MFA_CODE_REQUIRED when MFA enabled but no code provided")
        void login_mfaEnabled_noCode_throwsException() {
            LoginDTO dto = createLoginDTO();
            User user = createTestUser();
            when(userMapper.selectByUsername("testuser")).thenReturn(user);
            when(mfaService.isMfaEnabled(1L)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.login(dto, "127.0.0.1", "Chrome"));
            assertEquals(ErrorCode.MFA_CODE_REQUIRED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw MFA_CODE_INVALID when MFA code is wrong")
        void login_mfaCodeInvalid_throwsException() {
            LoginDTO dto = createLoginDTO();
            dto.setMfaCode("000000");
            User user = createTestUser();
            when(userMapper.selectByUsername("testuser")).thenReturn(user);
            when(mfaService.isMfaEnabled(1L)).thenReturn(true);
            doThrow(new BusinessException(ErrorCode.SYSTEM_ERROR)).when(mfaService).verifyCode(1L, "000000");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.login(dto, "127.0.0.1", "Chrome"));
            assertEquals(ErrorCode.MFA_CODE_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should record login history on successful login")
        void login_recordsLoginHistory() {
            LoginDTO dto = createLoginDTO();
            User user = createTestUser();
            when(userMapper.selectByUsername("testuser")).thenReturn(user);
            when(mfaService.isMfaEnabled(1L)).thenReturn(false);

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.login(1L)).then(invocation -> null);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn("mock-token");

                userService.login(dto, "192.168.1.1", "Firefox");

                verify(loginHistoryMapper).insert(argThat(history ->
                        history.getUserId().equals(1L)
                        && history.getLoginIp().equals("192.168.1.1")
                        && history.getDevice().equals("Firefox")
                        && history.getLoginTime() != null
                ));
                verify(userMapper).updateLoginInfo(1L, "192.168.1.1");
            }
        }
    }

    // ==================== logout Tests ====================

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("should logout successfully")
        void logout_success() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.logoutByTokenValue("test-token")).then(invocation -> null);

                assertDoesNotThrow(() -> userService.logout("test-token"));
                stpUtilMock.verify(() -> StpUtil.logoutByTokenValue("test-token"));
            }
        }
    }

    // ==================== refreshToken Tests ====================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token and return result")
        void refreshToken_success() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.renewTimeout("test-token", 7 * 24 * 60 * 60))
                        .then(invocation -> null);

                Map<String, Object> result = userService.refreshToken("test-token");

                assertNotNull(result);
                assertEquals("test-token", result.get("token"));
                assertEquals(7 * 24 * 60 * 60, result.get("expireTime"));
            }
        }
    }

    // ==================== getUserInfo Tests ====================

    @Nested
    @DisplayName("getUserInfo()")
    class GetUserInfoTests {

        @Test
        @DisplayName("should return UserVO when user exists")
        void getUserInfo_userExists_returnsUserVO() {
            User user = createTestUser();
            when(userMapper.selectOneById(1L)).thenReturn(user);

            UserVO result = userService.getUserInfo(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("testuser", result.getUsername());
            assertEquals("test@example.com", result.getEmail());
            assertEquals("Test User", result.getNickname());
        }

        @Test
        @DisplayName("should return null when user does not exist")
        void getUserInfo_userNotFound_returnsNull() {
            when(userMapper.selectOneById(999L)).thenReturn(null);

            UserVO result = userService.getUserInfo(999L);

            assertNull(result);
        }
    }

    // ==================== getUserRoles Tests ====================

    @Nested
    @DisplayName("getUserRoles()")
    class GetUserRolesTests {

        @Test
        @DisplayName("should return user roles")
        void getUserRoles_success() {
            List<String> roles = Arrays.asList("admin", "user");
            when(userRoleMapper.selectRoleCodesByUserId(1L)).thenReturn(roles);

            List<String> result = userService.getUserRoles(1L);

            assertEquals(2, result.size());
            assertTrue(result.contains("admin"));
            assertTrue(result.contains("user"));
        }

        @Test
        @DisplayName("should return empty list when user has no roles")
        void getUserRoles_noRoles_returnsEmptyList() {
            when(userRoleMapper.selectRoleCodesByUserId(1L)).thenReturn(Collections.emptyList());

            List<String> result = userService.getUserRoles(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== register Tests ====================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register successfully")
        void register_success() {
            UserRegisterDTO dto = createRegisterDTO();
            doNothing().when(verificationCodeService).verifyCode("new@example.com", "123456", "register");
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByEmail("new@example.com")).thenReturn(null);
            doAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(100L);
                return 1;
            }).when(userMapper).insert(any(User.class));
            when(userMapper.selectOneById(100L)).thenAnswer(invocation -> {
                User u = createTestUser();
                u.setId(100L);
                u.setUsername("newuser");
                u.setEmail("new@example.com");
                u.setNickname("New User");
                return u;
            });

            Object result = userService.register(dto);

            assertNotNull(result);
            verify(verificationCodeService).verifyCode("new@example.com", "123456", "register");
            verify(userMapper).insert(argThat(user ->
                    user.getUsername().equals("newuser")
                    && user.getEmail().equals("new@example.com")
                    && user.getStatus() == 1
                    && user.getIsDeleted() == 0
                    && user.getLoginCount() == 0
            ));
        }

        @Test
        @DisplayName("should throw exception when verification code is invalid")
        void register_invalidVerificationCode_throwsException() {
            UserRegisterDTO dto = createRegisterDTO();
            doThrow(new RuntimeException("验证码无效")).when(verificationCodeService)
                    .verifyCode("new@example.com", "123456", "register");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(dto));
            assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), ex.getCode());
            assertEquals("验证码无效或已过期", ex.getMessage());
        }

        @Test
        @DisplayName("should throw USER_ALREADY_EXISTS when username exists")
        void register_usernameExists_throwsException() {
            UserRegisterDTO dto = createRegisterDTO();
            doNothing().when(verificationCodeService).verifyCode(anyString(), anyString(), anyString());
            when(userMapper.selectByUsername("newuser")).thenReturn(createTestUser());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(dto));
            assertEquals(ErrorCode.USER_ALREADY_EXISTS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw EMAIL_ALREADY_EXISTS when email exists")
        void register_emailExists_throwsException() {
            UserRegisterDTO dto = createRegisterDTO();
            doNothing().when(verificationCodeService).verifyCode(anyString(), anyString(), anyString());
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByEmail("new@example.com")).thenReturn(createTestUser());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(dto));
            assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should use username as nickname when nickname is null")
        void register_nullNickname_usesUsername() {
            UserRegisterDTO dto = createRegisterDTO();
            dto.setNickname(null);
            doNothing().when(verificationCodeService).verifyCode(anyString(), anyString(), anyString());
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByEmail("new@example.com")).thenReturn(null);
            doAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(101L);
                return 1;
            }).when(userMapper).insert(any(User.class));
            when(userMapper.selectOneById(101L)).thenAnswer(invocation -> {
                User u = createTestUser();
                u.setId(101L);
                u.setUsername("newuser");
                u.setNickname("newuser");
                return u;
            });

            userService.register(dto);

            verify(userMapper).insert(argThat(user -> user.getNickname().equals("newuser")));
        }

        @Test
        @DisplayName("should register successfully even when email is empty")
        void register_emptyEmail_skipsEmailCheck() {
            UserRegisterDTO dto = createRegisterDTO();
            dto.setEmail("");
            doNothing().when(verificationCodeService).verifyCode(eq(""), anyString(), anyString());
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            doAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(102L);
                return 1;
            }).when(userMapper).insert(any(User.class));
            when(userMapper.selectOneById(102L)).thenReturn(createTestUser());

            Object result = userService.register(dto);

            assertNotNull(result);
            verify(userMapper, never()).selectByEmail(anyString());
        }
    }

    // ==================== updateUserInfo Tests ====================

    @Nested
    @DisplayName("updateUserInfo()")
    class UpdateUserInfoTests {

        @Test
        @DisplayName("should update user info successfully")
        void updateUserInfo_success() {
            User user = createTestUser();
            UserUpdateDTO dto = createUserUpdateDTO();
            when(userMapper.selectOneById(1L)).thenReturn(user);

            userService.updateUserInfo(1L, dto);

            verify(userMapper).update(argThat(u ->
                    u.getNickname().equals("Updated Nickname")
                    && u.getAvatar().equals("https://example.com/new-avatar.jpg")
                    && u.getBio().equals("Updated bio")
                    && u.getUpdatedTime() != null
            ));
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user does not exist")
        void updateUserInfo_userNotFound_throwsException() {
            UserUpdateDTO dto = createUserUpdateDTO();
            when(userMapper.selectOneById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateUserInfo(999L, dto));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should only update non-null fields")
        void updateUserInfo_partialUpdate_success() {
            User user = createTestUser();
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("New Nickname");
            // avatar and bio are null
            when(userMapper.selectOneById(1L)).thenReturn(user);

            userService.updateUserInfo(1L, dto);

            verify(userMapper).update(argThat(u ->
                    u.getNickname().equals("New Nickname")
                    && u.getAvatar().equals("https://example.com/avatar.jpg") // unchanged
                    && u.getBio().equals("Test bio") // unchanged
            ));
        }

        @Test
        @DisplayName("should not modify anything when all fields are null")
        void updateUserInfo_allNull_noChanges() {
            User user = createTestUser();
            UserUpdateDTO dto = new UserUpdateDTO();
            when(userMapper.selectOneById(1L)).thenReturn(user);

            userService.updateUserInfo(1L, dto);

            verify(userMapper).update(argThat(u ->
                    u.getNickname().equals("Test User")
                    && u.getAvatar().equals("https://example.com/avatar.jpg")
                    && u.getBio().equals("Test bio")
                    && u.getUpdatedTime() != null
            ));
        }
    }

    // ==================== updatePassword Tests ====================

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePasswordTests {

        @Test
        @DisplayName("should update password successfully")
        void updatePassword_success() {
            User user = createTestUser();
            PasswordUpdateDTO dto = createPasswordUpdateDTO();
            when(userMapper.selectOneById(1L)).thenReturn(user);

            userService.updatePassword(1L, dto);

            verify(userMapper).updatePassword(eq(1L), anyString());
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when user does not exist")
        void updatePassword_userNotFound_throwsException() {
            PasswordUpdateDTO dto = createPasswordUpdateDTO();
            when(userMapper.selectOneById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(999L, dto));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("should throw ORIGINAL_PASSWORD_ERROR when old password is wrong")
        void updatePassword_wrongOldPassword_throwsException() {
            User user = createTestUser();
            PasswordUpdateDTO dto = createPasswordUpdateDTO();
            dto.setOldPassword("wrongoldpassword");
            when(userMapper.selectOneById(1L)).thenReturn(user);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updatePassword(1L, dto));
            assertEquals(ErrorCode.ORIGINAL_PASSWORD_ERROR.getCode(), ex.getCode());
        }
    }

    // ==================== getUserStats Tests ====================

    @Nested
    @DisplayName("getUserStats()")
    class GetUserStatsTests {

        @Test
        @DisplayName("should return user stats with all counts")
        void getUserStats_success() {
            when(articleMapper.countPublishedByUserId(1L)).thenReturn(10L);
            when(commentMapper.countByUserId(1L)).thenReturn(25);
            when(articleLikeMapper.selectCountByUserId(1L)).thenReturn(50L);

            Map<String, Object> stats = userService.getUserStats(1L);

            assertNotNull(stats);
            assertEquals(10L, stats.get("articleCount"));
            assertEquals(25, stats.get("commentCount"));
            assertEquals(50L, stats.get("likeCount"));
        }

        @Test
        @DisplayName("should return zero likeCount when null")
        void getUserStats_nullLikeCount_returnsZero() {
            when(articleMapper.countPublishedByUserId(1L)).thenReturn(0L);
            when(commentMapper.countByUserId(1L)).thenReturn(0);
            when(articleLikeMapper.selectCountByUserId(1L)).thenReturn(null);

            Map<String, Object> stats = userService.getUserStats(1L);

            assertNotNull(stats);
            assertEquals(0L, stats.get("likeCount"));
        }
    }

    // ==================== getUserRecentActivities Tests ====================

    @Nested
    @DisplayName("getUserRecentActivities()")
    class GetUserRecentActivitiesTests {

        @Test
        @DisplayName("should return combined activities sorted by time descending")
        void getUserRecentActivities_success() {
            Article article = new Article();
            article.setId(1L);
            article.setTitle("Test Article");
            article.setCreatedTime(LocalDateTime.of(2025, 3, 1, 10, 0));

            Comment comment = new Comment();
            comment.setId(1L);
            comment.setCreateTime(LocalDateTime.of(2025, 3, 2, 10, 0));

            ArticleLike like = new ArticleLike();
            like.setArticleId(1L);
            like.setUserId(1L);
            like.setCreatedTime(LocalDateTime.of(2025, 3, 3, 10, 0));

            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Collections.singletonList(article));
            when(commentMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Collections.singletonList(comment));
            when(articleLikeMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Collections.singletonList(like));

            List<Map<String, Object>> activities = userService.getUserRecentActivities(1L);

            assertNotNull(activities);
            assertEquals(3, activities.size());
            // Most recent first (like > comment > article)
            assertTrue(activities.get(0).get("id").toString().contains("like_"));
            assertTrue(activities.get(1).get("id").toString().contains("comment_"));
            assertTrue(activities.get(2).get("id").toString().contains("article_"));
        }

        @Test
        @DisplayName("should limit to 10 activities")
        void getUserRecentActivities_limitsTo10() {
            // Create 4 articles, 4 comments, 4 likes = 12 total, should return 10
            List<Article> articles = Arrays.asList(
                    createArticle(1L, "A1", LocalDateTime.of(2025, 1, 1, 0, 0)),
                    createArticle(2L, "A2", LocalDateTime.of(2025, 1, 2, 0, 0)),
                    createArticle(3L, "A3", LocalDateTime.of(2025, 1, 3, 0, 0)),
                    createArticle(4L, "A4", LocalDateTime.of(2025, 1, 4, 0, 0))
            );
            List<Comment> comments = Arrays.asList(
                    createComment(1L, LocalDateTime.of(2025, 2, 1, 0, 0)),
                    createComment(2L, LocalDateTime.of(2025, 2, 2, 0, 0)),
                    createComment(3L, LocalDateTime.of(2025, 2, 3, 0, 0)),
                    createComment(4L, LocalDateTime.of(2025, 2, 4, 0, 0))
            );
            List<ArticleLike> likes = Arrays.asList(
                    createArticleLike(1L, LocalDateTime.of(2025, 3, 1, 0, 0)),
                    createArticleLike(2L, LocalDateTime.of(2025, 3, 2, 0, 0)),
                    createArticleLike(3L, LocalDateTime.of(2025, 3, 3, 0, 0)),
                    createArticleLike(4L, LocalDateTime.of(2025, 3, 4, 0, 0))
            );

            when(articleMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(articles);
            when(commentMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(comments);
            when(articleLikeMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(likes);

            List<Map<String, Object>> activities = userService.getUserRecentActivities(1L);

            assertEquals(10, activities.size());
        }

        @Test
        @DisplayName("should return empty list when no activities")
        void getUserRecentActivities_noActivities_returnsEmptyList() {
            when(articleMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
            when(commentMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
            when(articleLikeMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            List<Map<String, Object>> activities = userService.getUserRecentActivities(1L);

            assertNotNull(activities);
            assertTrue(activities.isEmpty());
        }

        @Test
        @DisplayName("should format activity descriptions correctly")
        void getUserRecentActivities_correctDescriptions() {
            Article article = new Article();
            article.setId(1L);
            article.setTitle("My Blog Post");
            article.setCreatedTime(LocalDateTime.of(2025, 3, 1, 10, 0));

            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Collections.singletonList(article));
            when(commentMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
            when(articleLikeMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            List<Map<String, Object>> activities = userService.getUserRecentActivities(1L);

            assertEquals(1, activities.size());
            assertEquals("article_1", activities.get(0).get("id"));
            assertEquals("发布了文章《My Blog Post》", activities.get(0).get("description"));
        }

        // Helper methods for activities tests
        private Article createArticle(Long id, String title, LocalDateTime createdTime) {
            Article a = new Article();
            a.setId(id);
            a.setTitle(title);
            a.setCreatedTime(createdTime);
            return a;
        }

        private Comment createComment(Long id, LocalDateTime createTime) {
            Comment c = new Comment();
            c.setId(id);
            c.setCreateTime(createTime);
            return c;
        }

        private ArticleLike createArticleLike(Long articleId, LocalDateTime createdTime) {
            ArticleLike l = new ArticleLike();
            l.setArticleId(articleId);
            l.setUserId(1L);
            l.setCreatedTime(createdTime);
            return l;
        }
    }
}
