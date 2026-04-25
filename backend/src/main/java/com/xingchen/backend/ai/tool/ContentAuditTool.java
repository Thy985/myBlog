package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentAuditTool implements Tool {

    private final ArticleService articleService;

    private static final Pattern EMPTY_PATTERN = Pattern.compile("^(|null|\\s*)$", Pattern.CASE_INSENSITIVE);
    private static final double SIMILARITY_THRESHOLD = 0.85;

    @Override
    public String getName() {
        return "content_audit";
    }

    @Override
    public String getDescription() {
        return "内容质量审计工具：识别重复内容、低质量文章、提出优化建议。支持审计所有文章、按分类审计、或分析特定文章的质量。";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
            new ToolParameter(
                "action",
                "操作类型：audit_all（审计所有文章）| audit_category（按分类审计）| analyze_duplicates（分析重复）| suggest_optimization（优化建议）",
                "string",
                false,
                "audit_all"
            ),
            new ToolParameter(
                "categoryId",
                "分类ID（用于按分类审计）",
                "integer",
                false,
                null
            ),
            new ToolParameter(
                "similarityThreshold",
                "相似度阈值（0-1），低于此值视为重复",
                "float",
                false,
                0.85
            )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String action = (String) parameters.get("action");
        String message = (String) parameters.get("message");
        Integer categoryId = (Integer) parameters.get("categoryId");
        Double threshold = parameters.get("similarityThreshold") != null
            ? ((Number) parameters.get("similarityThreshold")).doubleValue()
            : SIMILARITY_THRESHOLD;

        if (!StpUtil.isLogin()) {
            return ToolResult.error("用户未登录，无法进行内容审计");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        if (action == null || action.isEmpty()) {
            action = inferAction(message);
        }

        return switch (action.toLowerCase()) {
            case "audit_all" -> auditAllArticles(userId, threshold);
            case "audit_category" -> auditByCategory(userId, categoryId, threshold);
            case "analyze_duplicates" -> analyzeDuplicates(userId, threshold);
            case "suggest_optimization" -> suggestOptimization(userId);
            default -> auditAllArticles(userId, threshold);
        };
    }

    private String inferAction(String message) {
        if (message == null) return "audit_all";
        String lower = message.toLowerCase();
        if (lower.contains("重复")) return "analyze_duplicates";
        if (lower.contains("优化")) return "suggest_optimization";
        if (lower.contains("分类")) return "audit_category";
        return "audit_all";
    }

    private ToolResult auditAllArticles(Long userId, double threshold) {
        log.info("审计用户 {} 的所有文章", userId);
        long startTime = System.currentTimeMillis();

        PageResult<ArticleListVO> pageResult = articleService.getUserArticles(userId, 1, 100);
        List<ArticleListVO> articles = pageResult.getList();

        if (articles == null || articles.isEmpty()) {
            return ToolResult.success(
                Map.of("total", 0, "duplicates", List.of(), "lowQuality", List.of(), "suggestions", List.of()),
                "您还没有文章"
            );
        }

        List<Map<String, Object>> auditResults = analyzeArticles(articles, threshold);
        List<Map<String, Object>> duplicates = (List<Map<String, Object>>) auditResults.get(0).get("duplicates");
        List<Map<String, Object>> lowQuality = (List<Map<String, Object>>) auditResults.get(0).get("lowQuality");
        List<String> suggestions = (List<String>) auditResults.get(0).get("suggestions");

        Map<String, Object> result = new HashMap<>();
        result.put("total", articles.size());
        result.put("duplicates", duplicates);
        result.put("duplicateGroups", duplicates.size());
        result.put("lowQuality", lowQuality);
        result.put("lowQualityCount", lowQuality.size());
        result.put("suggestions", suggestions);
        result.put("qualityScore", calculateQualityScore(articles.size(), duplicates.size(), lowQuality.size()));

        long elapsed = System.currentTimeMillis() - startTime;
        return ToolResult.success(result,
            String.format("审计完成！共 %d 篇文章，发现 %d 组重复内容，%d 篇低质量文章，生成 %d 条优化建议。耗时 %dms",
                articles.size(), duplicates.size(), lowQuality.size(), suggestions.size(), elapsed));
    }

    private ToolResult auditByCategory(Long userId, Integer categoryId, double threshold) {
        log.info("按分类审计用户 {} 的文章，分类: {}", userId, categoryId);
        PageResult<ArticleListVO> pageResult = articleService.getUserArticles(userId, 1, 100);
        List<ArticleListVO> articles = pageResult.getList();

        if (categoryId != null) {
            articles = articles.stream()
                .filter(a -> a.getCategoryName() != null && a.getCategoryName().contains(String.valueOf(categoryId)))
                .collect(Collectors.toList());
        }

        if (articles.isEmpty()) {
            return ToolResult.success(
                Map.of("total", 0, "categoryArticles", List.of()),
                "该分类下没有文章"
            );
        }

        List<Map<String, Object>> auditResults = analyzeArticles(articles, threshold);

        Map<String, Object> result = new HashMap<>();
        result.put("categoryTotal", articles.size());
        result.put("duplicates", auditResults.get(0).get("duplicates"));
        result.put("lowQuality", auditResults.get(0).get("lowQuality"));
        result.put("suggestions", auditResults.get(0).get("suggestions"));

        return ToolResult.success(result, String.format("分类审计完成！共 %d 篇文章", articles.size()));
    }

    private ToolResult analyzeDuplicates(Long userId, double threshold) {
        log.info("分析用户 {} 的重复文章", userId);
        PageResult<ArticleListVO> pageResult = articleService.getUserArticles(userId, 1, 100);
        List<ArticleListVO> articles = pageResult.getList();

        List<ArticleListVO> duplicates = articles.stream()
            .filter(a -> {
                String title = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
                return title.contains("测试") || title.contains("test") || title.contains("aaa") || title.contains("bbb");
            })
            .collect(Collectors.toList());

        Map<String, List<ArticleListVO>> titleGroups = articles.stream()
            .filter(a -> a.getTitle() != null)
            .collect(Collectors.groupingBy(a -> normalizeTitle(a.getTitle())));

        List<Map<String, Object>> duplicateGroups = new ArrayList<>();
        for (Map.Entry<String, List<ArticleListVO>> entry : titleGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> group = new HashMap<>();
                group.put("normalizedTitle", entry.getKey());
                group.put("count", entry.getValue().size());
                group.put("articles", entry.getValue().stream()
                    .map(a -> Map.of(
                        "id", a.getId(),
                        "title", a.getTitle(),
                        "publishTime", a.getPublishTime() != null ? a.getPublishTime().toString() : "未发布"
                    ))
                    .collect(Collectors.toList()));
                group.put("recommendation", entry.getValue().size() > 2 ? "建议合并或删除多余文章" : "建议审核是否需要保留");
                duplicateGroups.add(group);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalGroups", duplicateGroups.size());
        result.put("duplicateGroups", duplicateGroups);
        result.put("testArticles", duplicates.stream()
            .map(a -> Map.of("id", a.getId(), "title", a.getTitle()))
            .collect(Collectors.toList()));

        return ToolResult.success(result,
            String.format("重复分析完成！发现 %d 组标题相似的重复文章，%d 篇测试文章", duplicateGroups.size(), duplicates.size()));
    }

    private ToolResult suggestOptimization(Long userId) {
        log.info("为用户 {} 生成优化建议", userId);
        PageResult<ArticleListVO> pageResult = articleService.getUserArticles(userId, 1, 100);
        List<ArticleListVO> articles = pageResult.getList();

        List<String> suggestions = new ArrayList<>();

        for (ArticleListVO article : articles) {
            String title = article.getTitle() != null ? article.getTitle() : "";

            if (title.contains("测试") || title.contains("test")) {
                suggestions.add(String.format("删除或改写测试文章《%s》(ID:%d)", title, article.getId()));
            }

            if (article.getReadNum() != null && article.getReadNum() < 10) {
                suggestions.add(String.format("提升《%s》(ID:%d)的曝光：优化标题和摘要", title, article.getId()));
            }

            if (title.length() < 10) {
                suggestions.add(String.format("标题过短的文章《%s》(ID:%d)需要重新命名", title, article.getId()));
            }

            if (article.getDescription() == null || article.getDescription().isEmpty()) {
                suggestions.add(String.format("《%s》(ID:%d)缺少摘要，建议添加", title, article.getId()));
            }
        }

        suggestions.add("建议使用AI工具批量生成SEO优化的新文章");
        suggestions.add("建议建立定期内容审计机制，每周优化低质量文章");

        Map<String, Object> result = new HashMap<>();
        result.put("totalSuggestions", suggestions.size());
        result.put("suggestions", suggestions);

        return ToolResult.success(result,
            String.format("优化建议生成完成！共 %d 条建议", suggestions.size()));
    }

    private List<Map<String, Object>> analyzeArticles(List<ArticleListVO> articles, double threshold) {
        List<Map<String, Object>> duplicates = new ArrayList<>();
        List<Map<String, Object>> lowQuality = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        for (ArticleListVO article : articles) {
            String title = article.getTitle() != null ? article.getTitle() : "";

            if (title.contains("测试") || title.contains("test") || title.contains("aaa") || title.contains("bbb")) {
                lowQuality.add(Map.of(
                    "id", article.getId(),
                    "title", title,
                    "reason", "标题包含测试标识",
                    "recommendation", "删除或改写"
                ));
            }

            if (article.getReadNum() != null && article.getReadNum() == 0) {
                lowQuality.add(Map.of(
                    "id", article.getId(),
                    "title", title,
                    "reason", "阅读数为0",
                    "recommendation", "检查是否发布或优化内容"
                ));
            }
        }

        Map<String, List<ArticleListVO>> titleGroups = articles.stream()
            .filter(a -> a.getTitle() != null)
            .collect(Collectors.groupingBy(a -> normalizeTitle(a.getTitle())));

        for (Map.Entry<String, List<ArticleListVO>> entry : titleGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.add(Map.of(
                    "normalizedTitle", entry.getKey(),
                    "count", entry.getValue().size(),
                    "articleIds", entry.getValue().stream().map(ArticleListVO::getId).collect(Collectors.toList()),
                    "titles", entry.getValue().stream().map(ArticleListVO::getTitle).collect(Collectors.toList()),
                    "action", "merge_or_delete"
                ));
            }
        }

        if (articles.size() > 50) {
            suggestions.add("文章数量充足，建议提升内容质量而非数量");
        } else if (articles.size() < 10) {
            suggestions.add("文章数量偏少，建议定期发布新内容");
        }

        if (duplicates.size() > 5) {
            suggestions.add(String.format("发现 %d 组重复内容，建议清理或合并", duplicates.size()));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        results.add(Map.of(
            "duplicates", duplicates,
            "lowQuality", lowQuality,
            "suggestions", suggestions
        ));

        return results;
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
            .replaceAll("[^\\u4e00-\\u9fa5a-z0-9]", "")
            .trim();
    }

    private double calculateQualityScore(int total, int duplicates, int lowQuality) {
        if (total == 0) return 0;
        double score = 100.0;
        score -= duplicates * 5;
        score -= lowQuality * 10;
        return Math.max(0, Math.min(100, score));
    }

    @Override
    public long getTimeout() {
        return 60000;
    }
}
