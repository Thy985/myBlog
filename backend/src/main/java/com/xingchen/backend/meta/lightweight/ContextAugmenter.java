package com.xingchen.backend.meta.lightweight;

import com.xingchen.backend.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 临时上下文增强器
 * 
 * 动态扩展 RAG 检索范围（< 50 行核心逻辑）
 */
@Component
@RequiredArgsConstructor
public class ContextAugmenter {
    
    private final KnowledgeBaseService knowledgeBaseService;
    
    // 扩展策略
    private final Map<String, AugmentStrategy> strategies = Map.of(
            "code", new AugmentStrategy(
                    List.of("编程", "代码", "算法", "设计模式"),
                    3,  // 额外检索3篇
                    "code"  // 代码类文档
            ),
            "blog", new AugmentStrategy(
                    List.of("写作", "文章", "结构", "技巧"),
                    2,
                    "blog"
            ),
            "explain", new AugmentStrategy(
                    List.of("概念", "原理", "基础"),
                    2,
                    null  // 不限制分类
            )
    );
    
    /**
     * 增强上下文
     */
    public AugmentedContext augment(String query, String templateKey) {
        // 1. 基础检索
        String baseContext = knowledgeBaseService.search(query, 3);
        
        // 2. 检查是否需要扩展
        AugmentStrategy strategy = strategies.get(templateKey);
        if (strategy == null) {
            return new AugmentedContext(baseContext, false);
        }
        
        // 3. 扩展检索
        List<String> extendedContexts = new ArrayList<>();
        
        for (String keyword : strategy.relatedKeywords()) {
            String extended = knowledgeBaseService.search(keyword, strategy.extraCount());
            if (!extended.isEmpty()) {
                extendedContexts.add(extended);
            }
        }
        
        // 4. 合并上下文
        String combined = combineContexts(baseContext, extendedContexts);
        
        return new AugmentedContext(combined, !extendedContexts.isEmpty());
    }
    
    /**
     * 智能扩展（基于查询分析）
     */
    public AugmentedContext smartAugment(String query) {
        // 分析查询意图
        Set<String> detectedTopics = detectTopics(query);
        
        if (detectedTopics.isEmpty()) {
            return augment(query, "default");
        }
        
        // 为每个检测到的主题扩展
        List<String> contexts = new ArrayList<>();
        contexts.add(knowledgeBaseService.search(query, 3));
        
        for (String topic : detectedTopics) {
            AugmentStrategy strategy = strategies.get(topic);
            if (strategy != null) {
                for (String kw : strategy.relatedKeywords().subList(0, 2)) {
                    contexts.add(knowledgeBaseService.search(kw, 2));
                }
            }
        }
        
        return new AugmentedContext(
                combineContexts("", contexts),
                contexts.size() > 1
        );
    }
    
    private Set<String> detectTopics(String query) {
        String lower = query.toLowerCase();
        Set<String> topics = new HashSet<>();
        
        if (lower.contains("代码") || lower.contains("编程")) topics.add("code");
        if (lower.contains("文章") || lower.contains("写作")) topics.add("blog");
        if (lower.contains("解释") || lower.contains("什么是")) topics.add("explain");
        
        return topics;
    }
    
    private String combineContexts(String base, List<String> extensions) {
        StringBuilder sb = new StringBuilder();
        
        if (!base.isEmpty()) {
            sb.append("## 核心知识\n").append(base).append("\n\n");
        }
        
        if (!extensions.isEmpty()) {
            sb.append("## 扩展知识\n");
            for (int i = 0; i < extensions.size(); i++) {
                if (!extensions.get(i).isEmpty()) {
                    sb.append("[").append(i + 1).append("] ")
                      .append(extensions.get(i)).append("\n\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    // ========== 数据类 ==========
    
    private record AugmentStrategy(
            List<String> relatedKeywords,
            int extraCount,
            String categoryFilter
    ) {}
    
    public record AugmentedContext(
            String context,
            boolean augmented
    ) {}
}