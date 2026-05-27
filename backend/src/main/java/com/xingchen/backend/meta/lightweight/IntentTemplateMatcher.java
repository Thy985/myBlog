package com.xingchen.backend.meta.lightweight;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 意图-模板匹配器
 *
 * 根据用户输入匹配最佳 Prompt 模板
 * 算法：加权关键词匹配 + 长度惩罚 + 首次匹配优先
 */
@Component
public class IntentTemplateMatcher {

    // 关键词权重（高频词权重更高）
    private static final Map<String, Double> KEYWORD_WEIGHTS = Map.of(
            "写代码", 1.8, "代码审查", 1.8, "code review", 1.8, "代码有问题", 1.8,
            "写文章", 1.5, "写博客", 1.5, "博客文章", 1.5, "文章", 1.5,
            "写一个", 1.5, "实现", 1.5, "函数", 1.5, "接口", 1.5,
            "什么是", 1.3, "介绍一下", 1.3, "怎么实现", 1.3,
            "编程", 1.2, "function", 1.2, "class", 1.2,
            "代码", 1.0, "编程", 1.0, "程序", 1.0,
            "审查", 1.0, "review", 1.0, "检查", 1.0, "优化", 1.0,
            "解释", 0.9, "说明", 0.9, "how to", 0.9,
            "文档", 0.9, "doc", 0.9, "写文档", 0.9, "接口文档", 0.9,
            "博客", 0.9, "content", 0.9, "写作", 0.9, "发帖", 0.9,
            "生成", 0.8, "创建", 0.8, "新增", 0.8
    );

    // 模板注册表（模板key -> 核心关键词列表）
    private final Map<String, List<String>> patterns = new LinkedHashMap<>();

    public IntentTemplateMatcher() {
        // 代码类
        patterns.put("code", List.of("写代码", "代码", "编程", "function", "class", "实现", "写一个", "函数", "接口"));
        // 博客写作
        patterns.put("blog", List.of("写文章", "博客", "写作", "content", "文章", "发帖", "写博客"));
        // 代码审查
        patterns.put("code_review", List.of("审查", "review", "检查", "优化", "代码审查", "code review"));
        // 解释说明
        patterns.put("explain", List.of("解释", "说明", "什么是", "how to", "介绍一下", "怎么实现"));
        // 文档生成
        patterns.put("doc", List.of("文档", "doc", "写文档", "生成文档", "接口文档"));
        // 创意写作
        patterns.put("creative", List.of("故事", "小说", "诗歌", "创意", "文案", "营销"));
        // RAG问答
        patterns.put("rag", List.of("根据知识库", "基于文档", "参考", "相关资料"));
    }

    /**
     * 匹配模板
     */
    public MatchResult match(String userInput) {
        String lower = userInput.toLowerCase();

        String bestKey = null;
        double bestScore = 0.0;

        // 遍历所有模式找最佳匹配
        for (Map.Entry<String, List<String>> entry : patterns.entrySet()) {
            double score = calculateWeightedScore(lower, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                bestKey = entry.getKey();
            }
        }

        // 未匹配到返回 chat 默认模板
        if (bestKey == null || bestScore < 0.3) {
            return MatchResult.builder()
                    .templateKey("chat")
                    .confidence(0.5)
                    .build();
        }

        return MatchResult.builder()
                .templateKey(bestKey)
                .confidence(Math.min(bestScore, 1.0))
                .build();
    }

    /**
     * 加权评分算法
     * - 匹配关键词权重
     * - 长度惩罚（输入过长降低置信度）
     */
    private double calculateWeightedScore(String input, List<String> keywords) {
        double score = 0.0;

        for (String keyword : keywords) {
            if (input.contains(keyword.toLowerCase())) {
                // 使用预定义权重，默认为 1.0
                score += KEYWORD_WEIGHTS.getOrDefault(keyword, 1.0);
            }
        }

        // 长度惩罚：输入过长（>50字）降低 20% 置信度
        if (input.length() > 50) {
            score *= 0.8;
        }

        return score;
    }

    @Data
    @Builder
    public static class MatchResult {
        private String templateKey;
        private double confidence;
    }
}