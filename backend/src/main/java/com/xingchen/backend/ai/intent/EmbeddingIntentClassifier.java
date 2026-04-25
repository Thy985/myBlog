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
    private static final Map<Intent.IntentType, List<String>> INTENT_EXAMPLES;
    
    static {
        INTENT_EXAMPLES = new HashMap<>();
        INTENT_EXAMPLES.put(Intent.IntentType.CREATE_ARTICLE, List.of(
            "帮我写一篇文章",
            "生成一篇技术博客",
            "创作一篇关于Java的文章",
            "draft a blog post",
            "write an article about spring boot"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.EDIT_ARTICLE, List.of(
            "修改这篇文章",
            "编辑一下内容",
            "update the article",
            "改一下标题"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.PUBLISH_ARTICLE, List.of(
            "发布文章",
            "publish the article",
            "将文章发布出去"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.DELETE_ARTICLE, List.of(
            "删除文章",
            "delete the article",
            "remove this article",
            "删除 ID 为 123 的文章"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.LIST_CATEGORIES, List.of(
            "查看我的分类列表",
            "列出我的分类",
            "show my categories",
            "有哪些分类",
            "获取分类列表"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.CREATE_CATEGORY, List.of(
            "创建一个分类",
            "新建分类",
            "add a new category",
            "新增文章分类"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.LIST_TAGS, List.of(
            "查看我的标签列表",
            "列出我的标签",
            "show my tags",
            "有哪些标签",
            "获取标签列表"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.CREATE_TAG, List.of(
            "创建一个标签",
            "新建标签",
            "add a new tag",
            "新增文章标签"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.LIST_ARTICLES, List.of(
            "查看我的文章列表",
            "列出我的文章",
            "show my articles",
            "我的所有文章",
            "获取文章列表"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.SEARCH_ARTICLES, List.of(
            "搜索相关文章",
            "查找关于微服务的内容",
            "search for articles",
            "find blog posts"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.SCHEDULE_TASK, List.of(
            "每天自动写文章",
            "定时发布任务",
            "schedule a daily task",
            "每周生成一篇博客"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.LIST_TASKS, List.of(
            "查看所有任务",
            "列出定时任务",
            "show all tasks",
            "有哪些任务在运行"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.SEARCH, List.of(
            "搜索相关内容",
            "查找某个内容",
            "search for content",
            "find something"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.SUMMARIZE, List.of(
            "总结这篇文章",
            "概括一下主要内容",
            "summarize this article",
            "给我个摘要"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.CONTENT_AUDIT, List.of(
            "审计我的文章内容",
            "检查文章质量",
            "analyze my articles quality",
            "发现重复或低质量文章",
            "内容审计"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.OPTIMIZE_ARTICLE, List.of(
            "优化文章质量",
            "提升文章内容",
            "improve my article",
            "优化我的文章"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.CONTENT_OPPORTUNITY, List.of(
            "发现内容机会",
            "找值得写的主题",
            "find topics to write about",
            "内容缺口分析",
            "我应该写什么文章"
        ));
        INTENT_EXAMPLES.put(Intent.IntentType.ONLINE_SEARCH, List.of(
            "搜索最新的技术趋势",
            "search online for latest news",
            "联网搜索",
            "实时搜索",
            "search the internet"
        ));
    }
    
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
                boolean requiresTool = isToolRequired(bestMatch);
                List<String> tools = requiresTool ? INTENT_TOOLS.getOrDefault(bestMatch, List.of()) : List.of();
                return Intent.builder()
                        .type(bestMatch)
                        .confidence(bestScore)
                        .originalMessage(message)
                        .requiresMemory(bestMatch != Intent.IntentType.CHAT)
                        .requiresTool(requiresTool)
                        .possibleTools(tools)
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

    // 意图类型到工具的映射
    private static final Map<Intent.IntentType, List<String>> INTENT_TOOLS;
    
    static {
        INTENT_TOOLS = new HashMap<>();
        INTENT_TOOLS.put(Intent.IntentType.CREATE_ARTICLE, List.of("article_generator"));
        INTENT_TOOLS.put(Intent.IntentType.EDIT_ARTICLE, List.of("article_update"));
        INTENT_TOOLS.put(Intent.IntentType.PUBLISH_ARTICLE, List.of("article_publish"));
        INTENT_TOOLS.put(Intent.IntentType.DELETE_ARTICLE, List.of("article_delete"));
        INTENT_TOOLS.put(Intent.IntentType.LIST_ARTICLES, List.of("article_query"));
        INTENT_TOOLS.put(Intent.IntentType.SEARCH_ARTICLES, List.of("article_query"));
        INTENT_TOOLS.put(Intent.IntentType.LIST_CATEGORIES, List.of("category_manager"));
        INTENT_TOOLS.put(Intent.IntentType.CREATE_CATEGORY, List.of("category_manager"));
        INTENT_TOOLS.put(Intent.IntentType.EDIT_CATEGORY, List.of("category_manager"));
        INTENT_TOOLS.put(Intent.IntentType.DELETE_CATEGORY, List.of("category_manager"));
        INTENT_TOOLS.put(Intent.IntentType.LIST_TAGS, List.of("tag_manager"));
        INTENT_TOOLS.put(Intent.IntentType.CREATE_TAG, List.of("tag_manager"));
        INTENT_TOOLS.put(Intent.IntentType.EDIT_TAG, List.of("tag_manager"));
        INTENT_TOOLS.put(Intent.IntentType.DELETE_TAG, List.of("tag_manager"));
        INTENT_TOOLS.put(Intent.IntentType.CONTENT_AUDIT, List.of("content_audit"));
        INTENT_TOOLS.put(Intent.IntentType.CONTENT_OPPORTUNITY, List.of("content_opportunity"));
        INTENT_TOOLS.put(Intent.IntentType.ONLINE_SEARCH, List.of("tavily_search"));
        INTENT_TOOLS.put(Intent.IntentType.OPTIMIZE_ARTICLE, List.of("article_update", "content_audit"));
        INTENT_TOOLS.put(Intent.IntentType.CODE_GENERATE, List.of("code-executor"));
        INTENT_TOOLS.put(Intent.IntentType.KNOWLEDGE_QUERY, List.of("hybrid-search"));
    }

    private boolean isToolRequired(Intent.IntentType type) {
        return switch (type) {
            case UNKNOWN, CHAT -> false;
            case CREATE_ARTICLE, EDIT_ARTICLE, PUBLISH_ARTICLE,
                 DELETE_ARTICLE, LIST_ARTICLES, SEARCH_ARTICLES,
                 CREATE_CATEGORY, EDIT_CATEGORY, DELETE_CATEGORY, LIST_CATEGORIES,
                 CREATE_TAG, EDIT_TAG, DELETE_TAG, LIST_TAGS,
                 CONTENT_AUDIT, CONTENT_OPPORTUNITY, OPTIMIZE_ARTICLE,
                 ONLINE_SEARCH,
                 SCHEDULE_TASK, LIST_TASKS, CANCEL_TASK,
                 CODE_GENERATE, KNOWLEDGE_QUERY -> true;
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