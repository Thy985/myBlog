package com.xingchen.backend.ai.intent;

import com.xingchen.backend.ai.model.Intent;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于Embedding的意图分类器
 * 使用向量相似度匹配，比正则更灵活
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingIntentClassifier implements IntentClassifierInterface {
    
    private final EmbeddingModel embeddingModel;
    
    // 意图示例和对应的Embedding
    private final Map<Intent.IntentType, List<float[]>> intentEmbeddings = new HashMap<>();
    
    // 意图示例语料
    private static final Map<Intent.IntentType, List<String>> INTENT_EXAMPLES = Map.of(
        Intent.IntentType.CREATE_ARTICLE, List.of(
            "帮我写一篇文章",
            "生成一篇技术博客",
            "创作一篇关于Java的文章",
            "draft a blog post",
            "write an article about spring boot"
        ),
        Intent.IntentType.EDIT_ARTICLE, List.of(
            "修改这篇文章",
            "编辑一下内容",
            "update the article",
            "改一下标题"
        ),
        Intent.IntentType.SCHEDULE_TASK, List.of(
            "每天自动写文章",
            "定时发布任务",
            "schedule a daily task",
            "每周生成一篇博客"
        ),
        Intent.IntentType.LIST_TASKS, List.of(
            "查看所有任务",
            "列出定时任务",
            "show all tasks",
            "有哪些任务在运行"
        ),
        Intent.IntentType.SEARCH, List.of(
            "搜索相关文章",
            "查找关于微服务的内容",
            "search for articles",
            "find blog posts"
        ),
        Intent.IntentType.SUMMARIZE, List.of(
            "总结这篇文章",
            "概括一下主要内容",
            "summarize this article",
            "给我个摘要"
        )
    );
    
    @PostConstruct
    public void init() {
        log.info("初始化Embedding意图分类器...");
        
        for (var entry : INTENT_EXAMPLES.entrySet()) {
            List<float[]> embeddings = new ArrayList<>();
            for (String example : entry.getValue()) {
                try {
                    float[] embedding = embeddingModel.embed(example).content().vector();
                    embeddings.add(embedding);
                } catch (Exception e) {
                    log.error("Embedding计算失败: {}", example, e);
                }
            }
            intentEmbeddings.put(entry.getKey(), embeddings);
        }
        
        log.info("Embedding意图分类器初始化完成");
    }
    
    @Override
    public Intent classify(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Intent.unknown(message, 0.0);
        }

        try {
            // 计算输入消息的Embedding
            float[] messageEmbedding = embeddingModel.embed(message).content().vector();

            // 计算与每个意图的相似度
            Intent.IntentType bestMatch = null;
            double bestScore = 0.0;

            for (var entry : intentEmbeddings.entrySet()) {
                double maxSimilarity = 0.0;
                for (float[] intentEmbedding : entry.getValue()) {
                    double similarity = cosineSimilarity(messageEmbedding, intentEmbedding);
                    maxSimilarity = Math.max(maxSimilarity, similarity);
                }

                if (maxSimilarity > bestScore) {
                    bestScore = maxSimilarity;
                    bestMatch = entry.getKey();
                }
            }

            // 阈值判断
            if (bestMatch != null && bestScore >= 0.75) {
                log.debug("Embedding匹配意图: type={}, score={}", bestMatch, bestScore);
                return Intent.builder()
                        .type(bestMatch)
                        .confidence(bestScore)
                        .originalMessage(message)
                        .requiresMemory(bestMatch != Intent.IntentType.CHAT)
                        .requiresTool(isToolRequired(bestMatch))
                        .build();
            }

            // 低于阈值，返回UNKNOWN
            return Intent.unknown(message, bestScore);

        } catch (Exception e) {
            log.error("Embedding分类失败", e);
            return Intent.unknown(message, 0.0);
        }
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private boolean isToolRequired(Intent.IntentType type) {
        return switch (type) {
            case UNKNOWN, CHAT -> false;
            case CREATE_ARTICLE, EDIT_ARTICLE, PUBLISH_ARTICLE,
                 SCHEDULE_TASK, LIST_TASKS, CANCEL_TASK -> true;
            default -> false;
        };
    }
    
    @Override
    public String getName() {
        return "EmbeddingClassifier";
    }
    
    @Override
    public boolean isAvailable() {
        // 检查embeddingModel是否可用
        return embeddingModel != null;
    }
}