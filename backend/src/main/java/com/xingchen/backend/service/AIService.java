package com.xingchen.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AIService {

    String chat(String message);

    String chatWithContext(String message, List<Map<String, String>> history);

    /**
     * 带上下文和系统提示词的对话
     */
    default String chatWithContext(String message, List<Map<String, String>> history, String systemPrompt) {
        return chatWithContext(message, history);
    }

    String chatWithRag(String message);

    String chatWithRagAndContext(String message, List<Map<String, String>> history);

    String chatWithWebSearch(String message, List<Map<String, String>> history);

    /**
     * 使用用户配置的 API Key 进行对话
     * @param userId 用户ID
     * @param message 消息内容
     * @return AI 回复
     */
    String chatWithUserApiKey(Long userId, String message);

    /**
     * 使用用户配置的 API Key 进行对话（带自定义系统提示词）
     * @param userId 用户ID
     * @param message 消息内容
     * @param systemPrompt 自定义系统提示词
     * @return AI 回复
     */
    String chatWithUserApiKeyAndPrompt(Long userId, String message, String systemPrompt);

    Map<String, Object> streamChat(String message, java.util.function.Consumer<String> onChunk);

    /**
     * 使用用户 API Key 的流式对话
     * @param userId 用户ID
     * @param message 消息内容
     * @param onChunk 流式回调
     * @return 结果Map
     */
    Map<String, Object> streamChatWithUserApiKey(Long userId, String message, java.util.function.Consumer<String> onChunk);

    void streamChatSSE(String message, SseEmitter emitter);

    void indexArticle(Long articleId, String title, String content);

    void indexAllArticles();

    void clearKnowledgeBase();
}
