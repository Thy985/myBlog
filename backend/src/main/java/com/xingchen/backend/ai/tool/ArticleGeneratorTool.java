package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.ImageService;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 文章生成工具
 * 调用AI生成文章内容，并创建草稿
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleGeneratorTool implements Tool {

    private final AIService aiService;
    private final ArticleService articleService;
    private final ImageService imageService;

    @Override
    public String getName() {
        return "article_generator";
    }

    @Override
    public String getDescription() {
        return "生成博客文章，支持指定主题、字数和风格，生成后保存为草稿";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
                new ToolParameter(
                        "topic",
                        "文章主题",
                        "string",
                        true,
                        null
                ),
                new ToolParameter(
                        "wordCount",
                        "文章字数",
                        "integer",
                        false,
                        1000
                ),
                new ToolParameter(
                        "style",
                        "文章风格",
                        "string",
                        false,
                        "technical"
                ),
                new ToolParameter(
                        "categoryId",
                        "分类ID",
                        "integer",
                        false,
                        1
                ),
                new ToolParameter(
                        "tagIds",
                        "标签ID列表",
                        "array",
                        false,
                        null
                ),
                new ToolParameter(
                        "saveAsDraft",
                        "是否保存为草稿",
                        "boolean",
                        false,
                        true
                ),
                new ToolParameter(
                        "autoCover",
                        "是否自动生成封面图",
                        "boolean",
                        false,
                        true
                )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String topic = (String) parameters.get("topic");
        Integer wordCount = (Integer) parameters.getOrDefault("wordCount", 1000);
        String style = (String) parameters.getOrDefault("style", "technical");
        Integer categoryId = (Integer) parameters.getOrDefault("categoryId", 1);
        @SuppressWarnings("unchecked")
        List<Long> tagIds = (List<Long>) parameters.get("tagIds");
        Boolean saveAsDraft = (Boolean) parameters.getOrDefault("saveAsDraft", true);
        Boolean autoCover = (Boolean) parameters.getOrDefault("autoCover", true);

        log.info("执行文章生成工具: topic={}, wordCount={}, style={}, autoCover={}", topic, wordCount, style, autoCover);

        try {
            // 检查用户是否登录
            if (!StpUtil.isLogin()) {
                return ToolResult.error("用户未登录，无法生成文章");
            }
            Long userId = StpUtil.getLoginIdAsLong();

            // 构建生成提示词
            String prompt = buildGenerationPrompt(topic, wordCount, style);

            // 调用AI生成文章
            String generatedContent = aiService.chat(prompt);

            // 生成标题
            String title = generateTitle(topic, style);

            // 生成摘要
            String summary = generateSummary(generatedContent, 200);

            // 生成封面图
            String coverImage = null;
            if (autoCover) {
                coverImage = imageService.searchCoverImage(topic, 1200, 630);
                log.info("文章封面图: {}", coverImage);
            }

            if (saveAsDraft) {
                // 创建文章DTO
                ArticleCreateDTO createDTO = new ArticleCreateDTO();
                createDTO.setTitle(title);
                createDTO.setContent(generatedContent);
                createDTO.setSummary(summary);
                createDTO.setTitleImage(coverImage);
                createDTO.setCategoryId(categoryId.longValue());
                createDTO.setTagIds(tagIds);
                createDTO.setViewStatus(0); // 0-私密（草稿状态）

                // 保存文章
                ArticleVO articleVO = articleService.createArticle(userId, createDTO);

                Map<String, Object> result = Map.of(
                        "articleId", articleVO.getId(),
                        "title", articleVO.getTitle(),
                        "coverImage", coverImage,
                        "wordCount", generatedContent.length(),
                        "preview", summary,
                        "status", "draft",
                        "message", "文章已生成并保存为草稿，ID: " + articleVO.getId()
                );

                return ToolResult.success(result, "文章生成成功，已保存为草稿");
            } else {
                // 仅返回生成内容，不保存
                Map<String, Object> result = Map.of(
                        "title", title,
                        "content", generatedContent,
                        "summary", summary,
                        "coverImage", coverImage,
                        "wordCount", generatedContent.length(),
                        "status", "preview",
                        "message", "文章已生成（预览模式，未保存）"
                );

                return ToolResult.success(result, "文章生成成功（预览）");
            }

        } catch (Exception e) {
            log.error("文章生成失败", e);
            return ToolResult.error("文章生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建文章生成提示词
     */
    private String buildGenerationPrompt(String topic, Integer wordCount, String style) {
        String styleDesc = switch (style.toLowerCase()) {
            case "technical", "tech" -> "技术专业风格，使用专业术语，结构清晰";
            case "tutorial", "guide" -> "教程风格，步骤详细，易于理解";
            case "review" -> "评测风格，客观分析，优缺点并重";
            case "news" -> "新闻风格，简洁明了，事实为主";
            default -> "专业风格，结构清晰";
        };

        return String.format("""
            请撰写一篇关于"%s"的博客文章。

            要求：
            1. 字数：约%d字
            2. 风格：%s
            3. 使用Markdown格式
            4. 包含标题、正文内容
            5. 内容要有深度，有实用价值

            请直接输出文章内容，不需要额外的说明。
            """, topic, wordCount, styleDesc);
    }

    /**
     * 根据主题和风格生成标题
     */
    private String generateTitle(String topic, String style) {
        String prefix = switch (style.toLowerCase()) {
            case "technical", "tech" -> "技术解析：";
            case "tutorial", "guide" -> "实战指南：";
            case "review" -> "深度评测：";
            case "news" -> "行业动态：";
            default -> "";
        };
        return prefix + topic;
    }

    /**
     * 生成文章摘要
     */
    private String generateSummary(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // 去除Markdown标记
        String plainText = content.replaceAll("[#*_`\\[\\]()\\]", "").replaceAll("\\s+", " ").trim();
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        return plainText.substring(0, maxLength) + "...";
    }

    @Override
    public long getTimeout() {
        return 120000; // 120秒超时（生成文章可能需要较长时间）
    }
}
