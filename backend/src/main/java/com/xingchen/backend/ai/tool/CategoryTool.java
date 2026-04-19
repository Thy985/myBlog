package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.dto.CategoryCreateDTO;
import com.xingchen.backend.service.CategoryService;
import com.xingchen.backend.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 分类管理工具
 * 支持查看、创建、更新、删除文章分类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryTool implements Tool {

    private final CategoryService categoryService;

    private static final Pattern LIST_PATTERN = Pattern.compile(".*(列出|查看|显示|list|show).*");
    private static final Pattern CREATE_PATTERN = Pattern.compile(".*(创建|新建|新增|add).*");
    private static final Pattern DELETE_PATTERN = Pattern.compile(".*(删除|delete).*");

    @Override
    public String getName() {
        return "category_manager";
    }

    @Override
    public String getDescription() {
        return "管理文章分类，支持查看分类列表、创建分类、更新分类、删除分类";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
                new ToolParameter(
                        "action",
                        "操作类型：list|get|create|update|delete",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "categoryId",
                        "分类ID（用于get/update/delete操作）",
                        "integer",
                        false,
                        null
                ),
                new ToolParameter(
                        "name",
                        "分类名称（用于create/update操作）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "description",
                        "分类描述（用于create/update操作）",
                        "string",
                        false,
                        null
                )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String action = (String) parameters.get("action");
        String message = (String) parameters.get("message");
        log.info("执行分类管理工具: action={}, message={}", action, message);

        try {
            if (!StpUtil.isLogin()) {
                return ToolResult.error("用户未登录，无法管理分类");
            }
            Long userId = StpUtil.getLoginIdAsLong();

            // 如果没有明确指定 action，从消息中推断
            if (action == null || action.isEmpty()) {
                action = inferAction(message);
            }

            if (action == null) {
                return ToolResult.error("无法从消息中推断操作类型，请明确指定操作：查看分类、创建分类、删除分类等");
            }

            return switch (action.toLowerCase()) {
                case "list" -> listCategories(userId);
                case "get" -> getCategory(parameters);
                case "create" -> createCategory(userId, parameters);
                case "update" -> updateCategory(userId, parameters);
                case "delete" -> deleteCategory(userId, parameters);
                default -> ToolResult.error("未知操作: " + action + "，支持的操作: list|get|create|update|delete");
            };
        } catch (Exception e) {
            log.error("分类管理失败: action={}", action, e);
            return ToolResult.error("分类管理失败: " + e.getMessage());
        }
    }

    /**
     * 从消息中推断 action
     */
    private String inferAction(String message) {
        if (message == null) return null;
        String lowerMsg = message.toLowerCase();

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
        if (lowerMsg.contains("分类")) {
            return "list";
        }
        return null;
    }

    /**
     * 获取用户分类列表
     */
    private ToolResult listCategories(Long userId) {
        List<CategoryVO> categories = categoryService.getUserCategoryList(userId);

        List<Map<String, Object>> categoryList = categories.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "description", c.getDescription() != null ? c.getDescription() : "",
                        "articleCount", c.getArticleCount() != null ? c.getArticleCount() : 0,
                        "sortOrder", c.getSortOrder() != null ? c.getSortOrder() : 0
                ))
                .toList();

        Map<String, Object> result = Map.of(
                "categories", categoryList,
                "total", categoryList.size()
        );

        return ToolResult.success(result, "获取分类列表成功，共 " + categoryList.size() + " 个分类");
    }

    /**
     * 获取单个分类详情
     */
    private ToolResult getCategory(Map<String, Object> parameters) {
        Integer categoryId = (Integer) parameters.get("categoryId");
        if (categoryId == null) {
            return ToolResult.error("缺少分类ID");
        }

        CategoryVO category = categoryService.getCategoryById(categoryId.longValue());
        if (category == null) {
            return ToolResult.error("分类不存在: " + categoryId);
        }

        Map<String, Object> result = Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "description", category.getDescription() != null ? category.getDescription() : "",
                "articleCount", category.getArticleCount() != null ? category.getArticleCount() : 0,
                "sortOrder", category.getSortOrder() != null ? category.getSortOrder() : 0
        );

        return ToolResult.success(result, "获取分类成功: " + category.getName());
    }

    /**
     * 创建分类
     */
    private ToolResult createCategory(Long userId, Map<String, Object> parameters) {
        String name = (String) parameters.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ToolResult.error("分类名称不能为空");
        }

        String description = (String) parameters.get("description");

        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName(name.trim());
        dto.setDescription(description);

        CategoryVO category = categoryService.createCategory(dto);

        Map<String, Object> result = Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "description", category.getDescription() != null ? category.getDescription() : "",
                "message", "分类创建成功，ID: " + category.getId()
        );

        return ToolResult.success(result, "分类创建成功: " + category.getName());
    }

    /**
     * 更新分类
     */
    private ToolResult updateCategory(Long userId, Map<String, Object> parameters) {
        Integer categoryId = (Integer) parameters.get("categoryId");
        if (categoryId == null) {
            return ToolResult.error("缺少分类ID");
        }

        String name = (String) parameters.get("name");
        String description = (String) parameters.get("description");

        CategoryCreateDTO dto = new CategoryCreateDTO();
        if (name != null) dto.setName(name.trim());
        if (description != null) dto.setDescription(description);

        CategoryVO category = categoryService.updateCategory(categoryId.longValue(), dto);

        Map<String, Object> result = Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "message", "分类更新成功"
        );

        return ToolResult.success(result, "分类更新成功: " + category.getName());
    }

    /**
     * 删除分类
     */
    private ToolResult deleteCategory(Long userId, Map<String, Object> parameters) {
        Integer categoryId = (Integer) parameters.get("categoryId");
        if (categoryId == null) {
            return ToolResult.error("缺少分类ID");
        }

        categoryService.deleteCategory(categoryId.longValue());

        Map<String, Object> result = Map.of(
                "id", categoryId,
                "status", "deleted",
                "message", "分类删除成功"
        );

        return ToolResult.success(result, "分类删除成功，ID: " + categoryId);
    }

    @Override
    public long getTimeout() {
        return 10000;
    }
}
