package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.entity.FeishuAppConfig;
import com.xingchen.backend.service.FeishuAppConfigService;
import com.xingchen.backend.service.FeishuWebSocketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu/app")
@RequiredArgsConstructor
@Slf4j
public class FeishuAppConfigController {

    private final FeishuAppConfigService feishuAppConfigService;
    private final FeishuWebSocketManager feishuWebSocketManager;

    @SaCheckLogin
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        FeishuAppConfig config = feishuAppConfigService.getByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        if (config != null) {
            result.put("appId", config.getAppId());
            result.put("enabled", config.getEnabled());
            result.put("connectionStatus", config.getConnectionStatus());
            result.put("hasAppSecret", config.getAppSecret() != null && !config.getAppSecret().isEmpty());
        } else {
            result.put("appId", null);
            result.put("enabled", 0);
            result.put("connectionStatus", "DISCONNECTED");
            result.put("hasAppSecret", false);
        }

        return Result.success(result);
    }

    @SaCheckLogin
    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody Map<String, Object> params) {
        Long userId = StpUtil.getLoginIdAsLong();

        FeishuAppConfig config = new FeishuAppConfig();
        config.setUserId(userId);
        config.setAppId((String) params.get("appId"));
        config.setAppSecret((String) params.get("appSecret"));
        config.setEnabled(Boolean.TRUE.equals(params.get("enabled")) ? 1 : 0);

        feishuAppConfigService.saveOrUpdate(config);

        log.info("用户 {} 更新了飞书应用配置", userId);
        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping("/config")
    public Result<Void> deleteConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        feishuAppConfigService.deleteByUserId(userId);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection() {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean success = feishuAppConfigService.testConnection(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "连接测试通过" : "连接测试失败，请检查 App ID 和 App Secret");

        return Result.success(result);
    }

    @SaCheckLogin
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Long userId = StpUtil.getLoginIdAsLong();
        FeishuAppConfig config = feishuAppConfigService.getByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        if (config != null) {
            result.put("enabled", config.getEnabled());
            // 使用 WebSocketManager 获取实时连接状态
            String realTimeStatus = feishuWebSocketManager.getConnectionStatus(userId);
            result.put("connectionStatus", realTimeStatus);
            result.put("lastConnectedAt", config.getLastConnectedAt());
        } else {
            result.put("enabled", 0);
            result.put("connectionStatus", "DISCONNECTED");
            result.put("lastConnectedAt", null);
        }

        return Result.success(result);
    }

    @SaCheckLogin
    @PostMapping("/restart")
    public Result<Map<String, Object>> restartConnection() {
        Long userId = StpUtil.getLoginIdAsLong();
        FeishuAppConfig config = feishuAppConfigService.getByUserId(userId);

        if (config == null || config.getEnabled() == 0) {
            return Result.error(400, "飞书应用未配置或未启用");
        }

        feishuWebSocketManager.restartConnection(userId, config.getAppId(), config.getAppSecret());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "连接已重启");
        result.put("status", feishuWebSocketManager.getConnectionStatus(userId));

        return Result.success(result);
    }
}
