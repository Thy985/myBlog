package com.xingchen.backend.service;

import com.xingchen.backend.vo.ArticleListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentOptimizer {

    private final SEOService seoService;

    private static final int MIN_CONTENT_LENGTH = 300;
    private static final int OPTIMAL_CONTENT_LENGTH = 1000;
    private static final int MAX_HEADING_LENGTH = 60;
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[.*?\\]\\(.*?\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");

    public static class OptimizationIssue {
        private final String type;
        private final String severity;
        private final String description;
        private final String suggestion;
        private final Map<String, Object> details;

        public OptimizationIssue(String type, String severity, String description,
                               String suggestion, Map<String, Object> details) {
            this.type = type;
            this.severity = severity;
            this.description = description;
            this.suggestion = suggestion;
            this.details = details;
        }

        public String getType() { return type; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getSuggestion() { return suggestion; }
        public Map<String, Object> getDetails() { return details; }
    }

    public static class OptimizationResult {
        private final Long articleId;
        private final String title;
        private final List<OptimizationIssue> issues;
        private final String optimizedContent;
        private final String optimizedTitle;
        private final Map<String, Object> qualityScore;
        private final List<String> improvements;

        public OptimizationResult(Long articleId, String title,
                                List<OptimizationIssue> issues,
                                String optimizedContent, String optimizedTitle,
                                Map<String, Object> qualityScore,
                                List<String> improvements) {
            this.articleId = articleId;
            this.title = title;
            this.issues = issues;
            this.optimizedContent = optimizedContent;
            this.optimizedTitle = optimizedTitle;
            this.qualityScore = qualityScore;
            this.improvements = improvements;
        }

        public Long getArticleId() { return articleId; }
        public String getTitle() { return title; }
        public List<OptimizationIssue> getIssues() { return issues; }
        public String getOptimizedContent() { return optimizedContent; }
        public String getOptimizedTitle() { return optimizedTitle; }
        public Map<String, Object> getQualityScore() { return qualityScore; }
        public List<String> getImprovements() { return improvements; }
    }

    public List<OptimizationIssue> analyzeContent(String content, String title) {
        List<OptimizationIssue> issues = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            issues.add(new OptimizationIssue(
                    "EMPTY_CONTENT",
                    "HIGH",
                    "文章内容为空",
                    "请添加文章内容",
                    Map.of()
            ));
            return issues;
        }

        int contentLength = content.length();
        if (contentLength < MIN_CONTENT_LENGTH) {
            issues.add(new OptimizationIssue(
                    "CONTENT_TOO_SHORT",
                    "HIGH",
                    String.format("文章内容过短（%d字），建议至少%d字", contentLength, MIN_CONTENT_LENGTH),
                    "扩展内容，增加实用信息和详细说明",
                    Map.of("currentLength", contentLength, "recommendedLength", MIN_CONTENT_LENGTH)
            ));
        } else if (contentLength < OPTIMAL_CONTENT_LENGTH) {
            issues.add(new OptimizationIssue(
                    "CONTENT_COULD_BE_LONGER",
                    "MEDIUM",
                    String.format("文章内容（%d字）可以更丰富，建议达到%d字以上", contentLength, OPTIMAL_CONTENT_LENGTH),
                    "适当扩展内容，增加案例和详细说明",
                    Map.of("currentLength", contentLength, "recommendedLength", OPTIMAL_CONTENT_LENGTH)
            ));
        }

        int headingCount = countHeadings(content);
        if (headingCount == 0) {
            issues.add(new OptimizationIssue(
                    "NO_HEADINGS",
                    "HIGH",
                    "文章没有使用标题结构",
                    "添加多级标题（H1/H2/H3）组织内容结构",
                    Map.of("headingCount", 0)
            ));
        } else if (headingCount < 2) {
            issues.add(new OptimizationIssue(
                    "FEW_HEADINGS",
                    "MEDIUM",
                    String.format("文章标题结构较少（%d个），建议至少3-5个标题", headingCount),
                    "使用标题细分内容章节",
                    Map.of("headingCount", headingCount)
            ));
        }

        Matcher headingMatcher = HEADING_PATTERN.matcher(content);
        while (headingMatcher.find()) {
            String headingText = headingMatcher.group(2);
            if (headingText.length() > MAX_HEADING_LENGTH) {
                issues.add(new OptimizationIssue(
                        "HEADING_TOO_LONG",
                        "LOW",
                        String.format("标题过长（%d字符）：%s", headingText.length(), headingText),
                        "缩短标题，保持简洁清晰",
                        Map.of("headingLength", headingText.length(), "heading", headingText)
                ));
            }
        }

        int codeBlockCount = countPattern(content, CODE_BLOCK_PATTERN);
        int imageCount = countPattern(content, IMAGE_PATTERN);
        int linkCount = countPattern(content, LINK_PATTERN);

        if (codeBlockCount == 0) {
            issues.add(new OptimizationIssue(
                    "NO_CODE_BLOCKS",
                    "LOW",
                    "文章中没有代码块",
                    "如果是技术文章，添加代码示例可以增强实用性",
                    Map.of("codeBlockCount", 0)
            ));
        }

        if (imageCount == 0) {
            issues.add(new OptimizationIssue(
                    "NO_IMAGES",
                    "MEDIUM",
                    "文章中没有图片",
                    "添加相关图片可以增强可读性和SEO效果",
                    Map.of("imageCount", 0)
            ));
        } else if (imageCount > 0 && linkCount == 0) {
            issues.add(new OptimizationIssue(
                    "NO_INTERNAL_LINKS",
                    "LOW",
                    "文章中没有内部链接",
                    "添加相关文章的内部链接，提高页面停留时间",
                    Map.of("linkCount", 0)
            ));
        }

        if (title != null && title.length() > 60) {
            issues.add(new OptimizationIssue(
                    "TITLE_TOO_LONG",
                    "MEDIUM",
                    String.format("标题过长（%d字符），建议控制在60字符以内", title.length()),
                    "精简标题，保持简洁有吸引力",
                    Map.of("titleLength", title.length())
            ));
        }

        if (title != null && !标题包含有效关键词(title)) {
            issues.add(new OptimizationIssue(
                    "TITLE_NOT_SEMANTIC",
                    "LOW",
                    "标题可能不够语义清晰",
                    "确保标题能准确反映文章内容",
                    Map.of()
            ));
        }

        return issues;
    }

    public String generateOptimizationSuggestion(String content, String title, List<OptimizationIssue> issues) {
        if (issues.isEmpty()) {
            return "文章内容质量良好，无需优化！";
        }

        StringBuilder suggestion = new StringBuilder();
        suggestion.append("优化建议：\n\n");

        long highPriorityCount = issues.stream()
                .filter(i -> "HIGH".equals(i.getSeverity()))
                .count();

        if (highPriorityCount > 0) {
            suggestion.append("【高优先级】\n");
            issues.stream()
                    .filter(i -> "HIGH".equals(i.getSeverity()))
                    .forEach(issue -> {
                        suggestion.append(String.format("• %s\n  建议：%s\n\n",
                                issue.getDescription(), issue.getSuggestion()));
                    });
        }

        long mediumPriorityCount = issues.stream()
                .filter(i -> "MEDIUM".equals(i.getSeverity()))
                .count();

        if (mediumPriorityCount > 0) {
            suggestion.append("【中优先级】\n");
            issues.stream()
                    .filter(i -> "MEDIUM".equals(i.getSeverity()))
                    .forEach(issue -> {
                        suggestion.append(String.format("• %s\n  建议：%s\n\n",
                                issue.getDescription(), issue.getSuggestion()));
                    });
        }

        return suggestion.toString();
    }

    public OptimizationResult optimizeContent(ArticleListVO article, String content) {
        Long articleId = article.getId();
        String title = article.getTitle();

        List<OptimizationIssue> issues = analyzeContent(content, title);

        String optimizedContent = content;
        String optimizedTitle = title;

        List<String> improvements = new ArrayList<>();

        for (OptimizationIssue issue : issues) {
            switch (issue.getType()) {
                case "TITLE_TOO_LONG":
                    if (title.length() > 60) {
                        optimizedTitle = title.substring(0, 57) + "...";
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

        SEOService.SEOAnalysisResult seoResult = seoService.analyzeSEO(
                optimizedContent, optimizedTitle, null);

        Map<String, Object> qualityScore = calculateQualityScore(issues, content.length());

        return new OptimizationResult(
                articleId,
                title,
                issues,
                optimizedContent,
                optimizedTitle,
                qualityScore,
                improvements
        );
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

    private Map<String, Object> calculateQualityScore(List<OptimizationIssue> issues, int contentLength) {
        Map<String, Object> score = new HashMap<>();

        int baseScore = 100;

        for (OptimizationIssue issue : issues) {
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

    private int countHeadings(String content) {
        if (content == null) return 0;
        Matcher matcher = HEADING_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private int countPattern(String content, Pattern pattern) {
        if (content == null) return 0;
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private boolean 标题包含有效关键词(String title) {
        return title != null && title.length() >= 5;
    }
}