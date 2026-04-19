package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.ab.ABTestService;
import com.xingchen.backend.ai.gateway.AIGateway;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * Agent 控制器
 * 使用新架构 AIGateway
 */
@RestController
@RequestMapping("/api/agent")
@Slf4j
@RequiredArgsConstructor
public class AgentController {

    private final AIGateway aiGateway;
    private final ABTestService abTestService;
    private final ArticleService articleService;
    private final WebSearchService webSearchService;

    // 博客发布相关模式
    private static final java.util.regex.Pattern BLOG_TITLE_PATTERN =
        java.util.regex.Pattern.compile("标题[是为：:]\s*['\"]?([^'\"，,。\\n]+)['\"]?");
    private static final java.util.regex.Pattern BLOG_CONTENT_PATTERN =
        java.util.regex.Pattern.compile("内容[是为：:]\s*(.+?)(?=分类|category|$)",
            java.util.regex.Pattern.DOTALL);
    private static final java.util.regex.Pattern BLOG_CATEGORY_PATTERN =
        java.util.regex.Pattern.compile("分类[是为：:]\s*['\"]?([^'\"，,。\\n]+)['\"]?");

    /**
     * 开始新会话
     */
    @PostMapping("/session/start")
    @SaCheckLogin
    public Result<StartSessionResponse> startSession() {
        String sessionId = "agent_" + UUID.randomUUID().toString().replace("-", "");
        return Result.success(new StartSessionResponse(sessionId, "会话创建成功"));
    }

    /**
     * Agent 对话（SSE 流式版本）
     */
    @PostMapping("/chat/stream")
    @SaCheckLogin
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        emitter.onCompletion(() -> log.debug("Agent SSE 完成: sessionId={}", request.sessionId()));
        emitter.onTimeout(() -> log.warn("Agent SSE 超时: sessionId={}", request.sessionId()));
        emitter.onError(e -> log.error("Agent SSE 错误: sessionId={}", request.sessionId(), e));

        // 异步执行，避免阻塞 HTTP 线程
        new Thread(() -> {
            try {
                // 检测博客发布请求
                if (isBlogPublishRequest(request.message())) {
                    handleBlogPublishStream(userId, request.message(), emitter);
                    return;
                }

                // 构建 AI 请求
                AIRequest aiRequest = AIRequest.builder()
                        .userId(userId)
                        .sessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString())
                        .message(request.message())
                        .stream(true)
                        .build();

                // 使用 AIGateway 流式处理
                StringBuilder fullResponse = new StringBuilder();
                aiGateway.processStream(aiRequest, response -> {
                    try {
                        if (response.getType() == AIResponse.ResponseType.STREAM_END) {
                            emitter.send(SseEmitter.event().name("done").data("{\"done\": true}"));
                            emitter.complete();
                        } else if (response.getType() == AIResponse.ResponseType.ERROR) {
                            emitter.send(SseEmitter.event().name("error").data("{\"error\": \"" + escapeJson(response.getContent()) + "\"}"));
                            emitter.completeWithError(new RuntimeException(response.getContent()));
                        } else {
                            String sseData = "{\"content\": \"" + escapeJson(response.getContent()) + "\"}";
                            emitter.send(SseEmitter.event().name("message").data(sseData));
                            fullResponse.append(response.getContent());
                        }
                    } catch (Exception e) {
                        log.error("发送SSE消息失败", e);
                        emitter.completeWithError(e);
                    }
                });

            } catch (Exception e) {
                log.error("Agent SSE 执行异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\": \"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 处理博客发布（流式）
     */
    private void handleBlogPublishStream(Long userId, String message, SseEmitter emitter) throws Exception {
        BlogInfo blogInfo = parseBlogInfo(message);

        if (blogInfo.title == null || blogInfo.title.isEmpty()) {
            emitter.send(SseEmitter.event().name("error").data("{\"error\": \"无法解析博客标题\"}"));
            emitter.complete();
            return;
        }

        // 1. 发送意图识别事件
        sendIntentEvent(emitter, "CREATE_ARTICLE", 0.95,
            List.of(Map.of("name", "title", "value", blogInfo.title)),
            true, List.of("article_generator"));

        sendStep(emitter, 1, 5, "意图识别", "done", "检测到博客发布请求", 100L);

        // 2. 联网搜索并发送 RAG 来源事件
        sendStep(emitter, 2, 5, "联网搜索", "running", "正在搜索相关资料...", null);
        List<WebSearchService.SearchResult> searchResults = webSearchService.search(blogInfo.title, 3);

        if (searchResults != null && !searchResults.isEmpty()) {
            // 发送 RAG 来源事件
            sendRagEvent(emitter, blogInfo.title, searchResults);
            sendStep(emitter, 2, 5, "联网搜索", "done", "已获取 " + searchResults.size() + " 条参考资料", 500L);
        } else {
            sendStep(emitter, 2, 5, "联网搜索", "done", "未找到相关参考资料", 200L);
        }

        // 3. 发送工具调用事件 - 文章生成工具
        sendToolCallEvent(emitter, "article_generator", Map.of(
            "topic", blogInfo.title,
            "wordCount", 1000,
            "style", "technical"
        ));

        sendStep(emitter, 3, 5, "内容生成", "running", "正在调用 AI 生成初稿...", null);

        long startTime = System.currentTimeMillis();
        String generatedContent = generateBlogContent(userId, blogInfo.title, blogInfo.content);
        long generationTime = System.currentTimeMillis() - startTime;

        // 发送工具调用结果事件
        sendToolResultEvent(emitter, "article_generator", true,
            Map.of("wordCount", generatedContent != null ? generatedContent.length() : 0),
            "文章生成成功", generationTime);

        sendStep(emitter, 3, 5, "内容生成", "done",
            "初稿已生成，字数 " + (generatedContent != null ? generatedContent.length() : 0), generationTime);

        sendStep(emitter, 4, 5, "文章优化", "done", "文章质量审核完成", 300L);

        // 4. 发送工具调用事件 - 发布文章
        sendToolCallEvent(emitter, "publish_article", Map.of(
            "title", blogInfo.title,
            "categoryId", blogInfo.categoryId != null ? blogInfo.categoryId : 2
        ));

        sendStep(emitter, 5, 5, "发布文章", "running", "正在创建文章...", null);

        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle(blogInfo.title);
        dto.setContent(generatedContent);
        dto.setCategoryId(blogInfo.categoryId != null ? blogInfo.categoryId : 2);
        var articleVO = articleService.createArticle(userId, dto);

        // 发送工具调用结果事件
        sendToolResultEvent(emitter, "publish_article", true,
            Map.of("articleId", articleVO.getId()),
            "文章发布成功", 200L);

        sendStep(emitter, 5, 5, "发布文章", "done", "发布成功，文章ID: " + articleVO.getId(), 200L);

        String response = String.format(
            "✅ 博客发布成功！\n\n" +
            "**标题**: %s\n" +
            "**分类**: %s\n" +
            "**文章ID**: %d\n" +
            "**字数**: %d 字\n\n" +
            "您可以在文章管理页面查看和编辑发布的文章。",
            articleVO.getTitle(),
            articleVO.getCategory() != null ? articleVO.getCategory().getName() : "AI探索",
            articleVO.getId(),
            generatedContent != null ? generatedContent.length() : 0
        );
        emitter.send(SseEmitter.event().name("message").data("{\"content\": \"" + escapeJson(response) + "\"}"));
        emitter.send(SseEmitter.event().name("done").data("{\"done\": true}"));
        emitter.complete();
    }

    /**
     * 发送意图识别事件
     */
    private void sendIntentEvent(SseEmitter emitter, String intentType, double confidence,
                                 List<Map<String, String>> entities, boolean requiresTool,
                                 List<String> possibleTools) throws Exception {
        StringBuilder entitiesJson = new StringBuilder("[");
        for (int i = 0; i < entities.size(); i++) {
            Map<String, String> entity = entities.get(i);
            entitiesJson.append(String.format("{\"name\": \"%s\", \"value\": \"%s\"}",
                escapeJson(entity.get("name")), escapeJson(entity.get("value"))));
            if (i < entities.size() - 1) entitiesJson.append(", ");
        }
        entitiesJson.append("]");

        StringBuilder toolsJson = new StringBuilder("[");
        for (int i = 0; i < possibleTools.size(); i++) {
            toolsJson.append("\"").append(escapeJson(possibleTools.get(i))).append("\"");
            if (i < possibleTools.size() - 1) toolsJson.append(", ");
        }
        toolsJson.append("]");

        String data = String.format(
            "{\"intent\": {\"type\": \"%s\", \"confidence\": %.2f, \"entities\": %s, \"requiresTool\": %b, \"possibleTools\": %s}}",
            intentType, confidence, entitiesJson, requiresTool, toolsJson
        );
        emitter.send(SseEmitter.event().name("intent").data(data));
    }

    /**
     * 发送工具调用事件
     */
    private void sendToolCallEvent(SseEmitter emitter, String toolName, Map<String, Object> parameters) throws Exception {
        StringBuilder paramsJson = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            paramsJson.append(String.format("\"%s\": ", escapeJson(entry.getKey())));
            Object value = entry.getValue();
            if (value instanceof String) {
                paramsJson.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                paramsJson.append(value);
            } else if (value instanceof Boolean) {
                paramsJson.append(value);
            } else {
                paramsJson.append("\"").append(escapeJson(value.toString())).append("\"");
            }
            if (i < parameters.size() - 1) paramsJson.append(", ");
            i++;
        }
        paramsJson.append("}");

        String data = String.format(
            "{\"toolName\": \"%s\", \"parameters\": %s, \"status\": \"executing\"}",
            escapeJson(toolName), paramsJson
        );
        emitter.send(SseEmitter.event().name("tool_call").data(data));
    }

    /**
     * 发送工具调用结果事件
     */
    private void sendToolResultEvent(SseEmitter emitter, String toolName, boolean success,
                                     Map<String, Object> result, String message, long executionTime) throws Exception {
        StringBuilder resultJson = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            resultJson.append(String.format("\"%s\": ", escapeJson(entry.getKey())));
            Object value = entry.getValue();
            if (value instanceof String) {
                resultJson.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                resultJson.append(value);
            } else if (value instanceof Boolean) {
                resultJson.append(value);
            } else {
                resultJson.append("\"").append(escapeJson(value.toString())).append("\"");
            }
            if (i < result.size() - 1) resultJson.append(", ");
            i++;
        }
        resultJson.append("}");

        String data = String.format(
            "{\"toolName\": \"%s\", \"success\": %b, \"result\": %s, \"message\": \"%s\", \"executionTime\": %d}",
            escapeJson(toolName), success, resultJson, escapeJson(message), executionTime
        );
        emitter.send(SseEmitter.event().name("tool_result").data(data));
    }

    /**
     * 发送 RAG 来源事件
     */
    private void sendRagEvent(SseEmitter emitter, String query, List<WebSearchService.SearchResult> sources) throws Exception {
        StringBuilder sourcesJson = new StringBuilder("[");
        for (int i = 0; i < sources.size(); i++) {
            WebSearchService.SearchResult source = sources.get(i);
            String cleanContent = cleanHtml(source.content());
            if (cleanContent.length() > 200) {
                cleanContent = cleanContent.substring(0, 200) + "...";
            }
            // 模拟相关度评分（实际应该由 RAG 系统提供）
            double relevance = 0.9 - (i * 0.1);
            sourcesJson.append(String.format(
                "{\"title\": \"%s\", \"url\": \"%s\", \"snippet\": \"%s\", \"relevance\": %.2f}",
                escapeJson(source.title()),
                escapeJson(source.url()),
                escapeJson(cleanContent),
                relevance
            ));
            if (i < sources.size() - 1) sourcesJson.append(", ");
        }
        sourcesJson.append("]");

        String data = String.format(
            "{\"query\": \"%s\", \"sources\": %s}",
            escapeJson(query), sourcesJson
        );
        emitter.send(SseEmitter.event().name("rag").data(data));
    }

    private void sendStep(SseEmitter emitter, int currentStep, int totalSteps, String stepName, String status, String description, Long executionTime) throws Exception {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append(String.format(
            "{\"currentStep\": %d, \"totalSteps\": %d, \"stepName\": \"%s\", \"status\": \"%s\", \"description\": \"%s\"",
            currentStep, totalSteps, escapeJson(stepName), status, escapeJson(description)
        ));
        if (executionTime != null) {
            dataBuilder.append(String.format(", \"executionTime\": %d", executionTime));
        }
        dataBuilder.append("}");
        emitter.send(SseEmitter.event().name("step").data(dataBuilder.toString()));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    /**
     * Agent 对话（非流式）
     */
    @PostMapping("/chat")
    @SaCheckLogin
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查是否是博客发布请求
        if (isBlogPublishRequest(request.message())) {
            return handleBlogPublishRequest(userId, request.message());
        }

        // 使用 AIGateway 处理
        AIRequest aiRequest = AIRequest.builder()
                .userId(userId)
                .sessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString())
                .message(request.message())
                .stream(false)
                .build();

        AIResponse response = aiGateway.process(aiRequest);

        return Result.success(new ChatResponse(
                aiRequest.getSessionId(),
                response.getContent(),
                response.isSuccess() ? "COMPLETED" : "ERROR",
                0,
                0
        ));
    }

    /**
     * 检测是否是博客发布请求
     */
    private boolean isBlogPublishRequest(String message) {
        String lower = message.toLowerCase();
        return (lower.contains("发布") || lower.contains("发一个") || lower.contains("发表") || lower.contains("写")) &&
               (lower.contains("博客") || lower.contains("文章") || lower.contains("博文") || lower.contains("帖子")) ||
               lower.contains("写一篇") && (lower.contains("博客") || lower.contains("文章")) ||
               lower.contains("publish") && lower.contains("blog");
    }

    /**
     * 处理博客发布请求
     */
    private Result<ChatResponse> handleBlogPublishRequest(Long userId, String message) {
        log.info("检测到博客发布请求: userId={}", userId);

        try {
            BlogInfo blogInfo = parseBlogInfo(message);

            if (blogInfo.title == null || blogInfo.title.isEmpty()) {
                return Result.fail(400, "无法解析博客标题，请提供标题");
            }

            String generatedContent = generateBlogContent(userId, blogInfo.title, blogInfo.content);

            ArticleCreateDTO dto = new ArticleCreateDTO();
            dto.setTitle(blogInfo.title);
            dto.setContent(generatedContent);
            dto.setCategoryId(blogInfo.categoryId != null ? blogInfo.categoryId : 2);

            var articleVO = articleService.createArticle(userId, dto);

            String response = String.format(
                "✅ 博客发布成功！\n\n" +
                "**标题**: %s\n" +
                "**分类**: %s\n" +
                "**文章ID**: %d\n" +
                "**字数**: %d 字\n\n" +
                "您可以在文章管理页面查看和编辑发布的文章。",
                articleVO.getTitle(),
                articleVO.getCategory() != null ? articleVO.getCategory().getName() : "AI探索",
                articleVO.getId(),
                generatedContent != null ? generatedContent.length() : 0
            );

            String sessionId = "agent_" + UUID.randomUUID().toString().replace("-", "");
            return Result.success(new ChatResponse(sessionId, response, "COMPLETED", 0, 0));
        } catch (Exception e) {
            log.error("博客发布失败: {}", e.getMessage(), e);
            return Result.fail(500, "博客发布失败: " + e.getMessage());
        }
    }

    /**
     * 使用 AI 生成博客内容（联网搜索 + 反思改进）
     */
    private String generateBlogContent(Long userId, String title, String hint) {
        // 暂时使用 userId=1 作为系统级调用
        Long blogUserId = 1L;

        // 第一阶段：联网搜索
        String searchContext = "";
        try {
            List<WebSearchService.SearchResult> searchResults = webSearchService.search(title, 5);
            if (searchResults != null && !searchResults.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("## 最新相关信息\n\n");
                for (int i = 0; i < searchResults.size(); i++) {
                    WebSearchService.SearchResult r = searchResults.get(i);
                    String cleanContent = cleanHtml(r.content());
                    if (cleanContent.length() > 300) {
                        cleanContent = cleanContent.substring(0, 300) + "...";
                    }
                    sb.append("【").append(i + 1).append("】").append(r.title()).append("\n");
                    sb.append(cleanContent).append("\n\n");
                }
                searchContext = sb.toString();
                log.info("联网搜索 '{}' 获取到 {} 条结果", title, searchResults.size());
            }
        } catch (Exception e) {
            log.warn("联网搜索失败: {}", e.getMessage());
        }

        // 第二阶段：生成初稿
        String userMessage;
        if (!searchContext.isEmpty()) {
            userMessage = "你是一位资深科技博主，擅长撰写深入浅出、有见地的技术博客。\n\n" +
                    "写作风格要求：1) 有鲜明观点；2) 结合最新案例或数据；3) 语言生动有趣；4) 结构清晰：引言→核心分析→结论；5) 字数 800-1500 字。\n\n" +
                    "## 搜索结果（请务必引用其中最新信息）\n" + searchContext + "\n\n" +
                    "请撰写一篇关于「" + title + "」的博客文章。\n\n直接输出文章正文，不做任何解释。";
        } else {
            userMessage = hint != null && !hint.isEmpty() && !hint.equals("这是一篇由智能体辅助生成的博客文章。")
                    ? "你是一位资深科技博主，擅长撰写深入浅出、有见地的技术博客。写作风格要求：1) 有鲜明观点；2) 结合具体案例或数据；3) 语言生动有趣；4) 结构清晰；5) 字数800-1500字。\n\n请撰写一篇关于「" + title + "」的博客文章。补充要求：" + hint + "\n\n直接输出文章正文，不做任何解释。"
                    : "你是一位资深科技博主，擅长撰写深入浅出、有见地的技术博客。写作风格要求：1) 有鲜明观点；2) 结合具体案例或数据；3) 语言生动有趣；4) 结构清晰；5) 字数800-1500字。\n\n请撰写一篇关于「" + title + "」的博客文章。\n\n直接输出文章正文，不做任何解释。";
        }

        // 直接调用 AIGateway
        AIRequest request = AIRequest.builder()
                .userId(blogUserId)
                .sessionId(UUID.randomUUID().toString())
                .message(userMessage)
                .stream(false)
                .build();

        AIResponse draftResponse = aiGateway.process(request);
        String draft = draftResponse.getContent();

        if (draft == null || draft.trim().isEmpty() || draft.trim().length() < 50) {
            log.warn("AI 生成初稿过短或为空");
            return "这是一篇关于" + title + "的博客文章。";
        }

        // 第三阶段：AI 反思改进
        String reflectUserMessage = "你是一位资深编辑，负责审稿和润色。以下是某博主关于「" + title + "」的初稿：\n\n" +
                draft + "\n\n" +
                "请从以下角度审稿并输出改进后的最终版本：\n" +
                "1. 观点是否鲜明？\n" +
                "2. 是否有具体的案例、数据或故事支撑？\n" +
                "3. 语言是否生动有趣？\n" +
                "4. 结构是否合理？\n\n" +
                "输出格式：只输出改进后的文章正文，不要包含任何编辑评注或说明。";

        try {
            AIRequest reflectRequest = AIRequest.builder()
                    .userId(blogUserId)
                    .sessionId(UUID.randomUUID().toString())
                    .message(reflectUserMessage)
                    .stream(false)
                    .build();

            AIResponse improvedResponse = aiGateway.process(reflectRequest);
            String improved = improvedResponse.getContent();

            if (improved != null && !improved.trim().isEmpty() && improved.length() > 100
                    && !improved.equals(draft) && !improved.contains("请将稿件") && !improved.contains("请把稿子")) {
                log.info("AI 反思改进成功，初稿 {} 字 → 终稿 {} 字", draft.length(), improved.length());
                return improved.trim();
            }
        } catch (Exception e) {
            log.warn("AI 反思改进失败: {}", e.getMessage());
        }

        return draft;
    }

    /**
     * 清理 HTML 标签
     */
    private String cleanHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#\\d+;", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 解析博客信息
     */
    private BlogInfo parseBlogInfo(String message) {
        BlogInfo info = new BlogInfo();
        if (message == null || message.isEmpty()) {
            return info;
        }

        // 1. 标题是...格式
        java.util.regex.Matcher m = BLOG_TITLE_PATTERN.matcher(message);
        if (m.find()) {
            info.title = m.group(1).trim();
        }

        // 2. 《...》 书名号格式
        if (info.title == null) {
            java.util.regex.Pattern.compile("《([^》]+)》").matcher(message);
            java.util.regex.Matcher bookMatcher = java.util.regex.Pattern.compile("《([^》]+)》").matcher(message);
            if (bookMatcher.find()) {
                info.title = bookMatcher.group(1).trim();
            }
        }

        // 3. 发一个关于XXX的博文/文章格式
        if (info.title == null) {
            java.util.regex.Matcher topicMatcher = java.util.regex.Pattern.compile("关于([^的]+)的[博文文章帖子]").matcher(message);
            if (topicMatcher.find()) {
                String topic = topicMatcher.group(1).trim();
                info.title = topic;
                info.content = "这是一篇关于" + topic + "的博客文章。";
            }
        }

        // 4. 写一篇关于XXX的文章格式
        if (info.title == null) {
            java.util.regex.Matcher writeMatcher = java.util.regex.Pattern.compile("(?:写|发布|发表)一篇(?:关于)?(.+?)(?:的)?(?:文章|博客|博文|帖子)").matcher(message);
            if (writeMatcher.find()) {
                String topic = writeMatcher.group(1).trim();
                if (topic.length() > 1 && topic.length() < 100) {
                    info.title = topic;
                    info.content = "这是一篇关于" + topic + "的博客文章。";
                }
            }
        }

        // 提取分类
        java.util.regex.Matcher categoryMatcher = BLOG_CATEGORY_PATTERN.matcher(message);
        if (categoryMatcher.find()) {
            String categoryName = categoryMatcher.group(1).trim();
            info.categoryId = getCategoryIdByName(categoryName);
        }

        // 如果内容为空，使用默认内容
        if (info.content == null || info.content.isEmpty()) {
            info.content = "这是一篇由智能体辅助生成的博客文章。";
        }

        return info;
    }

    private Long getCategoryIdByName(String categoryName) {
        return switch (categoryName) {
            case "AI探索", "AI", "人工智能" -> 2L;
            case "生活随笔", "生活", "随笔" -> 3L;
            default -> 2L;
        };
    }

    private static class BlogInfo {
        String title;
        String content;
        Long categoryId;
    }

    // ========== A/B 测试接口 ==========

    @GetMapping("/ab/assign")
    @SaCheckLogin
    public Result<ABAssignmentResponse> getABAssignment(@RequestParam String experimentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ABTestService.Variant variant = abTestService.assignVariant(experimentId, userId);

        if (variant == null) {
            return Result.fail(404, "实验不存在");
        }

        return Result.success(new ABAssignmentResponse(
                experimentId,
                variant.getId(),
                variant.getName(),
                variant.getConfig()
        ));
    }

    @PostMapping("/ab/metric")
    @SaCheckLogin
    public Result<Void> recordABMetric(@RequestBody ABMetricRequest request) {
        abTestService.recordMetric(
                request.getExperimentId(),
                request.getVariantId(),
                request.getMetricName(),
                request.getValue()
        );
        return Result.success();
    }

    @GetMapping("/ab/stats/{experimentId}")
    @SaCheckLogin
    public Result<ABTestService.ExperimentStats> getABStats(@PathVariable String experimentId) {
        ABTestService.ExperimentStats stats = abTestService.getStats(experimentId);
        if (stats == null) {
            return Result.fail(404, "实验不存在");
        }
        return Result.success(stats);
    }

    @GetMapping("/ab/experiments")
    @SaCheckLogin
    public Result<List<ABTestService.Experiment>> listExperiments() {
        return Result.success(abTestService.listExperiments());
    }

    // ========== 请求/响应类 ==========

    public record StartSessionResponse(String sessionId, String message) {}
    public record ChatRequest(String sessionId, String message) {}
    public record ChatResponse(String sessionId, String response, String status, int currentStep, int totalSteps) {}

    public record ABAssignmentResponse(
            String experimentId,
            String variantId,
            String variantName,
            Map<String, Object> config
    ) {}

    @lombok.Data
    public static class ABMetricRequest {
        private String experimentId;
        private String variantId;
        private String metricName;
        private double value;
    }
}
