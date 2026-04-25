package com.xingchen.backend.agent.llm.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * LLM响应模型 (Model)
 * 统一封装所有LLM交互响应
 */
@Data
@Builder
public class LLMResponse {

    /** 响应ID */
    private String responseId;

    /** 响应内容 */
    private String content;

    /** 响应类型 */
    private ResponseType type;

    /** 使用的模型 */
    private String model;

    /** Token使用量 */
    private TokenUsage tokenUsage;

    /** 执行时间(ms) */
    private long executionTime;

    /** 工具调用记录 */
    private List<ToolCallRecord> toolCalls;

    /** 来源引用 */
    private List<SourceReference> sources;

    /** 响应时间 */
    private LocalDateTime timestamp;

    /** 额外数据 */
    private Map<String, Object> extraData;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return type != ResponseType.ERROR;
    }

    /**
     * 创建成功响应
     */
    public static LLMResponse success(String content) {
        return LLMResponse.builder()
                .type(ResponseType.SUCCESS)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建错误响应
     */
    public static LLMResponse error(String errorMessage) {
        return LLMResponse.builder()
                .type(ResponseType.ERROR)
                .content(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建流式响应块
     */
    public static LLMResponse chunk(String chunk) {
        return LLMResponse.builder()
                .type(ResponseType.STREAM_CHUNK)
                .content(chunk)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public enum ResponseType {
        SUCCESS,
        ERROR,
        STREAM_CHUNK,
        STREAM_END,
        TOOL_CALL
    }

    @Data
    @Builder
    public static class TokenUsage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }

    @Data
    @Builder
    public static class ToolCallRecord {
        private String toolName;
        private Map<String, Object> parameters;
        private Object result;
        private long executionTime;
    }

    @Data
    @Builder
    public static class SourceReference {
        private String title;
        private String url;
        private String snippet;
        private double relevance;
    }
}