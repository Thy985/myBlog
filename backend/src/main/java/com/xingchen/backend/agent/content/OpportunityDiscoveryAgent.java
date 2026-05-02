package com.xingchen.backend.agent.content;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.WebSearchService;
import com.xingchen.backend.vo.ArticleListVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class OpportunityDiscoveryAgent extends BaseAgent {

    private final ArticleService articleService;
    private final WebSearchService webSearchService;
    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(OpportunityDiscoveryAgent.class);

    public OpportunityDiscoveryAgent(ArticleService articleService,
                                    WebSearchService webSearchService,
                                    @Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("OpportunityDiscoveryAgent", Map.of("maxTokens", 3000, "temperature", 0.7));
        this.articleService = articleService;
        this.webSearchService = webSearchService;
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        Long userId = context.getUserId() != null ? context.getUserId() : (Long) context.getParam("userId", 0L);
        String niche = (String) context.getParam("niche", "");
        Integer topN = (Integer) context.getParam("topN", 5);

        List<String> existingTopics = extractExistingTopics(userId);
        List<SearchTrend> trends = fetchSearchTrends(niche);
        List<ContentOpportunity> opportunities = analyzeOpportunities(existingTopics, trends);

        opportunities.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        List<ContentOpportunity> topOpportunities = opportunities.subList(
                0, Math.min(topN, opportunities.size())
        );

        Map<String, Object> data = Map.of(
                "existingTopics", existingTopics,
                "trends", trends,
                "opportunities", topOpportunities,
                "totalOpportunities", opportunities.size()
        );

        return AgentResult.success(data);
    }

    private List<String> extractExistingTopics(Long userId) {
        List<String> topics = new ArrayList<>();
        try {
            PageResult<ArticleListVO> pageResult = articleService.getArticleList(1, 500, null, null, null, userId);
            List<ArticleListVO> articles = pageResult.getList();

            if (articles != null) {
                for (ArticleListVO article : articles) {
                    if (article.getTitle() != null) {
                        topics.add(article.getTitle());
                    }
                    if (article.getDescription() != null) {
                        topics.add(article.getDescription());
                    }
                }
            }
        } catch (Exception e) {
            AGENT_LOG.warn("提取现有主题失败: userId={}", userId, e);
        }
        return topics;
    }

    private List<SearchTrend> fetchSearchTrends(String niche) {
        List<SearchTrend> trends = new ArrayList<>();
        try {
            String[] defaultQueries = {
                    niche.isEmpty() ? "AI technology trends 2024" : niche + " trends",
                    niche.isEmpty() ? "programming tutorials" : niche + " tutorial",
                    niche.isEmpty() ? "best practices software development" : niche + " best practices"
            };

            for (String query : defaultQueries) {
                try {
                    List<WebSearchService.SearchResult> results = webSearchService.search(query, 5);
                    for (WebSearchService.SearchResult result : results) {
                        trends.add(new SearchTrend(
                                query,
                                result.title(),
                                result.content(),
                                calculateTrendScore(result)
                        ));
                    }
                } catch (Exception e) {
                    AGENT_LOG.debug("搜索失败: query={}", query, e);
                }
            }
        } catch (Exception e) {
            AGENT_LOG.warn("获取搜索趋势失败: {}", e.getMessage());
        }
        return trends;
    }

    private List<ContentOpportunity> analyzeOpportunities(List<String> existingTopics,
                                                         List<SearchTrend> trends) {
        List<ContentOpportunity> opportunities = new ArrayList<>();

        if (trends.isEmpty()) {
            return generateFallbackOpportunities();
        }

        String prompt = buildAnalysisPrompt(existingTopics, trends);

        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                opportunities = parseOpportunities(response.getContent(), trends);
            }
        } catch (Exception e) {
            AGENT_LOG.warn("LLM分析机会失败: {}", e.getMessage());
            opportunities = generateFallbackOpportunities();
        }

        if (opportunities.isEmpty()) {
            opportunities = generateFallbackOpportunities();
        }

        return opportunities;
    }

    private String buildAnalysisPrompt(List<String> existingTopics, List<SearchTrend> trends) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("分析以下搜索趋势，找出内容机会：\n\n");

        prompt.append("现有主题：\n");
        for (int i = 0; i < Math.min(20, existingTopics.size()); i++) {
            prompt.append("- ").append(existingTopics.get(i)).append("\n");
        }
        prompt.append("\n搜索趋势：\n");
        for (SearchTrend trend : trends.subList(0, Math.min(10, trends.size()))) {
            prompt.append("- 关键词: ").append(trend.getKeyword())
                    .append(", 标题: ").append(trend.getTitle())
                    .append("\n");
        }

        prompt.append("""
                \n请找出：
                1. 现有内容未覆盖的搜索需求
                2. 高搜索量但竞争度低的主题
                3. 新兴话题和趋势

                返回JSON数组格式：
                [{"title": "建议标题", "keywords": ["关键词1", "关键词2"], "searchVolume": "高/中/低", "competition": "高/中/低", "reason": "原因"}]
                """);

        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private List<ContentOpportunity> parseOpportunities(String content, List<SearchTrend> trends) {
        List<ContentOpportunity> opportunities = new ArrayList<>();
        try {
            int jsonStart = content.indexOf('[');
            int jsonEnd = content.lastIndexOf(']');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                List<Map<String, String>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonStr, List.class);

                for (Map<String, String> item : parsed) {
                    ContentOpportunity opp = new ContentOpportunity(
                            item.getOrDefault("title", ""),
                            parseList(item.getOrDefault("keywords", "[]")),
                            item.getOrDefault("searchVolume", "中"),
                            item.getOrDefault("competition", "中"),
                            item.getOrDefault("reason", ""),
                            calculateScore(item)
                    );
                    opportunities.add(opp);
                }
            }
        } catch (Exception e) {
            AGENT_LOG.debug("解析机会数据失败: {}", e.getMessage());
        }
        return opportunities;
    }

    private List<String> parseList(String str) {
        List<String> list = new ArrayList<>();
        try {
            list = new com.fasterxml.jackson.databind.ObjectMapper().readValue(str, List.class);
        } catch (Exception e) {
            String[] parts = str.replace("[", "").replace("]", "").split(",");
            for (String part : parts) {
                String trimmed = part.trim().replace("\"", "");
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    private List<ContentOpportunity> generateFallbackOpportunities() {
        return List.of(
                new ContentOpportunity("AI工具评测与对比", List.of("AI工具", "评测", "对比"),
                        "高", "中", "用户搜索需求明确，但高质量对比内容较少", 85.0),
                new ContentOpportunity("新手入门完整指南", List.of("教程", "入门", "指南"),
                        "高", "中", "搜索量大，结构化内容稀缺", 80.0),
                new ContentOpportunity("最新技术趋势解读", List.of("趋势", "技术", "解读"),
                        "中", "低", "竞争度低，可快速占据排名", 75.0),
                new ContentOpportunity("常见问题与解决方案", List.of("问题", "解决", "方案"),
                        "高", "低", "刚需内容，用户意图明确", 90.0),
                new ContentOpportunity("实战案例与经验分享", List.of("案例", "经验", "分享"),
                        "中", "中", "差异化内容空间大", 70.0)
        );
    }

    private double calculateTrendScore(WebSearchService.SearchResult result) {
        double score = 50.0;
        String snippet = result.content().toLowerCase();

        if (snippet.contains("new") || snippet.contains("latest") || snippet.contains("2024")) {
            score += 15;
        }
        if (snippet.contains("guide") || snippet.contains("tutorial")) {
            score += 10;
        }
        if (snippet.contains("best") || snippet.contains("top")) {
            score += 10;
        }

        return Math.min(100, score);
    }

    private double calculateScore(Map<String, String> item) {
        double score = 50.0;
        String volume = item.getOrDefault("searchVolume", "中");
        String competition = item.getOrDefault("competition", "中");

        score += switch (volume) {
            case "高" -> 30;
            case "中" -> 20;
            default -> 10;
        };

        score -= switch (competition) {
            case "高" -> 25;
            case "中" -> 15;
            default -> 5;
        };

        return Math.max(0, Math.min(100, score));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class SearchTrend {
        private String keyword;
        private String title;
        private String snippet;
        private double score;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ContentOpportunity {
        private String title;
        private List<String> keywords;
        private String searchVolume;
        private String competition;
        private String reason;
        private double score;
    }
}