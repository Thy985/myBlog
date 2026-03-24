package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.aspect.RateLimit;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.LoginDTO;
import com.xingchen.backend.entity.LoginHistory;
import com.xingchen.backend.mapper.LoginHistoryMapper;
import com.xingchen.backend.service.UserService;
import com.xingchen.backend.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final LoginHistoryMapper loginHistoryMapper;

    /**
     * 用户登录接口
     * 处理用户登录请求，验证账号密码并返回登录信息和权限角色
     * 
     * @param dto 登录数据传输对象，包含用户名和密码
     * @param request HTTP请求对象，用于获取客户端IP和设备信息
     * @return Result<Map<String, Object>> 登录结果，包含token、用户信息、角色权限等
     */
    @PostMapping("/login")
    @RateLimit(key = "login", capacity = 5, timeWindow = 300)// 限流5次/300秒
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        // 获取客户端IP地址和设备信息
        String ip = IpUtils.getClientIp(request);
        String device = request.getHeader("User-Agent");
        Map<String, Object> result = userService.login(dto, ip, device);
        
        // 获取用户角色信息并判断是否为管理员
        Long userId = (Long) result.get("userId");
        List<String> roles = userService.getUserRoles(userId);
        result.put("roles", roles);
        result.put("isAdmin", roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"));
        
        return Result.success(result);
    }

    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout() {
        userService.logout(StpUtil.getTokenValue());
        return Result.success();
    }

    @PostMapping("/refresh")
    @SaCheckLogin
    public Result<Map<String, Object>> refreshToken() {
        Map<String, Object> result = userService.refreshToken(StpUtil.getTokenValue());
        return Result.success(result);
    }

    /**
     * 获取当前登录用户信息
     * 需要登录权限，返回用户基本信息和角色权限信息
     * 
     * @return Result<Map<String, Object>> 用户信息结果，包含用户详情、角色列表和管理员标识
     */
    @GetMapping("/info")
    @SaCheckLogin
    public Result<Map<String, Object>> getUserInfo() {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = new HashMap<>();
        result.put("user", userService.getUserInfo(userId));
        
        // 获取用户角色信息并判断管理员权限
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
