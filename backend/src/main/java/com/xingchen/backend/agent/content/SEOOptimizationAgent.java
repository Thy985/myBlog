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
public class SEOOptimizationAgent extends BaseAgent {

    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(SEOOptimizationAgent.class);

    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_LOW = "LOW";

    public SEOOptimizationAgent(@Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("SEOOptimizationAgent", Map.of("maxTokens", 3000, "temperature", 0.5));
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        Long articleId = (Long) context.getParam("articleId");
        if (articleId == null) {
            throw new AgentException("Article ID is required", "MISSING_ARTICLE_ID");
        }

        Long userId = context.getUserId() != null ? context.getUserId() : (Long) context.getParam("userId", 0L);
        String optimizationType = (String) context.getParam("type", "FULL");

        SEOSuggestions suggestions = generateSuggestions(articleId, userId, optimizationType);

        Map<String, Object> data = Map.of(
                "articleId", articleId,
                "suggestions", suggestions.suggestions(),
                "priority", suggestions.priority(),
                "estimatedImpact", suggestions.estimatedImpact()
        );

        return AgentResult.success(data);
    }

    private SEOSuggestions generateSuggestions(Long articleId, Long userId, String optimizationType) {
        List<SEOIssue> issues = analyzeCurrentSEO(articleId, userId);
        List<String> suggestions = new ArrayList<>();
        int highPriority = 0;
        int mediumPriority = 0;
        double totalImpact = 0;

        for (SEOIssue issue : issues) {
            suggestions.add(formatSuggestion(issue));

            switch (issue.priority()) {
                case PRIORITY_HIGH -> {
                    highPriority++;
                    totalImpact += 20;
                }
                case PRIORITY_MEDIUM -> {
                    mediumPriority++;
                    totalImpact += 10;
                }
                default -> totalImpact += 5;
            }
        }

        String priority = highPriority > 0 ? PRIORITY_HIGH :
                mediumPriority > 0 ? PRIORITY_MEDIUM : PRIORITY_LOW;

        return new SEOSuggestions(suggestions, priority, Math.min(100, totalImpact));
    }

    private List<SEOIssue> analyzeCurrentSEO(Long articleId, Long userId) {
        List<SEOIssue> issues = new ArrayList<>();

        issues.addAll(generateLLMBasedSuggestions(articleId));

        return issues;
    }

    private List<SEOIssue> generateLLMBasedSuggestions(Long articleId) {
        List<SEOIssue> issues = new ArrayList<>();

        String prompt = String.format("""
                请分析以下文章的SEO优化建议（不需要实际文章内容，只需给出通用建议）：

                文章ID：%d

                请从以下维度给出优化建议：
                1. 标题优化 - 是否包含关键词、是否足够吸引人
                2. 关键词布局 - 关键词密度、分布是否合理
                3. 内容结构 - 段落结构、标题层级是否清晰
                4. 内链策略 - 是否需要添加更多内链
                5. 外链建设 - 是否需要引用外部权威资源

                返回JSON数组格式：
                [{"code": "ISSUE_CODE", "title": "问题标题", "priority": "HIGH/MEDIUM/LOW", "suggestion": "建议内容"}]
                如果没有明显问题，返回空数组。
                """, articleId);

        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                issues.addAll(parseIssues(response.getContent()));
            }
        } catch (Exception e) {
            AGENT_LOG.warn("LLM生成SEO建议失败: articleId={}", articleId, e);
        }

        if (issues.isEmpty()) {
            issues.add(new SEOIssue(
                    "GENERAL",
                    "基础SEO优化",
                    PRIORITY_LOW,
                    "文章已满足基础SEO要求，可继续关注内容质量和用户体验"
            ));
        }

        return issues;
    }

    @SuppressWarnings("unchecked")
    private List<SEOIssue> parseIssues(String content) {
        List<SEOIssue> issues = new ArrayList<>();
        try {
            int jsonStart = content.indexOf('[');
            int jsonEnd = content.lastIndexOf(']');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                List<Map<String, String>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonStr, List.class);

                for (Map<String, String> item : parsed) {
                    issues.add(new SEOIssue(
                            item.getOrDefault("code", "UNKNOWN"),
                            item.getOrDefault("title", ""),
                            item.getOrDefault("priority", "LOW"),
                            item.getOrDefault("suggestion", "")
                    ));
                }
            }
        } catch (Exception e) {
            AGENT_LOG.debug("解析SEO问题失败: {}", e.getMessage());
        }
        return issues;
    }

    private String formatSuggestion(SEOIssue issue) {
        return String.format("[%s] %s: %s",
                issue.priority(), issue.title(), issue.suggestion());
    }

    private record SEOIssue(String code, String title, String priority, String suggestion) {}

    private record SEOSuggestions(List<String> suggestions, String priority, double estimatedImpact) {}
}