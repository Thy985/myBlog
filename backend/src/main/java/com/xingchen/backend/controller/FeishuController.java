package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.entity.FeishuConfig;
import com.xingchen.backend.service.FeishuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
@RequiredArgsConstructor
@Slf4j
public class FeishuController {

    private final FeishuService feishuService;

    @SaCheckLogin
    @GetMapping("/config")
    public Result<FeishuConfig> getConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(feishuService.getByUserId(userId));
    }

    @SaCheckLogin
    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody FeishuConfig config) {
        Long userId = StpUtil.getLoginIdAsLong();
        config.setUserId(userId);
        feishuService.saveOrUpdate(config);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/test")
    public Result<Void> testConnection() {
        Long userId = StpUtil.getLoginIdAsLong();
        feishuService.sendMessage(userId, "🧪 测试消息 - 飞书配置成功！");
        return Result.success();
    }

    @PostMapping("/webhook")
    public Result<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("收到飞书 Webhook: {}", payload);
        
        String msgType = (String) payload.get("msg_type");
        if ("text".equals(msgType)) {
            Map<String, Object> content = (Map<String, Object>) payload.get("content");
            String text = (String) content.get("text");
            
            log.info("飞书消息: {}", text);
        }
        
        return Result.success();
    }
}
