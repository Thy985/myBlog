package com.xingchen.backend.service.impl;

import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleContent;
import com.xingchen.backend.mapper.ArticleContentMapper;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.ArticleGenerationService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章生成服务（分步流程版）
 * Step 1: 联网搜索收集素材
 * Step 2: 检索知识库找相关文章
 * Step 3: 生成大纲
 * Step 4: 撰写正文
 * Step 5: 格式校验
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleGenerationServiceImpl implements ArticleGenerationService {

    private final AIService aiService;
    private final ArticleMapper articleMapper;
    private final ArticleContentMapper articleContentMapper;
    private final WebSearchService webSearchService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    @Transactional
    public Article generateArticle(Long userId, String topic, Integer wordCount, String style) {
        log.info("开始为用户 {} 分步生成文章，主题: {}", userId, topic);
        int targetWords = wordCount != null ? wordCount : 2000;
        String targetStyle = style != null ? style : "professional";

        try {
            // Step 1: 收集素材
            log.info("[Step 1/5] 联网搜索素材: {}", topic);
            String research = collectResearch(topic);

            // Step 2: 检索知识库
            log.info("[Step 2/5] 检索知识库");
            String kbContext = searchKnowledgeBase(topic);

            // Step 3: 生成大纲
            log.info("[Step 3/5] 生成大纲");
            String outline = generateOutline(userId, topic, targetWords, targetStyle, research, kbContext);

            // Step 4: 撰写正文
            log.info("[Step 4/5] 撰写正文");
            String content = generateContent(userId, topic, outline, targetWords, targetStyle, research);

            // Step 5: 格式校验
            log.info("[Step 5/5] 格式校验");
            content = validateAndFix(content, targetWords);

            String title = extractTitle(content, topic);

            // 保存
            Article article = new Article();
            article.setTitle(title);
            article.setUserId(userId);
            article.setStatus(0); // 草稿状态
            article.setIsDeleted(0);
            article.setCommentStatus(1); // 开启评论
            article.setTopStatus(0); // 不置顶
            article.setViewStatus(1); // 公开
            article.setReadNum(0);
            article.setCommentNum(0);
            article.setLikeNum(0);
            article.setCollectNum(0);
            article.setShareNum(0);
            article.setCreatedTime(LocalDateTime.now());
            article.setUpdatedTime(LocalDateTime.now());
            articleMapper.insert(article);

            ArticleContent ac = new ArticleContent();
            ac.setArticleId(article.getId());
            ac.setContent(content);
            articleContentMapper.insert(ac);

            // 索引到知识库
            knowledgeBaseService.addDocument(article.getId(), title, content);

            log.info("文章生成完成: {} (ID: {}, 约 {} 字)", title, article.getId(), content.length());
            return article;

        } catch (Exception e) {
            log.error("文章生成失败: {} - {}", topic, e.getMessage(), e);
            throw new RuntimeException("文章生成失败: " + e.getMessage(), e);
        }
    }

    private String collectResearch(String topic) {
        try {
            List<WebSearchService.SearchResult> results = webSearchService.search(topic, 3);
            if (results == null || results.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (WebSearchService.SearchResult r : results) {
                sb.append("- ").append(r.title()).append(": ").append(r.content()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("素材收集失败: {}", e.getMessage());
            return "";
        }
    }

    private String searchKnowledgeBase(String topic) {
        try {
            String result = knowledgeBaseService.search(topic, 3);
            return result != null ? result : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String generateOutline(Long userId, String topic, int wordCount, String style,
                                    String research, String kbContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("请为「%s」生成详细文章大纲。目标 %d 字，风格：%s。\n", topic, wordCount, style));
        prompt.append("要求：3-5 个主要章节，每章 2-3 个要点，标注预估字数。\n");
        if (!research.isBlank()) prompt.append("\n参考素材：\n").append(research);
        if (!kbContext.isBlank()) prompt.append("\n博客已有相关内容：\n").append(kbContext.substring(0, Math.min(1000, kbContext.length())));
        return aiService.chatWithUserApiKey(userId, prompt.toString());
    }

    private String generateContent(Long userId, String topic, String outline, int wordCount,
                                    String style, String research) {
        String prompt = String.format("""
                根据以下大纲撰写完整博客文章。
                
                大纲：
                %s
                
                要求：约 %d 字，风格 %s，Markdown 格式，代码用代码块，章节自然过渡，不要 AI 味结尾。
                %s
                """,
                outline, wordCount, style,
                research.isBlank() ? "" : "参考素材：\n" + research
        );
        return aiService.chatWithUserApiKey(userId, prompt);
    }

    private String validateAndFix(String content, int targetWords) {
        if (content == null || content.isBlank()) throw new RuntimeException("生成内容为空");

        // 去除 markdown 外壳
        if (content.startsWith("```markdown")) {
            content = content.replaceFirst("^```markdown\\s*\\n?", "");
            if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
        }
        if (content.startsWith("```md")) {
            content = content.replaceFirst("^```md\\s*\\n?", "");
            if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
        }

        content = content.trim();
        if (!content.startsWith("#")) content = "# " + content;

        int actual = content.replaceAll("[\\s#*`>\\-|]", "").length();
        if (actual < targetWords * 0.5) log.warn("字数偏少: 目标 {} 实际 {}", targetWords, actual);

        return content;
    }

    private String extractTitle(String content, String fallback) {
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith("# ") && !line.startsWith("## ")) return line.substring(2).trim();
        }
        return fallback;
    }

    @Override
    public String generateArticlePreview(Long userId, String topic) {
        return aiService.chatWithUserApiKey(userId,
                "用 3 句话预览你将如何写「" + topic + "」的文章：文章角度、核心内容、读者收获。");
    }

    @Override
    @Transactional
    public Article generateArticleByTask(Long userId, Long taskId) {
        // TODO: 根据任务ID获取任务配置并生成文章
        throw new UnsupportedOperationException("根据任务生成文章功能尚未实现");
    }
}
