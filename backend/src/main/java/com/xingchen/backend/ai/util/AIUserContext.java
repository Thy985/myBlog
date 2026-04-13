package com.xingchen.backend.ai.util;

import lombok.Builder;
import lombok.Data;

/**
 * AI用户上下文
 * 封装用户相关的AI配置信息
 */
@Data
@Builder
public class AIUserContext {
    
    /** 用户ID */
    private Long userId;
    
    /** 会话ID */
    private String sessionId;
    
    /** 用户配置的Provider */
    private String provider;
    
    /** 用户配置的模型 */
    private String model;
    
    /** 温度参数 */
    private Double temperature;
    
    /** 最大Token数 */
    private Integer maxTokens;
    
    /** Top P参数 */
    private Double topP;
    
    /** 是否启用记忆 */
    private Boolean memoryEnabled;
    
    /** 是否启用RAG */
    private Boolean ragEnabled;
    
    /** 用户配额信息 */
    private UserQuota quota;
    
    /**
     * 获取有效的温度参数
     */
    public double getEffectiveTemperature() {
        return temperature != null ? temperature : 0.7;
    }
    
    /**
     * 获取有效的最大Token数
     */
    public int getEffectiveMaxTokens() {
        return maxTokens != null ? maxTokens : 4096;
    }
    
    /**
     * 获取有效的Top P
     */
    public double getEffectiveTopP() {
        return topP != null ? topP : 0.9;
    }
    
    /**
     * 检查是否有配额
     */
    public boolean hasQuota() {
        return quota != null && quota.hasRemaining();
    }
    
    /**
     * 用户配额
     */
    @Data
    @Builder
    public static class UserQuota {
        private Integer totalQuota;
        private Integer usedQuota;
        private Integer remainingQuota;
        
        public boolean hasRemaining() {
            return remainingQuota != null && remainingQuota > 0;
        }
        
        /**
         * 消耗配额
         */
        public void consume(int tokens) {
            if (usedQuota == null) {
                usedQuota = 0;
            }
            usedQuota += tokens;
            if (remainingQuota != null) {
                remainingQuota = Math.max(0, remainingQuota - tokens);
            }
        }
    }
    
    /**
     * 创建默认上下文
     */
    public static AIUserContext defaultContext(Long userId) {
        return AIUserContext.builder()
                .userId(userId)
                .temperature(0.7)
                .maxTokens(4096)
                .topP(0.9)
                .memoryEnabled(true)
                .ragEnabled(false)
                .build();
    }
}