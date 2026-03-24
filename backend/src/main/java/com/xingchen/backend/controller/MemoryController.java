package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @SaCheckLogin
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Long userId = StpUtil.getLoginIdAsLong();

        Map<String, Object> stats = new HashMap<>();
        stats.put("conversationCount", memoryService.getConversationCount(userId));
        stats.put("entityCount", memoryService.getEntityCount(userId));

        return Result.success(stats);
    }

    @SaCheckLogin
    @GetMapping("/conversations")
    public Result<?> getConversations(@RequestParam(defaultValue = "20") int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(memoryService.getRecentConversations(userId, limit));
    }

    @SaCheckLogin
    @DeleteMapping
    public Result<Void> clearMemory() {
        Long userId = StpUtil.getLoginIdAsLong();
        memoryService.clearUserMemory(userId);
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/preferences")
    public Result<Map<String, Object>> getPreferences() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(memoryService.getPreferences(userId));
    }
}
