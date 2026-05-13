package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 文章查询工具
 * 支持查看用户文章列表、搜索文章、获取文章详情等
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleQueryTool implements Tool {

    private final ArticleService articleService;

    private static final Pattern LIST_PATTERN = Pattern.compile(".*(列出|查看|显示|list|show|我的).*(文章|article|博客).*");
    private static final Pattern SEARCH_PATTERN = Pattern.compile(".*(搜索|查找|search|find).*");
    private static final Pattern DETAIL_PATTERN = Pattern.compile(".*(详情|detail|内容).*(文章|id).*");
    private static final Pattern HOT_PATTERN = Pattern.compile(".*(热门|hot).*(文章|文章列表).*");

    @Override
    public String getName() {
        return "article_query";
    }

    @Override
    public String getDescription() {
        return "查询文章，支持查看我的文章列表、搜索文章、获取文章详情等";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
                new ToolParameter(
                        "action",
                        "操作类型：list|search|detail|hot|archives",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "articleId",
                        "文章ID（用于detail操作）",
                        "integer",
                        false,
                        null
                ),
                new ToolParameter(
                        "keyword",
                        "搜索关键词（用于search操作）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "page",
                        "页码（用于list/search操作）",
                        "integer",
                        false,
                        1
                ),
                new ToolParameter(
                        "pageSize",
                        "每页数量（用于list/search操作）",
                        "integer",
                        false,
                        10
                )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String action = (String) parameters.get("action");
        String message = (String) parameters.get("message");
        log.info("执行文章查询工具: action={}, message={}", action, message);

        try {
            if (!StpUtil.isLogin()) {
                return ToolResult.error("用户未登录，无法查询文章");
            }
            Long userId = StpUtil.getLoginIdAsLong();

            // 如果没有明确指定 action，从消息中推断
            if (action == null || action.isEmpty()) {
                action = inferAction(message, parameters);
            }

            if (action == null) {
                return ToolResult.error("无法从消息中推断操作类型，请明确指定操作：查看我的文章、搜索文章等");
            }

            return switch (action.toLowerCase()) {
                case "list" -> listMyArticles(userId, parameters);
                case "search" -> searchArticles(userId, parameters);
                case "detail" -> getArticleDetail(userId, parameters);
                case "hot" -> listHotArticles(parameters);
                case "archives" -> getArchives(userId, parameters);
                default -> ToolResult.error("未知操作: " + action + "，支持的操作: list|search|detail|hot|archives");
            };
        } catch (Exception e) {
            log.error("文章查询失败: action={}", action, e);
            return ToolResult.error("文章查询失败: " + e.getMessage());
        }
    }

    /**
     * 从消息中推断 action
     */
    private String inferAction(String message, Map<String, Object> parameters) {
        if (message == null) return null;
        String lowerMsg = message.toLowerCase();

        if (SEARCH_PATTERN.matcher(lowerMsg).matches()) {
            return "search";
        }
        if (DETAIL_PATTERN.matcher(lowerMsg).matches()) {
            return "detail";
        }
        if (HOT_PATTERN.matcher(lowerMsg).matches()) {
            return "hot";
        }
        if (LIST_PATTERN.matcher(lowerMsg).matches()) {
            return "list";
        }
        // 检查关键词
        String keyword = (String) parameters.get("keyword");
        if (keyword != null && !keyword.isEmpty()) {
            return "search";
        }
        // 检查文章ID
        Integer articleId = (Integer) parameters.get("articleId");
        if (articleId != null) {
            return "detail";
        }
        // 默认是 list
        return "list";
    }

    /**
     * 获取我的文章列表
     */
    private ToolResult listMyArticles(Long userId, Map<String, Object> parameters) {
        Integer page = (Integer) parameters.getOrDefault("page", 1);
        Integer pageSize = (Integer) parameters.getOrDefault("pageSize", 10);
        String keyword = (String) parameters.get("keyword");

        PageResult<ArticleListVO> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = articleService.getUserArticles(userId, page, pageSize, keyword.trim(), userId);
        } else {
            result = articleService.getUserArticles(userId, page, pageSize);
        }

        return buildArticleListResult(result, "获取文章列表成功");
    }

    /**
     * 搜索文章
     */
    private ToolResult searchArticles(Long userId, Map<String, Object> parameters) {
        Integer page = (Integer) parameters.getOrDefault("page", 1);
        Integer pageSize = (Integer) parameters.getOrDefault("pageSize", 10);
        String keyword = (String) parameters.get("keyword");

        if (keyword == null || keyword.trim().isEmpty()) {
            return ToolResult.error("搜索关键词不能为空");
        }

        PageResult<ArticleListVO> result = articleService.getArticleList(
                page, pageSize, keyword.trim(),
                null, null, userId, userId
        );

        return buildArticleListResult(result, "搜索完成");
    }

    /**
     * 获取文章详情
     */
    private ToolResult getArticleDetail(Long userId, Map<String, Object> parameters) {
        Integer articleId = (Integer) parameters.get("articleId");
        if (articleId == null) {
            return ToolResult.error("缺少文章ID");
        }

        ArticleVO article = articleService.getArticleById(articleId.longValue(), userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", article.getId());
        result.put("title", article.getTitle());
        result.put("content", article.getContent() != null ? article.getContent() : "");
        result.put("description", article.getDescription() != null ? article.getDescription() : "");
        result.put("thumbnail", article.getThumbnail() != null ? article.getThumbnail() : "");
        result.put("status", article.getStatus() != null ? article.getStatus() : "");
        result.put("isTop", article.getIsTop() != null ? article.getIsTop() : 0);
        result.put("readNum", article.getReadNum() != null ? article.getReadNum() : 0);
        result.put("likeNum", article.getLikeNum() != null ? article.getLikeNum() : 0);
        result.put("commentNum", article.getCommentNum() != null ? article.getCommentNum() : 0);
        result.put("collectNum", article.getCollectNum() != null ? article.getCollectNum() : 0);
        result.put("publishTime", article.getPublishTime() != null ? article.getPublishTime().toString() : "");
        result.put("createTime", article.getCreatedTime() != null ? article.getCreatedTime().toString() : "");
        result.put("updateTime", article.getUpdatedTime() != null ? article.getUpdatedTime().toString() : "");

        return ToolResult.success(result, "获取文章详情成功: " + article.getTitle());
    }

    /**
     * 获取热门文章
     */
    private ToolResult listHotArticles(Map<String, Object> parameters) {
        Integer limit = (Integer) parameters.getOrDefault("pageSize", 10);
        var articles = articleService.getHotArticles(limit);

        var articleList = articles.stream()
                .map(a -> Map.of(
                        "id", a.getId(),
                        "title", a.getTitle(),
                        "description", a.getDescription() != null ? a.getDescription() : "",
                        "thumbnail", a.getThumbnail() != null ? a.getThumbnail() : "",
                        "readNum", a.getReadNum() != null ? a.getReadNum() : 0,
                        "likeNum", a.getLikeNum() != null ? a.getLikeNum() : 0
                ))
                .toList();

        Map<String, Object> result = Map.of(
                "articles", articleList,
                "total", articleList.size()
        );

        return ToolResult.success(result, "获取热门文章成功，共 " + articleList.size() + " 篇");
    }

    /**
     * 获取文章归档
     */
    private ToolResult getArchives(Long userId, Map<String, Object> parameters) {
        Integer page = (Integer) parameters.getOrDefault("page", 1);
        Integer pageSize = (Integer) parameters.getOrDefault("pageSize", 10);

        var archiveResult = articleService.getUserArticleArchive(userId, page, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("archives", archiveResult.get("articles"));
        result.put("stats", archiveResult.get("stats"));
        result.put("total", archiveResult.get("total"));
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ToolResult.success(result, "获取文章归档成功");
    }

    /**
     * 构建文章列表结果
     */
    private ToolResult buildArticleListResult(PageResult<ArticleListVO> result, String message) {
        var articleList = result.getList().stream()
                .map(a -> Map.of(
                        "id", a.getId(),
                        "title", a.getTitle(),
                        "description", a.getDescription() != null ? a.getDescription() : "",
                        "thumbnail", a.getThumbnail() != null ? a.getThumbnail() : "",
                        "categoryName", a.getCategoryName() != null ? a.getCategoryName() : "",
                        "status", a.getStatus() != null ? a.getStatus() : "",
                        "isTop", a.getIsTop() != null ? a.getIsTop() : 0,
                        "readNum", a.getReadNum() != null ? a.getReadNum() : 0,
                        "likeNum", a.getLikeNum() != null ? a.getLikeNum() : 0,
                        "publishTime", a.getPublishTime() != null ? a.getPublishTime().toString() : ""
                ))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("articles", articleList);
        data.put("total", result.getTotal());
        data.put("page", result.getPage());
        data.put("size", result.getSize());
        data.put("pages", result.getPages());

        return ToolResult.success(data, message + "，共 " + result.getTotal() + " 篇");
    }

    @Override
    public long getTimeout() {
        return 10000;
    }
}
