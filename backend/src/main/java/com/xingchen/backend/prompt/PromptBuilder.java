package com.xingchen.backend.prompt;

import com.xingchen.backend.memory.MemoryServiceV2;
import com.xingchen.backend.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示词构建器
 *
 * 根据对话上下文动态构建 System Prompt
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptTemplateManager templateManager;
    private final KnowledgeBaseService knowledgeBaseService;
    private final MemoryServiceV2 memoryService;

    /**
     * 构建对话提示词
     */
    public String buildChatPrompt(Long userId, String userMessage, String taskType) {
        StringBuilder prompt = new StringBuilder();
        
        // 1. 基础角色设定
        String basePrompt = templateManager.getTemplate(taskType);
        prompt.append(basePrompt).append("\n\n");
        
        // 2. 添加记忆上下文
        String memoryContext = memoryService.retrieveMemory(userId, userMessage, 3);
        if (!memoryContext.isEmpty()) {
            prompt.append("## 用户相关信息\n").append(memoryContext).append("\n\n");
        }
        
        // 3. 添加知识库上下文
        String knowledgeContext = knowledgeBaseService.search(userMessage, 3);
        if (!knowledgeContext.isEmpty()) {
            prompt.append("## 相关知识\n").append(knowledgeContext).append("\n\n");
        }
        
        // 4. 输出格式要求
        prompt.append("## 回复要求\n");
        prompt.append("1. 保持回答简洁准确\n");
        prompt.append("2. 使用 Markdown 格式\n");
        prompt.append("3. 代码使用代码块包裹\n");
        
        return prompt.toString();
    }

    /**
     * 构建 RAG 提示词
     */
    public String buildRagPrompt(String userMessage) {
        String basePrompt = templateManager.getTemplate("rag");
        String knowledgeContext = knowledgeBaseService.search(userMessage, 5);
        
        Map<String, String> variables = new HashMap<>();
        variables.put("knowledge_context", knowledgeContext);
        
        return templateManager.render("rag", variables);
    }

    /**
     * 构建代码助手提示词
     */
    public String buildCodePrompt(String userMessage, String language) {
        String basePrompt = templateManager.getTemplate("code");
        
        if (language != null && !language.isEmpty()) {
            basePrompt += "\n\n请使用 " + language + " 语言编写代码。";
        }
        
        // 搜索相关技术文档
        String knowledgeContext = knowledgeBaseService.search(userMessage, 3);
        if (!knowledgeContext.isEmpty()) {
            basePrompt += "\n\n参考文档：\n" + knowledgeContext;
        }
        
        return basePrompt;
    }

    /**
     * 构建创意写作提示词
     */
    public String buildCreativePrompt(String userMessage, String style, String tone) {
        String basePrompt = templateManager.getTemplate("creative");
        
        if (style != null && !style.isEmpty()) {
            basePrompt += "\n\n写作风格：" + style;
        }
        
        if (tone != null && !tone.isEmpty()) {
            basePrompt += "\n语气语调：" + tone;
        }
        
        return basePrompt;
    }

    /**
     * 构建博客写作提示词
     */
    public String buildBlogPrompt(String topic, String targetAudience, int wordCount) {
        String basePrompt = templateManager.getTemplate("blog");
        
        Map<String, String> variables = new HashMap<>();
        variables.put("topic", topic);
        variables.put("target_audience", targetAudience != null ? targetAudience : "技术人员");
        variables.put("word_count", String.valueOf(wordCount));
        
        String prompt = basePrompt + "\n\n";
        prompt += "主题：" + topic + "\n";
        prompt += "目标读者：" + targetAudience + "\n";
        prompt += "字数要求：约 " + wordCount + " 字\n\n";
        
        // 搜索相关资料
        String knowledgeContext = knowledgeBaseService.search(topic, 3);
        if (!knowledgeContext.isEmpty()) {
            prompt += "参考资料：\n" + knowledgeContext + "\n\n";
        }
        
        return prompt;
    }

    /**
     * 构建代码审查提示词
     */
    public String buildCodeReviewPrompt(String code, String language) {
        String basePrompt = templateManager.getTemplate("code_review");
        
        basePrompt += "\n\n待审查代码（" + language + "）：\n";
        basePrompt += "```" + language + "\n";
        basePrompt += code;
        basePrompt += "\n```\n";
        
        return basePrompt;
    }

    /**
     * 构建文档生成提示词
     */
    public String buildDocPrompt(String code, String docType) {
        String basePrompt = templateManager.getTemplate("doc");
        
        basePrompt += "\n\n需要生成文档的代码：\n";
        basePrompt += "```\n" + code + "\n```\n";
        basePrompt += "\n文档类型：" + docType + "\n";
        
        return basePrompt;
    }

    /**
     * 动态构建提示词（通用）
     */
    public String buildDynamicPrompt(String templateKey, Map<String, String> context) {
        return templateManager.render(templateKey, context);
    }
}