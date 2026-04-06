package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.ab.ABTestService;
import com.xingchen.backend.agent.AgentExecutor;
import com.xingchen.backend.agent.AgentSessionManager;
import com.xingchen.backend.agent.AgentState;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 控制器
 *
 * 支持多轮对话和复杂任务执行
 */
@RestController
@RequestMapping("/api/agent")
@Slf4j
@RequiredArgsConstructor
public class AgentController {

    private final AgentSessionManager sessionManager;
    private final AgentExecutor agentExecutor;
    private final ABTestService abTestService;
    private final AIService aiService;
    private final ArticleService articleService;

    // 博客发布相关模式
    private static final Pattern BLOG_TITLE_PATTERN = Pattern.compile("标题[是为：:]\s*['\"]?([^'\"，,。\\n]+)['\"]?");
    private static final Pattern BLOG_CONTENT_PATTERN = Pattern.compile("内容[是为：:]\s*(.+?)(?=分类|category|$)", Pattern.DOTALL);
    private static final Pattern BLOG_CATEGORY_PATTERN = Pattern.compile("分类[是为：:]\s*['\"]?([^'\"，,。\\n]+)['\"]?");

    /**
     * 开始新会话
     */
    @PostMapping("/session/start")
    @SaCheckLogin
    public Result<StartSessionResponse> startSession() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 检查活跃会话数
        int activeCount = sessionManager.getActiveSessionCount(userId);
        if (activeCount >= 5) {
            return Result.fail(429, "活跃会话过多，请先结束其他会话");
        }
        
        AgentState state = sessionManager.createSession(userId);
        
        return Result.success(new StartSessionResponse(
                state.getSessionId(),
                "会话创建成功，请开始对话"
        ));
    }

    /**
     * Agent 对话
     */
    @PostMapping("/chat")
    @SaCheckLogin
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查是否是博客发布请求
        if (isBlogPublishRequest(request.getMessage())) {
            return handleBlogPublishRequest(userId, request.getMessage(), request.getSessionId());
        }

        // 获取或创建会话
        AgentState state = sessionManager.getOrCreateSession(
                request.getSessionId(),
                userId
        );

        // 检查是否需要规划
        if (state.getStatus() == AgentState.TaskStatus.IDLE) {
            // 首次对话，判断是否需要 Agent 模式
            if (isComplexTask(request.getMessage())) {
                // 启动 Agent 工作流
                AgentExecutor.AgentResult result = agentExecutor.execute(
                        request.getMessage(),
                        state
                );

                sessionManager.updateSession(state);

                if (result.success()) {
                    return Result.success(new ChatResponse(
                            state.getSessionId(),
                            result.result(),
                            state.getStatus().name(),
                            state.getCurrentStep(),
                            state.getTotalSteps()
                    ));
                } else {
                    return Result.fail(500, result.error());
                }
            }
        }

        // 普通对话模式 - 使用 AI 服务进行对话
        String response = aiService.chatWithUserApiKeyAndPrompt(
                userId,
                request.getMessage(),
                "你是一个有帮助的AI助手，请简洁明了地回答用户问题。"
        );

        state.addTurn(request.getMessage(), response);
        sessionManager.updateSession(state);

        return Result.success(new ChatResponse(
                state.getSessionId(),
                response,
                state.getStatus().name(),
                state.getCurrentStep(),
                state.getTotalSteps()
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
    private Result<ChatResponse> handleBlogPublishRequest(Long userId, String message, String sessionId) {
        log.info("检测到博客发布请求: userId={}, message={}", userId, message);

        try {
            // 解析博客信息
            BlogInfo blogInfo = parseBlogInfo(message);

            if (blogInfo.title == null || blogInfo.title.isEmpty()) {
                return Result.fail(400, "无法解析博客标题，请提供标题");
            }

            // 使用 AI 生成博客内容
            String generatedContent = generateBlogContent(userId, blogInfo.title, blogInfo.content);

            // 创建文章
            ArticleCreateDTO dto = new ArticleCreateDTO();
            dto.setTitle(blogInfo.title);
            dto.setContent(generatedContent);
            dto.setCategoryId(blogInfo.categoryId != null ? blogInfo.categoryId : 2); // 默认 AI 探索分类

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

            return Result.success(new ChatResponse(
                    sessionId,
                    response,
                    "IDLE",
                    0,
                    0
            ));
        } catch (Exception e) {
            log.error("博客发布失败: {}", e.getMessage(), e);
            return Result.fail(500, "博客发布失败: " + e.getMessage());
        }
    }

    /**
     * 使用 AI 生成博客内容
     */
    private String generateBlogContent(Long userId, String title, String hint) {
        String systemPrompt = "你是一个专业的博客文章写手。请根据用户给定的主题撰写一篇结构完整、内容丰富的博客文章。\n" +
                "要求：\n" +
                "1. 文章要有引言、正文、结论等完整结构\n" +
                "2. 内容要有深度，避免泛泛而谈\n" +
                "3. 语言生动有趣，适合博客读者\n" +
                "4. 字数控制在500-1000字左右\n" +
                "5. 直接输出文章内容，不要包含任何解释性文字";

        String userMessage = hint != null && !hint.isEmpty() && !hint.equals("这是一篇由智能体辅助生成的博客文章。")
                ? "请撰写一篇关于「" + title + "」的博客文章。" + hint
                : "请撰写一篇关于「" + title + "」的博客文章。";

        try {
            String content = aiService.chatWithUserApiKeyAndPrompt(userId, userMessage, systemPrompt);
            if (content != null && !content.trim().isEmpty()) {
                log.info("AI 生成博客内容成功，字数: {}", content.length());
                return content.trim();
            }
        } catch (Exception e) {
            log.warn("AI 生成博客内容失败，使用默认内容: {}", e.getMessage());
        }

        // 如果 AI 生成失败，使用默认内容
        return "这是一篇关于" + title + "的博客文章。";
    }

    /**
     * 解析博客信息
     */
    private BlogInfo parseBlogInfo(String message) {
        BlogInfo info = new BlogInfo();
        if (message == null || message.isEmpty()) {
            return info;
        }

        // 尝试多种方式提取标题
        // 1. 标题是...格式
        Pattern titlePattern1 = Pattern.compile("标题[是为：:]\s*['\"]?([^'\"，,。\\n]+)['\"]?");
        Matcher titleMatcher1 = titlePattern1.matcher(message);
        if (titleMatcher1.find()) {
            info.title = titleMatcher1.group(1).trim();
        }

        // 2. 《...》 书名号格式
        if (info.title == null) {
            Pattern bookPattern = Pattern.compile("《([^》]+)》");
            Matcher bookMatcher = bookPattern.matcher(message);
            if (bookMatcher.find()) {
                info.title = bookMatcher.group(1).trim();
            }
        }

        // 3. '标题' 或 "标题" 格式
        if (info.title == null) {
            Pattern titlePattern2 = Pattern.compile("['\"]([^'\"]+)['\"][,\\s]+.*?(?:内容|about)");
            Matcher titleMatcher2 = titlePattern2.matcher(message);
            if (titleMatcher2.find()) {
                info.title = titleMatcher2.group(1).trim();
            }
        }

        // 4. 提取引号或书名号内的内容
        if (info.title == null) {
            Pattern quotePattern = Pattern.compile("['\"\"]([^'\"]+)['\"\"]");
            Matcher quoteMatcher = quotePattern.matcher(message);
            if (quoteMatcher.find()) {
                info.title = quoteMatcher.group(1).trim();
            }
        }

        // 5. 处理"发一个关于XXX的博文/文章"格式 - 提取主题作为标题
        if (info.title == null) {
            Pattern topicPattern = Pattern.compile("关于([^的]+)的[博文文章帖子]");
            Matcher topicMatcher = topicPattern.matcher(message);
            if (topicMatcher.find()) {
                String topic = topicMatcher.group(1).trim();
                info.title = topic;
                info.content = "这是一篇关于" + topic + "的博客文章。";
            }
        }

        // 6. 处理"发一个XXX博文"格式（无关于，直接提取主题）
        if (info.title == null) {
            // 处理"写一篇关于XXX的文章"格式
            Pattern topicPattern1 = Pattern.compile("(?:写|发布|发表)一篇(?:关于)?(.+?)(?:的)?(?:文章|博客|博文|帖子)");
            Matcher topicMatcher1 = topicPattern1.matcher(message);
            if (topicMatcher1.find()) {
                String topic = topicMatcher1.group(1).trim();
                if (topic.length() > 1 && topic.length() < 100) {
                    info.title = topic;
                    info.content = "这是一篇关于" + topic + "的博客文章。";
                }
            }
        }

        // 7. 处理"关于XXX的文章"格式
        if (info.title == null) {
            Pattern aboutPattern = Pattern.compile("关于(.+?)(?:的)?(?:文章|博客|博文|帖子)");
            Matcher aboutMatcher = aboutPattern.matcher(message);
            if (aboutMatcher.find()) {
                String topic = aboutMatcher.group(1).trim();
                if (topic.length() > 1 && topic.length() < 100) {
                    info.title = topic;
                    info.content = "这是一篇关于" + topic + "的博客文章。";
                }
            }
        }

        // 提取内容 - 标题后面的所有内容
        if (info.title != null && info.content == null) {
            int titleIndex = message.indexOf(info.title);
            if (titleIndex >= 0) {
                info.content = message.substring(titleIndex + info.title.length());
                // 清理内容
                info.content = info.content.replaceAll("^(关于|内容|with|about|，|,|:|\\s)+", "");
                info.content = info.content.replaceAll("分类[是为：:][^，,。\\n]+", "");
                info.content = info.content.trim();
                if (info.content.isEmpty()) {
                    info.content = null;
                }
            }
        }

        // 如果内容为空，使用默认内容
        if (info.content == null || info.content.isEmpty()) {
            info.content = "这是一篇由智能体辅助生成的博客文章。";
        }

        // 提取分类
        Pattern categoryPattern = Pattern.compile("分类[是为：:]\\s*['\"]?([^'\"，,。\\n]+)['\"]?");
        Matcher categoryMatcher = categoryPattern.matcher(message);
        if (categoryMatcher.find()) {
            String categoryName = categoryMatcher.group(1).trim();
            info.categoryId = getCategoryIdByName(categoryName);
        }

        return info;
    }

    /**
     * 根据分类名称获取分类 ID
     */
    private Long getCategoryIdByName(String categoryName) {
        // 简单的分类映射
        return switch (categoryName) {
            case "AI探索", "AI", "人工智能" -> 2L;
            case "生活随笔", "生活", "随笔" -> 3L;
            default -> 2L; // 默认 AI 探索
        };
    }

    /**
     * 博客信息内部类
     */
    private static class BlogInfo {
        String title;
        String content;
        Long categoryId;
    }

    /**
     * 获取会话状态
     */
    @GetMapping("/session/{sessionId}")
    @SaCheckLogin
    public Result<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();

        return sessionManager.getSession(sessionId, userId)
                .map(state -> Result.success(new SessionStatusResponse(
                        state.getSessionId(),
                        state.getStatus().name(),
                        state.getCurrentTask(),
                        state.getCurrentStep(),
                        state.getTotalSteps(),
                        state.getHistoryAsText(10)
                )))
                .orElse(Result.fail(404, "会话不存在或无权访问"));
    }

    /**
     * 结束会话
     */
    @PostMapping("/session/{sessionId}/end")
    @SaCheckLogin
    public Result<Void> endSession(@PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();

        boolean ended = sessionManager.endSession(sessionId, userId);
        if (!ended) {
            return Result.fail(404, "会话不存在或无权访问");
        }

        return Result.success();
    }

    /**
     * 列出活跃会话
     */
    @GetMapping("/sessions")
    @SaCheckLogin
    public Result<List<SessionInfo>> listSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 这里简化实现，实际应该查询用户的所有会话
        int count = sessionManager.getActiveSessionCount(userId);
        
        return Result.success(List.of(new SessionInfo(
                "summary",
                count + " 个活跃会话"
        )));
    }

    // ========== A/B 测试接口 ==========

    /**
     * 获取 A/B 分组
     */
    @GetMapping("/ab/assign")
    @SaCheckLogin
    public Result<ABAssignmentResponse> getABAssignment(
            @RequestParam String experimentId) {
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

    /**
     * 记录 A/B 指标
     */
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

    /**
     * 获取实验统计
     */
    @GetMapping("/ab/stats/{experimentId}")
    @SaCheckLogin
    public Result<ABTestService.ExperimentStats> getABStats(
            @PathVariable String experimentId) {
        ABTestService.ExperimentStats stats = abTestService.getStats(experimentId);
        
        if (stats == null) {
            return Result.fail(404, "实验不存在");
        }
        
        return Result.success(stats);
    }

    /**
     * 列出所有实验
     */
    @GetMapping("/ab/experiments")
    @SaCheckLogin
    public Result<List<ABTestService.Experiment>> listExperiments() {
        return Result.success(abTestService.listExperiments());
    }

    // ========== 私有方法 ==========

    private boolean isComplexTask(String message) {
        // 判断是否为复杂任务
        String lower = message.toLowerCase();
        return lower.contains("步骤") || 
               lower.contains("流程") || 
               lower.contains("规划") ||
               lower.contains("计划") ||
               lower.contains("分析") ||
               message.length() > 100;
    }

    // ========== 请求/响应类 ==========

    @lombok.Data
    public static class StartSessionResponse {
        private String sessionId;
        private String message;
        
        public StartSessionResponse(String sessionId, String message) {
            this.sessionId = sessionId;
            this.message = message;
        }
    }

    @lombok.Data
    public static class ChatRequest {
        private String sessionId;
        private String message;
    }

    @lombok.Data
    public static class ChatResponse {
        private String sessionId;
        private String response;
        private String status;
        private int currentStep;
        private int totalSteps;
        
        public ChatResponse(String sessionId, String response, String status,
                           int currentStep, int totalSteps) {
            this.sessionId = sessionId;
            this.response = response;
            this.status = status;
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
        }
    }

    @lombok.Data
    public static class SessionStatusResponse {
        private String sessionId;
        private String status;
        private String currentTask;
        private int currentStep;
        private int totalSteps;
        private String recentHistory;
        
        public SessionStatusResponse(String sessionId, String status, String currentTask,
                                     int currentStep, int totalSteps, String recentHistory) {
            this.sessionId = sessionId;
            this.status = status;
            this.currentTask = currentTask;
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
            this.recentHistory = recentHistory;
        }
    }

    @lombok.Data
    public static class SessionInfo {
        private String sessionId;
        private String info;
        
        public SessionInfo(String sessionId, String info) {
            this.sessionId = sessionId;
            this.info = info;
        }
    }

    @lombok.Data
    public static class ABAssignmentResponse {
        private String experimentId;
        private String variantId;
        private String variantName;
        private Map<String, Object> config;
        
        public ABAssignmentResponse(String experimentId, String variantId, 
                                    String variantName, Map<String, Object> config) {
            this.experimentId = experimentId;
            this.variantId = variantId;
            this.variantName = variantName;
            this.config = config;
        }
    }

    @lombok.Data
    public static class ABMetricRequest {
        private String experimentId;
        private String variantId;
        private String metricName;
        private double value;
    }
}