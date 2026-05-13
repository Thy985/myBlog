package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.aspect.RateLimit;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.LoginDTO;
import com.xingchen.backend.entity.LoginHistory;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.LoginHistoryMapper;
import com.xingchen.backend.service.MfaService;
import com.xingchen.backend.service.UserService;
import com.xingchen.backend.util.IpUtils;
import com.xingchen.backend.vo.UserVO;
import org.springframework.beans.BeanUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String CAPTCHA_CODE_KEY = "CAPTCHA_CODE";
    private static final String TOKEN_COOKIE_NAME = "X-Auth-Token";
    private static final int TOKEN_MAX_AGE = 7 * 24 * 60 * 60;

    private final UserService userService;
    private final LoginHistoryMapper loginHistoryMapper;
    private final MfaService mfaService;

    /**
     * 用户登录接口
     * 处理用户登录请求，验证账号密码并返回登录信息和权限角色
     */
    @PostMapping("/login")
    @RateLimit(key = "login", capacity = 5, timeWindow = 300)
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request, HttpServletResponse response) {
        verifyCaptcha(dto.getCaptcha(), request);

        String ip = IpUtils.getClientIp(request);
        String device = request.getHeader("User-Agent");
        Map<String, Object> result = userService.login(dto, ip, device);

        Long userId = (Long) result.get("userId");
        List<String> roles = userService.getUserRoles(userId);
        result.put("roles", roles);
        result.put("isAdmin", roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"));

        // 添加完整用户信息
        UserVO userVO = userService.getUserInfo(userId);
        Map<String, Object> userMap = new HashMap<>();
        if (userVO != null) {
            BeanUtils.copyProperties(userVO, userMap);
        }
        result.put("user", userMap);

        String token = (String) result.get("token");
        if (token != null) {
            ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .path("/")
                    .maxAge(TOKEN_MAX_AGE)
                    .sameSite("Lax")
                    .domain(getCookieDomain(request))
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
        }

        return Result.success(result);
    }

    /**
     * 校验验证码
     * 通过环境变量 AUTH_BYPASS_CAPTCHA 控制（仅允许开发环境临时开启）
     */
    private void verifyCaptcha(String captcha, HttpServletRequest request) {
        // 显式环境变量控制，默认关闭
        String bypassCaptcha = System.getenv("AUTH_BYPASS_CAPTCHA");
        if ("true".equalsIgnoreCase(bypassCaptcha)) {
            return;
        }

        if (captcha == null || captcha.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        HttpSession session = request.getSession(false);
        String cachedCode = session != null ? (String) session.getAttribute(CAPTCHA_CODE_KEY) : null;
        if (cachedCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请刷新验证码");
        }
        if (!cachedCode.equalsIgnoreCase(captcha.trim())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        session.removeAttribute(CAPTCHA_CODE_KEY);
    }

    /**
     * 获取Cookie域名
     * 生产环境从请求Host推断，开发环境使用localhost
     */
    private String getCookieDomain(HttpServletRequest request) {
        String profile = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(profile)) {
            return "localhost";
        }
        // 生产环境：从请求头获取域名，过滤端口
        String host = request.getHeader("Host");
        if (host != null) {
            int portIndex = host.indexOf(':');
            return portIndex > 0 ? host.substring(0, portIndex) : host;
        }
        return null;
    }

    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = StpUtil.getTokenValue();
        userService.logout(token);

        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .domain(getCookieDomain(request))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return Result.success();
    }

    @PostMapping("/refresh")
    @SaCheckLogin
    public Result<Map<String, Object>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String oldToken = StpUtil.getTokenValue();
        Map<String, Object> result = userService.refreshToken(oldToken);

        String newToken = (String) result.get("token");
        if (newToken != null) {
            ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_NAME, newToken)
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .path("/")
                    .maxAge(TOKEN_MAX_AGE)
                    .sameSite("Lax")
                    .domain(getCookieDomain(request))
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());
        }

        return Result.success(result);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    @SaCheckLogin
    public Result<Map<String, Object>> getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = new HashMap<>();
        result.put("user", userService.getUserInfo(userId));

        List<String> roles = userService.getUserRoles(userId);
        result.put("roles", roles);
        result.put("isAdmin", roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"));

        return Result.success(result);
    }

    @GetMapping("/login-history")
    @SaCheckLogin
    public Result<List<LoginHistory>> getLoginHistory(@RequestParam(defaultValue = "20") Integer limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<LoginHistory> history = loginHistoryMapper.selectByUserId(userId, limit);
        return Result.success(history);
    }
}
