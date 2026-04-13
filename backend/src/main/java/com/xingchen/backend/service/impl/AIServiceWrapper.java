package com.xingchen.backend.service.impl;

import com.xingchen.backend.security.OutputFilter;
import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 服务包装器（安全增强版）
 * 在原有服务基础上添加输出过滤
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class AIServiceWrapper implements AIService {

    private final AIServiceImplV3 delegate;
    private final OutputFilter outputFilter;

    @Override
    public String chat(String message) {
        String response = delegate.chat(message);
        return filterOutput(response);
    }

    @Override
    public String chatWithContext(String message, List<Map<String, String>> history) {
        String response = delegate.chatWithContext(message, history);
        return filterOutput(response);
    }

    @Override
    public String chatWithContext(String message, List<Map<String, String>> history, String systemPrompt) {
        String response = delegate.chatWithContext(message, history, systemPrompt);
        return filterOutput(response);
    }

    @Override
    public String chatWithRag(String message) {
        String response = delegate.chatWithRag(message);
        return filterOutput(response);
    }

    @Override
    public String chatWithRagAndContext(String message, List<Map<String, String>> history) {
        String response = delegate.chatWithRagAndContext(message, history);
        return filterOutput(response);
    }

    @Override
    public String chatWithWebSearch(String message, List<Map<String, String>> history) {
        String response = delegate.chatWithWebSearch(message, history);
        return filterOutput(response);
    }

    @Override
    public String chatWithUserApiKey(Long userId, String message) {
        String response = delegate.chatWithUserApiKey(userId, message);
        return filterOutput(response);
    }

    @Override
    public String chatWithUserApiKeyAndPrompt(Long userId, String message, String systemPrompt) {
        String response = delegate.chatWithUserApiKeyAndPrompt(userId, message, systemPrompt);
        return filterOutput(response);
    }

    @Override
    public Map<String, Object> streamChat(String message, Consumer<String> onChunk) {
        // 流式输出也需要过滤
        Consumer<String> filteredConsumer = token -> {
            OutputFilter.FilterResult result = outputFilter.filter(token);
            if (result.isSafe()) {
                onChunk.accept(result.content());
            } else {
                log.warn("流式输出检测到不安全内容: {}", result.reason());
                onChunk.accept("[内容已过滤]");
            }
        };

        return delegate.streamChat(message, filteredConsumer);
    }

    @Override
    public Map<String, Object> streamChatWithUserApiKey(Long userId, String message, Consumer<String> onChunk) {
        // 流式输出也需要过滤
        Consumer<String> filteredConsumer = token -> {
            OutputFilter.FilterResult result = outputFilter.filter(token);
            if (result.isSafe()) {
                onChunk.accept(result.content());
            } else {
                log.warn("用户流式输出检测到不安全内容: {}, userId={}", result.reason(), userId);
                onChunk.accept("[内容已过滤]");
            }
        };

        return delegate.streamChatWithUserApiKey(userId, message, filteredConsumer);
    }

    @Override
    public void streamChatSSE(String message, SseEmitter emitter) {
        delegate.streamChatSSE(message, emitter);
    }

    @Override
    public void indexArticle(Long articleId, String title, String content) {
        delegate.indexArticle(articleId, title, content);
    }

    @Override
    public void indexAllArticles() {
        delegate.indexAllArticles();
    }

    @Override
    public void clearKnowledgeBase() {
        delegate.clearKnowledgeBase();
    }

    private String filterOutput(String response) {
        if (response == null) {
            return null;
        }

        OutputFilter.FilterResult result = outputFilter.filter(response);

        if (!result.isSafe()) {
            log.warn("输出内容被过滤: {}", result.reason());
            // 返回过滤后的内容，或返回提示信息
            return result.content();
        }

        return result.content();
    }
}
