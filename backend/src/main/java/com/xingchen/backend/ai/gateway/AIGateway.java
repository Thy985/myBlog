package com.xingchen.backend.ai.gateway;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.orchestrator.AgentOrchestrator;
import com.xingchen.backend.ai.security.SecurityFilterChain;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * AI网关
 * 统一入口，负责限流、鉴权、路由
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIGateway {
    
    private final AgentOrchestrator orchestrator;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final MeterRegistry meterRegistry;
    
    /**
     * 处理请求
     */
    public AIResponse process(AIRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 限流检查
            checkRateLimit(request);
            
            // 2. 请求校验
            validateRequest(request);
            
            // 3. 路由到编排器
            AIResponse response = orchestrator.handle(request);
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("网关处理完成: elapsed={}ms", elapsed);
            
            return response;
            
        } catch (RateLimitExceededException e) {
            log.warn("限流拦截: userId={}", request.getUserId());
            return AIResponse.error("请求过于频繁，请稍后再试");

        } catch (Exception e) {
            log.error("网关处理失败: userId={}, message={}", request.getUserId(), request.getMessage(), e);
            return AIResponse.error("服务暂时不可用");
        }
    }
    
    /**
     * 流式处理
     */
    public void processStream(AIRequest request, Consumer<AIResponse> onChunk) {
        try {
            // 限流检查
            checkRateLimit(request);
            
            // 请求校验
            validateRequest(request);
            
            // 流式处理
            orchestrator.handleStream(request, onChunk);
            
        } catch (RateLimitExceededException e) {
            onChunk.accept(AIResponse.error("请求过于频繁，请稍后再试"));
        } catch (Exception e) {
            log.error("流式处理失败", e);
            onChunk.accept(AIResponse.error("服务暂时不可用"));
        }
    }
    
    /**
     * 限流检查（用户级）
     */
    private void checkRateLimit(AIRequest request) {
        Long userId = request.getUserId();

        if (userId == null) {
            throw new RateLimitExceededException("用户未登录");
        }

        // 用户级限流（每个用户独立计数）- 使用默认配置
        RateLimiter userLimiter = rateLimiterRegistry.rateLimiter("ai.user." + userId);
        if (!userLimiter.acquirePermission()) {
            throw new RateLimitExceededException("您的请求过于频繁，请稍后再试");
        }

        // 用户会话级限流（防止单个会话刷接口）- 使用默认配置
        if (request.getSessionId() != null) {
            RateLimiter sessionLimiter = rateLimiterRegistry.rateLimiter("ai.session." + request.getSessionId());
            if (!sessionLimiter.acquirePermission()) {
                throw new RateLimitExceededException("当前会话请求过于频繁");
            }
        }

        // 全局限流 - 使用默认配置
        RateLimiter globalLimiter = rateLimiterRegistry.rateLimiter("ai-global");
        if (!globalLimiter.acquirePermission()) {
            throw new RateLimitExceededException("系统负载过高，请稍后再试");
        }
    }
    
    /**
     * 请求校验
     */
    private void validateRequest(AIRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        
        // 消息长度检查（粗略）
        if (request.getMessage().length() > 10000) {
            throw new IllegalArgumentException("消息过长");
        }
    }
    
    /**
     * 限流异常
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}