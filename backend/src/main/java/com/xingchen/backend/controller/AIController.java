package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.ChatClearDTO;
import com.xingchen.backend.dto.ChatRequest;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleContent;
import com.xingchen.backend.mapper.ArticleContentMapper;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.memory.MemoryServiceV2;
import com.xingchen.backend.prompt.PromptBuilder;
import com.xingchen.backend.repository.ArticleSearchRepository;
import com.xingchen.backend.repository.ArticleVectorRepository;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话控制器（修复版）
 * - SSE 流式接口加超时 + 异常处理 + 回调清理
 * - 防止内存泄漏
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;
    private final ChatSessionService chatSessionService;
    private final ArticleMapper articleMapper;
    private final ArticleContentMapper articleContentMapper;
    private final ArticleVectorRepository articleVectorRepository;
    private final ArticleSearchRepository articleSearchRepository;
    private final PromptBuilder promptBuilder;
    private final MemoryServiceV2 memoryServiceV2;

    @SaCheckLogin
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody ChatRequest req) {
        if (req.getMessage() == null || req.getMessage().trim().isEmpty()) {
            return Result.error(400, "消息不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        String sessionId = req.getSessionId() != null ? req.getSessionId() : "default";
        List<Map<String, String>> history = chatSessionService.getHistory(sessionId);

        String response;
        if (Boolean.TRUE.equals(req.getUseRag())) {
            response = aiService.chatWithRagAndContext(req.getMessage(), history);
        } else {
            // 使用 PromptBuilder 构建带记忆的提示词
            String systemPrompt = promptBuilder.buildChatPrompt(userId, req.getMessage(), "default");
            response = aiService.chatWithUserApiKeyAndPrompt(userId, req.getMessage(), systemPrompt);
        }

        chatSessionService.asyncSaveConversation(sessionId, req.getMessage(), response);

        // 异步提取并保存记忆
        if (req.getMessage().contains("喜欢") || req.getMessage().contains("偏好") ||
            req.getMessage().contains("项目") || req.getMessage().contains("技术")) {
            memoryServiceV2.addMemory(userId, req.getMessage(), "PREFERENCE");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("response", response);
        result.put("sessionId", sessionId);
        return Result.success(result);
    }

    /**
     * SSE 流式对话（修复版）
     * - 超时 2 分钟（而非 5 分钟）
     * - 注册 onCompletion/onTimeout/onError 回调防止泄漏
     * - 异常时正确关闭 emitter
     */
    @GetMapping("/stream")
    public SseEmitter streamChat(@RequestParam String message,
                                  @RequestParam(defaultValue = "default") String sessionId) {
        // 2 分钟超时
        SseEmitter emitter = new SseEmitter(120_000L);

        // 注册回调，防止内存泄漏
        emitter.onCompletion(() -> log.debug("SSE 完成: session={}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("SSE 超时: session={}", sessionId);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.warn("SSE 错误: session={}, error={}", sessionId, e.getMessage());
            emitter.complete();
        });

        // 发送初始事件（让客户端知道连接已建立）
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 异步流式调用
        aiService.streamChatSSE(message, emitter);

        return emitter;
    }

    @SaCheckRole("admin")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateArticle(@RequestBody Map<String, Object> request) {
        String topic = (String) request.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            return Result.error(400, "文章主题不能为空");
        }

        try {
            String content = aiService.chat("请写一篇关于「" + topic + "」的技术博客文章，使用 Markdown 格式");

            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            result.put("topic", topic);
            return Result.success(result);
        } catch (Exception e) {
            log.error("文章生成失败: {}", e.getMessage(), e);
            return Result.error(500, "文章生成失败: " + e.getMessage());
        }
    }

    @SaCheckRole("admin")
    @PostMapping("/rewrite")
    public Result<Map<String, Object>> rewriteArticle(@RequestBody Map<String, Object> request) {
        Long articleId = Long.valueOf(request.get("articleId").toString());
        String style = (String) request.getOrDefault("style", "professional");

        Article article = articleMapper.selectOneById(articleId);
        if (article == null) {
            return Result.error(404, "文章不存在");
        }

        ArticleContent articleContent = articleContentMapper.selectOneById(articleId);
        String originalContent = articleContent != null ? articleContent.getContent() : "";

        String prompt = String.format(
                "请用%s的风格重写以下文章，保持核心内容不变：\n\n%s",
                style, originalContent
        );

        String rewritten = aiService.chat(prompt);

        Map<String, Object> result = new HashMap<>();
        result.put("content", rewritten);
        result.put("articleId", articleId);
        return Result.success(result);
    }

    @SaCheckLogin
    @PostMapping("/clear")
    public Result<Void> clearHistory(@RequestBody ChatClearDTO dto) {
        String sessionId = dto.getSessionId() != null ? dto.getSessionId() : "default";
        chatSessionService.clearHistory(sessionId);
        return Result.success(null);
    }

    @SaCheckRole("admin")
    @PostMapping("/index")
    public Result<Void> indexArticles() {
        aiService.indexAllArticles();
        return Result.success(null);
    }

    @SaCheckRole("admin")
    @PostMapping("/clear-kb")
    public Result<Void> clearKnowledgeBase() {
        aiService.clearKnowledgeBase();
        return Result.success(null);
    }

    @SaCheckRole("admin")
    @GetMapping("/vector/info")
    public Result<Map<String, Object>> getVectorInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("type", "PostgreSQL pgvector");
        info.put("articleVectorCount", articleVectorRepository.count());
        info.put("articleSearchCount", articleSearchRepository.count());
        return Result.success(info);
    }

    @SaCheckRole("admin")
    @PostMapping("/vector/delete")
    public Result<Void> deleteVectorCollection(@RequestParam(required = false, defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.fail(400, "请确认删除操作：添加 ?confirm=true 参数");
        }
        // PostgreSQL 表由迁移脚本管理，只需清空数据
        // 这里暂时不提供清空功能，以防误操作
        return Result.success(null);
    }

    @SaCheckRole("admin")
    @PostMapping("/vector/recreate")
    public Result<String> recreateVectorCollection(@RequestParam(required = false, defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.fail(400, "请确认重建操作：添加 ?confirm=true 参数");
        }
        try {
            // PostgreSQL 表永久存在，无需重建，只需重新索引
            aiService.indexAllArticles();
            return Result.success("已开始重新索引所有文章到 PostgreSQL");
        } catch (Exception e) {
            return Result.fail(500, "重建失败: " + e.getMessage());
        }
    }
}
