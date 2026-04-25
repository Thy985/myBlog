package com.xingchen.backend.ai.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.ImageService;
import com.xingchen.backend.service.SEOService;
import com.xingchen.backend.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final SEOService seoService;

    @Override
    public String getName() {
        return "article_generator";
    }

    @Override
    public String getDescription() {
        return "生成SEO优化的博客文章，支持指定主题、关键词、字数和风格，生成后保存为草稿";
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
                        "primaryKeyword",
                        "主关键词（SEO优化用）",
                        "string",
                        false,
                        null
                ),
                new ToolParameter(
                        "secondaryKeywords",
                        "次关键词列表（SEO优化用）",
                        "array",
                        false,
                        null
                ),
                new ToolParameter(
                        "wordCount",
                        "文章字数",
                        "integer",
                        false,
                        1500
                ),
                new ToolParameter(
                        "style",
                        "文章风格：technical/tutorial/review/news",
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
        String primaryKeyword = (String) parameters.get("primaryKeyword");
        @SuppressWarnings("unchecked")
        List<String> secondaryKeywords = (List<String>) parameters.get("secondaryKeywords");
        Integer wordCount = (Integer) parameters.getOrDefault("wordCount", 1500);
        String style = (String) parameters.getOrDefault("style", "technical");
        Integer categoryId = (Integer) parameters.getOrDefault("categoryId", 1);
        @SuppressWarnings("unchecked")
        List<Long> tagIds = (List<Long>) parameters.get("tagIds");
        Boolean saveAsDraft = (Boolean) parameters.getOrDefault("saveAsDraft", true);
        Boolean autoCover = (Boolean) parameters.getOrDefault("autoCover", true);

        if (primaryKeyword == null || primaryKeyword.isEmpty()) {
            primaryKeyword = seoService.extractPrimaryKeyword(topic);
        }

        log.info("执行SEO优化文章生成: topic={}, primaryKeyword={}, wordCount={}, style={}",
                topic, primaryKeyword, wordCount, style);

        try {
            if (!StpUtil.isLogin()) {
                return ToolResult.error("用户未登录，无法生成文章");
            }
            Long userId = StpUtil.getLoginIdAsLong();

            String prompt = seoService.buildSEOOptimizedPrompt(
                    topic, wordCount, style, primaryKeyword, secondaryKeywords);

            String generatedContent = aiService.chat(prompt);

            SEOService.SEOAnalysisResult seoAnalysis = seoService.analyzeSEO(
                    generatedContent, topic, primaryKeyword);

            String optimizedTitle = seoService.optimizeTitle(topic, primaryKeyword);
            String metaDescription = seoService.generateMetaDescription(
                    generatedContent, primaryKeyword, 160);

            String coverImage = null;
            if (autoCover) {
                coverImage = imageService.searchCoverImage(topic, 1200, 630);
                log.info("文章封面图: {}", coverImage);
            }

            if (saveAsDraft) {
                ArticleCreateDTO createDTO = new ArticleCreateDTO();
                createDTO.setTitle(optimizedTitle);
                createDTO.setContent(generatedContent);
                createDTO.setSummary(metaDescription);
                createDTO.setTitleImage(coverImage);
                createDTO.setCategoryId(categoryId.longValue());
                createDTO.setTagIds(tagIds);
                createDTO.setViewStatus(0);

                ArticleVO articleVO = articleService.createArticle(userId, createDTO);

                List<Map<String, Object>> keywordList = seoAnalysis.getKeywords().stream()
                        .limit(5)
                        .map(k -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("keyword", k.getKeyword());
                            m.put("frequency", k.getFrequency());
                            m.put("score", k.getScore());
                            return m;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> result = new HashMap<>();
                result.put("articleId", articleVO.getId());
                result.put("title", optimizedTitle);
                result.put("originalTopic", topic);
                result.put("primaryKeyword", primaryKeyword);
                result.put("coverImage", coverImage);
                result.put("wordCount", generatedContent.length());
                result.put("seoScore", seoAnalysis.getSeoScore());
                result.put("keywords", keywordList);
                result.put("preview", metaDescription);
                result.put("status", "draft");
                result.put("message", String.format("SEO优化文章已生成并保存为草稿，ID: %d，SEO评分: %s",
                        articleVO.getId(), seoAnalysis.getSeoScore().get("grade")));

                return ToolResult.success(result, "SEO优化文章生成成功，已保存为草稿");
            } else {
                List<Map<String, Object>> keywordList = seoAnalysis.getKeywords().stream()
                        .limit(5)
                        .map(k -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("keyword", k.getKeyword());
                            m.put("frequency", k.getFrequency());
                            m.put("score", k.getScore());
                            return m;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> result = new HashMap<>();
                result.put("title", optimizedTitle);
                result.put("originalTopic", topic);
                result.put("primaryKeyword", primaryKeyword);
                result.put("content", generatedContent);
                result.put("summary", metaDescription);
                result.put("coverImage", coverImage);
                result.put("wordCount", generatedContent.length());
                result.put("seoScore", seoAnalysis.getSeoScore());
                result.put("keywords", keywordList);
                result.put("status", "preview");
                result.put("message", "SEO优化文章已生成（预览模式，未保存）");

                return ToolResult.success(result, "SEO优化文章生成成功（预览）");
            }

        } catch (Exception e) {
            log.error("SEO文章生成失败", e);
            return ToolResult.error("SEO文章生成失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeout() {
        return 120000;
    }
}
