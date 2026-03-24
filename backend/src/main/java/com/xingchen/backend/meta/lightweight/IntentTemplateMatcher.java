package com.xingchen.backend.meta.lightweight;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 意图-模板匹配器
 * 
 * 根据用户输入匹配最佳 Prompt 模板（< 50 行核心逻辑）
 */
@Component
public class IntentTemplateMatcher {
    
    // 模板注册表
    private final Map<String, TemplatePattern> patterns = new HashMap<>();
    
    public IntentTemplateMatcher() {
        // 预定义模板
        register("code", List.of("写代码", "代码", "编程", "function", "class"), 0.8);
        register("blog", List.of("写文章", "博客", "写作", "content"), 0.8);
        register("review", List.of("审查", "review", "检查", "优化"), 0.7);
        register("explain", List.of("解释", "说明", "什么是", "how to"), 0.6);
        register("chat", List.of(), 0.3); // 默认模板
    }
    
    /**
     * 匹配模板
     */
    public MatchResult match(String userInput) {
        String lower = userInput.toLowerCase();
        
        // 1. 关键词匹配
        for (Map.Entry<String, TemplatePattern> entry : patterns.entrySet()) {
            double score = calculateScore(lower, entry.getValue());
            if (score >= entry.getValue().threshold) {
                return MatchResult.builder()
                        .templateKey(entry.getKey())
                        .confidence(score)
                        .build();
            }
        }
        
        // 2. 默认模板
        return MatchResult.builder()
                .templateKey("chat")
                .confidence(0.5)
                .build();
    }
    
    private double calculateScore(String input, TemplatePattern pattern) {
        long matchCount = pattern.keywords.stream()
                .filter(input::contains)
                .count();
        return Math.min(matchCount * 0.3, 1.0);
    }
    
    private void register(String key, List<String> keywords, double threshold) {
        patterns.put(key, new TemplatePattern(keywords, threshold));
    }
    
    @Data
    @Builder
    public static class MatchResult {
        private String templateKey;
        private double confidence;
    }
    
    private record TemplatePattern(List<String> keywords, double threshold) {}
}