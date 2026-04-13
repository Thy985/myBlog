package com.xingchen.backend.ai.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;

import java.util.function.Consumer;

/**
 * LLM服务接口
 * 抽象不同LLM提供商
 */
public interface LLMService {
    
    /**
     * 普通对话
     * @param request 请求
     * @return 响应
     */
    AIResponse chat(AIRequest request);
    
    /**
     * 流式对话
     * @param request 请求
     * @param onChunk 流式回调
     */
    void streamChat(AIRequest request, Consumer<AIResponse> onChunk);
    
    /**
     * 获取服务名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * 获取模型列表
     */
    default java.util.List<String> getAvailableModels() {
        return java.util.List.of();
    }
}