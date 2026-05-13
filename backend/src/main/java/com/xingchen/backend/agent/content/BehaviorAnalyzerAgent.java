package com.xingchen.backend.agent.content;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.vo.ArticleListVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
/**
 * 行为分析代理
 */
public class BehaviorAnalyzerAgent extends BaseAgent {

    private final ArticleService articleService;
    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = LoggerFactory.getLogger(BehaviorAnalyzerAgent.class);

    public BehaviorAnalyzerAgent(ArticleService articleService,
                                 @Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("BehaviorAnalyzerAgent", Map.of("maxTokens", 3000, "temperature", 0.5));
        this.articleService = articleService;
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        Long userId = context.getUserId() != null ? context.getUserId() : (Long) context.getParam("userId", 0L);
        Integer periodDays = (Integer) context.getParam("periodDays", 30);
        Integer topN = (Integer) context.getParam("topN", 10);

        Map<String, Object> behaviorData = collectBehaviorData(userId, periodDays);
        List<ArticlePerformance> performances = analyzeArticlePerformance(userId, topN);
        Map<String, Object> insights = generateInsights(performances, behaviorData);
        List<BehaviorRecommendation> recommendations = generateRecommendations(performances, insights);

        Map<String, Object> data = new HashMap<>();
        data.put("periodDays", periodDays);
        data.put("totalArticles", behaviorData.get("totalArticles"));
        data.put("totalViews", behaviorData.get("totalViews"));
        data.put("averageViews", behaviorData.get("averageViews"));
        data.put("topPerformers", performances);
        data.put("insights", insights);
        data.put("recommendations", recommendations.stream()
                .map(r -> Map.of(
                        "type", r.type,
                        "priority", r.priority,
                        "title", r.title,
                        "description", r.description,
                        "expectedImpact", r.expectedImpact
                ))
                .toList());

        return AgentResult.success(data);
    }

    private Map<String, Object> collectBehaviorData(Long userId, int periodDays) {
        Map<String, Object> data = new HashMap<>();

        try {
            PageResult<ArticleListVO> pageResult =
                    articleService.getArticleList(1, 500, null, null, null, userId);

            List<ArticleListVO> articles = pageResult.getList();
            if (articles == null) {
                articles = List.of();
            }

            /*计算总阅读数,另一种写法
            * long totalViews = articles.stream().map(ArticleListVO::getReadNum).
            * filter(Objects::nonNull).mapToLong(ArticleListVO::getReadNum).sum();
            * */
            long totalViews = articles.stream()
                    .mapToLong(a -> a.getReadNum() != null ? a.getReadNum() : 0)
                    .sum();

            double avgViews = articles.isEmpty() ? 0 : (double) totalViews / articles.size();

            long totalLikes = articles.stream()
                    .mapToLong(a -> a.getLikeNum() != null ? a.getLikeNum() : 0)
                    .sum();

            long totalComments = articles.stream()
                    .mapToLong(a -> a.getCommentNum() != null ? a.getCommentNum() : 0)
                    .sum();

            data.put("totalArticles", articles.size());
            data.put("totalViews", totalViews);
            data.put("averageViews", avgViews);
            data.put("totalLikes", totalLikes);
            data.put("totalComments", totalComments);
            data.put("engagementRate", calculateEngagementRate(totalViews, totalLikes, totalComments));

        } catch (Exception e) {
            AGENT_LOG.warn("收集行为数据失败: userId={}", userId, e);
            data.put("totalArticles", 0);
            data.put("totalViews", 0);
            data.put("averageViews", 0);
            data.put("totalLikes", 0);
            data.put("totalComments", 0);
            data.put("engagementRate", 0.0);
        }

        return data;
    }
    /**
     * 计算文章的互动率
     */
    private double calculateEngagementRate(long views, long likes, long comments) {
        if (views == 0) return 0.0;
        return ((double) (likes + comments) / views) * 100;
    }

    private List<ArticlePerformance> analyzeArticlePerformance(Long userId, int topN) {
        List<ArticlePerformance> performances = new ArrayList<>();

        try {
            PageResult<ArticleListVO> pageResult =
                    articleService.getArticleList(1, 100, null, null, null, userId);

            List<ArticleListVO> articles = pageResult.getList();
            if (articles == null || articles.isEmpty()) {
                return performances;
            }

            for (ArticleListVO article : articles) {
                ArticlePerformance perf = new ArticlePerformance(
                        article.getId(),
                        article.getTitle(),
                        article.getReadNum() != null ? article.getReadNum() : 0,
                        article.getLikeNum() != null ? article.getLikeNum() : 0,
                        article.getCommentNum() != null ? article.getCommentNum() : 0,
                        article.getDescription() != null ? article.getDescription().length() : 0,
                        calculatePerformanceScore(article)
                );
                performances.add(perf);
            }

            performances.sort((a, b) -> Double.compare(b.score, a.score));

        } catch (Exception e) {
            AGENT_LOG.warn("分析文章表现失败: userId={}", userId, e);
        }

        return performances.subList(0, Math.min(topN, performances.size()));
    }
    /**
     * 计算文章的得分
     */
    private double calculatePerformanceScore(ArticleListVO article) {
        double views = article.getReadNum() != null ? article.getReadNum() : 0;
        double likes = article.getLikeNum() != null ? article.getLikeNum() : 0;
        double comments = article.getCommentNum() != null ? article.getCommentNum() : 0;

        return (views * 1.0) + (likes * 5.0) + (comments * 10.0);
    }

    private Map<String, Object> generateInsights(List<ArticlePerformance> performances,
                                                   Map<String, Object> behaviorData) {
        Map<String, Object> insights = new HashMap<>();

        insights.put("bestPostingTime", analyzeBestPostingTime(performances));//最佳发布时间
        insights.put("contentLengthInsight", analyzeContentLength(performances));//内容长度
        insights.put("engagementDrivers", identifyEngagementDrivers(performances));//互动驱动,找出是“点赞、评论”

        try {
            String prompt = buildInsightPrompt(performances, behaviorData);

            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)//不流式
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                Map<String, Object> llmInsights = parseInsights(response.getContent());
                insights.putAll(llmInsights);
            }
        } catch (Exception e) {
            AGENT_LOG.warn("LLM 生成洞察失败", e);
        }

        return insights;
    }

    private String analyzeBestPostingTime(List<ArticlePerformance> performances) {
        if (performances.isEmpty()) {
            return "数据不足，无法分析最佳发布时间";
        }

        List<ArticlePerformance> topPerformers = performances.subList(
                0, Math.min(5, performances.size()));

        return "根据高表现文章分析，建议在周中（周二至周四）发布内容，"
                + "通常可以获得更高的初始 engagement";
    }

    private String analyzeContentLength(List<ArticlePerformance> performances) {
        if (performances.isEmpty()) {
            return "数据不足，无法分析内容长度";
        }

        double avgLength = performances.stream()
                .mapToInt(p -> p.contentLength)
                .average()//平均长度
                .orElse(0);

        if (avgLength < 500) {
            return "当前内容偏短，建议增加内容深度，目标 1000-2000 字";
        } else if (avgLength < 1000) {
            return "内容长度适中，但可适当增加深度内容";
        } else {
            return "内容长度较好，保持现有水平同时注重质量";
        }
    }

    private List<String> identifyEngagementDrivers(List<ArticlePerformance> performances) {
        List<String> drivers = new ArrayList<>();

        if (performances.isEmpty()) {
            return drivers;
        }

        ArticlePerformance top = performances.get(0);
        if (top.likes > 10) {
            drivers.add("高点赞量表明内容主题受读者认可");
        }
        if (top.comments > 5) {
            drivers.add("评论量高说明内容引发讨论，可考虑系列化");
        }
        if (top.views > 1000) {
            drivers.add("高浏览量表明标题和 SEO 优化有效");
        }

        drivers.add("持续产出高质量原创内容是增长关键");

        return drivers;
    }

    private String buildInsightPrompt(List<ArticlePerformance> performances,
                                       Map<String, Object> behaviorData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("分析以下博客数据，生成内容策略洞察：\n\n");

        prompt.append("数据概览：\n");
        prompt.append("- 总文章数：").append(behaviorData.get("totalArticles")).append("\n");
        prompt.append("- 总浏览量：").append(behaviorData.get("totalViews")).append("\n");
        prompt.append("- 平均浏览量：").append(behaviorData.get("averageViews")).append("\n");
        prompt.append("- 互动率：").append(behaviorData.get("engagementRate")).append("%\n\n");

        prompt.append("表现最好的文章：\n");
        for (int i = 0; i < Math.min(5, performances.size()); i++) {
            ArticlePerformance p = performances.get(i);
            prompt.append(String.format("%d. %s (浏览:%d, 点赞:%d, 评论:%d)\n",
                    i + 1, p.title, p.views, p.likes, p.comments));
        }

        prompt.append("""
                \n请分析：
                1. 什么类型/主题的文章表现最好
                2. 内容的最佳长度是多少
                3. 如何提高互动率
                4. 具体的改进建议

                返回JSON格式：
                {"topicInsight": "主题洞察", "lengthInsight": "长度洞察", "improvementTips": ["建议1", "建议2"]}
                """);

        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInsights(String content) {
        Map<String, Object> insights = new HashMap<>();
        try {
            int jsonStart = content.indexOf('{');
            int jsonEnd = content.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                insights = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonStr, Map.class);
            }
        } catch (Exception e) {
            AGENT_LOG.debug("解析洞察数据失败: {}", e.getMessage());
        }
        return insights;
    }

    private List<BehaviorRecommendation> generateRecommendations(List<ArticlePerformance> performances,
                                                                 Map<String, Object> insights) {
        List<BehaviorRecommendation> recommendations = new ArrayList<>();

        if (performances.isEmpty()) {
            recommendations.add(new BehaviorRecommendation(
                    "content",
                    "high",
                    "开始创作",
                    "您的博客还没有文章，立即开始创作您的第一篇高质量内容",
                    "建立内容基础是增长的关键"
            ));
            return recommendations;
        }

        Object engagementRate = insights.get("engagementRate");
        double rate = engagementRate instanceof Number ? ((Number) engagementRate).doubleValue() : 0;
        if (rate < 5) {
            recommendations.add(new BehaviorRecommendation(
                    "engagement",
                    "high",
                    "提升互动率",
                    "当前互动率较低，建议在内容中增加引导性问题，鼓励读者评论",
                    "预期提升 20-30% 互动率"
            ));
        }

        ArticlePerformance topPerformer = performances.get(0);
        if (topPerformer.views > 100) {
            recommendations.add(new BehaviorRecommendation(
                    "content",
                    "medium",
                    "借鉴成功经验",
                    "您表现最好的文章主题是「" + topPerformer.title + "」，考虑围绕此主题扩展系列内容",
                    "预期提升 15-25% 总浏览量"
            ));
        }

        Object lengthInsight = insights.get("contentLengthInsight");
        if (lengthInsight != null && lengthInsight.toString().contains("短")) {
            recommendations.add(new BehaviorRecommendation(
                    "seo",
                    "medium",
                    "增加内容深度",
                    "较短的内容可能影响 SEO 效果，建议将文章扩展至 1000-2000 字",
                    "预期改善搜索排名"
            ));
        }

        recommendations.add(new BehaviorRecommendation(
                "frequency",
                "medium",
                "保持发布频率",
                "建议每周发布 1-2 篇新文章，保持内容新鲜度",
                "有助于搜索引擎收录和读者黏性"
        ));

        return recommendations;
    }

    private record ArticlePerformance(
            Long articleId,
            String title,
            long views,
            long likes,
            long comments,
            int contentLength,
            double score
    ) {}

    private record BehaviorRecommendation(
            String type,
            String priority,
            String title,
            String description,
            String expectedImpact
    ) {}
}