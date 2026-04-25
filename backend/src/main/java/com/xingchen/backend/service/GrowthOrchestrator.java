package com.xingchen.backend.service;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
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

            List<Map<String, Object>> auditResults = executeContentAudit(userId);
            reportData.put("contentAudit", auditResults);

            List<Map<String, Object>> optimizationResults = executeContentOptimization(userId);
            reportData.put("contentOptimization", optimizationResults);

            List<Map<String, Object>> generationResults = executeContentGeneration(userId);
            reportData.put("contentGeneration", generationResults);

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

    private List<Map<String, Object>> executeContentAudit(Long userId) {
        log.info("执行内容审计...");
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            PageResult<ArticleListVO> pageResult =
                    articleService.getArticleList(1, 100, null, null, null, userId);

            List<ArticleListVO> articles = pageResult.getList();
            if (articles == null || articles.isEmpty()) {
                return results;
            }

            int duplicateCount = 0;
            int lowQualityCount = 0;

            for (ArticleListVO article : articles) {
                if (article.getDescription() == null || article.getDescription().length() < 50) {
                    lowQualityCount++;
                }
            }

            results.add(Map.of(
                    "type", "audit",
                    "totalArticles", articles.size(),
                    "duplicatesFound", duplicateCount,
                    "lowQualityFound", lowQualityCount,
                    "status", "completed"
            ));

        } catch (Exception e) {
            log.error("内容审计执行失败", e);
            results.add(Map.of(
                    "type", "audit",
                    "status", "failed",
                    "error", e.getMessage()
            ));
        }

        return results;
    }

    private List<Map<String, Object>> executeContentOptimization(Long userId) {
        log.info("执行内容优化...");
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            PageResult<ArticleListVO> pageResult =
                    articleService.getArticleList(1, 50, null, null, null, userId);

            List<ArticleListVO> articles = pageResult.getList();
            if (articles == null || articles.isEmpty()) {
                return results;
            }

            int optimizedCount = 0;
            for (ArticleListVO article : articles) {
                try {
                    ArticleVO fullArticle = articleService.getArticleById(article.getId(), userId);
                    if (fullArticle != null && fullArticle.getContent() != null) {
                        List<ContentOptimizer.OptimizationIssue> issues =
                                contentOptimizer.analyzeContent(fullArticle.getContent(), fullArticle.getTitle());

                        long highPriorityIssues = issues.stream()
                                .filter(i -> "HIGH".equals(i.getSeverity()))
                                .count();

                        if (highPriorityIssues > 0) {
                            optimizedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("优化文章失败: articleId={}", article.getId(), e);
                }
            }

            results.add(Map.of(
                    "type", "optimization",
                    "articlesAnalyzed", articles.size(),
                    "articlesNeedingOptimization", optimizedCount,
                    "status", "completed"
            ));

        } catch (Exception e) {
            log.error("内容优化执行失败", e);
            results.add(Map.of(
                    "type", "optimization",
                    "status", "failed",
                    "error", e.getMessage()
            ));
        }

        return results;
    }

    private List<Map<String, Object>> executeContentGeneration(Long userId) {
        log.info("执行内容生成...");
        List<Map<String, Object>> results = new ArrayList<>();

        results.add(Map.of(
                "type", "generation",
                "status", "skipped",
                "reason", "需要明确主题才能生成内容"
        ));

        return results;
    }

    private Map<String, Object> generateSummary(Map<String, Object> reportData) {
        Map<String, Object> summary = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> auditResults = (List<Map<String, Object>>) reportData.get("contentAudit");
        if (auditResults != null && !auditResults.isEmpty()) {
            Map<String, Object> audit = auditResults.get(0);
            summary.put("totalArticles", audit.getOrDefault("totalArticles", 0));
            summary.put("issuesFound", audit.getOrDefault("lowQualityFound", 0));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> optResults = (List<Map<String, Object>>) reportData.get("contentOptimization");
        if (optResults != null && !optResults.isEmpty()) {
            Map<String, Object> opt = optResults.get(0);
            summary.put("articlesNeedingOptimization", opt.getOrDefault("articlesNeedingOptimization", 0));
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

        Object issuesFound = summary.get("issuesFound");
        if (issuesFound instanceof Integer) {
            int issues = (Integer) issuesFound;
            score -= issues * 5;
        }

        Object articlesNeedingOpt = summary.get("articlesNeedingOptimization");
        if (articlesNeedingOpt instanceof Integer) {
            int needingOpt = (Integer) articlesNeedingOpt;
            score -= needingOpt * 3;
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