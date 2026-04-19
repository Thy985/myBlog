package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.service.TagService;
import com.xingchen.backend.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 标签管理工具
 * 支持查看、创建、更新、删除文章标签
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TagTool implements Tool {

    private final TagService tagService;

    private static final Pattern LIST_PATTERN = Pattern.compile(".*(列出|查看|显示|list|show).*");
    private static final Pattern CREATE_PATTERN = Pattern.compile(".*(创建|新建|新增|add).*");
    private static final Pattern DELETE_PATTERN = Pattern.compile(".*(删除|delete).*");
    private static final Pattern HOT_PATTERN = Pattern.compile(".*(热门|hot|最热).*");

    @Override
    public String getName() {
        return "tag_manager";
    }

    @Override
    public String getDescription() {
        return "管理文章标签，支持查看标签列表、创建标签、更新标签、删除标签";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
                new ToolParameter(
                        "action",
                        "操作类型：list|get|create|update|delete|hot",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "tagId",
                        "标签ID（用于get/update/delete操作）",
                        "integer",
                        false,
                        null
                ),
                new ToolParameter(
                        "name",
                        "标签名称（用于create/update操作）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "color",
                        "标签颜色（用于create/update操作，如 #FF5733）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "limit",
                        "返回数量限制（用于list/hot操作）",
                        "integer",
                        false,
                        20
                )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String action = (String) parameters.get("action");
        String message = (String) parameters.get("message");
        log.info("执行标签管理工具: action={}, message={}", action, message);

        try {
            if (!StpUtil.isLogin()) {
                return ToolResult.error("用户未登录，无法管理标签");
            }
            Long userId = StpUtil.getLoginIdAsLong();

            // 如果没有明确指定 action，从消息中推断
            if (action == null || action.isEmpty()) {
                action = inferAction(message);
            }

            if (action == null) {
                return ToolResult.error("无法从消息中推断操作类型，请明确指定操作：查看标签、创建标签、删除标签等");
            }

            return switch (action.toLowerCase()) {
                case "list" -> listTags(userId, parameters);
                case "hot" -> listHotTags(parameters);
                case "get" -> getTag(parameters);
                case "create" -> createTag(userId, parameters);
                case "update" -> updateTag(userId, parameters);
                case "delete" -> deleteTag(userId, parameters);
                default -> ToolResult.error("未知操作: " + action + "，支持的操作: list|get|create|update|delete|hot");
            };
        } catch (Exception e) {
            log.error("标签管理失败: action={}", action, e);
            return ToolResult.error("标签管理失败: " + e.getMessage());
        }
    }

    /**
     * 从消息中推断 action
     */
    private String inferAction(String message) {
        if (message == null) return null;
        String lowerMsg = message.toLowerCase();

        if (HOT_PATTERN.matcher(lowerMsg).matches()) {
            return "hot";
        }
        if (CREATE_PATTERN.matcher(lowerMsg).matches()) {
            return "create";
        }
        if (DELETE_PATTERN.matcher(lowerMsg).matches()) {
            return "delete";
        }
        if (LIST_PATTERN.matcher(lowerMsg).matches()) {
            return "list";
        }
        // 默认认为是 list
        if (lowerMsg.contains("标签")) {
            return "list";
        }
        return null;
    }

    /**
     * 获取用户标签列表
     */
    private ToolResult listTags(Long userId, Map<String, Object> parameters) {
        Integer limit = (Integer) parameters.getOrDefault("limit", 20);
        List<TagVO> tags = tagService.getUserTagList(userId);

        List<Map<String, Object>> tagList = tags.stream()
                .limit(limit)
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "name", t.getName(),
                        "color", t.getColor() != null ? t.getColor() : "#666666",
                        "articleCount", t.getArticleCount() != null ? t.getArticleCount() : 0
                ))
                .toList();

        Map<String, Object> result = Map.of(
                "tags", tagList,
                "total", tagList.size()
        );

        return ToolResult.success(result, "获取标签列表成功，共 " + tagList.size() + " 个标签");
    }

    /**
     * 获取热门标签
     */
    private ToolResult listHotTags(Map<String, Object> parameters) {
        Integer limit = (Integer) parameters.getOrDefault("limit", 10);
        List<TagVO> tags = tagService.getHotTags(limit);

        List<Map<String, Object>> tagList = tags.stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "name", t.getName(),
                        "color", t.getColor() != null ? t.getColor() : "#FF5733",
                        "articleCount", t.getArticleCount() != null ? t.getArticleCount() : 0
                ))
                .toList();

        Map<String, Object> result = Map.of(
                "tags", tagList,
                "total", tagList.size()
        );

        return ToolResult.success(result, "获取热门标签成功，共 " + tagList.size() + " 个");
    }

    /**
     * 获取单个标签详情
     */
    private ToolResult getTag(Map<String, Object> parameters) {
        Integer tagId = (Integer) parameters.get("tagId");
        if (tagId == null) {
            return ToolResult.error("缺少标签ID");
        }

        TagVO tag = tagService.getTagById(tagId.longValue());
        if (tag == null) {
            return ToolResult.error("标签不存在: " + tagId);
        }

        Map<String, Object> result = Map.of(
                "id", tag.getId(),
                "name", tag.getName(),
                "color", tag.getColor() != null ? tag.getColor() : "#666666",
                "articleCount", tag.getArticleCount() != null ? tag.getArticleCount() : 0
        );

        return ToolResult.success(result, "获取标签成功: " + tag.getName());
    }

    /**
     * 创建标签
     */
    private ToolResult createTag(Long userId, Map<String, Object> parameters) {
        String name = (String) parameters.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ToolResult.error("标签名称不能为空");
        }

        String color = (String) parameters.getOrDefault("color", "#666666");

        TagVO tag = tagService.createTag(name.trim(), color);

        Map<String, Object> result = Map.of(
                "id", tag.getId(),
                "name", tag.getName(),
                "color", tag.getColor(),
                "message", "标签创建成功，ID: " + tag.getId()
        );

        return ToolResult.success(result, "标签创建成功: " + tag.getName());
    }

    /**
     * 更新标签
     */
    private ToolResult updateTag(Long userId, Map<String, Object> parameters) {
        Integer tagId = (Integer) parameters.get("tagId");
        if (tagId == null) {
            return ToolResult.error("缺少标签ID");
        }

        String name = (String) parameters.get("name");
        String color = (String) parameters.get("color");

        TagVO tag = tagService.updateTag(tagId.longValue(), name, color);

        Map<String, Object> result = Map.of(
                "id", tag.getId(),
                "name", tag.getName(),
                "color", tag.getColor(),
                "message", "标签更新成功"
        );

        return ToolResult.success(result, "标签更新成功: " + tag.getName());
    }

    /**
     * 删除标签
     */
    private ToolResult deleteTag(Long userId, Map<String, Object> parameters) {
        Integer tagId = (Integer) parameters.get("tagId");
        if (tagId == null) {
            return ToolResult.error("缺少标签ID");
        }

        tagService.deleteTag(tagId.longValue());

        Map<String, Object> result = Map.of(
                "id", tagId,
                "status", "deleted",
                "message", "标签删除成功"
        );

        return ToolResult.success(result, "标签删除成功，ID: " + tagId);
    }

    @Override
    public long getTimeout() {
        return 10000;
    }
}
