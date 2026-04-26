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
            Pattern.compile(".*(写|生成|创作|draft|发一个|发表|发).*(文章|article|blog|博客|博文|帖子).*"),
            0.9,
            List.of("article_generator")
        ),
        // 编辑文章
        new IntentPattern(
            Intent.IntentType.EDIT_ARTICLE,
            Pattern.compile(".*(编辑|修改|更新|update|edit).*(文章|article).*"),
            0.85,
            List.of("article_update")
        ),
        // 删除文章
        new IntentPattern(
            Intent.IntentType.DELETE_ARTICLE,
            Pattern.compile(".*(删除|remove|delete).*(文章|article).*"),
            0.9,
            List.of("article_delete")
        ),
        // 发布文章
        new IntentPattern(
            Intent.IntentType.PUBLISH_ARTICLE,
            Pattern.compile(".*(发布|publish|上线).*(文章|article|博客|博文|帖子).*"),
            0.9,
            List.of("article_publish")
        ),
        // 列出我的分类
        new IntentPattern(
            Intent.IntentType.LIST_CATEGORIES,
            Pattern.compile(".*(列出|查看|显示|list|show).*(分类|category|分类列表).*"),
            0.85,
            List.of("category_manager")
        ),
        // 创建分类
        new IntentPattern(
            Intent.IntentType.CREATE_CATEGORY,
            Pattern.compile(".*(创建|新建|新增|add).*(分类|category).*"),
            0.85,
            List.of("category_manager")
        ),
        // 编辑分类
        new IntentPattern(
            Intent.IntentType.EDIT_CATEGORY,
            Pattern.compile(".*(编辑|修改|更新|edit).*(分类|category).*"),
            0.85,
            List.of("category_manager")
        ),
        // 创建标签
        new IntentPattern(
            Intent.IntentType.CREATE_TAG,
            Pattern.compile(".*(创建|新建|新增|add).*(标签|tag).*"),
            0.85,
            List.of("tag_manager")
        ),
        // 编辑标签
        new IntentPattern(
            Intent.IntentType.EDIT_TAG,
            Pattern.compile(".*(编辑|修改|更新|edit).*(标签|tag).*"),
            0.85,
            List.of("tag_manager")
        ),
        // 列出我的标签
        new IntentPattern(
            Intent.IntentType.LIST_TAGS,
            Pattern.compile(".*(列出|查看|显示|list|show).*(标签|tag|标签列表).*"),
            0.85,
            List.of("tag_manager")
        ),
        // 创建标签
        new IntentPattern(
            Intent.IntentType.CREATE_TAG,
            Pattern.compile(".*(创建|新建|新增|add).*(标签|tag).*"),
            0.85,
            List.of("tag_manager")
        ),
        // 列出我的文章
        new IntentPattern(
            Intent.IntentType.LIST_ARTICLES,
            Pattern.compile(".*(列出|查看|显示|list|show).*(我的)?(文章|article|博客).*"),
            0.8,
            List.of("article_query")
        ),
        // 搜索文章
        new IntentPattern(
            Intent.IntentType.SEARCH_ARTICLES,
            Pattern.compile(".*(搜索|查找|search|find).*(文章|article|博客).*"),
            0.8,
            List.of("article_query")
        ),
        // 定时任务
        new IntentPattern(
            Intent.IntentType.SCHEDULE_TASK,
            Pattern.compile(".*(定时|每天|每周|schedule|cron).*(任务|task|写文章).*"),
            0.85,
            List.of()
        ),
        // 列出任务
        new IntentPattern(
            Intent.IntentType.LIST_TASKS,
            Pattern.compile(".*(列出|查看|显示|list|show).*(任务|task|所有任务).*"),
            0.8,
            List.of()
        ),
        // 取消任务
        new IntentPattern(
            Intent.IntentType.CANCEL_TASK,
            Pattern.compile(".*(取消|删除|cancel|delete|stop).*(任务|task).*"),
            0.8,
            List.of()
        ),
        // 总结
        new IntentPattern(
            Intent.IntentType.SUMMARIZE,
            Pattern.compile(".*(总结|概括|summarize|summary).*(文章|内容|这段|这篇).*"),
            0.8,
            List.of()
        ),
        // 翻译
        new IntentPattern(
            Intent.IntentType.TRANSLATE,
            Pattern.compile(".*(翻译|translate).*(成|到|to).*"),
            0.85,
            List.of()
        ),
        // 代码生成
        new IntentPattern(
            Intent.IntentType.CODE_GENERATE,
            Pattern.compile(".*(写|生成|create).*(代码|code|程序|function).*"),
            0.8,
            List.of("code-executor")
        ),
        // 知识库查询
        new IntentPattern(
            Intent.IntentType.KNOWLEDGE_QUERY,
            Pattern.compile(".*(查询|搜索|find).*(知识库|knowledge|文档).*"),
            0.75,
            List.of("hybrid-search")
        ),
        // 内容审计
        new IntentPattern(
            Intent.IntentType.CONTENT_AUDIT,
            Pattern.compile(".*(审计|检查|分析|审核).*(文章|内容|质量).*"),
            0.85,
            List.of("content_audit")
        ),
        // 优化文章
        new IntentPattern(
            Intent.IntentType.OPTIMIZE_ARTICLE,
            Pattern.compile(".*(优化|改进|improve).*(文章|内容|质量).*"),
            0.8,
            List.of("article_update")
        ),
        // 内容机会发现
        new IntentPattern(
            Intent.IntentType.CONTENT_OPPORTUNITY,
            Pattern.compile(".*(发现|找|find|discover).*(内容|topic|主题|机会|缺口).*"),
            0.85,
            List.of("content_opportunity")
        ),
        // 联网搜索
        new IntentPattern(
            Intent.IntentType.ONLINE_SEARCH,
            Pattern.compile(".*(搜索|search|联网|online|实时).*"),
            0.9,
            List.of("tavily_search")
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
                log.debug("正则匹配意图: type={}, confidence={}, tools={}",
                    pattern.type, pattern.confidence, pattern.possibleTools);
                return Intent.builder()
                        .type(pattern.type)
                        .confidence(pattern.confidence)
                        .originalMessage(message)
                        .requiresMemory(pattern.type != Intent.IntentType.CHAT)// 非聊天意图需要内存
                        .requiresTool(!pattern.possibleTools.isEmpty())// 需要工具
                        .possibleTools(pattern.possibleTools)/// 工具
                        .build();
            }
        }

        // 无匹配，返回低置信度聊天
        return Intent.unknown(message, 0.3);
    }

    @Override
    public String getName() {
        return "RegexClassifier";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private record IntentPattern(
        Intent.IntentType type,
        Pattern regex,
        double confidence,
        List<String> possibleTools
    ) {}
}
