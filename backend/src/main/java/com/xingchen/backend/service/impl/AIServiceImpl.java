package com.xingchen.backend.service.impl;

import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.MemoryService;
import com.xingchen.backend.service.UserApiKeyService;
import com.xingchen.backend.service.WebSearchService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * AI 服务实现（完整重构版）
 *
 * 对标 OpenClaw 架构：
 * 1. LangChain4j 替代手写 HTTP
 * 2. System Prompt 动态组装（人格 + 长期记忆 + 知识库上下文）
 * 3. Token 估算 + 自动压缩
 * 4. 多模型支持（用户自定义 Key + 系统默认 + 降级）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final WebSearchService webSearchService;
    private final UserApiKeyService userApiKeyService;
    private final MemoryService memoryService;
    private final ChatLanguageModel defaultChatModel;
    private final StreamingChatLanguageModel defaultStreamModel;

    @Value("${glm.api.key}")
    private String glmApiKey;

    @Value("${glm.api.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String glmBaseUrl;

    @Value("${glm.api.model:glm-4-flash}")
    private String glmModel;

    // ============================================================
    // System Prompt 动态组装（对标 OpenClaw prompt_builder）
    // ============================================================

    private static final String SOUL = """
            你是「小星」，星尘博客的 AI 助手 ✨
            一个活泼开朗、热爱编程的 AI 小助手。
            喜欢用简洁有趣的方式回答问题，偶尔加 emoji。
            """;

    private static final String RULES = """
            【回答原则】
            - 有相关文章时，优先引用博客已有内容
            - 回答简洁精准，避免冗长废话
            - 代码用 Markdown 代码块格式
            - 不确定的事情要说明，不编造
            """;

    /**
     * 动态构建 System Prompt（核心方法）
     * 按需注入：人格 + 用户记忆 + 知识库上下文 + 搜索结果
     */
    private String buildSystemPrompt(Long userId, String userMessage, boolean useRag, boolean useWebSearch) {
        StringBuilder prompt = new StringBuilder();

        // 1. 人格核心
        prompt.append(SOUL).append("\n");

        // 2. 注入长期记忆（对标 OpenClaw MEMORY.md）
        if (userId != null) {
            try {
                String memory = memoryService.getUserMemory(userId);
                if (memory != null && !memory.isBlank() && memory.length() > 50) {
                    // 只取有实际内容的记忆条目，限制长度
                    String compactMemory = extractMemoryItems(memory, 15);
                    if (!compactMemory.isBlank()) {
                        prompt.append("\n【用户记忆（跨会话共享）】\n").append(compactMemory).append("\n");
                    }
                }
            } catch (Exception e) {
                log.debug("读取用户记忆失败: {}", e.getMessage());
            }
        }

        // 3. 注入知识库检索结果（RAG）
        if (useRag && userMessage != null) {
            try {
                String relevant = knowledgeBaseService.search(userMessage, 3);
                if (relevant != null && !relevant.isBlank()) {
                    prompt.append("\n【博客知识库检索结果】\n请优先参考以下内容回答：\n")
                            .append(truncate(relevant, 2000)).append("\n");
                }
            } catch (Exception e) {
                log.debug("知识库检索失败: {}", e.getMessage());
            }
        }

        // 4. 注入联网搜索结果
        if (useWebSearch && userMessage != null) {
            try {
                List<WebSearchService.SearchResult> results = webSearchService.search(userMessage, 3);
                if (results != null && !results.isEmpty()) {
                    prompt.append("\n【联网搜索结果】\n");
                    for (WebSearchService.SearchResult r : results) {
                        prompt.append("- ").append(r.title()).append(": ").append(r.content()).append("\n");
                    }
                }
            } catch (Exception e) {
                log.debug("联网搜索失败: {}", e.getMessage());
            }
        }

        // 5. 行为规范
        prompt.append("\n").append(RULES);

        return prompt.toString();
    }

    /**
     * 从 MEMORY.md 中提取有效记忆条目（去掉空分类和模板文字）
     */
    private String extractMemoryItems(String memory, int maxItems) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (String line : memory.split("\n")) {
            line = line.trim();
            if (line.startsWith("- ") && count < maxItems) {
                // 去掉时间戳，精简注入
                String clean = line.replaceAll("\\s*\\(\\d{4}-\\d{2}-\\d{2}\\)\\s*$", "")
                        .replaceAll("\\s*\\(更新于\\s*\\d{4}-\\d{2}-\\d{2}\\)\\s*$", "");
                result.append(clean).append("\n");
                count++;
            }
        }
        return result.toString();
    }

    // ============================================================
    // Token 估算（对标 OpenClaw token 管理）
    // ============================================================

    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chinese = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') chinese++;
        }
        int asciiWords = text.replaceAll("[\\u4e00-\\u9fff]", "").split("\\s+").length;
        return (int) (chinese * 1.5 + asciiWords * 0.75 + text.length() * 0.1);
    }

    private int estimateMessagesTokens(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage msg : messages) {
            total += estimateTokens(msg.toString()) + 4;
        }
        return total;
    }

    private static final int MAX_CONTEXT_TOKENS = 30000; // 留 2000 给输出

    // ============================================================
    // 基础对话
    // ============================================================

    @Override
    public String chat(String message) {
        return chatInternal(null, message, Collections.emptyList(), false, false);
    }

    @Override
    public String chatWithContext(String message, List<Map<String, String>> history) {
        return chatInternal(null, message, history, false, false);
    }

    @Override
    public String chatWithRag(String message) {
        return chatInternal(null, message, Collections.emptyList(), true, false);
    }

    @Override
    public String chatWithRagAndContext(String message, List<Map<String, String>> history) {
        return chatInternal(null, message, history, true, false);
    }

    @Override
    public String chatWithWebSearch(String message, List<Map<String, String>> history) {
        return chatInternal(null, message, history, false, true);
    }

    @Override
    public String chatWithUserApiKey(Long userId, String message) {
        try {
            ChatLanguageModel model = buildUserModel(userId);
            String systemPrompt = buildSystemPrompt(userId, message, true, false);
            List<ChatMessage> messages = List.of(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(message)
            );
            Response<AiMessage> response = model.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("用户 {} API Key 对话失败，降级到默认模型: {}", userId, e.getMessage());
            return chat(message);
        }
    }

    @Override
    public String chatWithUserApiKeyAndPrompt(Long userId, String message, String systemPrompt) {
        try {
            ChatLanguageModel model = buildUserModel(userId);
            List<ChatMessage> messages = List.of(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(message)
            );
            Response<AiMessage> response = model.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("用户 {} API Key 对话失败，降级到默认模型: {}", userId, e.getMessage());
            return chat(message);
        }
    }

    /**
     * 统一对话入口（核心方法）
     */
    private String chatInternal(Long userId, String message, List<Map<String, String>> history,
                                 boolean useRag, boolean useWebSearch) {
        try {
            String systemPrompt = buildSystemPrompt(userId, message, useRag, useWebSearch);
            List<ChatMessage> messages = buildMessages(systemPrompt, history, message);

            // Token 检查：超限时裁剪历史
            int tokens = estimateMessagesTokens(messages);
            if (tokens > MAX_CONTEXT_TOKENS && history != null && history.size() > 4) {
                log.info("Token 超限 ({})，裁剪历史消息", tokens);
                // 只保留最近 6 条
                List<Map<String, String>> trimmed = history.subList(
                        Math.max(0, history.size() - 6), history.size());
                messages = buildMessages(systemPrompt, trimmed, message);
            }

            Response<AiMessage> response = defaultChatModel.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("AI 对话失败: {}", e.getMessage(), e);
            return "抱歉，我暂时无法回答，请稍后再试 😅";
        }
    }

    // ============================================================
    // 流式对话
    // ============================================================

    @Override
    public Map<String, Object> streamChat(String message, Consumer<String> onChunk) {
        Map<String, Object> result = new HashMap<>();
        StringBuilder fullResponse = new StringBuilder();

        try {
            List<ChatMessage> messages = List.of(
                    SystemMessage.from(buildSystemPrompt(null, message, false, false)),
                    UserMessage.from(message)
            );

            CompletableFuture<Void> future = new CompletableFuture<>();

            defaultStreamModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    fullResponse.append(token);
                    onChunk.accept(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    future.complete(null);
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式对话错误: {}", error.getMessage());
                    future.completeExceptionally(error);
                }
            });

            future.get(120, TimeUnit.SECONDS);
            result.put("response", fullResponse.toString());
            result.put("success", true);
        } catch (Exception e) {
            log.error("流式对话失败: {}", e.getMessage(), e);
            result.put("response", "抱歉，我暂时无法回答 😅");
            result.put("success", false);
        }

        return result;
    }

    @Override
    @Async
    public void streamChatSSE(String message, SseEmitter emitter) {
        try {
            List<ChatMessage> messages = List.of(
                    SystemMessage.from(buildSystemPrompt(null, message, false, false)),
                    UserMessage.from(message)
            );

            defaultStreamModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    try {
                        emitter.send(SseEmitter.event().name("message").data(token));
                    } catch (IOException e) {
                        log.warn("SSE 发送失败: {}", e.getMessage());
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    try {
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.warn("SSE 完成通知失败: {}", e.getMessage());
                    }
                }

                @Override
                public void onError(Throwable error) {
                    log.error("SSE 流式错误: {}", error.getMessage());
                    try {
                        emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                    } catch (IOException ignored) {}
                    emitter.completeWithError(error);
                }
            });
        } catch (Exception e) {
            log.error("SSE 启动失败: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    // ============================================================
    // 知识库操作
    // ============================================================

    @Override
    public void indexArticle(Long articleId, String title, String content) {
        knowledgeBaseService.addDocument(articleId, title, content);
        log.info("文章已索引: {} - {}", articleId, title);
    }

    @Override
    public void indexAllArticles() {
        log.info("开始索引所有文章...");
    }

    @Override
    public void clearKnowledgeBase() {
        knowledgeBaseService.clearAll();
        log.info("知识库已清空");
    }

    // ============================================================
    // 内部工具方法
    // ============================================================

    private List<ChatMessage> buildMessages(String systemPrompt, List<Map<String, String>> history, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        if (history != null) {
            int start = Math.max(0, history.size() - 40);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                String role = msg.get("role");
                String content = msg.get("content");
                if (content == null || content.isBlank()) continue;

                if ("user".equals(role)) {
                    messages.add(UserMessage.from(content));
                } else if ("assistant".equals(role)) {
                    messages.add(AiMessage.from(content));
                }
            }
        }

        messages.add(UserMessage.from(userMessage));
        return messages;
    }

    private ChatLanguageModel buildUserModel(Long userId) {
        UserApiKey config = userApiKeyService.getByUserId(userId);
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            return defaultChatModel;
        }

        String apiKey = config.getDecryptedApiKey();
        String baseUrl = config.getBaseUrl();
        String model = config.getDefaultModel();

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com/v1")
                .modelName(model != null && !model.isBlank() ? model : "gpt-4o-mini")
                .temperature(0.7)
                .maxTokens(4096)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
