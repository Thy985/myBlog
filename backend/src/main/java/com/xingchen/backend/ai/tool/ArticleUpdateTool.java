package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.dto.ArticleUpdateDTO;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 文章更新工具
 * 更新文章的标题、内容、摘要等信息
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleUpdateTool implements Tool {

    private final ArticleService articleService;

    @Override
    public String getName() {
        return "article_update";
    }

    @Override
    public String getDescription() {
        return "更新文章内容，支持修改标题、内容、摘要、分类、标签等";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
                new ToolParameter(
                        "articleId",
                        "文章ID",
                        "integer",
                        true,
                        null
                ),
                new ToolParameter(
                        "title",
                        "文章标题（可选）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "content",
                        "文章内容（可选）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "summary",
                        "文章摘要（可选）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "categoryId",
                        "分类ID（可选）",
                        "integer",
                        false,
                        null
                ),
                new ToolParameter(
                        "tagIds",
                        "标签ID列表（可选）",
                        "array",
                        false,
                        null
                ),
                new ToolParameter(
                        "thumbnail",
                        "缩略图URL（可选）",
                        "string",
                        false,
                        null
                )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Integer articleId = (Integer) parameters.get("articleId");
        String title = (String) parameters.get("title");
        String content = (String) parameters.get("content");
        String summary = (String) parameters.get("summary");
        Integer categoryId = (Integer) parameters.get("categoryId");
        @SuppressWarnings("unchecked")
        List<Long> tagIds = (List<Long>) parameters.get("tagIds");
        String thumbnail = (String) parameters.get("thumbnail");

        log.info("执行文章更新工具: articleId={}, title={}", articleId, title);

        try {
            // 检查用户是否登录
            if (!StpUtil.isLogin()) {
                return ToolResult.error("用户未登录，无法更新文章");
            }
            Long userId = StpUtil.getLoginIdAsLong();

            if (articleId == null || articleId <= 0) {
                return ToolResult.error("文章ID无效");
            }

            // 检查是否有可更新的字段
            if (title == null && content == null && summary == null && 
                categoryId == null && tagIds == null && thumbnail == null) {
                return ToolResult.error("至少需要提供一个要更新的字段");
            }

            // 创建更新DTO
            ArticleUpdateDTO updateDTO = new ArticleUpdateDTO();
            if (title != null) updateDTO.setTitle(title);
            if (content != null) updateDTO.setContent(content);
            if (summary != null) updateDTO.setSummary(summary);
            if (categoryId != null) updateDTO.setCategoryId(categoryId.longValue());
            if (tagIds != null) updateDTO.setTagIds(tagIds);
            if (thumbnail != null) updateDTO.setThumbnail(thumbnail);

            // 调用文章服务更新
            ArticleVO article = articleService.updateArticle(userId, articleId.longValue(), updateDTO);

            Map<String, Object> result = Map.of(
                    "articleId", article.getId(),
                    "title", article.getTitle(),
                    "updatedAt", article.getUpdatedTime(),
                    "message", "文章更新成功，ID: " + article.getId()
            );

            return ToolResult.success(result, "文章更新成功");

        } catch (Exception e) {
            log.error("文章更新失败: articleId={}", articleId, e);
            return ToolResult.error("文章更新失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeout() {
        return 30000; // 30秒超时
    }
}
