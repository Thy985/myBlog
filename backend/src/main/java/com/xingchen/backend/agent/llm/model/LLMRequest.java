package com.xingchen.backend.agent.llm.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * LLM请求模型 (Model)
 * 统一封装所有LLM交互请求
 */
@Data
@Builder
public class LLMRequest {

    /** 用户ID */
    private Long userId;

    /** 会话ID */
    private String sessionId;

    /** 用户消息 */
    private String message;

    /** 首选模型 */
    private String preferredModel;

    /** 系统提示词 */
    private String systemPrompt;

    /** 是否需要流式输出 */
    private boolean stream;

    /** 历史消息 */
    private List<Map<String, String>> history;

    /** 认证令牌 */
    private String token;

    /** 额外参数 */
    private Map<String, Object> extraParams;

    /**
     * 获取消息长度
     */
    public int getMessageLength() {
        return message != null ? message.length() : 0;
    }

    /**
     * 是否需要记忆
     */
    public boolean needsMemory() {
        return extraParams != null && Boolean.TRUE.equals(extraParams.get("useMemory"));
    }

    /**
     * 是否需要RAG
     */
    public boolean needsRag() {
        return extraParams != null && Boolean.TRUE.equals(extraParams.get("useRag"));
    }
}