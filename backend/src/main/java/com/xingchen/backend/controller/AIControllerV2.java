package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.memory.MemoryServiceV2;
import com.xingchen.backend.prompt.PromptBuilder;
import com.xingchen.backend.security.annotation.UserRateLimit;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AI 控制器 V2（增强版）
 * 
 * 新增特性：
 * 1. 多场景提示词支持
 * 2. 记忆上下文自动注入
 * 3. 流式对话优化
 * 4. 代码审查/文档生成
 */
@RestController
@RequestMapping("/api/ai/v2")
@Slf4j
@RequiredArgsConstructor
public class AIControllerV2 {

    private final AIService aiService;
    private final PromptBuilder promptBuilder;
    private final MemoryService memoryService;
    private final MemoryServiceV2 memoryServiceV2;

    /**
     * 通用对话（带记忆和知识库）
     */
    @PostMapping("/chat")
    @SaCheckLogin
    @UserRateLimit
    public Result<String> chat(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 构建动态提示词
        String systemPrompt = promptBuilder.buildChatPrompt(
                userId,
                request.getMessage(),
                request.getTaskType()
        );

        // 调用 AI（使用用户配置的 API Key）
        String response = aiService.chatWithUserApiKeyAndPrompt(
                userId,
                request.getMessage(),
                systemPrompt
        );

        // 异步提取记忆
        extractMemoryAsync(userId, request.getMessage(), response);

        return Result.success(response);
    }

    /**
     * 代码助手
     */
    @PostMapping("/code")
    @SaCheckLogin
    @UserRateLimit
    public Result<String> codeAssistant(@RequestBody CodeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        String systemPrompt = promptBuilder.buildCodePrompt(
                request.getMessage(),
                request.getLanguage()
        );
        
        String response = aiService.chatWithContext(
                request.getMessage(),
                request.getHistory(),
                systemPrompt
        );
        
        return Result.success(response);
    }

    /**
     * 代码审查
     */
    @PostMapping("/code-review")
    @SaCheckLogin
    @UserRateLimit
    public Result<String> codeReview(@RequestBody CodeReviewRequest request) {
        String systemPrompt = promptBuilder.buildCodeReviewPrompt(
                request.getCode(),
                request.getLanguage()
        );
        
        String response = aiService.chat(systemPrompt);
        return Result.success(response);
    }

    /**
     * 博客写作助手
     */
    @PostMapping("/blog")
    @SaCheckLogin
    @UserRateLimit
    public Result<String> blogAssistant(@RequestBody BlogRequest request) {
        String systemPrompt = promptBuilder.buildBlogPrompt(
                request.getTopic(),
                request.getTargetAudience(),
                request.getWordCount()
        );
        
        String response = aiService.chatWithContext(
                "请根据以上要求撰写文章",
                null,
                systemPrompt
        );
        
        return Result.success(response);
    }

    /**
     * 文档生成
     */
    @PostMapping("/doc")
    @SaCheckLogin
    @UserRateLimit
    public Result<String> generateDoc(@RequestBody DocRequest request) {
        String systemPrompt = promptBuilder.buildDocPrompt(
                request.getCode(),
                request.getDocType()
        );
        
        String response = aiService.chat(systemPrompt);
        return Result.success(response);
    }

    /**
     * 流式对话
     */
    @GetMapping("/stream")
    @SaCheckLogin
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String taskType) {

        Long userId = StpUtil.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(120000L);

        // 在异步线程中执行，避免阻塞
        new Thread(() -> {
            try {
                String systemPrompt = promptBuilder.buildChatPrompt(userId, message, taskType);
                StringBuilder fullResponse = new StringBuilder();

                aiService.streamChat(message, token -> {
                    try {
                        // 将每个 token 发送给客户端
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(token));
                        fullResponse.append(token);
                    } catch (Exception e) {
                        log.error("发送流式消息失败", e);
                        emitter.completeWithError(e);
                    }
                });

                // 发送完成事件
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("done"));
                emitter.complete();

                // 流结束后异步提取记忆
                extractMemoryAsync(userId, message, fullResponse.toString());

            } catch (Exception e) {
                log.error("流式对话失败", e);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 获取用户记忆
     */
    @GetMapping("/memory")
    @SaCheckLogin
    public Result<String> getUserMemory() {
        Long userId = StpUtil.getLoginIdAsLong();
        String memory = memoryServiceV2.getUserMemoryContext(userId);
        return Result.success(memory);
    }

    /**
     * 搜索记忆
     */
    @PostMapping("/memory/search")
    @SaCheckLogin
    public Result<String> searchMemory(@RequestBody Map<String, String> request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String query = request.get("query");
        String memory = memoryServiceV2.retrieveMemory(userId, query, 5);
        return Result.success(memory);
    }

    /**
     * 手动添加记忆
     */
    @PostMapping("/memory/add")
    @SaCheckLogin
    public Result<Void> addMemory(@RequestBody Map<String, String> request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String content = request.get("content");
        String type = request.getOrDefault("type", "GENERAL");

        memoryServiceV2.addMemory(userId, content, type);
        return Result.success();
    }

    // ========== 异步提取记忆 ==========
    
    private void extractMemoryAsync(Long userId, String userMessage, String aiResponse) {
        // 提取用户偏好
        if (containsPreference(userMessage)) {
            memoryServiceV2.addMemory(userId, extractPreference(userMessage), "PREFERENCE");
        }

        // 提取项目信息
        if (containsProjectInfo(userMessage)) {
            memoryServiceV2.addMemory(userId, extractProjectInfo(userMessage), "PROJECT");
        }
    }

    private boolean containsPreference(String message) {
        return message.contains("喜欢") || message.contains("偏好") || 
               message.contains("习惯") || message.contains("常用");
    }

    private boolean containsProjectInfo(String message) {
        return message.contains("项目") || message.contains("技术栈") || 
               message.contains("框架") || message.contains("架构");
    }

    private String extractPreference(String message) {
        // 简单提取，实际可以用 LLM 提取
        return "用户偏好: " + message;
    }

    private String extractProjectInfo(String message) {
        return "项目信息: " + message;
    }

    // ========== 请求类 ==========
    
    @lombok.Data
    public static class ChatRequest {
        private String message;
        private List<Map<String, String>> history;
        private String taskType = "default";
    }

    @lombok.Data
    public static class CodeRequest {
        private String message;
        private List<Map<String, String>> history;
        private String language;
    }

    @lombok.Data
    public static class CodeReviewRequest {
        private String code;
        private String language;
    }

    @lombok.Data
    public static class BlogRequest {
        private String topic;
        private String targetAudience;
        private int wordCount = 2000;
    }

    @lombok.Data
    public static class DocRequest {
        private String code;
        private String docType = "API";
    }
}