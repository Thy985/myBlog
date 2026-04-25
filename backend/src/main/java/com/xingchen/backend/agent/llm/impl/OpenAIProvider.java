package com.xingchen.backend.agent.llm.impl;

import com.xingchen.backend.agent.llm.LLMProvider;
import com.xingchen.backend.agent.llm.model.LLMRequest;
import com.xingchen.backend.agent.llm.model.LLMResponse;
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
import java.util.function.Consumer;

/**
 * OpenAI Provider (Adapter)
 * 基于OpenAI API的实现
 */
@Slf4j
public class OpenAIProvider implements LLMProvider {

    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final int timeoutSeconds;

    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingModel;

    public OpenAIProvider(String apiKey, String baseUrl, String defaultModel, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.openai.com/v1";
        this.defaultModel = defaultModel != null ? defaultModel : "gpt-3.5-turbo";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 60;
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public String getModelName() {
        return defaultModel;
    }

    @Override
    public AIResponse chat(AIRequest request) {
        long startTime = System.currentTimeMillis();
        String model = request.getPreferredModel() != null ? request.getPreferredModel() : defaultModel;

        try {
            ChatLanguageModel chatModelInstance = getChatModelInstance(model);
            List<ChatMessage> messages = buildMessages(request);

            Response<AiMessage> response = chatModelInstance.generate(messages);
            long elapsed = System.currentTimeMillis() - startTime;
            String content = response.content().text();

            log.debug("OpenAI 调用完成: model={}, elapsed={}ms", model, elapsed);

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
            log.error("OpenAI 调用失败: model={}, elapsed={}ms", model, elapsed, e);
            return AIResponse.error("AI服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        String model = request.getPreferredModel() != null ? request.getPreferredModel() : defaultModel;

        try {
            StreamingChatLanguageModel streamingModelInstance = getStreamingModelInstance(model);
            List<ChatMessage> messages = buildMessages(request);
            StringBuilder contentBuilder = new StringBuilder();

            streamingModelInstance.generate(messages, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    contentBuilder.append(token);
                    onChunk.accept(AIResponse.chunk(token));
                }

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
                    log.error("OpenAI 流式调用失败", error);
                    onChunk.accept(AIResponse.error("流式输出失败: " + error.getMessage()));
                }
            });

        } catch (Exception e) {
            log.error("OpenAI 流式调用失败", e);
            onChunk.accept(AIResponse.error("流式服务调用失败: " + e.getMessage()));
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public boolean supportsToolCalling() {
        return true;
    }

    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo",
                "gpt-4",
                "gpt-3.5-turbo"
        );
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public ChatLanguageModel getChatModel() {
        return getChatModelInstance(defaultModel);
    }

    @Override
    public StreamingChatLanguageModel getStreamingModel() {
        return getStreamingModelInstance(defaultModel);
    }
    /
    private synchronized ChatLanguageModel getChatModelInstance(String model) {
        if (chatModel == null) {
            chatModel = OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        }
        return chatModel;
    }
    /**
     * 获取流式模型
     *
     * @param model
     * @return
     */
    private synchronized StreamingChatLanguageModel getStreamingModelInstance(String model) {
        if (streamingModel == null) {
            streamingModel = OpenAiStreamingChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        }
        return streamingModel;
    }
    /**
     * 构建消息
     *
     * @param request
     * @return
     */
    protected List<ChatMessage> buildMessages(AIRequest request) {
        return MessageBuilder.buildMessages(
                request.getSystemPrompt(),
                request.getHistory(),
                request.getMessage()
        );
    }
}