package com.xingchen.backend.agent.content;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ContentAuditAgent extends BaseAgent {

    private final ArticleService articleService;
    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(ContentAuditAgent.class);

    public static final String AUDIT_PASS = "PASS";
    public static final String AUDIT_WARNING = "WARNING";
    public static final String AUDIT_FAIL = "FAIL";
    public static final String AUDIT_NEED_HUMAN = "NEED_HUMAN_REVIEW";

    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "赌博", "毒品", "枪支", "色情", "诈骗", "暴力", "恐怖"
    );

    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "医疗", "健康", "金融", "投资", "法律", "处方", "手术"
    );

    public ContentAuditAgent(ArticleService articleService,
                            @Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("ContentAuditAgent", Map.of("maxTokens", 4000, "temperature", 0.3));
        this.articleService = articleService;
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        Long userId = context.getUserId() != null ? context.getUserId() : (Long) context.getParam("userId", 0L);
        Long articleId = (Long) context.getParam("articleId");
        Integer limit = (Integer) context.getParam("limit", 100);

        if (articleId != null) {
            return auditSingleArticle(userId, articleId);
        } else {
            return auditAllArticles(userId, limit);
        }
    }

    private AgentResult auditSingleArticle(Long userId, Long articleId) {
        List<AuditResult> layerResults = new ArrayList<>();

        ArticleVO article = articleService.getArticleById(articleId, userId);
        if (article == null) {
            return AgentResult.failure("ARTICLE_NOT_FOUND", "文章不存在");
        }

        Layer1Result layer1 = layer1AutoCheck(article);
        layerResults.add(layer1.toAuditResult());

        Layer2Result layer2 = layer2AICheck(article);
        layerResults.add(layer2.toAuditResult());

        List<Map<String, Object>> allIssues = new ArrayList<>();
        allIssues.addAll(layer1.issues);
        allIssues.addAll(layer2.issues);

        String finalStatus = determineFinalStatus(layer1, layer2);
        boolean needHumanReview = AUDIT_NEED_HUMAN.equals(finalStatus);

        Map<String, Object> data = Map.of(
                "articleId", articleId,
                "title", article.getTitle(),
                "finalStatus", finalStatus,
                "needHumanReview", needHumanReview,
                "layerResults", layerResults,
                "allIssues", allIssues,
                "layer1Passed", layer1.passed,
                "layer2Passed", layer2.passed
        );

        return AgentResult.success(data);
    }

    private AgentResult auditAllArticles(Long userId, Integer limit) {
        List<Map<String, Object>> issues = new ArrayList<>();

        PageResult<ArticleListVO> pageResult = articleService.getArticleList(1, limit, null, null, null, userId);
        List<ArticleListVO> articles = pageResult.getList();

        if (articles == null || articles.isEmpty()) {
            return AgentResult.success(Map.of(
                    "totalArticles", 0,
                    "auditResults", List.of(),
                    "summary", "No articles to audit"
            ));
        }

        int passCount = 0;
        int warningCount = 0;
        int failCount = 0;
        int needHumanCount = 0;

        for (ArticleListVO article : articles) {
            try {
                ArticleVO fullArticle = articleService.getArticleById(article.getId(), userId);
                if (fullArticle == null) continue;

                Layer1Result layer1 = layer1AutoCheck(fullArticle);
                Layer2Result layer2 = layer2AICheck(fullArticle);

                String status = determineFinalStatus(layer1, layer2);

                switch (status) {
                    case AUDIT_PASS -> passCount++;
                    case AUDIT_WARNING -> warningCount++;
                    case AUDIT_FAIL -> failCount++;
                    case AUDIT_NEED_HUMAN -> needHumanCount++;
                }

                for (Map<String, Object> issue : layer1.issues) {
                    issues.add(Map.of(
                            "code", issue.get("code"),
                            "type", issue.get("type"),
                            "articleId", article.getId(),
                            "severity", issue.get("severity"),
                            "description", issue.get("description")
                    ));
                }
                for (Map<String, Object> issue : layer2.issues) {
                    issues.add(Map.of(
                            "code", issue.get("code"),
                            "type", issue.get("type"),
                            "articleId", article.getId(),
                            "severity", issue.get("severity"),
                            "description", issue.get("description")
                    ));
                }
            } catch (Exception e) {
                AGENT_LOG.warn("审计文章失败: articleId={}", article.getId(), e);
            }
        }

        Map<String, Object> data = Map.of(
                "totalArticles", articles.size(),
                "passCount", passCount,
                "warningCount", warningCount,
                "failCount", failCount,
                "needHumanCount", needHumanCount,
                "auditResults", issues,
                "healthScore", calculateHealthScore(passCount, warningCount, failCount, needHumanCount, articles.size())
        );

        return AgentResult.success(data);
    }

    private Layer1Result layer1AutoCheck(ArticleVO article) {
        List<Map<String, Object>> issues = new ArrayList<>();
        boolean passed = true;

        if (article.getContent() == null || article.getContent().length() < 100) {
            issues.add(createIssue("LOW_CONTENT_LENGTH", "内容过短", article.getId(), "HIGH", "文章内容少于100字"));
            passed = false;
        }

        if (article.getDescription() == null || article.getDescription().length() < 50) {
            issues.add(createIssue("LOW_DESCRIPTION_LENGTH", "描述过短", article.getId(), "MEDIUM", "SEO描述少于50字"));
            passed = false;
        }

        if (article.getTitle() == null || article.getTitle().length() < 5) {
            issues.add(createIssue("LOW_TITLE_LENGTH", "标题过短", article.getId(), "HIGH", "标题少于5字"));
            passed = false;
        }

        List<Map<String, Object>> sensitiveIssues = checkSensitiveWords(article);
        issues.addAll(sensitiveIssues);
        if (!sensitiveIssues.isEmpty()) passed = false;

        if (article.getContent() != null && article.getContent().length() > 5000) {
            issues.add(createIssue("CONTENT_TOO_LONG", "内容过长", article.getId(), "LOW", "文章超过5000字，可能影响阅读体验"));
        }

        return new Layer1Result(issues, passed);
    }

    private List<Map<String, Object>> checkSensitiveWords(ArticleVO article) {
        List<Map<String, Object>> issues = new ArrayList<>();
        String content = article.getContent() != null ? article.getContent().toLowerCase() : "";
        String title = article.getTitle() != null ? article.getTitle().toLowerCase() : "";

        for (String word : SENSITIVE_WORDS) {
            if (content.contains(word) || title.contains(word)) {
                issues.add(createIssue("SENSITIVE_CONTENT", "敏感内容", article.getId(), "HIGH",
                        "检测到敏感词: " + word));
            }
        }

        return issues;
    }

    private Layer2Result layer2AICheck(ArticleVO article) {
        List<Map<String, Object>> issues = new ArrayList<>();
        boolean passed = true;

        List<String> highRiskChecks = checkHighRiskContent(article);
        for (String risk : highRiskChecks) {
            issues.add(createIssue("HIGH_RISK_CONTENT", "高风险内容", article.getId(), "MEDIUM", risk));
            passed = false;
        }

        List<String> factChecks = performFactCheck(article);
        for (String fact : factChecks) {
            issues.add(createIssue("FACT_CHECK", "事实性校验", article.getId(), "LOW", fact));
        }

        String styleCheck = checkStyleConsistency(article);
        if (!styleCheck.isEmpty()) {
            issues.add(createIssue("STYLE_ISSUE", "风格问题", article.getId(), "LOW", styleCheck));
        }

        return new Layer2Result(issues, passed);
    }

    private List<String> checkHighRiskContent(ArticleVO article) {
        List<String> risks = new ArrayList<>();
        String content = article.getContent() != null ? article.getContent() : "";
        String title = article.getTitle() != null ? article.getTitle() : "";

        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (content.contains(keyword) || title.contains(keyword)) {
                risks.add("涉及" + keyword + "领域，建议人工审核确保内容准确性");
            }
        }

        if (risks.size() >= 3) {
            risks.add(0, "内容涉及多个高风险领域，需要谨慎处理");
        }

        return risks;
    }

    private List<String> performFactCheck(ArticleVO article) {
        List<String> facts = new ArrayList<>();

        try {
            String contentPreview = article.getContent() != null ?
                    article.getContent().substring(0, Math.min(1000, article.getContent().length())) : "";

            String prompt = String.format(
                    "请检查以下文章的潜在事实性问题：\n\n标题：%s\n\n内容：\n%s\n\n请检查：\n1. 是否有明显的错误信息\n2. 是否有未经证实的声明\n3. 是否有过于绝对的表述\n\n返回JSON数组格式：[\"问题1\", \"问题2\"]\n如果没有发现问题，返回空数组。",
                    article.getTitle(), contentPreview);

            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                facts.addAll(parseFactCheckResults(response.getContent()));
            }
        } catch (Exception e) {
            AGENT_LOG.warn("AI事实性校验失败: articleId={}", article.getId(), e);
        }

        return facts;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseFactCheckResults(String content) {
        List<String> facts = new ArrayList<>();
        try {
            int jsonStart = content.indexOf('[');
            int jsonEnd = content.lastIndexOf(']');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                List<String> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonStr, List.class);
                facts.addAll(parsed);
            }
        } catch (Exception e) {
            AGENT_LOG.debug("解析事实检查结果失败: {}", e.getMessage());
        }
        return facts;
    }

    private String checkStyleConsistency(ArticleVO article) {
        if (article.getContent() == null || article.getContent().length() < 500) {
            return "";
        }

        String content = article.getContent();

        boolean hasEmoji = Pattern.compile("[\\x{10000}-\\x{10FFFF}]").matcher(content).find();
        boolean hasCodeBlock = content.contains("```") || content.contains("`");

        if (hasEmoji && hasCodeBlock) {
            return "内容和代码混排可能影响阅读体验，建议分区展示";
        }

        if (content.split("\\n").length < 5) {
            return "建议增加段落分隔，提高可读性";
        }

        return "";
    }

    private String determineFinalStatus(Layer1Result layer1, Layer2Result layer2) {
        boolean hasHighSeverity = layer1.issues.stream()
                .anyMatch(i -> "HIGH".equals(i.get("severity")));
        boolean hasSensitiveContent = layer1.issues.stream()
                .anyMatch(i -> "SENSITIVE_CONTENT".equals(i.get("code")));
        boolean hasHighRiskContent = layer2.issues.stream()
                .anyMatch(i -> "HIGH_RISK_CONTENT".equals(i.get("code")));

        if (hasSensitiveContent || hasHighSeverity) {
            return AUDIT_NEED_HUMAN;
        }

        if (!layer1.passed || !layer2.passed) {
            return AUDIT_WARNING;
        }

        if (hasHighRiskContent) {
            return AUDIT_NEED_HUMAN;
        }

        return AUDIT_PASS;
    }

    private int calculateHealthScore(int pass, int warning, int fail, int needHuman, int total) {
        if (total == 0) return 100;
        double score = ((double) pass / total) * 100 - (warning * 2) - (fail * 5) - (needHuman * 3);
        return Math.max(0, Math.min(100, (int) score));
    }

    private Map<String, Object> createIssue(String code, String type, Long articleId, String severity, String description) {
        String layer = code.startsWith("SENSITIVE") || code.startsWith("LOW_") ? "1" : "2";
        return Map.of(
                "code", code,
                "type", type,
                "articleId", articleId,
                "severity", severity,
                "description", description,
                "layer", layer
        );
    }

    private static class Layer1Result {
        final List<Map<String, Object>> issues;
        final boolean passed;

        Layer1Result(List<Map<String, Object>> issues, boolean passed) {
            this.issues = issues;
            this.passed = passed;
        }

        AuditResult toAuditResult() {
            return new AuditResult(passed ? AUDIT_PASS : AUDIT_WARNING, issues);
        }
    }

    private static class Layer2Result {
        final List<Map<String, Object>> issues;
        final boolean passed;

        Layer2Result(List<Map<String, Object>> issues, boolean passed) {
            this.issues = issues;
            this.passed = passed;
        }

        AuditResult toAuditResult() {
            return new AuditResult(passed ? AUDIT_PASS : AUDIT_WARNING, issues);
        }
    }

    private static class AuditResult {
        final String status;
        final List<Map<String, Object>> issues;

        AuditResult(String status, List<Map<String, Object>> issues) {
            this.status = status;
            this.issues = issues;
        }
    }

}
