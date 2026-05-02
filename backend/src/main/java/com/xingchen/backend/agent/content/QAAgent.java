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
public class QAAgent extends BaseAgent {

    private final HybridSearchService hybridSearchService;
    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(QAAgent.class);

    public QAAgent(HybridSearchService hybridSearchService,
                   @Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("QAAgent", Map.of("maxTokens", 3000, "temperature", 0.3));
        this.hybridSearchService = hybridSearchService;
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        String question = (String) context.getParam("question");
        if (question == null || question.isBlank()) {
            question = (String) context.getInput();
        }

        if (question == null || question.isBlank()) {
            throw new AgentException("Question is required", "MISSING_QUESTION");
        }

        Integer topK = (Integer) context.getParam("topK", 5);
        Long userId = context.getUserId() != null ? context.getUserId() : (Long) context.getParam("userId", 0L);
        String sessionId = (String) context.getParam("sessionId");

        QAContext qaContext = buildQAContext(question, topK, sessionId);
        String answer = generateAnswer(qaContext);

        List<Map<String, Object>> sourcesData = qaContext.sources().stream()
                .map(s -> Map.<String, Object>of(
                        "articleId", s.articleId(),
                        "title", s.title(),
                        "content", s.content(),
                        "similarity", s.relevanceScore()
                ))
                .toList();

        Map<String, Object> data = Map.of(
                "question", question,
                "answer", answer,
                "sources", sourcesData,
                "confidence", qaContext.confidence(),
                "suggestedQuestions", qaContext.suggestedQuestions()
        );

        return AgentResult.success(data);
    }

    private QAContext buildQAContext(String question, int topK, String sessionId) {
        List<SourceContent> sources = new ArrayList<>();
        double confidence = 0.0;

        try {
            var searchResults = hybridSearchService.hybridSearch(question, topK);

            for (var result : searchResults) {
                sources.add(new SourceContent(
                        result.articleId(),
                        result.title(),
                        result.snippet() != null ? result.snippet() : "",
                        result.finalScore()
                ));
                confidence += result.finalScore();
            }

            if (!sources.isEmpty()) {
                confidence = confidence / sources.size();
            }
        } catch (Exception e) {
            AGENT_LOG.warn("检索失败: question={}", question, e);
        }

        List<String> suggestedQuestions = generateSuggestedQuestions(question, sources);

        return new QAContext(question, sources, confidence, suggestedQuestions);
    }

    private String generateAnswer(QAContext qaContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一位专业、耐心的技术博客问答助手。\n\n");

        if (!qaContext.sources().isEmpty()) {
            prompt.append("【相关博客文章】\n");
            prompt.append("==========\n");
            for (int i = 0; i < qaContext.sources().size(); i++) {
                SourceContent source = qaContext.sources().get(i);
                prompt.append(String.format("[%d] %s\n%s\n\n",
                        i + 1,
                        source.title(),
                        source.content()));
            }
            prompt.append("==========\n\n");
            prompt.append("请基于以上博客文章内容，准确回答用户问题。");
        } else {
            prompt.append("抱歉，我在博客中没有找到与您问题相关的内容。");
        }

        prompt.append("\n\n【用户问题】\n");
        prompt.append(qaContext.originalQuestion()).append("\n\n");

        if (!qaContext.sources().isEmpty()) {
            prompt.append("""
                请按以下格式回答：
                1. 直接回答问题
                2. 结合参考文章给出具体解释
                3. 如有相关链接建议，注明文章ID

                如果现有文章无法完全回答问题，请说明并给出基于常识的建议。
                """);
        }

        try {
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
            AGENT_LOG.error("生成回答失败", e);
        }

        return "抱歉，暂时无法回答您的问题，请稍后再试。";
    }

    private List<String> generateSuggestedQuestions(String originalQuestion, List<SourceContent> sources) {
        if (sources.isEmpty()) {
            return List.of();
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("基于用户问题「").append(originalQuestion).append("」及相关文章，");
        prompt.append("推荐3个用户可能想了解的跟进问题。\n\n");
        prompt.append("返回JSON数组格式：[\"问题1\", \"问题2\", \"问题3\"]");

        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt.toString())
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                return parseQuestions(response.getContent());
            }
        } catch (Exception e) {
            AGENT_LOG.warn("生成跟进问题失败", e);
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseQuestions(String content) {
        List<String> questions = new ArrayList<>();
        try {
            int jsonStart = content.indexOf('[');
            int jsonEnd = content.lastIndexOf(']');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                List<String> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonStr, List.class);
                questions.addAll(parsed);
            }
        } catch (Exception e) {
            AGENT_LOG.debug("解析跟进问题失败: {}", e.getMessage());
        }
        return questions;
    }

    public record QAContext(
            String originalQuestion,
            List<SourceContent> sources,
            double confidence,
            List<String> suggestedQuestions
    ) {}

    public record SourceContent(
            Long articleId,
            String title,
            String content,
            double relevanceScore
    ) {}
}
