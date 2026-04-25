package com.xingchen.backend.agent.llm;

import com.xingchen.backend.agent.llm.model.LLMRequest;
import com.xingchen.backend.agent.llm.model.LLMResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * LLM Provider 管理器 (Router)
 * 负责根据配置和请求选择具体的 LLM Provider
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LLMProviderManager {

    private final List<LLMProvider> providers;

    @Value("${ai.agent.llm.default-provider:OpenAI}")
    private String defaultProviderName;

    private final Map<String, LLMProvider> providerCache = new ConcurrentHashMap<>();

    /**
     * 执行对话
     */
    public LLMResponse chat(LLMRequest request) {
        LLMProvider provider = getProvider(request.getPreferredModel());
        if (provider == null) {
            log.warn("未找到可用的LLM Provider: {}", request.getPreferredModel());
            return LLMResponse.error("未找到可用的LLM提供商");
        }

        log.debug("选择Provider: {}, 模型: {}", provider.getProviderName(), provider.getModelName());

        com.xingchen.backend.ai.model.AIRequest aiRequest = convertToAIRequest(request);
        com.xingchen.backend.ai.model.AIResponse aiResponse = provider.chat(aiRequest);

        return convertToLLMResponse(aiResponse);
    }

    /**
     * 执行流式对话
     */
    public void streamChat(LLMRequest request, Consumer<LLMResponse> onChunk) {
        LLMProvider provider = getProvider(request.getPreferredModel());
        if (provider == null) {
            log.warn("未找到可用的LLM Provider: {}", request.getPreferredModel());
            onChunk.accept(LLMResponse.error("未找到可用的LLM提供商"));
            return;
        }

        com.xingchen.backend.ai.model.AIRequest aiRequest = convertToAIRequest(request);

        provider.streamChat(aiRequest, aiChunk -> {
            onChunk.accept(convertToLLMResponse(aiChunk));
        });
    }

    /**
     * 获取指定名称的Provider
     */
    private LLMProvider getProvider(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            return getDefaultProvider();
        }

        return providerCache.computeIfAbsent(providerName, name ->
            providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(this::getDefaultProvider)
        );
    }

    /**
     * 获取默认Provider
     */
    private LLMProvider getDefaultProvider() {
        return providerCache.computeIfAbsent(defaultProviderName, name ->
            providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() ->
                    providers.stream()
                        .filter(LLMProvider::isAvailable)
                        .findFirst()
                        .orElse(null)
                )
        );
    }

    /**
     * 获取所有可用的Provider
     */
    public List<LLMProvider> getAvailableProviders() {
        return providers.stream()
            .filter(LLMProvider::isAvailable)
            .toList();
    }

    /**
     * 获取指定Provider
     */
    public LLMProvider getProviderByName(String name) {
        return providers.stream()
            .filter(p -> p.getProviderName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * 切换默认Provider
     */
    public void setDefaultProvider(String providerName) {
        this.defaultProviderName = providerName;
        providerCache.clear();
        log.info("切换默认Provider为: {}", providerName);
    }

    /**
     * 转换AIRequest到LLMRequest
     */
    private com.xingchen.backend.ai.model.AIRequest convertToAIRequest(LLMRequest request) {
        return com.xingchen.backend.ai.model.AIRequest.builder()
            .userId(request.getUserId())
            .sessionId(request.getSessionId())
            .message(request.getMessage())
            .preferredModel(request.getPreferredModel())
            .systemPrompt(request.getSystemPrompt())
            .stream(request.isStream())
            .history(request.getHistory())
            .token(request.getToken())
            .extraParams(request.getExtraParams())
            .build();
    }

    /**
     * 转换AIResponse到LLMResponse
     */
    private LLMResponse convertToLLMResponse(com.xingchen.backend.ai.model.AIResponse response) {
        if (response == null) {
            return LLMResponse.error("LLM响应为空");
        }

        LLMResponse.LLMResponseBuilder builder = LLMResponse.builder()
            .responseId(response.getResponseId())
            .content(response.getContent())
            .model(response.getModel())
            .executionTime(response.getExecutionTime())
            .timestamp(response.getTimestamp())
            .extraData(response.getExtraData());

        if (response.getType() != null) {
            switch (response.getType()) {
                case SUCCESS -> builder.type(LLMResponse.ResponseType.SUCCESS);
                case ERROR -> builder.type(LLMResponse.ResponseType.ERROR);
                case STREAM_CHUNK -> builder.type(LLMResponse.ResponseType.STREAM_CHUNK);
                case STREAM_END -> builder.type(LLMResponse.ResponseType.STREAM_END);
                case TOOL_CALL -> builder.type(LLMResponse.ResponseType.TOOL_CALL);
            }
        }

        if (response.getTokenUsage() != null) {
            builder.tokenUsage(LLMResponse.TokenUsage.builder()
                .promptTokens(response.getTokenUsage().getPromptTokens())
                .completionTokens(response.getTokenUsage().getCompletionTokens())
                .totalTokens(response.getTokenUsage().getTotalTokens())
                .build());
        }

        return builder.build();
    }
}