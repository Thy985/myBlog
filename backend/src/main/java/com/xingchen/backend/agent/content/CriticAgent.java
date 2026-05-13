package com.xingchen.backend.agent.content;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class CriticAgent extends BaseAgent {

    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(CriticAgent.class);

    private static final double PASS_THRESHOLD = 0.7;
    private static final double REVISION_THRESHOLD = 0.5;

    public CriticAgent(@Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("CriticAgent", Map.of("maxTokens", 3000, "temperature", 0.3));
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        Map<String, Object> contentToReview = (Map<String, Object>) context.getParam("content");
        String contentType = (String) context.getParam("contentType", "article");
        String reviewFocus = (String) context.getParam("reviewFocus", "all");

        if (contentToReview == null || contentToReview.isEmpty()) {
            throw new AgentException("待审核内容不能为空", "EMPTY_CONTENT");
        }

        Map<String, Object> qualityReport = assessQuality(contentToReview, contentType, reviewFocus);
        double overallScore = (double) qualityReport.get("overallScore");
        String verdict = determineVerdict(overallScore);

        Map<String, Object> suggestions = generateSuggestions(qualityReport, contentType);
        List<String> revisionPoints = identifyRevisionPoints(qualityReport, contentType);

        Map<String, Object> data = Map.of(
                "overallScore", overallScore,
                "verdict", verdict,
                "qualityReport", qualityReport,
                "suggestions", suggestions,
                "revisionPoints", revisionPoints,
                "needsRevision", verdict.equals("REVISION") || verdict.equals("REJECT"),
                "approved", verdict.equals("APPROVED")
        );

        AGENT_LOG.info("内容审核完成: score={}, verdict={}", overallScore, verdict);
        return AgentResult.success(data);
    }

    private Map<String, Object> assessQuality(Map<String, Object> content, String contentType, String reviewFocus) {
        Map<String, Object> report = new LinkedHashMap<>();

        double totalScore = 0;
        int aspectsChecked = 0;

        if ("all".equals(reviewFocus) || "accuracy".equals(reviewFocus)) {
            double accuracyScore = assessAccuracy(content);
            report.put("accuracy", accuracyScore);
            totalScore += accuracyScore;
            aspectsChecked++;
        }

        if ("all".equals(reviewFocus) || "relevance".equals(reviewFocus)) {
            double relevanceScore = assessRelevance(content);
            report.put("relevance", relevanceScore);
            totalScore += relevanceScore;
            aspectsChecked++;
        }

        if ("all".equals(reviewFocus) || "clarity".equals(reviewFocus)) {
            double clarityScore = assessClarity(content);
            report.put("clarity", clarityScore);
            totalScore += clarityScore;
            aspectsChecked++;
        }

        if ("all".equals(reviewFocus) || "seo".equals(reviewFocus)) {
            double seoScore = assessSEO(content);
            report.put("seo", seoScore);
            totalScore += seoScore;
            aspectsChecked++;
        }

        if ("all".equals(reviewFocus) || "originality".equals(reviewFocus)) {
            double originalityScore = assessOriginality(content);
            report.put("originality", originalityScore);
            totalScore += originalityScore;
            aspectsChecked++;
        }

        report.put("aspectsChecked", aspectsChecked);
        report.put("totalScore", totalScore);
        report.put("overallScore", aspectsChecked > 0 ? totalScore / aspectsChecked : 0);

        return report;
    }

    private double assessAccuracy(Map<String, Object> content) {
        String title = content.get("title") != null ? content.get("title").toString() : "";
        String contentText = content.get("content") != null ? content.get("content").toString() :
                content.get("description") != null ? content.get("description").toString() : "";

        double score = 0.8;

        if (title.length() < 10) {
            score -= 0.2;
        }

        if (contentText.length() < 100) {
            score -= 0.3;
        }

        return Math.max(0, Math.min(1, score));
    }

    private double assessRelevance(Map<String, Object> content) {
        List<String> keywords = extractKeywords(content);
        String contentText = content.get("content") != null ? content.get("content").toString() :
                content.get("description") != null ? content.get("description").toString() : "";

        double score = 0.7;

        if (keywords.isEmpty()) {
            score -= 0.2;
        } else {
            int matchCount = 0;
            for (String keyword : keywords) {
                if (contentText.toLowerCase().contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            double matchRatio = (double) matchCount / keywords.size();
            score = 0.5 + (matchRatio * 0.3);
        }

        return Math.max(0, Math.min(1, score));
    }

    private double assessClarity(Map<String, Object> content) {
        String contentText = content.get("content") != null ? content.get("content").toString() :
                content.get("description") != null ? content.get("description").toString() : "";

        double score = 0.75;

        String[] sentences = contentText.split("[。！？.!?]");
        if (sentences.length < 3) {
            score -= 0.15;
        }

        if (contentText.contains("...") || contentText.contains("…")) {
            score -= 0.1;
        }

        return Math.max(0, Math.min(1, score));
    }

    private double assessSEO(Map<String, Object> content) {
        String title = content.get("title") != null ? content.get("title").toString() : "";
        String description = content.get("description") != null ? content.get("description").toString() : "";

        double score = 0.6;

        if (title.length() >= 10 && title.length() <= 60) {
            score += 0.15;
        }

        if (description.length() >= 50 && description.length() <= 160) {
            score += 0.15;
        }

        List<String> keywords = extractKeywords(content);
        if (!keywords.isEmpty()) {
            boolean hasKeywordInTitle = false;
            for (String keyword : keywords) {
                if (title.toLowerCase().contains(keyword.toLowerCase())) {
                    hasKeywordInTitle = true;
                    break;
                }
            }
            if (hasKeywordInTitle) {
                score += 0.1;
            }
        }

        return Math.max(0, Math.min(1, score));
    }

    private double assessOriginality(Map<String, Object> content) {
        return 0.8;
    }

    private List<String> extractKeywords(Map<String, Object> content) {
        List<String> keywords = new ArrayList<>();
        Object keywordsObj = content.get("keywords");
        if (keywordsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) keywordsObj;
            keywords.addAll(list);
        } else if (keywordsObj instanceof String) {
            String[] parts = ((String) keywordsObj).split("[,，;；]");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    keywords.add(trimmed);
                }
            }
        }
        return keywords;
    }

    private String determineVerdict(double overallScore) {
        if (overallScore >= PASS_THRESHOLD) {
            return "APPROVED";
        } else if (overallScore >= REVISION_THRESHOLD) {
            return "REVISION";
        } else {
            return "REJECT";
        }
    }

    private Map<String, Object> generateSuggestions(Map<String, Object> qualityReport, String contentType) {
        Map<String, Object> suggestions = new LinkedHashMap<>();

        for (String aspect : List.of("accuracy", "relevance", "clarity", "seo", "originality")) {
            if (qualityReport.containsKey(aspect)) {
                double score = (double) qualityReport.get(aspect);
                if (score < PASS_THRESHOLD) {
                    suggestions.put(aspect, getSuggestionForAspect(aspect, score));
                }
            }
        }

        return suggestions;
    }

    private String getSuggestionForAspect(String aspect, double score) {
        return switch (aspect) {
            case "accuracy" -> score < 0.5 ?
                    "内容准确性存在严重问题，建议重新核实信息来源" :
                    "内容基本准确，但建议补充更多权威来源";
            case "relevance" -> score < 0.5 ?
                    "内容与主题相关性不足，建议重新聚焦核心主题" :
                    "内容相关性一般，建议加强主题相关性";
            case "clarity" -> score < 0.5 ?
                    "表达不够清晰，建议重构句子结构" :
                    "表达基本清晰，可适当增加过渡句";
            case "seo" -> score < 0.5 ?
                    "SEO优化严重不足，建议重新优化标题和描述" :
                    "SEO基础达标，建议优化关键词密度";
            case "originality" -> score < 0.5 ?
                    "内容原创度不足，建议增加独特见解" :
                    "内容有一定原创性，可继续深化个人视角";
            default -> "建议改进";
        };
    }

    private List<String> identifyRevisionPoints(Map<String, Object> qualityReport, String contentType) {
        List<String> revisionPoints = new ArrayList<>();

        for (String aspect : List.of("accuracy", "relevance", "clarity", "seo", "originality")) {
            if (qualityReport.containsKey(aspect)) {
                double score = (double) qualityReport.get(aspect);
                if (score < REVISION_THRESHOLD) {
                    revisionPoints.add(String.format("[%s] 需要重点修改 (当前分数: %.0f%%)",
                            getAspectChineseName(aspect), score * 100));
                }
            }
        }

        return revisionPoints;
    }

    private String getAspectChineseName(String aspect) {
        return switch (aspect) {
            case "accuracy" -> "准确性";
            case "relevance" -> "相关性";
            case "clarity" -> "清晰度";
            case "seo" -> "SEO优化";
            case "originality" -> "原创性";
            default -> aspect;
        };
    }

    public record CriticResult(
            double overallScore,
            String verdict,
            Map<String, Object> qualityReport,
            Map<String, Object> suggestions,
            List<String> revisionPoints,
            boolean needsRevision,
            boolean approved
    ) {
        public static CriticResult fromMap(Map<String, Object> map) {
            return new CriticResult(
                    (double) map.get("overallScore"),
                    (String) map.get("verdict"),
                    (Map<String, Object>) map.get("qualityReport"),
                    (Map<String, Object>) map.get("suggestions"),
                    (List<String>) map.get("revisionPoints"),
                    (boolean) map.get("needsRevision"),
                    (boolean) map.get("approved")
            );
        }
    }
}