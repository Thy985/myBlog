package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.ai.service.UserAIConfigService;
import com.xingchen.backend.ai.util.AIUserContext;
import com.xingchen.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI管理控制器
 * 用户管理自己的AI配置
 */
@RestController
@RequestMapping("/api/v2/ai/admin")
@RequiredArgsConstructor
@Slf4j
public class AIAdminController {
    
    private final UserAIConfigService userAIConfigService;
    
    /**
     * 获取当前用户的AI配置
     */
    @SaCheckLogin
    @GetMapping("/config")
    public Result<AIUserContext> getUserConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        AIUserContext context = userAIConfigService.getUserContext(userId);
        return Result.success(context);
    }
    
    /**
     * 检查用户是否配置了AI
     */
    @SaCheckLogin
    @GetMapping("/configured")
    public Result<Boolean> isConfigured() {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean configured = userAIConfigService.isAIConfigured(userId);
        return Result.success(configured);
    }
    
    /**
     * 获取用户记忆统计
     */
    @SaCheckLogin
    @GetMapping("/memory/stats")
    public Result<Map<String, Object>> getMemoryStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        var stats = userAIConfigService.getUserMemoryStats(userId);
        
        return Result.success(Map.of(
                "shortTermMemoryCount", stats.shortTermMemoryCount(),
                "activeSessions", stats.activeSessions()
        ));
    }
    
    /**
     * 获取用户的所有会话
     */
    @SaCheckLogin
    @GetMapping("/sessions")
    public Result<List<String>> getUserSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<String> sessions = userAIConfigService.getUserSessions(userId);
        return Result.success(sessions);
    }
    
    /**
     * 清除用户所有记忆
     */
    @SaCheckLogin
    @DeleteMapping("/memory")
    public Result<Void> clearUserMemory() {
        Long userId = StpUtil.getLoginIdAsLong();
        userAIConfigService.clearUserMemory(userId);
        return Result.success();
    }
    
    /**
     * 清除特定会话
     */
    @SaCheckLogin
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        userAIConfigService.clearUserSession(userId, sessionId);
        return Result.success();
    }
    
    /**
     * 刷新用户配置缓存
     */
    @SaCheckLogin
    @PostMapping("/config/refresh")
    public Result<Void> refreshConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        userAIConfigService.refreshUserConfig(userId);
        return Result.success();
    }
    
    /**
     * 获取支持的Provider列表
     */
    @SaCheckLogin
    @GetMapping("/providers")
    public Result<List<UserAIConfigService.ProviderInfo>> getSupportedProviders() {
        return Result.success(userAIConfigService.getSupportedProviders());
    }
    
    // ==================== 管理员接口 ====================
    
    /**
     * 获取指定用户的AI配置（管理员）
     */
    @SaCheckRole("admin")
    @GetMapping("/users/{userId}/config")
    public Result<AIUserContext> getUserConfigByAdmin(@PathVariable Long userId) {
        AIUserContext context = userAIConfigService.getUserContext(userId);
        return Result.success(context);
    }
    
    /**
     * 清除指定用户的记忆（管理员）
     */
    @SaCheckRole("admin")
    @DeleteMapping("/users/{userId}/memory")
    public Result<Void> clearUserMemoryByAdmin(@PathVariable Long userId) {
        userAIConfigService.clearUserMemory(userId);
        log.info("管理员清除用户 {} 的记忆", userId);
        return Result.success();
    }
}