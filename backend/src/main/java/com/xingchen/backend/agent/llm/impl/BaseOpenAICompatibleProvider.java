package com.xingchen.backend.agent.llm.impl;

import com.xingchen.backend.agent.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.util.MessageBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * OpenAI兼容Provider基类 (Adapter Base)
 * 适用于所有使用OpenAI API格式的提供商（OpenAI, OpenRouter, GLM等）
 */
@Slf4j
public abstract class BaseOpenAICompatibleProvider implements LLMProvider {

    protected final String apiKey;
    protected final String baseUrl;
    protected final String defaultModel;
    protected final int timeoutSeconds;// 超时时间

    protected final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();// 缓存
    protected final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

    protected BaseOpenAICompatibleProvider(String apiKey, String baseUrl, String defaultModel, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel != null ? defaultModel : getDefaultModel();
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 60;
    }

    protected abstract String getDefaultModel();

    @Override
    public String getModelName() {
        return defaultModel;
    }

    @Override
    public AIResponse chat(AIRequest request) {
        long startTime = System.currentTimeMillis();
        String model = request.getPreferredModel() != null ? request.getPreferredModel() : defaultModel;

        try {
            ChatLanguageModel chatModel = getChatModel(model);
            List<ChatMessage> messages = buildMessages(request);

            Response<AiMessage> response = chatModel.generate(messages);

            long elapsed = System.currentTimeMillis() - startTime;
            String content = response.content().text();

            log.debug("{} 调用完成: model={}, elapsed={}ms", getProviderName(), model, elapsed);

            return AIResponse.builder()
                    .type(AIResponse.ResponseType.SUCCESS)
                    .content(content)
                    .model(model)
                    .executionTime(elapsed)
                    .tokenUsage(AIResponse.TokenUsage.builder()
                            .totalTokens(estimateTokens(content) + estimateTokens(request.getMessage()))
                            .build())
                    .build();

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("{} 调用失败: model={}, elapsed={}ms", getProviderName(), model, elapsed, e);
            return AIResponse.error("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 构建消息列表
     * @param request
     * @return
     */
    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        String model = request.getPreferredModel() != null ? request.getPreferredModel() : defaultModel;

        try {
            StreamingChatLanguageModel streamingModel = getStreamingModel(model);
            List<ChatMessage> messages = buildMessages(request);
            StringBuilder contentBuilder = new StringBuilder();

            streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                /**
                 * 每次接收一个token
                 * @param token
                 */
                @Override
                public void onNext(String token) {// 每次接收一个token
                    contentBuilder.append(token);//追加 token
                    onChunk.accept(AIResponse.chunk(token));//发送 chunk
                }
                /**
                 * 输出完成
                 * @param response
                 */
                @Override
                public void onComplete(Response<AiMessage> response) {
                    onChunk.accept(AIResponse.builder()
                            .type(AIResponse.ResponseType.STREAM_END)
                            .content(contentBuilder.toString())
                            .model(model)
                            .build());
                }
                @Override
                public void onError(Throwable error) {
                    log.error("{} 流式调用失败", getProviderName(), error);
                    onChunk.accept(AIResponse.error("流式输出失败: " + error.getMessage()));
                }
            });

        } catch (Exception e) {
            log.error("{} 流式调用失败", getProviderName(), e);
            onChunk.accept(AIResponse.error("流式服务调用失败: " + e.getMessage()));
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }// 支持流式输出

    @Override
    public boolean supportsToolCalling() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }// 是否可用

    @Override
    public ChatLanguageModel getChatModel() {
        return getChatModel(defaultModel);
    }

    @Override
    public StreamingChatLanguageModel getStreamingModel() {
        return getStreamingModel(defaultModel);
    }

    /**
     * 获取 ChatModel
     * @param model
     * @return
     */
    protected synchronized ChatLanguageModel getChatModel(String model) {
        return chatModelCache.computeIfAbsent(model, m ->
            OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(m)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build()
        );
    }
    /**
     * 获取 StreamingChatModel
     * @param model
     * @return
     */
    protected synchronized StreamingChatLanguageModel getStreamingModel(String model) {
        return streamingModelCache.computeIfAbsent(model, m ->
            OpenAiStreamingChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(m)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build()
        );
    }
/**
     * 构建消息列表（委托给 MessageBuilder）
     * @param request AI 请求对象
     * @return ChatMessage 列表
     */
    protected List<ChatMessage> buildMessages(AIRequest request) {
        return MessageBuilder.buildMessages(
                request.getSystemPrompt(),
                request.getHistory(),
                request.getMessage()
        );
    }
}