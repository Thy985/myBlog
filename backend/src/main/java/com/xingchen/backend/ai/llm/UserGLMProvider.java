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
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 用户级GLM Provider
 * 每个用户独立的Provider实例
 */
@Slf4j
public class UserGLMProvider implements LLMProvider {
    
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final int timeoutSeconds = 60;
    
    public UserGLMProvider(String apiKey, String baseUrl, String defaultModel) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://open.bigmodel.cn/api/paas/v4";
        this.defaultModel = defaultModel != null ? defaultModel : "glm-4-flash";
    }
    
    @Override
    public String getProviderName() {
        return "GLM";
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
            
            log.debug("用户GLM调用完成: model={}, elapsed={}ms", model, elapsed);
            
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
            log.error("用户GLM调用失败: model={}, elapsed={}ms", model, elapsed, e);
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
                    log.error("用户流式调用失败", error);
                    onChunk.accept(AIResponse.error("流式输出失败: " + error.getMessage()));
                }
            });
            
        } catch (Exception e) {
            log.error("用户GLM流式调用失败", e);
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
        return List.of("glm-4-plus", "glm-4", "glm-4-air", "glm-4-airx", "glm-4-flash");
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
        
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        
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
        
        messages.add(new UserMessage(request.getMessage()));
        
        return messages;
    }
}