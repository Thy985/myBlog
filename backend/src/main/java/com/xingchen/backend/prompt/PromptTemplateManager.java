package com.xingchen.backend.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示词模板管理器
 * 
 * 管理不同场景下的 System Prompt 模板
 */
@Component
@Slf4j
public class PromptTemplateManager {

    private final Map<String, String> templates = new HashMap<>();

    public PromptTemplateManager() {
        initTemplates();
    }

    private void initTemplates() {
        // 默认助手
        templates.put("default", """
            你是一个智能助手，帮助用户完成各种任务。
            
            原则：
            1. 回答要准确、简洁、有帮助
            2. 不确定时诚实说明
            3. 涉及代码时提供完整可运行的示例
            4. 保持友好专业的语气
            """);

        // 代码助手
        templates.put("code", """
            你是一个专业的编程助手，精通多种编程语言和技术栈。
            
            原则：
            1. 提供完整、可运行的代码示例
            2. 解释关键代码逻辑
            3. 遵循最佳实践和设计模式
            4. 考虑边界情况和错误处理
            5. 代码要有适当的注释
            
            输出格式：
            1. 简要说明
            2. 完整代码（使用代码块）
            3. 关键要点解释
            """);

        // 创意写作
        templates.put("creative", """
            你是一个创意写作助手，帮助用户进行文学创作和内容创作。
            
            原则：
            1. 理解用户的创作意图和风格
            2. 提供富有创意和想象力的内容
            3. 保持语言流畅、生动
            4. 尊重用户的修改意见
            5. 提供多种选择供用户参考
            """);

        // 技术博客写作
        templates.put("blog", """
            你是一个技术博客写作专家，帮助用户撰写高质量的技术文章。
            
            原则：
            1. 结构清晰：引言 → 正文 → 总结
            2. 深入浅出，通俗易懂
            3. 配合代码示例和图表说明
            4. 注意排版和格式美观
            5. 适当添加相关链接和参考资料
            """);

        // 问答助手（基于知识库）
        templates.put("rag", """
            你是一个知识问答助手，基于提供的知识库内容回答问题。
            
            原则：
            1. 优先使用知识库中的信息
            2. 知识库信息不足时，基于通用知识补充
            3. 明确区分知识库内容和推测内容
            4. 回答要准确、有依据
            5. 适当引用知识库来源
            
            知识库内容：
            {{knowledge_context}}
            """);

        // 对话助手（带记忆）
        templates.put("chat", """
            你是一个对话助手，能够记住用户的信息和偏好。
            
            用户记忆：
            {{memory_context}}
            
            原则：
            1. 记住用户的偏好和习惯
            2. 保持对话的连贯性
            3. 适时引用之前的对话内容
            4. 主动学习和更新用户信息
            5. 保持友好和个性化的语气
            """);

        // 代码审查
        templates.put("code_review", """
            你是一个代码审查专家，帮助用户审查代码质量。
            
            审查维度：
            1. 代码风格和规范性
            2. 潜在 Bug 和风险
            3. 性能优化建议
            4. 安全漏洞检查
            5. 可维护性和可读性
            
            输出格式：
            1. 总体评价
            2. 具体问题（按严重程度排序）
            3. 改进建议
            4. 修改后的代码示例
            """);

        // 文档生成
        templates.put("doc", """
            你是一个技术文档专家，帮助用户生成专业的技术文档。
            
            文档要求：
            1. 结构完整：概述、安装、使用、API、示例、FAQ
            2. 语言简洁明了
            3. 包含必要的代码示例
            4. 格式规范统一
            5. 适合目标读者阅读
            """);
    }

    /**
     * 获取模板
     */
    public String getTemplate(String key) {
        return templates.getOrDefault(key, templates.get("default"));
    }

    /**
     * 渲染模板（替换变量）
     */
    public String render(String templateKey, Map<String, String> variables) {
        String template = getTemplate(templateKey);
        
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return template;
    }

    /**
     * 动态构建 RAG 提示词
     */
    public String buildRagPrompt(String basePrompt, String knowledgeContext) {
        if (knowledgeContext == null || knowledgeContext.isEmpty()) {
            return basePrompt;
        }
        
        return basePrompt + "\n\n基于以下知识库内容回答：\n" + 
               "---\n" + 
               knowledgeContext + 
               "---\n\n" +
               "请结合知识库内容回答，如果知识库中没有相关信息，请基于你的知识回答。";
    }

    /**
     * 动态构建记忆提示词
     */
    public String buildMemoryPrompt(String basePrompt, String memoryContext) {
        if (memoryContext == null || memoryContext.isEmpty()) {
            return basePrompt;
        }
        
        return basePrompt + "\n\n用户相关信息：\n" + memoryContext;
    }

    /**
     * 注册自定义模板
     */
    public void registerTemplate(String key, String template) {
        templates.put(key, template);
        log.info("注册提示词模板: {}", key);
    }

    /**
     * 列出所有模板
     */
    public Map<String, String> listTemplates() {
        return new HashMap<>(templates);
    }
}