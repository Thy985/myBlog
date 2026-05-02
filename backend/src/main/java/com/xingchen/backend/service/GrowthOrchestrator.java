package com.xingchen.backend.service;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.content.ContentAuditAgent;
import com.xingchen.backend.agent.content.ContentGenerationAgent;
import com.xingchen.backend.agent.content.OpportunityDiscoveryAgent;
import com.xingchen.backend.agent.content.SEOOptimizationAgent;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthOrchestrator {

    private final ContentAuditAgent contentAuditAgent;
    private final OpportunityDiscoveryAgent opportunityDiscoveryAgent;
    private final ContentGenerationAgent contentGenerationAgent;
    private final SEOOptimizationAgent seoOptimizationAgent;
    private final ContentOptimizer contentOptimizer;
    private final SEOService seoService;
    private final ArticleService articleService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String GROWTH_LOCK_KEY = "growth:orchestrator:lock";
    private static final String GROWTH_REPORT_KEY = "growth:report:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class GrowthTaskResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;
        private final LocalDateTime timestamp;

        public GrowthTaskResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class GrowthReport {
        private String reportId;
        private LocalDateTime generatedAt;
        private Map<String, Object> summary;
        private List<Map<String, Object>> tasks;
        private List<Map<String, Object>> recommendations;

        public GrowthReport() {
            this.generatedAt = LocalDateTime.now();
            this.tasks = new ArrayList<>();
            this.recommendations = new ArrayList<>();
        }

        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public Map<String, Object> getSummary() { return summary; }
        public void setSummary(Map<String, Object> summary) { this.summary = summary; }
        public List<Map<String, Object>> getTasks() { return tasks; }
        public void setTasks(List<Map<String, Object>> tasks) { this.tasks = tasks; }
        public List<Map<String, Object>> getRecommendations() { return recommendations; }
        public void setRecommendations(List<Map<String, Object>> recommendations) { this.recommendations = recommendations; }
    }

    public GrowthTaskResult executeDailyGrowthTask(Long userId) {
        log.info("开始执行每日增长任务: userId={}", userId);

        String lockKey = GROWTH_LOCK_KEY + ":" + userId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("增长任务正在执行中，跳过: userId={}", userId);
            return new GrowthTaskResult(false, "增长任务正在执行中，请稍后再试", Map.of());
        }

        try {
            Map<String, Object> reportData = new HashMap<>();

            BaseAgent.AgentResult auditResult = executeContentAuditAgent(userId);
            reportData.put("contentAudit", auditResult.isSuccess() ? auditResult.getData() : null);

            BaseAgent.AgentResult seoResult = executeSEOOptimizationAgent(userId);
            reportData.put("seoOptimization", seoResult.isSuccess() ? seoResult.getData() : null);

            BaseAgent.AgentResult opportunityResult = executeOpportunityDiscoveryAgent(userId);
            reportData.put("opportunityDiscovery", opportunityResult.isSuccess() ? opportunityResult.getData() : null);

            Map<String, Object> summary = generateSummary(reportData);
            reportData.put("summary", summary);

            String reportId = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(GROWTH_REPORT_KEY + reportId, reportData, 7, TimeUnit.DAYS);

            log.info("每日增长任务完成: userId={}, reportId={}", userId, reportId);

            return new GrowthTaskResult(true, "每日增长任务执行成功", Map.of(
                    "reportId", reportId,
                    "summary", summary
            ));

        } catch (Exception e) {
            log.error("每日增长任务执行失败: userId={}", userId, e);
            return new GrowthTaskResult(false, "增长任务执行失败: " + e.getMessage(), Map.of());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private BaseAgent.AgentResult executeContentAuditAgent(Long userId) {
        log.info("执行内容审核 Agent...");

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .input("content audit task")
                    .params(Map.of("userId", userId, "limit", 100))
                    .build();

            return contentAuditAgent.execute(context);
        } catch (Exception e) {
            log.error("内容审核 Agent 执行失败", e);
            return BaseAgent.AgentResult.failure("CONTENT_AUDIT_FAILED", e.getMessage());
        }
    }

    private BaseAgent.AgentResult executeSEOOptimizationAgent(Long userId) {
        log.info("执行 SEO 优化 Agent...");

        try {
            PageResult<ArticleListVO> pageResult =
                    articleService.getArticleList(1, 10, null, null, null, userId);
            List<ArticleListVO> articles = pageResult.getList();

            if (articles == null || articles.isEmpty()) {
                return BaseAgent.AgentResult.success(Map.of("suggestions", List.of(), "message", "无文章需要优化"));
            }

            ArticleListVO articleToOptimize = articles.get(0);

            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .input("SEO optimization task")
                    .params(Map.of(
                            "articleId", articleToOptimize.getId(),
                            "userId", userId,
                            "type", "FULL"
                    ))
                    .build();

            return seoOptimizationAgent.execute(context);
        } catch (Exception e) {
            log.error("SEO 优化 Agent 执行失败", e);
            return BaseAgent.AgentResult.failure("SEO_OPT_FAILED", e.getMessage());
        }
    }

    private BaseAgent.AgentResult executeOpportunityDiscoveryAgent(Long userId) {
        log.info("执行机会发现 Agent...");

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .input("opportunity discovery task")
                    .params(Map.of(
                            "userId", userId,
                            "niche", "",
                            "topN", 5
                    ))
                    .build();

            return opportunityDiscoveryAgent.execute(context);
        } catch (Exception e) {
            log.error("机会发现 Agent 执行失败", e);
            return BaseAgent.AgentResult.failure("OPPORTUNITY_DISCOVERY_FAILED", e.getMessage());
        }
    }

    public GrowthTaskResult generateContent(Long userId, String topic, String tags, String category) {
        log.info("生成内容: userId={}, topic={}", userId, topic);

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .input(topic)
                    .params(new HashMap<>() {{
                        put("topic", topic);
                        put("userId", userId);
                        put("tags", tags != null ? tags : "");
                        put("category", category != null ? category : "");
                    }})
                    .build();

            BaseAgent.AgentResult result = contentGenerationAgent.execute(context);

            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();

                ArticleCreateDTO createDTO = new ArticleCreateDTO();
                createDTO.setTitle((String) data.get("title"));
                createDTO.setContent((String) data.get("content"));
                createDTO.setSummary((String) data.get("description"));

                ArticleVO createdArticle = articleService.createArticle(userId, createDTO);

                return new GrowthTaskResult(true, "内容生成成功",
                        Map.of("articleId", createdArticle.getId(), "title", createdArticle.getTitle()));
            } else {
                return new GrowthTaskResult(false, result.getError(), Map.of());
            }
        } catch (Exception e) {
            log.error("内容生成失败: userId={}", userId, e);
            return new GrowthTaskResult(false, "内容生成失败: " + e.getMessage(), Map.of());
        }
    }

    public GrowthTaskResult optimizeArticle(Long userId, Long articleId) {
        log.info("优化文章: userId={}, articleId={}", userId, articleId);

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .input("SEO optimization task")
                    .params(Map.of(
                            "articleId", articleId,
                            "userId", userId,
                            "type", "FULL"
                    ))
                    .build();

            BaseAgent.AgentResult result = seoOptimizationAgent.execute(context);

            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultData = (Map<String, Object>) result.getData();
                return new GrowthTaskResult(true, "文章优化建议已生成", resultData);
            } else {
                return new GrowthTaskResult(false, result.getError(), Map.of());
            }
        } catch (Exception e) {
            log.error("文章优化失败: userId={}, articleId={}", userId, articleId, e);
            return new GrowthTaskResult(false, "文章优化失败: " + e.getMessage(), Map.of());
        }
    }

    private Map<String, Object> generateSummary(Map<String, Object> reportData) {
        Map<String, Object> summary = new HashMap<>();

        Object auditData = reportData.get("contentAudit");
        if (auditData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> audit = (Map<String, Object>) auditData;
            summary.put("totalArticles", audit.getOrDefault("totalArticles", 0));
            summary.put("passCount", audit.getOrDefault("passCount", 0));
            summary.put("warningCount", audit.getOrDefault("warningCount", 0));
            summary.put("failCount", audit.getOrDefault("failCount", 0));
            summary.put("healthScore", audit.getOrDefault("healthScore", 0));
        }

        Object opportunityData = reportData.get("opportunityDiscovery");
        if (opportunityData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> opportunity = (Map<String, Object>) opportunityData;
            summary.put("opportunitiesFound", opportunity.getOrDefault("totalOpportunities", 0));
        }

        Object seoData = reportData.get("seoOptimization");
        if (seoData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> seo = (Map<String, Object>) seoData;
            summary.put("seoSuggestions", seo.getOrDefault("suggestions", List.of()));
        }

        summary.put("growthScore", calculateGrowthScore(summary));
        summary.put("generatedAt", LocalDateTime.now().format(DATE_FORMATTER));

        return summary;
    }

    private int calculateGrowthScore(Map<String, Object> summary) {
        int score = 100;

        Object totalArticles = summary.get("totalArticles");
        if (totalArticles instanceof Integer) {
            int articles = (Integer) totalArticles;
            if (articles < 5) score -= 30;
            else if (articles < 10) score -= 15;
        }

        Object failCount = summary.get("failCount");
        if (failCount instanceof Integer) {
            score -= ((Integer) failCount) * 10;
        }

        Object warningCount = summary.get("warningCount");
        if (warningCount instanceof Integer) {
            score -= ((Integer) warningCount) * 3;
        }

        return Math.max(0, Math.min(100, score));
    }

    public GrowthReport generateGrowthReport(Long userId, String period) {
        GrowthReport report = new GrowthReport();
        report.setReportId(UUID.randomUUID().toString());

        try {
            PageResult<ArticleListVO> pageResult =
                    articleService.getArticleList(1, 100, null, null, null, userId);

            List<ArticleListVO> articles = pageResult.getList();
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalArticles", articles != null ? articles.size() : 0);
            summary.put("period", period);
            summary.put("generatedAt", LocalDateTime.now().format(DATE_FORMATTER));

            if (articles != null) {
                int totalViews = articles.stream()
                        .mapToInt(a -> a.getViewCount() != null ? a.getViewCount() : 0)
                        .sum();
                int avgViews = articles.isEmpty() ? 0 : totalViews / articles.size();

                summary.put("totalViews", totalViews);
                summary.put("averageViews", avgViews);

                long articlesWithContent = articles.stream()
                        .filter(a -> a.getDescription() != null && a.getDescription().length() > 100)
                        .count();
                summary.put("articlesWithContent", articlesWithContent);

                summary.put("contentQuality", calculateContentQuality(articles));
            }

            report.setSummary(summary);

            List<Map<String, Object>> tasks = new ArrayList<>();
            tasks.add(Map.of(
                    "name", "Content Audit",
                    "status", "available",
                    "description", "审计内容质量，发现重复和低质量文章"
            ));
            tasks.add(Map.of(
                    "name", "Content Optimization",
                    "status", "available",
                    "description", "优化低质量文章内容"
            ));
            tasks.add(Map.of(
                    "name", "Opportunity Discovery",
                    "status", "available",
                    "description", "发现内容缺口，找到值得撰写的主题"
            ));
            report.setTasks(tasks);

            List<Map<String, Object>> recommendations = generateRecommendations(articles);
            report.setRecommendations(recommendations);

        } catch (Exception e) {
            log.error("生成增长报告失败: userId={}", userId, e);
        }

        return report;
    }

    private String calculateContentQuality(List<ArticleListVO> articles) {
        if (articles == null || articles.isEmpty()) {
            return "unknown";
        }

        long goodCount = articles.stream()
                .filter(a -> a.getDescription() != null && a.getDescription().length() >= 100)
                .count();

        double goodRatio = (double) goodCount / articles.size();

        if (goodRatio >= 0.8) return "excellent";
        if (goodRatio >= 0.6) return "good";
        if (goodRatio >= 0.4) return "medium";
        return "poor";
    }

    private List<Map<String, Object>> generateRecommendations(List<ArticleListVO> articles) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        if (articles == null || articles.isEmpty()) {
            recommendations.add(Map.of(
                    "type", "action",
                    "priority", "high",
                    "recommendation", "开始撰写第一篇文章",
                    "action", "generate_content"
            ));
            return recommendations;
        }

        long shortContent = articles.stream()
                .filter(a -> a.getDescription() == null || a.getDescription().length() < 50)
                .count();

        if (shortContent > articles.size() * 0.3) {
            recommendations.add(Map.of(
                    "type", "optimization",
                    "priority", "high",
                    "recommendation", String.format("发现 %d 篇内容过短的文章，建议优化", shortContent),
                    "action", "optimize_short_content"
            ));
        }

        recommendations.add(Map.of(
                "type", "discovery",
                "priority", "medium",
                "recommendation", "定期发现内容缺口，保持内容更新",
                "action", "discover_opportunities"
        ));

        recommendations.add(Map.of(
                "type", "generation",
                "priority", "medium",
                "recommendation", "保持每周1-2篇新文章的发布频率",
                "action", "maintain_frequency"
        ));

        return recommendations;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledDailyGrowth() {
        log.info("开始执行定时每日增长任务...");

        try {
            Set<Object> userIds = redisTemplate.opsForSet().members("active:users");
            if (userIds != null) {
                for (Object userIdObj : userIds) {
                    if (userIdObj instanceof Long || userIdObj instanceof String) {
                        Long userId = userIdObj instanceof Long ? (Long) userIdObj : Long.parseLong((String) userIdObj);
                        try {
                            executeDailyGrowthTask(userId);
                        } catch (Exception e) {
                            log.error("用户 {} 定时任务执行失败", userId, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("定时每日增长任务执行失败", e);
        }
    }
}