package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文章删除工具
 * 删除用户的文章（软删除）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleDeleteTool implements Tool {

    private final ArticleService articleService;

    @Override
    public String getName() {
        return "article_delete";
    }

    @Override
    public String getDescription() {
        return "删除文章，将文章移入回收站（可恢复）";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
                new ToolParameter(
                        "articleId",
                        "文章ID（可从消息中自动提取）",
                        "integer",
                        false,
                        null
                ),
                new ToolParameter(
                        "confirm",
                        "确认删除（防止误操作）",
                        "boolean",
                        false,
                        false
                )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Integer articleId = (Integer) parameters.get("articleId");
        Boolean confirm = (Boolean) parameters.getOrDefault("confirm", false);
        String message = (String) parameters.get("message");

        if (articleId == null && message != null) {
            articleId = extractArticleIdFromMessage(message);
        }

        log.info("执行文章删除工具: articleId={}, confirm={}", articleId, confirm);

        try {
            // 检查用户是否登录
            if (!StpUtil.isLogin()) {
                return ToolResult.error("用户未登录，无法删除文章");
            }
            Long userId = StpUtil.getLoginIdAsLong();

            if (articleId == null || articleId <= 0) {
                return ToolResult.error("无法从消息中提取文章ID，请提供要删除的文章ID，例如：delete article with ID 123");
            }

            // 调用文章服务删除
            articleService.deleteArticle(userId, articleId.longValue());

            Map<String, Object> result = Map.of(
                    "articleId", articleId,
                    "status", "deleted",
                    "message", "文章删除成功，ID: " + articleId + "，已移入回收站"
            );

            return ToolResult.success(result, "文章删除成功");

        } catch (Exception e) {
            log.error("文章删除失败: articleId={}", articleId, e);
            return ToolResult.error("文章删除失败: " + e.getMessage());
        }
    }

    private Integer extractArticleIdFromMessage(String message) {
        if (message == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:ID|id)[:\\s]*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        pattern = java.util.regex.Pattern.compile("\\b(\\d{2,})\\b");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    @Override
    public long getTimeout() {
        return 10000; // 10秒超时
    }
}
