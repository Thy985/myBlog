package com.xingchen.backend.ai.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * OpenRouter提供商实现
 * 支持多模型路由
 */
@Component
@Slf4j
public class OpenRouterProvider implements LLMProvider {
    
    @Value("${openrouter.api-key:}")
    private String apiKey;
    
    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;
    
    @Value("${openrouter.default-model:openai/gpt-3.5-turbo}")
    private String defaultModel;
    
    @Value("${openrouter.timeout:60}")
    private int timeoutSeconds;
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public OpenRouterProvider(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }
    
    @Override
    public String getProviderName() {
        return "OpenRouter";
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
            
            log.debug("OpenRouter调用完成: model={}, elapsed={}ms", model, elapsed);
            
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
            log.error("OpenRouter调用失败: model={}, elapsed={}ms", model, elapsed, e);
            
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
            
            streamingModel.generate(messages, new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
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
            log.error("OpenRouter流式调用失败", e);
            onChunk.accept(AIResponse.error("流式服务调用失败: " + e.getMessage()));
        }
    }
    
    @Override
    public ChatLanguageModel getChatModel() {
        return buildChatModel(defaultModel);
    }
    
    @Override
    public StreamingChatLanguageModel getStreamingModel() {
        return buildStreamingModel(defaultModel);
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
                "openai/gpt-4",
                "openai/gpt-4-turbo",
                "openai/gpt-3.5-turbo",
                "anthropic/claude-3-opus",
                "anthropic/claude-3-sonnet",
                "google/gemini-pro",
                "meta-llama/llama-2-70b-chat"
        );
    }
    
    private ChatLanguageModel buildChatModel(String model) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
    
    private StreamingChatLanguageModel buildStreamingModel(String model) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
    
    private List<ChatMessage> buildMessages(AIRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 系统提示词
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        
        // 历史消息
        if (request.getHistory() != null) {
            for (var entry : request.getHistory()) {
                String role = entry.get("role");
                String content = entry.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AiMessage(content));
                }
            }
        }
        
        // 当前消息
        messages.add(new UserMessage(request.getMessage()));
        
        return messages;
    }
}