package com.xingchen.backend.ai.intent;

import com.xingchen.backend.ai.model.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于正则的意图分类器
 * 最快，适合常见意图
 */
@Component
@Slf4j
public class RegexIntentClassifier implements IntentClassifierInterface {
    
    private final List<IntentPattern> patterns = List.of(
        // 创建文章
        new IntentPattern(
            Intent.IntentType.CREATE_ARTICLE,
            Pattern.compile(".*(写|生成|创作|draft).*(文章|article|blog|博客).*"),
            0.9
        ),
        // 编辑文章
        new IntentPattern(
            Intent.IntentType.EDIT_ARTICLE,
            Pattern.compile(".*(编辑|修改|update|edit).*(文章|article).*"),
            0.85
        ),
        // 发布文章
        new IntentPattern(
            Intent.IntentType.PUBLISH_ARTICLE,
            Pattern.compile(".*(发布|publish|上线).*(文章|article).*"),
            0.9
        ),
        // 定时任务
        new IntentPattern(
            Intent.IntentType.SCHEDULE_TASK,
            Pattern.compile(".*(定时|每天|每周|schedule|cron).*(任务|task|写文章).*"),
            0.85
        ),
        // 列出任务
        new IntentPattern(
            Intent.IntentType.LIST_TASKS,
            Pattern.compile(".*(列出|查看|显示|list|show).*(任务|task|所有任务).*"),
            0.8
        ),
        // 取消任务
        new IntentPattern(
            Intent.IntentType.CANCEL_TASK,
            Pattern.compile(".*(取消|删除|cancel|delete|stop).*(任务|task).*"),
            0.8
        ),
        // 搜索
        new IntentPattern(
            Intent.IntentType.SEARCH,
            Pattern.compile(".*(搜索|查找|search|find|query).*(文章|内容|信息).*"),
            0.75
        ),
        // 总结
        new IntentPattern(
            Intent.IntentType.SUMMARIZE,
            Pattern.compile(".*(总结|概括|summarize|summary).*(文章|内容|这段|这篇).*"),
            0.8
        ),
        // 翻译
        new IntentPattern(
            Intent.IntentType.TRANSLATE,
            Pattern.compile(".*(翻译|translate).*(成|到|to).*"),
            0.85
        ),
        // 代码生成
        new IntentPattern(
            Intent.IntentType.CODE_GENERATE,
            Pattern.compile(".*(写|生成|create).*(代码|code|程序|function).*"),
            0.8
        ),
        // 知识库查询
        new IntentPattern(
            Intent.IntentType.KNOWLEDGE_QUERY,
            Pattern.compile(".*(查询|搜索|find).*(知识库|knowledge|文档).*"),
            0.75
        )
    );
    
    @Override
    public Intent classify(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Intent.unknown(message, 0.0);
        }

        String lowerMessage = message.toLowerCase();

        for (IntentPattern pattern : patterns) {
            if (pattern.regex.matcher(lowerMessage).matches()) {
                log.debug("正则匹配意图: type={}, confidence={}", pattern.type, pattern.confidence);
                return Intent.builder()
                        .type(pattern.type)
                        .confidence(pattern.confidence)
                        .originalMessage(message)
                        .requiresMemory(pattern.type != Intent.IntentType.CHAT)
                        .requiresTool(isToolRequired(pattern.type))
                        .build();
            }
        }

        // 无匹配，返回低置信度聊天
        return Intent.unknown(message, 0.3);
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
        return "RegexClassifier";
    }
    
    @Override
    public boolean isAvailable() {
        // 基于正则的分类器始终可用
        return true;
    }
    
    private record IntentPattern(Intent.IntentType type, Pattern regex, double confidence) {}
}