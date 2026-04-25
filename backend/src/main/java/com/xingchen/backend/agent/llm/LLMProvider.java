package com.xingchen.backend.agent.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.util.List;
import java.util.function.Consumer;

/**
 * LLM Provider 核心接口 (Port)
 * 定义所有LLM厂商实现必须遵循的标准
 */
public interface LLMProvider {

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 普通对话
     */
    AIResponse chat(AIRequest request);

    /**
     * 流式对话
     */
    void streamChat(AIRequest request, Consumer<AIResponse> onChunk);

    /**
     * 获取LangChain4j模型（用于高级功能）
     */
    default ChatLanguageModel getChatModel() {
        return null;
    }

    /**
     * 获取流式模型
     */
    default StreamingChatLanguageModel getStreamingModel() {
        return null;
    }

    /**
     * 估算Token数量
     */
    default int estimateTokens(String text) {
        if (text == null) return 0;
        int chineseChars = text.replaceAll("[\\u4e00-\\u9fa5]", "").length();
        int englishWords = text.split("\\s+").length;
        return chineseChars + (int)(englishWords * 1.3);
    }

    /**
     * 是否支持工具调用
     */
    default boolean supportsToolCalling() {
        return false;
    }

    /**
     * 是否支持流式输出
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * 是否可用（已配置且可调用）
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取可用的模型列表
     */
    default List<String> getAvailableModels() {
        return List.of(getModelName());
    }
}