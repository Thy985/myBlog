package com.xingchen.backend.service.impl;

import com.xingchen.backend.security.InputSanitizer;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.MemoryService;
import com.xingchen.backend.service.UserApiKeyService;
import com.xingchen.backend.service.WebSearchService;
import com.xingchen.backend.config.MultiModelConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * AI 服务实现（安全增强版）
 *
 * 新增特性：
 * 1. 输入安全过滤
 * 2. 熔断降级
 * 3. 多模型路由
 * 4. Token 预算管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIServiceImplV2 implements AIService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final WebSearchService webSearchService;
    private final UserApiKeyService userApiKeyService;
    private final MemoryService memoryService;
    private final InputSanitizer inputSanitizer;
    private final MultiModelConfig.ModelRouter modelRouter;
    private final MultiModelConfig.ModelRegistry modelRegistry;

    @Value("${ai.system.prompt:你是一个智能助手，帮助用户完成各种任务。}")
    private String baseSystemPrompt;

    @Value("${ai.token.budget:8000}")
    private int tokenBudget;

    /**
     * 安全过滤后的对话
     */
    @Override
    @CircuitBreaker(name = "aiChat", fallbackMethod = "fallbackChat")
    @Retry(name = "aiChat")
    public String chat(String message) {
        // 输入安全过滤
        String sanitizedMessage = inputSanitizer.sanitize(message);

        ChatLanguageModel model = modelRouter.getModel("default");
        return model.generate(sanitizedMessage);
    }

    /**
     * 带上下文的对话（安全增强）
     */
    @Override
    @CircuitBreaker(name = "aiChatContext", fallbackMethod = "fallbackChatWithContext")
    public String chatWithContext(String message, List<Map<String, String>> history) {
        // 输入安全过滤
        String sanitizedMessage = inputSanitizer.sanitize(message);

        // 构建消息列表
        List<ChatMessage> messages = buildMessages(sanitizedMessage, history, null);

        // Token 预算检查
        if (estimateTokens(messages) > tokenBudget) {
            messages = compressMessages(messages);
        }

        ChatLanguageModel model = modelRouter.getModel("default");
        Response<AiMessage> response = model.generate(messages);
        return response.content().text();
    }

    /**
     * RAG 增强对话（安全增强）
     */
    @Override
    @CircuitBreaker(name = "aiChatRag", fallbackMethod = "fallbackChatWithRag")
    public String chatWithRagAndContext(String message, List<Map<String, String>> history) {
        // 输入安全过滤
        String sanitizedMessage = inputSanitizer.sanitize(message);

        // 检索知识库
        String kbContext = knowledgeBaseService.search(sanitizedMessage, 3);

        // 构建增强提示
        String enhancedPrompt = buildRagPrompt(sanitizedMessage, kbContext);

        return chatWithContext(enhancedPrompt, history);
    }

    /**
     * 任务类型路由对话
     */
    public String chatWithRouting(String taskType, String message) {
        String sanitizedMessage = inputSanitizer.sanitize(message);
        ChatLanguageModel model = modelRouter.getModel(taskType);
        return model.generate(sanitizedMessage);
    }

    // ========== Fallback 方法 ==========

    private String fallbackChat(String message, Exception ex) {
        log.warn("AI 服务降级，使用 fallback 模型: {}", ex.getMessage());
        try {
            ChatLanguageModel fallbackModel = modelRouter.getFallback();
            return fallbackModel.generate(message);
        } catch (Exception e) {
            log.error("Fallback 也失败了", e);
            return "AI 服务暂时不可用，请稍后重试。";
        }
    }

    private String fallbackChatWithContext(String message, List<Map<String, String>> history, Exception ex) {
        log.warn("AI 上下文对话降级: {}", ex.getMessage());
        return "服务繁忙，请简化您的问题后重试。";
    }

    private String fallbackChatWithRag(String message, List<Map<String, String>> history, Exception ex) {
        log.warn("AI RAG 对话降级: {}", ex.getMessage());
        // 降级为普通对话
        return chatWithContext(message, history);
    }

    private SseEmitter fallbackStream(String message, Consumer<String> onToken, Exception ex) {
        log.warn("AI 流式对话降级: {}", ex.getMessage());
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event()
                    .data("服务暂时不可用，请稍后重试")
                    .name("error"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    // ========== 私有方法 ==========

    private List<ChatMessage> buildMessages(String message, List<Map<String, String>> history, String systemPrompt) {
        List<ChatMessage> messages = new ArrayList<>();

        // System Prompt
        String finalSystemPrompt = systemPrompt != null ? systemPrompt : baseSystemPrompt;
        messages.add(new SystemMessage(finalSystemPrompt));

        // 历史消息
        if (history != null) {
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AiMessage(content));
                }
            }
        }

        // 当前消息
        messages.add(new UserMessage(message));

        return messages;
    }

    private String buildRagPrompt(String message, String kbContext) {
        if (kbContext == null || kbContext.isEmpty()) {
            return message;
        }
        return String.format("""
            基于以下知识库内容回答问题：

            知识库内容：
            %s

            用户问题：%s

            请结合知识库内容回答，如果知识库中没有相关信息，请基于你的知识回答。
            """, kbContext, message);
    }

    private int estimateTokens(List<ChatMessage> messages) {
        // 简单估算：每个字符约 0.5 个 token
        int totalChars = messages.stream()
                .mapToInt(m -> m.text().length())
                .sum();
        return totalChars / 2;
    }

    private List<ChatMessage> compressMessages(List<ChatMessage> messages) {
        // 保留 system prompt 和最近的消息
        List<ChatMessage> compressed = new ArrayList<>();
        compressed.add(messages.get(0)); // System prompt

        // 只保留最近 10 条
        int start = Math.max(1, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            compressed.add(messages.get(i));
        }

        return compressed;
    }

    // ========== 接口方法实现 ==========

    @Override
    public String chatWithRag(String message) {
        return chatWithRagAndContext(message, null);
    }

    @Override
    public String chatWithWebSearch(String message, List<Map<String, String>> history) {
        // 先进行网络搜索
        var searchResults = webSearchService.search(message, 3);

        // 构建搜索结果摘要
        StringBuilder sb = new StringBuilder();
        for (var result : searchResults) {
            sb.append("- ").append(result.title()).append(": ").append(result.content()).append("\n");
        }

        // 构建增强提示
        String enhancedPrompt = String.format("""
            基于以下网络搜索结果回答问题：

            搜索结果：
            %s

            用户问题：%s

            请结合搜索结果回答。
            """, sb.toString(), message);

        return chatWithContext(enhancedPrompt, history);
    }

    @Override
    public String chatWithUserApiKey(Long userId, String message) {
        // 使用用户的 API Key 进行对话
        // 这里简化实现，实际应该根据用户配置选择模型
        return chat(message);
    }

    @Override
    public String chatWithUserApiKeyAndPrompt(Long userId, String message, String systemPrompt) {
        // 使用用户的 API Key 和自定义系统提示词进行对话
        String sanitizedMessage = inputSanitizer.sanitize(message);
        List<ChatMessage> messages = buildMessages(sanitizedMessage, null, systemPrompt);

        ChatLanguageModel model = modelRouter.getModel("default");
        Response<AiMessage> response = model.generate(messages);
        return response.content().text();
    }

    @Override
    public Map<String, Object> streamChat(String message, Consumer<String> onChunk) {
        // 简化实现，返回空结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "流式对话已启动");
        return result;
    }

    @Override
    public void streamChatSSE(String message, SseEmitter emitter) {
        // 输入安全过滤
        String sanitizedMessage = inputSanitizer.sanitize(message);

        try {
            StreamingChatLanguageModel model = modelRegistry.getStreaming("default");

            model.generate(sanitizedMessage, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    try {
                        emitter.send(SseEmitter.event()
                                .data(token)
                                .name("message"));
                    } catch (IOException e) {
                        log.error("发送 SSE 消息失败", e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式生成失败", error);
                    emitter.completeWithError(error);
                }
            });
        } catch (Exception e) {
            log.error("SSE 流式对话失败", e);
            try {
                emitter.send(SseEmitter.event()
                        .data("服务暂时不可用，请稍后重试")
                        .name("error"));
                emitter.complete();
            } catch (IOException ioException) {
                emitter.completeWithError(ioException);
            }
        }
    }

    @Override
    public void indexArticle(Long articleId, String title, String content) {
        // 将文章索引到知识库
        knowledgeBaseService.addDocument(articleId, title, content);
    }

    @Override
    public void indexAllArticles() {
        // 索引所有文章 - 简化实现
        log.info("索引所有文章功能待实现");
    }

    @Override
    public void clearKnowledgeBase() {
        // 清空知识库
        knowledgeBaseService.clearAll();
    }
}
