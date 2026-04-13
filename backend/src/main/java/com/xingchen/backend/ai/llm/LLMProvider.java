package com.xingchen.backend.ai.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

import java.util.function.Consumer;


/**
 * LLM提供商接口
 * 封装具体模型实现
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
        // 简单估算：中文1字≈1token，英文1词≈1.3token
        if (text == null) return 0;
        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
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
    default java.util.List<String> getAvailableModels() {
        return java.util.List.of(getModelName());
    }
}