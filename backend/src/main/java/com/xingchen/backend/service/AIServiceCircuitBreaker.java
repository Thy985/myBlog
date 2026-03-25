package com.xingchen.backend.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 服务熔断降级包装器
 * <p>
 * 为 AIService 提供熔断保护和降级策略
 * 主 AI 失败时自动切换到备用 AI
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIServiceCircuitBreaker {

    private final AIService aiService;
    private final AIService glmService;  // 备用 AI 服务

    /**
     * 带熔断保护的对话
     * <p>
     * 主 AI 失败时自动切换到备用 AI
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "chatFallback")
    @Retry(name = "aiService")
    public String chat(String message) {
        return aiService.chat(message);
    }

    /**
     * 降级方法：使用备用 AI
     */
    private String chatFallback(String message, Exception ex) {
        log.warn("主 AI 服务熔断/失败，切换到备用 AI: {}", ex.getMessage());
        try {
            return glmService.chat(message);
        } catch (Exception e) {
            log.error("备用 AI 也失败: {}", e.getMessage());
            return "AI 服务暂时不可用，请稍后重试";
        }
    }

    /**
     * 带熔断保护的对话（带上下文）
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "chatWithContextFallback")
    @Retry(name = "aiService")
    public String chatWithContext(String message, List<Map<String, String>> history) {
        return aiService.chatWithContext(message, history);
    }

    private String chatWithContextFallback(String message, List<Map<String, String>> history, Exception ex) {
        log.warn("主 AI 服务熔断/失败，切换到备用 AI（带上下文）");
        try {
            return glmService.chatWithContext(message, history);
        } catch (Exception e) {
            log.error("备用 AI 也失败: {}", e.getMessage());
            return "AI 服务暂时不可用，请稍后重试";
        }
    }

    /**
     * 带熔断保护的 RAG 对话
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "chatWithRagFallback")
    @Retry(name = "aiService")
    public String chatWithRag(String message) {
        return aiService.chatWithRag(message);
    }

    private String chatWithRagFallback(String message, Exception ex) {
        log.warn("主 AI 服务熔断/失败，RAG 降级为普通对话");
        try {
            // 降级：不使用 RAG，直接对话
            return glmService.chat(message);
        } catch (Exception e) {
            log.error("备用 AI 也失败: {}", e.getMessage());
            return "AI 服务暂时不可用，请稍后重试";
        }
    }

    /**
     * 带熔断保护的用户 API Key 对话
     */
    @CircuitBreaker(name = "aiService", fallbackMethod = "chatWithUserApiKeyFallback")
    @Retry(name = "aiService")
    public String chatWithUserApiKey(Long userId, String message) {
        return aiService.chatWithUserApiKey(userId, message);
    }

    private String chatWithUserApiKeyFallback(Long userId, String message, Exception ex) {
        log.warn("用户 {} 的 AI 服务调用失败: {}", userId, ex.getMessage());
        // 用户自定义 Key 失败时，不降级到系统 Key（避免费用混淆）
        return "您的 AI 服务暂时不可用，请检查 API Key 配置或稍后重试";
    }

    /**
     * 检查 AI 服务健康状态
     */
    public boolean isHealthy() {
        try {
            // 简单健康检查
            String response = aiService.chat("test");
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取服务状态信息
     */
    public String getStatus() {
        if (isHealthy()) {
            return "AI 服务正常运行";
        } else {
            return "AI 服务异常，已启用降级策略";
        }
    }
}
