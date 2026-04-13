package com.xingchen.backend.ai.security;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.config.AIAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 输入长度过滤器
 * 防止超长输入导致Token爆炸
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InputLengthFilter implements SecurityFilter {
    
    private final AIAgentProperties properties;
    
    @Override
    public AIRequest filter(AIRequest request) throws SecurityFilterChain.SecurityException {
        String message = request.getMessage();
        if (message == null) {
            return request;
        }
        
        int maxLength = properties.getSecurity().getMaxInputLength();
        if (message.length() > maxLength) {
            log.warn("输入超长: userId={}, length={}, max={}", 
                    request.getUserId(), message.length(), maxLength);
            throw new SecurityFilterChain.SecurityException("输入内容过长，请控制在 " + maxLength + " 字符以内");
        }
        
        return request;
    }
    
    @Override
    public int getOrder() {
        return 10; // 最先执行
    }
}