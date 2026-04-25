package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.dto.ArticleUpdateDTO;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.ContentOptimizer;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleOptimizerTool implements Tool {

    private final ArticleService articleService;
    private final ContentOptimizer contentOptimizer;

    @Override
    public String getName() {
        return "article_optimizer";
    }

    @Override
    public String getDescription() {
        return "文章内容优化工具：分析文章质量问题，生成优化建议，支持自动优化";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
                new ToolParameter(
                        "operation",
                        "操作类型：analyze（分析问题）/ optimize（优化）/ batch（批量优化）",
                        "string",
                        true,
                        null
                ),
                new ToolParameter(
                        "articleId",
                        "文章ID（analyze和optimize时必填）",
                        "integer",
                        false,
                        null
                ),
                new ToolParameter(
                        "articleIds",
                        "文章ID列表（batch时必填）",
                        "array",
                        false,
                        null
                ),
                new ToolParameter(
                        "applyOptimizations",
                        "是否应用优化（仅optimize时有效）",
                        "boolean",
                        false,
                        false
                )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String operation = (String) parameters.get("operation");
        if (operation == null) {
            return ToolResult.error("操作类型不能为空，请指定 analyze / optimize / batch");
        }

        if (!StpUtil.isLogin()) {
            return ToolResult.error("用户未登录，无法优化文章");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            return switch (operation.toLowerCase()) {
                case "analyze" -> analyzeArticle(parameters, userId);
                case "optimize" -> optimizeArticle(parameters, userId);
                case "batch" -> batchOptimizeArticles(parameters, userId);
                default -> ToolResult.error("不支持的操作：" + operation + "，请使用 analyze / optimize / batch");
            };
        } catch (Exception e) {
            log.error("文章优化失败: operation={}", operation, e);
            return ToolResult.error("文章优化失败: " + e.getMessage());
        }
    }

    private ToolResult analyzeArticle(Map<String, Object> parameters, Long userId) {
        Object articleIdObj = parameters.get("articleId");
        if (articleIdObj == null) {
            return ToolResult.error("文章ID不能为空");
        }
        Long articleId = ((Number) articleIdObj).longValue();

        ArticleVO article = articleService.getArticleById(articleId, userId);
        if (article == null) {
            return ToolResult.error("文章不存在或无权访问");
        }

        String content = article.getContent();
        if (content == null) {
            content = "";
        }

        List<ContentOptimizer.OptimizationIssue> issues =
                contentOptimizer.analyzeContent(content, article.getTitle());

        String suggestions = contentOptimizer.generateOptimizationSuggestion(
                content, article.getTitle(), issues);

        Map<String, Object> seoScore = analyzeBasicSEO(content, article.getTitle());

        Map<String, Object> result = new HashMap<>();
        result.put("articleId", articleId);
        result.put("title", article.getTitle());
        result.put("contentLength", content.length());
        result.put("issueCount", issues.size());
        result.put("issues", issues.stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("type", i.getType());
                    m.put("severity", i.getSeverity());
                    m.put("description", i.getDescription());
                    m.put("suggestion", i.getSuggestion());
                    return m;
                })
                .collect(Collectors.toList()));
        result.put("suggestions", suggestions);
        result.put("seoScore", seoScore);

        long highIssues = issues.stream()
                .filter(i -> "HIGH".equals(i.getSeverity()))
                .count();
        long mediumIssues = issues.stream()
                .filter(i -> "MEDIUM".equals(i.getSeverity()))
                .count();
        result.put("summary", String.format("发现 %d 个问题（高优先级: %d, 中优先级: %d）",
                issues.size(), highIssues, mediumIssues));

        return ToolResult.success(result, "文章分析完成");
    }

    private ToolResult optimizeArticle(Map<String, Object> parameters, Long userId) {
        Object articleIdObj = parameters.get("articleId");
        if (articleIdObj == null) {
            return ToolResult.error("文章ID不能为空");
        }
        Long articleId = ((Number) articleIdObj).longValue();

        Boolean applyOptimizations = (Boolean) parameters.getOrDefault("applyOptimizations", false);

        ArticleVO article = articleService.getArticleById(articleId, userId);
        if (article == null) {
            return ToolResult.error("文章不存在或无权访问");
        }

        String content = article.getContent();
        if (content == null) {
            content = "";
        }

        List<ContentOptimizer.OptimizationIssue> issues =
                contentOptimizer.analyzeContent(content, article.getTitle());

        String suggestions = contentOptimizer.generateOptimizationSuggestion(content, article.getTitle(), issues);

        String optimizedContent = content;
        String optimizedTitle = article.getTitle();
        List<String> improvements = new ArrayList<>();

        for (ContentOptimizer.OptimizationIssue issue : issues) {
            switch (issue.getType()) {
                case "TITLE_TOO_LONG":
                    if (article.getTitle() != null && article.getTitle().length() > 60) {
                        optimizedTitle = article.getTitle().substring(0, 57) + "...";
                        improvements.add("标题已精简");
                    }
                    break;
                case "NO_HEADINGS":
                    optimizedContent = addSampleHeadings(optimizedContent);
                    improvements.add("已添加标题结构");
                    break;
                case "NO_IMAGES":
                    improvements.add("建议添加相关图片");
                    break;
                default:
                    break;
            }
        }

        Map<String, Object> qualityScore = calculateQualityScore(issues, content.length());

        Map<String, Object> result = new HashMap<>();
        result.put("articleId", articleId);
        result.put("originalTitle", article.getTitle());
        result.put("optimizedTitle", optimizedTitle);
        result.put("qualityScore", qualityScore);
        result.put("issueCount", issues.size());
        result.put("improvements", improvements);
        result.put("issues", issues.stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("type", i.getType());
                    m.put("severity", i.getSeverity());
                    m.put("description", i.getDescription());
                    m.put("suggestion", i.getSuggestion());
                    return m;
                })
                .collect(Collectors.toList()));

        if (applyOptimizations != null && applyOptimizations) {
            ArticleUpdateDTO updateDTO = new ArticleUpdateDTO();
            updateDTO.setTitle(optimizedTitle);
            updateDTO.setContent(optimizedContent);
            articleService.updateArticle(userId, articleId, updateDTO);
            result.put("applied", true);
            result.put("message", "优化已应用到文章");
        } else {
            result.put("applied", false);
            result.put("optimizedContent", optimizedContent);
            result.put("message", "优化建议已生成，可选择是否应用到文章");
        }

        return ToolResult.success(result, "文章优化完成");
    }

    private ToolResult batchOptimizeArticles(Map<String, Object> parameters, Long userId) {
        @SuppressWarnings("unchecked")
        List<Long> articleIds = (List<Long>) parameters.get("articleIds");
        if (articleIds == null || articleIds.isEmpty()) {
            return ToolResult.error("文章ID列表不能为空");
        }

        int maxBatch = 20;
        if (articleIds.size() > maxBatch) {
            return ToolResult.error("批量处理最多支持 " + maxBatch + " 篇文章");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long articleId : articleIds) {
            try {
                ArticleVO article = articleService.getArticleById(articleId, userId);
                if (article == null) {
                    failCount++;
                    continue;
                }

                String content = article.getContent();
                if (content == null) {
                    content = "";
                }

                List<ContentOptimizer.OptimizationIssue> issues =
                        contentOptimizer.analyzeContent(content, article.getTitle());
                Map<String, Object> qualityScore = calculateQualityScore(issues, content.length());

                Map<String, Object> item = new HashMap<>();
                item.put("articleId", articleId);
                item.put("title", article.getTitle());
                item.put("qualityScore", qualityScore.get("totalScore"));
                item.put("issueCount", issues.size());
                item.put("highPriorityIssues", issues.stream()
                        .filter(i -> "HIGH".equals(i.getSeverity()))
                        .count());

                results.add(item);
                successCount++;

            } catch (Exception e) {
                log.warn("批量优化文章失败: articleId={}", articleId, e);
                failCount++;
            }
        }

        results.sort((a, b) -> {
            Integer scoreA = (Integer) a.get("qualityScore");
            Integer scoreB = (Integer) b.get("qualityScore");
            return scoreA.compareTo(scoreB);
        });

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalProcessed", successCount + failCount);
        summary.put("successCount", successCount);
        summary.put("failCount", failCount);
        summary.put("needsOptimization", results.stream()
                .filter(r -> (Integer) r.get("qualityScore") < 60)
                .count());
        summary.put("goodQuality", results.stream()
                .filter(r -> (Integer) r.get("qualityScore") >= 80)
                .count());

        Map<String, Object> result = new HashMap<>();
        result.put("results", results);
        result.put("summary", summary);
        result.put("message", String.format("批量分析完成：处理 %d 篇，成功 %d，失败 %d",
                successCount + failCount, successCount, failCount));

        return ToolResult.success(result, "批量优化完成");
    }

    private Map<String, Object> analyzeBasicSEO(String content, String title) {
        Map<String, Object> score = new HashMap<>();

        if (content == null) {
            content = "";
        }

        int contentLength = content.length();
        score.put("contentLength", contentLength);
        score.put("hasEnoughContent", contentLength >= 500);

        boolean hasHeadings = content.matches("(?s).*#\\s+.*");
        score.put("hasHeadings", hasHeadings);

        int headingCount = 0;
        Matcher matcher = Pattern.compile("^#+\\s+.+$", Pattern.MULTILINE).matcher(content);
        while (matcher.find()) headingCount++;
        score.put("headingCount", headingCount);

        boolean hasLists = content.matches("(?s).*[-*]\\s+.*");
        score.put("hasLists", hasLists);

        int scoreValue = 0;
        if (contentLength >= 500) scoreValue += 20;
        if (contentLength >= 1000) scoreValue += 20;
        if (hasHeadings) scoreValue += 20;
        if (headingCount >= 3) scoreValue += 15;
        if (hasLists) scoreValue += 15;
        if (contentLength >= 500) scoreValue += 10;
        score.put("totalScore", scoreValue);
        score.put("grade", scoreValue >= 80 ? "A" : scoreValue >= 60 ? "B" : scoreValue >= 40 ? "C" : "D");

        return score;
    }

    private Map<String, Object> calculateQualityScore(List<ContentOptimizer.OptimizationIssue> issues, int contentLength) {
        Map<String, Object> score = new HashMap<>();

        int baseScore = 100;

        for (ContentOptimizer.OptimizationIssue issue : issues) {
            switch (issue.getSeverity()) {
                case "HIGH":
                    baseScore -= 20;
                    break;
                case "MEDIUM":
                    baseScore -= 10;
                    break;
                case "LOW":
                    baseScore -= 5;
                    break;
            }
        }

        baseScore = Math.max(0, baseScore);

        score.put("totalScore", baseScore);
        score.put("grade", baseScore >= 80 ? "A" : baseScore >= 60 ? "B" : baseScore >= 40 ? "C" : "D");
        score.put("issueCount", issues.size());
        score.put("highPriorityIssues", issues.stream()
                .filter(i -> "HIGH".equals(i.getSeverity()))
                .count());
        score.put("mediumPriorityIssues", issues.stream()
                .filter(i -> "MEDIUM".equals(i.getSeverity()))
                .count());
        score.put("lowPriorityIssues", issues.stream()
                .filter(i -> "LOW".equals(i.getSeverity()))
                .count());
        score.put("contentLength", contentLength);
        score.put("contentQuality", contentLength >= 1000 ? "good" : contentLength >= 500 ? "medium" : "low");

        return score;
    }

    private String addSampleHeadings(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String[] paragraphs = content.split("\n\n");
        if (paragraphs.length < 2) {
            return "## 概述\n\n" + content;
        }

        StringBuilder result = new StringBuilder();
        result.append("# ").append(paragraphs[0].trim()).append("\n\n");

        for (int i = 1; i < paragraphs.length && i <= 5; i++) {
            result.append("## 第").append(i).append("部分\n\n");
            result.append(paragraphs[i].trim()).append("\n\n");
        }

        if (paragraphs.length > 6) {
            result.append("## 总结\n\n");
            for (int i = 6; i < paragraphs.length; i++) {
                result.append(paragraphs[i].trim()).append("\n\n");
            }
        }

        return result.toString();
    }

    @Override
    public long getTimeout() {
        return 60000;
    }
}