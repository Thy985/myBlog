package com.xingchen.backend.agent.content;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.repository.ArticleVectorRepository;
import com.xingchen.backend.vector.HybridSearchService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class RAGEnhancedAgent extends BaseAgent {

    private final HybridSearchService hybridSearchService;
    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(RAGEnhancedAgent.class);

    public RAGEnhancedAgent(HybridSearchService hybridSearchService,
                          @Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("RAGEnhancedAgent", Map.of("maxTokens", 4000, "temperature", 0.5));
        this.hybridSearchService = hybridSearchService;
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        String query = (String) context.getParam("query");
        if (query == null || query.isBlank()) {
            query = (String) context.getInput();
        }

        if (query == null || query.isBlank()) {
            throw new AgentException("Query is required", "MISSING_QUERY");
        }

        Integer topK = (Integer) context.getParam("topK", 5);
        Long userId = context.getUserId() != null ? context.getUserId() : (Long) context.getParam("userId", 0L);

        List<RelevantContent> relevantContents = retrieveRelevantContent(query, topK);
        String prompt = buildPromptWithContext(query, relevantContents);

        String response = generateResponse(prompt);

        Map<String, Object> data = Map.of(
                "query", query,
                "relevantContents", relevantContents.stream()
                        .map(c -> Map.of(
                                "articleId", c.articleId(),
                                "content", c.content(),
                                "similarity", c.similarity()
                        ))
                        .toList(),
                "response", response,
                "contextUsed", !relevantContents.isEmpty()
        );

        return AgentResult.success(data);
    }

    public List<RelevantContent> retrieveRelevantContent(String query, int topK) {
        try {
            var results = hybridSearchService.hybridSearch(query, topK);

            return results.stream()
                    .map(r -> new RelevantContent(
                            r.articleId(),
                            r.snippet() != null ? r.snippet() : r.title(),
                            r.finalScore()
                    ))
                    .toList();
        } catch (Exception e) {
            AGENT_LOG.warn("检索相关内容失败: query={}", query, e);
            return List.of();
        }
    }

    private String buildPromptWithContext(String query, List<RelevantContent> relevantContents) {
        StringBuilder prompt = new StringBuilder();

        if (!relevantContents.isEmpty()) {
            prompt.append("参考信息：\n");
            prompt.append("==========\n");
            for (int i = 0; i < relevantContents.size(); i++) {
                RelevantContent content = relevantContents.get(i);
                prompt.append(String.format("[%d] 文章ID=%d (相似度: %.2f):\n%s\n\n",
                        i + 1,
                        content.articleId(),
                        content.similarity(),
                        content.content()));
            }
            prompt.append("==========\n\n");
        }

        prompt.append("用户问题：").append(query).append("\n\n");

        prompt.append("""
                请根据参考信息回答用户问题。如果参考信息中有相关内容，请结合参考信息给出准确的回答。
                如果参考信息不足以回答问题，请说明并给出基于常识的回答。
                """);

        return prompt.toString();
    }

    private String generateResponse(String prompt) {
        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                return response.getContent();
            }
        } catch (Exception e) {
            AGENT_LOG.error("生成RAG响应失败", e);
        }

        return "抱歉，暂时无法回答您的问题。";
    }

    public String generateContentWithRAG(String topic, String keywords) {
        try {
            String query = topic + " " + keywords;
            List<RelevantContent> relevantContents = retrieveRelevantContent(query, 5);

            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一位专业的内容创作者。\n\n");

            if (!relevantContents.isEmpty()) {
                prompt.append("参考现有文章内容：\n");
                prompt.append("==========\n");
                for (RelevantContent content : relevantContents) {
                    prompt.append("- ").append(content.content()).append("\n");
                }
                prompt.append("==========\n\n");
            }

            prompt.append("请为主题「").append(topic).append("」撰写一篇SEO优化的文章。\n");
            prompt.append("关键词：").append(keywords).append("\n\n");

            prompt.append("要求：\n");
            prompt.append("1. 2000-3000字\n");
            prompt.append("2. 结构清晰，包含引言、正文、结论\n");
            prompt.append("3. 自然融入关键词\n");
            prompt.append("4. Markdown格式\n");
            prompt.append("5. 结合参考内容，避免重复\n");

            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt.toString())
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                return response.getContent();
            }
        } catch (Exception e) {
            AGENT_LOG.error("RAG增强内容生成失败: topic={}", topic, e);
        }

        return null;
    }

    public String optimizeContentWithRAG(String originalContent, String optimizationGoal) {
        try {
            List<RelevantContent> relevantContents = retrieveRelevantContent(optimizationGoal, 3);

            StringBuilder prompt = new StringBuilder();
            prompt.append("原始内容：\n");
            prompt.append("==========\n");
            prompt.append(originalContent).append("\n");
            prompt.append("==========\n\n");

            if (!relevantContents.isEmpty()) {
                prompt.append("参考优化方向：\n");
                for (RelevantContent content : relevantContents) {
                    prompt.append("- ").append(content.content()).append("\n");
                }
                prompt.append("\n");
            }

            prompt.append("优化目标：").append(optimizationGoal).append("\n\n");
            prompt.append("请基于原始内容和优化目标，生成优化后的内容。");

            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt.toString())
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                return response.getContent();
            }
        } catch (Exception e) {
            AGENT_LOG.error("RAG增强内容优化失败", e);
        }

        return originalContent;
    }

    public List<String> suggestRelatedTopics(String topic, int limit) {
        try {
            List<RelevantContent> contents = retrieveRelevantContent(topic, limit);

            StringBuilder prompt = new StringBuilder();
            prompt.append("基于以下主题「").append(topic).append("」，推荐相关的文章主题：\n\n");

            for (RelevantContent content : contents) {
                prompt.append("- ").append(content.content()).append("\n");
            }

            prompt.append("\n请推荐").append(limit).append("个相关但不同角度的主题。");
            prompt.append("返回JSON数组格式：[{\"title\": \"主题\", \"reason\": \"推荐理由\"}]");

            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt.toString())
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                return parseRelatedTopics(response.getContent());
            }
        } catch (Exception e) {
            AGENT_LOG.error("推荐相关主题失败: topic={}", topic, e);
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseRelatedTopics(String content) {
        List<String> topics = new ArrayList<>();
        try {
            int jsonStart = content.indexOf('[');
            int jsonEnd = content.lastIndexOf(']');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                List<Map<String, String>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonStr, List.class);

                for (Map<String, String> item : parsed) {
                    topics.add(item.getOrDefault("title", ""));
                }
            }
        } catch (Exception e) {
            AGENT_LOG.debug("解析相关主题失败: {}", e.getMessage());
        }
        return topics;
    }

    public record RelevantContent(
            Long articleId,
            String content,
            double similarity
    ) {}
}