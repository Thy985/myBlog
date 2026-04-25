package com.xingchen.backend.ai.llm;

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

@Slf4j
public abstract class BaseOpenAICompatibleProvider implements LLMProvider {

    protected final String apiKey;
    protected final String baseUrl;
    protected final String defaultModel;
    protected final int timeoutSeconds;

    protected BaseOpenAICompatibleProvider(String apiKey, String baseUrl, String defaultModel, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : getDefaultBaseUrl();
        this.defaultModel = defaultModel != null ? defaultModel : getDefaultModel();
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 60;
    }

    protected abstract String getDefaultModel();

    protected String getDefaultBaseUrl() {
        return "https://api.openai.com/v1";
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
            ChatLanguageModel chatModel = buildChatModel(model);
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

    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        String model = request.getPreferredModel() != null ? request.getPreferredModel() : defaultModel;

        try {
            StreamingChatLanguageModel streamingModel = buildStreamingModel(model);
            List<ChatMessage> messages = buildMessages(request);

            StringBuilder contentBuilder = new StringBuilder();

            streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
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
                    log.error("流式调用失败", error);
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
    }

    @Override
    public boolean supportsToolCalling() {
        return true;
    }

    protected ChatLanguageModel buildChatModel(String model) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    protected StreamingChatLanguageModel buildStreamingModel(String model) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    protected List<ChatMessage> buildMessages(AIRequest request) {
        return MessageBuilder.buildMessages(
                request.getSystemPrompt(),
                request.getHistory(),
                request.getMessage()
        );
    }
}