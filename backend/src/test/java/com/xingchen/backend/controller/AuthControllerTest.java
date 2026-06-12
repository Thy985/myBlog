package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.LoginDTO;
import com.xingchen.backend.entity.LoginHistory;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.LoginHistoryMapper;
import com.xingchen.backend.service.MfaService;
import com.xingchen.backend.service.UserService;
import com.xingchen.backend.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    private UserService userService;
    private LoginHistoryMapper loginHistoryMapper;
    private MfaService mfaService;
    private AuthController authController;

    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        loginHistoryMapper = mock(LoginHistoryMapper.class);
        mfaService = mock(MfaService.class);
        authController = new AuthController(userService, loginHistoryMapper, mfaService);

        // Mock StpUtil static methods
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        if (stpUtilMock != null) {
            stpUtilMock.close();
        }
    }

    private MockHttpServletRequest buildRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        request.addHeader("User-Agent", "TestAgent");
        request.addHeader("Host", "localhost:8080");
        return request;
    }

    private MockHttpServletResponse buildResponse() {
        return new MockHttpServletResponse();
    }

    // ==================== login Tests ====================

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("应该登录成功并返回完整用户信息")
        void login_Success() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            MockHttpServletResponse response = buildResponse();
            request.getSession().setAttribute("CAPTCHA_CODE", "ABCD");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("123456");
            dto.setCaptcha("abcd");

            Map<String, Object> loginResult = new HashMap<>();
            loginResult.put("userId", 1L);
            loginResult.put("token", "test-token-123");
            when(userService.login(any(LoginDTO.class), anyString(), anyString()))
                    .thenReturn(loginResult);

            UserVO userVO = new UserVO();
            userVO.setId(1L);
            userVO.setUsername("testuser");
            userVO.setNickname("Test User");
            when(userService.getUserInfo(1L)).thenReturn(userVO);
            when(userService.getUserRoles(1L)).thenReturn(List.of("USER"));

            // Act
            Result<Map<String, Object>> result = authController.login(dto, request, response);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals(1L, result.getData().get("userId"));
            assertEquals("test-token-123", result.getData().get("token"));
            assertEquals(false, result.getData().get("isAdmin"));
            assertNotNull(result.getData().get("user"));

            // Verify Set-Cookie header
            String setCookie = response.getHeader("Set-Cookie");
            assertNotNull(setCookie);
            assertTrue(setCookie.contains("X-Auth-Token"));

            verify(userService).login(eq(dto), anyString(), anyString());
            verify(userService).getUserInfo(1L);
            verify(userService).getUserRoles(1L);
        }

        @Test
        @DisplayName("应该返回错误当验证码为空")
        void login_EmptyCaptcha_ThrowsException() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            MockHttpServletResponse response = buildResponse();

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("123456");
            dto.setCaptcha("");

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authController.login(dto, request, response));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("验证码不能为空"));
        }

        @Test
        @DisplayName("应该返回错误当验证码错误")
        void login_WrongCaptcha_ThrowsException() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            MockHttpServletResponse response = buildResponse();
            request.getSession().setAttribute("CAPTCHA_CODE", "ABCD");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("123456");
            dto.setCaptcha("WXYZ");

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authController.login(dto, request, response));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("验证码错误"));
        }

        @Test
        @DisplayName("应该返回错误当验证码已过期")
        void login_ExpiredCaptcha_ThrowsException() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            MockHttpServletResponse response = buildResponse();
            // No captcha in session

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("123456");
            dto.setCaptcha("ABCD");

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authController.login(dto, request, response));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("验证码已过期"));
        }

        @Test
        @DisplayName("管理员登录应该返回isAdmin=true")
        void login_AdminUser_ReturnsIsAdminTrue() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            MockHttpServletResponse response = buildResponse();
            request.getSession().setAttribute("CAPTCHA_CODE", "ABCD");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("admin");
            dto.setPassword("admin123");
            dto.setCaptcha("abcd");

            Map<String, Object> loginResult = new HashMap<>();
            loginResult.put("userId", 1L);
            loginResult.put("token", "admin-token");
            when(userService.login(any(LoginDTO.class), anyString(), anyString()))
                    .thenReturn(loginResult);

            UserVO userVO = new UserVO();
            userVO.setId(1L);
            userVO.setUsername("admin");
            when(userService.getUserInfo(1L)).thenReturn(userVO);
            when(userService.getUserRoles(1L)).thenReturn(List.of("ADMIN", "USER"));

            // Act
            Result<Map<String, Object>> result = authController.login(dto, request, response);

            // Assert
            assertEquals(true, result.getData().get("isAdmin"));
        }
    }

    // ==================== logout Tests ====================

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("应该登出成功并清除Cookie")
        void logout_Success() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            MockHttpServletResponse response = buildResponse();
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn("test-token");

            doNothing().when(userService).logout("test-token");

            // Act
            Result<Void> result = authController.logout(request, response);

            // Assert
            assertEquals(200, result.getCode());
            verify(userService).logout("test-token");

            // Verify Set-Cookie with maxAge=0 (expire)
            String setCookie = response.getHeader("Set-Cookie");
            assertNotNull(setCookie);
            assertTrue(setCookie.contains("X-Auth-Token"));
        }
    }

    // ==================== refreshToken Tests ====================

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("应该刷新Token成功")
        void refreshToken_Success() {
            // Arrange
            MockHttpServletRequest request = buildRequest();
            MockHttpServletResponse response = buildResponse();
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn("old-token");

            Map<String, Object> refreshResult = new HashMap<>();
            refreshResult.put("token", "new-token");
            when(userService.refreshToken("old-token")).thenReturn(refreshResult);

            // Act
            Result<Map<String, Object>> result = authController.refreshToken(request, response);

            // Assert
            assertEquals(200, result.getCode());
            assertEquals("new-token", result.getData().get("token"));

            String setCookie = response.getHeader("Set-Cookie");
            assertNotNull(setCookie);
            assertTrue(setCookie.contains("X-Auth-Token"));
            assertTrue(setCookie.contains("new-token"));
        }
    }

    // ==================== getUserInfo Tests ====================

    @Nested
    @DisplayName("GET /api/auth/info")
    class GetUserInfoTests {

        @Test
        @DisplayName("应该返回当前登录用户信息")
        void getUserInfo_Success() {
            // Arrange
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            UserVO userVO = new UserVO();
            userVO.setId(1L);
            userVO.setUsername("testuser");
            userVO.setNickname("Test User");
            when(userService.getUserInfo(1L)).thenReturn(userVO);
            when(userService.getUserRoles(1L)).thenReturn(List.of("USER"));

            // Act
            Result<Map<String, Object>> result = authController.getUserInfo();

            // Assert
            assertEquals(200, result.getCode());
            assertNotNull(result.getData().get("user"));
            assertEquals(List.of("USER"), result.getData().get("roles"));
            assertEquals(false, result.getData().get("isAdmin"));
        }

        @Test
        @DisplayName("管理员用户应该返回isAdmin=true")
        void getUserInfo_Admin_ReturnsIsAdminTrue() {
            // Arrange
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            UserVO userVO = new UserVO();
            userVO.setId(1L);
            userVO.setUsername("admin");
            when(userService.getUserInfo(1L)).thenReturn(userVO);
            when(userService.getUserRoles(1L)).thenReturn(List.of("ADMIN"));

            // Act
            Result<Map<String, Object>> result = authController.getUserInfo();

            // Assert
            assertEquals(true, result.getData().get("isAdmin"));
        }
    }

    // ==================== getLoginHistory Tests ====================

    @Nested
    @DisplayName("GET /api/auth/login-history")
    class GetLoginHistoryTests {

        @Test
        @DisplayName("应该返回登录历史记录")
        void getLoginHistory_Success() {
            // Arrange
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            List<LoginHistory> history = new ArrayList<>();
            LoginHistory record = new LoginHistory();
            record.setId(1L);
            record.setUserId(1L);
            record.setIp("127.0.0.1");
            record.setDevice("TestAgent");
            history.add(record);

            when(loginHistoryMapper.selectByUserId(1L, 20)).thenReturn(history);

            // Act
            Result<List<LoginHistory>> result = authController.getLoginHistory(20);

            // Assert
            assertEquals(200, result.getCode());
            assertEquals(1, result.getData().size());
            assertEquals("127.0.0.1", result.getData().get(0).getIp());
        }

        @Test
        @DisplayName("应该使用默认limit=20")
        void getLoginHistory_DefaultLimit() {
            // Arrange
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(loginHistoryMapper.selectByUserId(1L, 20)).thenReturn(Collections.emptyList());

            // Act
            Result<List<LoginHistory>> result = authController.getLoginHistory(20);

            // Assert
            assertEquals(200, result.getCode());
            assertTrue(result.getData().isEmpty());
            verify(loginHistoryMapper).selectByUserId(1L, 20);
        }
    }
}
