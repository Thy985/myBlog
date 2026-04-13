package com.xingchen.backend.ai.security;

import com.xingchen.backend.ai.model.AIRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 安全过滤器链
 * 按顺序执行所有安全过滤器
 */
@Component
@Slf4j
public class SecurityFilterChain {
    
    private final List<SecurityFilter> filters;
    
    public SecurityFilterChain(List<SecurityFilter> filters) {
        // 按优先级排序
        this.filters = filters.stream()
                .filter(SecurityFilter::isEnabled)
                .sorted(Comparator.comparingInt(SecurityFilter::getOrder))
                .toList();
        
        log.info("安全过滤器链初始化完成，共 {} 个过滤器", this.filters.size());
        this.filters.forEach(f -> log.info("  - {} (order={})", f.getClass().getSimpleName(), f.getOrder()));
    }
    
    /**
     * 执行过滤链
     */
    public AIRequest filter(AIRequest request) throws SecurityException {
        AIRequest current = request;
        
        for (SecurityFilter filter : filters) {
            try {
                current = filter.filter(current);
            } catch (SecurityException e) {
                log.warn("安全过滤器拦截: filter={}, reason={}", 
                        filter.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }
        
        return current;
    }
    
    /**
     * 安全异常
     */
    public static class SecurityException extends RuntimeException {
        public SecurityException(String message) {
            super(message);
        }
    }
}