package com.xingchen.backend.agent.content;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.service.ArticleGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ContentGenerationAgent extends BaseAgent {

    private final ArticleGenerationService articleGenerationService;
    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(ContentGenerationAgent.class);

    public ContentGenerationAgent(ArticleGenerationService articleGenerationService,
                                @Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("ContentGenerationAgent", Map.of("maxTokens", 4000, "temperature", 0.7));
        this.articleGenerationService = articleGenerationService;
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        String topic = (String) context.getParam("topic");
        if (topic == null || topic.isBlank()) {
            throw new AgentException("Topic is required", "MISSING_TOPIC");
        }

        Long userId = context.getUserId() != null ? context.getUserId() : (Long) context.getParam("userId", 0L);
        String category = (String) context.getParam("category", "");
        String tagsStr = (String) context.getParam("tags", "");
        List<String> keywords = parseTags(tagsStr);

        String content = generateContent(topic, keywords, context);
        String title = generateTitle(topic, content);
        String description = generateDescription(content);

        Map<String, Object> metadata = Map.of(
                "topic", topic,
                "title", title,
                "description", description,
                "contentLength", content.length(),
                "keywords", keywords
        );

        return AgentResult.success(metadata);
    }

    private String generateContent(String topic, List<String> keywords, AgentContext context) {
        String seoPrompt = buildSEOPrompt(topic, keywords);

        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(seoPrompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                return response.getContent();
            }
        } catch (Exception e) {
            AGENT_LOG.error("LLM生成内容失败: topic={}", topic, e);
        }

        return generateFallbackContent(topic, keywords);
    }

    private String buildSEOPrompt(String topic, List<String> keywords) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下主题撰写一篇SEO优化的文章：\n\n");
        prompt.append("主题：").append(topic).append("\n\n");

        if (keywords != null && !keywords.isEmpty()) {
            prompt.append("关键词：");
            for (String keyword : keywords) {
                prompt.append(keyword).append(", ");
            }
            prompt.append("\n");
        }

        prompt.append("""
                \n要求：
                1. 文章长度：2000-3000字
                2. 结构清晰，包含引言、正文、结论
                3. 在文章中自然融入关键词
                4. 使用Markdown格式
                5. 添加适当的标题层级（H1、H2、H3）
                6. 包含列表和代码块（如果适用）
                7. 文章要有深度和价值，避免泛泛而谈

                请直接输出文章内容：
                """);

        return prompt.toString();
    }

    private String generateTitle(String topic, String content) {
        String prompt = String.format("""
                根据以下文章内容生成一个SEO优化的标题：

                主题：%s
                内容摘要：%s

                要求：
                1. 标题长度：30-60字
                2. 包含核心关键词
                3. 吸引点击，有吸引力
                4. 不要使用疑问句

                直接输出标题，不要其他内容：
                """,
                topic,
                content.substring(0, Math.min(500, content.length())));

        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                String title = response.getContent().trim();
                if (title.length() >= 5 && title.length() <= 60) {
                    return title;
                }
            }
        } catch (Exception e) {
            AGENT_LOG.warn("LLM生成标题失败: {}", e.getMessage());
        }

        return topic + " - 完整指南";
    }

    private String generateDescription(String content) {
        String prompt = String.format("""
                根据以下文章内容生成一个SEO优化的描述（150字以内）：

                %s

                要求：
                1. 长度：100-150字
                2. 包含核心关键词
                3. 能吸引用户点击
                4. 概括文章主要内容

                直接输出描述，不要其他内容：
                """,
                content.substring(0, Math.min(1000, content.length())));

        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null && response.getContent() != null) {
                String description = response.getContent().trim();
                if (description.length() >= 50) {
                    return description.length() > 150 ? description.substring(0, 147) + "..." : description;
                }
            }
        } catch (Exception e) {
            AGENT_LOG.warn("LLM生成描述失败: {}", e.getMessage());
        }

        return content.substring(0, Math.min(140, content.length())) + "...";
    }

    private List<String> parseTags(String tagsStr) {
        List<String> tags = new ArrayList<>();
        if (tagsStr == null || tagsStr.isBlank()) {
            return tags;
        }

        String[] parts = tagsStr.split("[,，;；]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags;
    }

    private String generateFallbackContent(String topic, List<String> keywords) {
        StringBuilder content = new StringBuilder();
        content.append("# ").append(topic).append("\n\n");

        content.append("## 引言\n\n");
        content.append("本文将深入探讨").append(topic).append("，为您提供全面的指导和见解。\n\n");

        content.append("## 什么是").append(topic).append("\n\n");
        content.append("在开始深入之前，让我们先了解").append(topic).append("的基本概念。\n\n");

        content.append("## 核心要点\n\n");
        content.append("1. **理解基础**：掌握").append(topic).append("的核心原理\n");
        content.append("2. **实践应用**：将理论应用到实际场景中\n");
        content.append("3. **持续优化**：不断改进和提升\n\n");

        if (keywords != null && !keywords.isEmpty()) {
            content.append("## 关键词解读\n\n");
            for (String keyword : keywords) {
                content.append("- ").append(keyword).append("\n");
            }
            content.append("\n");
        }

        content.append("## 总结\n\n");
        content.append("希望通过本文，您对").append(topic).append("有了更深入的理解。如有任何问题，欢迎交流讨论。\n");

        return content.toString();
    }

    public record GenerationResult(
            String title,
            String description,
            String content,
            List<String> keywords,
            Map<String, Object> metadata
    ) {}
}