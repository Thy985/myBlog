package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.CommandDTO;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.CommandParserService;
import com.xingchen.backend.service.UserApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AIService aiService;
    private final CommandParserService commandParserService;
    private final UserApiKeyService userApiKeyService;

    @SaCheckLogin
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            return Result.error(400, "消息不能为空");
        }
        
        CommandDTO command = commandParserService.parse(message);
        
        Map<String, Object> result = new HashMap<>();
        
        switch (command.getIntent()) {
            case "create_article" -> {
                result.put("type", "article_generating");
                result.put("message", "正在为您生成文章: " + command.getTopic());
            }
            case "schedule_task" -> {
                result.put("type", "task_creating");
                result.put("message", "正在为您创建定时任务: " + command.getTopic());
            }
            case "list_tasks" -> {
                result.put("type", "task_list");
                result.put("message", "查询任务列表");
            }
            case "cancel_task" -> {
                result.put("type", "task_cancelling");
                result.put("message", "取消任务: " + command.getTaskId());
            }
            case "publish_article" -> {
                result.put("type", "article_publishing");
                result.put("message", "发布文章: " + command.getArticleId());
            }
            default -> {
                String reply = aiService.chat(message);
                result.put("type", "chat");
                result.put("reply", reply);
            }
        }
        
        result.put("intent", command.getIntent());
        result.put("command", command);
        
        return Result.success(result);
    }

    @SaCheckLogin
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("availableProviders", userApiKeyService.getAvailableProviders());
        return Result.success(result);
    }
}
