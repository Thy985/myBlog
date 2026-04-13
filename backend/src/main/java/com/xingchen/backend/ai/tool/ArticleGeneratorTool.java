package com.xingchen.backend.ai.tool;

import com.xingchen.backend.service.ArticleGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文章生成工具
 * 示例工具实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleGeneratorTool implements Tool {
    
    private final ArticleGenerationService articleGenerationService;
    
    @Override
    public String getName() {
        return "article_generator";
    }
    
    @Override
    public String getDescription() {
        return "生成博客文章，支持指定主题、字数和风格";
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
                )
        };
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String topic = (String) parameters.get("topic");
        Integer wordCount = (Integer) parameters.getOrDefault("wordCount", 1000);
        String style = (String) parameters.getOrDefault("style", "technical");
        
        log.info("执行文章生成工具: topic={}, wordCount={}, style={}", topic, wordCount, style);
        
        try {
            // 调用现有服务生成文章
            // Article article = articleGenerationService.generate(topic, wordCount, style);
            
            // 模拟返回
            Map<String, Object> result = Map.of(
                    "articleId", 12345,
                    "title", "关于" + topic + "的技术文章",
                    "wordCount", wordCount,
                    "preview", "这是一篇关于" + topic + "的文章预览..."
            );
            
            return ToolResult.success(result, "文章生成成功");
            
        } catch (Exception e) {
            log.error("文章生成失败", e);
            return ToolResult.error("文章生成失败: " + e.getMessage());
        }
    }
    
    @Override
    public long getTimeout() {
        return 60000; // 60秒超时
    }
}