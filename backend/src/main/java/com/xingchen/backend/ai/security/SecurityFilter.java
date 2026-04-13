package com.xingchen.backend.ai.security;

import com.xingchen.backend.ai.model.AIRequest;

/**
 * 安全过滤器接口
 */
public interface SecurityFilter {
    
    /**
     * 执行过滤
     * @param request 请求
     * @return 过滤后的请求
     * @throws SecurityException 如果检测到安全问题
     */
    AIRequest filter(AIRequest request) throws SecurityException;
    
    /**
     * 获取过滤器优先级，数字越小优先级越高
     */
    default int getOrder() {
        return 100;
    }
    
    /**
     * 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}