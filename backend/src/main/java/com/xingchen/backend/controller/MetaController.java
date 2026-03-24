package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.meta.lightweight.LightweightMetaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 微型元能力控制器
 */
@RestController
@RequestMapping("/api/meta")
@Slf4j
@RequiredArgsConstructor
public class MetaController {

    private final LightweightMetaOrchestrator metaOrchestrator;

    /**
     * 获取元能力状态
     */
    @GetMapping("/status")
    @SaCheckLogin
    public Result<LightweightMetaOrchestrator.MetaStats> getStatus() {
        return Result.success(metaOrchestrator.getStats());
    }

    /**
     * 记录反馈（用于偏好学习）
     */
    @PostMapping("/feedback")
    @SaCheckLogin
    public Result<Void> recordFeedback(@RequestBody FeedbackRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        metaOrchestrator.recordFeedback(userId, request.getUserInput(), request.isPositive());
        return Result.success();
    }

    @lombok.Data
    public static class FeedbackRequest {
        private String userInput;
        private boolean positive;
    }
}