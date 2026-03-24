package com.xingchen.backend.service.impl;

import com.xingchen.backend.cache.AICacheManager;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.meta.lightweight.LightweightMetaOrchestrator;
import com.xingchen.backend.observability.MetricsService;
import com.xingchen.backend.security.InputSanitizer;
import com.xingchen.backend.security.OutputFilter;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.MemoryService;
import com.xingchen.backend.service.UserApiKeyService;
import com.xingchen.backend.config.MultiModelConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * AI 服务实现 V3（生产级）
 * 
 * 新增特性：
 * 1. 智能缓存 - 减少重复调用成本
 * 2. 成本追踪 - Token 消耗统计
 * 3. 模型降级 - 自动切换备用模型
 * 4. 响应优化 - 流式响应 + 输出过滤
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIServiceImplV3 implements AIService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final MemoryService memoryService;
    private final InputSanitizer inputSanitizer;
    private final OutputFilter outputFilter;
    private final MultiModelConfig.ModelRouter modelRouter;
    private final AICacheManager cacheManager;
    private final MetricsService metricsService;
    private final LightweightMetaOrchestrator metaOrchestrator;
    private final UserApiKeyService userApiKeyService;

    @Value("${ai.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${ai.token.budget:8000}")
    private int tokenBudget;

    // 用户模型缓存，避免重复创建
    private final Map<Long, ChatLanguageModel> userModelCache = new ConcurrentHashMap<>();

    // 提供商基础URL映射
    private static final Map<String, String> PROVIDER_BASE_URLS = Map.of(
            "OPENAI", "https://api.openai.com/v1",
            "ANTHROPIC", "https://api.anthropic.com",
            "ZHIPU", "https://open.bigmodel.cn/api/paas/v4",
            "BAIDU", "https://qianfan.baidubce.com/v2",
            "AZURE", "https://api.openai.com/v1",
            "CUSTOM", ""
    );

    /**
     * 获取用户的 ChatLanguageModel，如果用户没有配置则使用系统默认
     */
    private ChatLanguageModel getUserModel(Long userId) {
        if (userId == null) {
            return modelRouter.getModel("default");
        }

        // 检查缓存
        ChatLanguageModel cachedModel = userModelCache.get(userId);
        if (cachedModel != null) {
            return cachedModel;
        }

        // 获取用户 API Key 配置
        UserApiKey userApiKey = userApiKeyService.getByUserId(userId);
        String effectiveApiKey = userApiKeyService.getEffectiveApiKey(userId);

        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            log.warn("用户 {} 没有配置 API Key，使用系统默认模型", userId);
            return modelRouter.getModel("default");
        }

        // 创建用户特定的模型
        try {
            String provider = userApiKey != null ? userApiKey.getProvider() : "ZHIPU";
            String baseUrl = getBaseUrl(provider, userApiKey);
            String modelName = userApiKey != null && userApiKey.getDefaultModel() != null
                    ? userApiKey.getDefaultModel()
                    : "glm-4-flash";

            ChatLanguageModel model = OpenAiChatModel.builder()
                    .apiKey(effectiveApiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(0.7)
                    .maxTokens(4096)
                    .timeout(Duration.ofSeconds(60))
                    .maxRetries(2)
                    .build();

            // 缓存模型
            userModelCache.put(userId, model);
            log.info("为用户 {} 创建模型成功，提供商: {}, 模型: {}", userId, provider, modelName);

            return model;
        } catch (Exception e) {
            log.error("为用户 {} 创建模型失败: {}", userId, e.getMessage());
            return modelRouter.getModel("default");
        }
    }

    /**
     * 获取基础 URL
     */
    private String getBaseUrl(String provider, UserApiKey userApiKey) {
        if (userApiKey != null && userApiKey.getBaseUrl() != null && !userApiKey.getBaseUrl().isEmpty()) {
            return userApiKey.getBaseUrl();
        }
        return PROVIDER_BASE_URLS.getOrDefault(provider, "https://open.bigmodel.cn/api/paas/v4");
    }

    /**
     * 清除用户模型缓存（当用户更新 API Key 时调用）
     */
    public void clearUserModelCache(Long userId) {
        userModelCache.remove(userId);
        log.info("已清除用户 {} 的模型缓存", userId);
    }

    /**
     * 通用对话（带缓存 + 微型元能力）
     */
    @Override
    @CircuitBreaker(name = "aiChat", fallbackMethod = "fallbackChat")
    @Retry(name = "aiChat")
    public String chat(String message) {
        return chatWithUserId(message, null);
    }

    /**
     * 带用户ID的对话
     */
    @CircuitBreaker(name = "aiChat", fallbackMethod = "fallbackChat")
    @Retry(name = "aiChat")
    public String chatWithUserId(String message, Long userId) {
        // 输入过滤
        String sanitizedMessage = inputSanitizer.sanitize(message);

        // 检查缓存
        if (cacheEnabled && cacheManager.shouldCache(sanitizedMessage)) {
            Optional<String> cached = cacheManager.getCachedResponse(sanitizedMessage, "default");
            if (cached.isPresent()) {
                log.debug("AI 缓存命中");
                return cached.get();
            }
        }

        // 调用模型
        Instant start = Instant.now();
        ChatLanguageModel model = getUserModel(userId);
        String response = model.generate(sanitizedMessage);
        long latency = Duration.between(start, Instant.now()).toMillis();

        // 增加使用次数统计
        if (userId != null) {
            userApiKeyService.incrementUsage(userId);
        }
        
        // 输出过滤
        OutputFilter.FilterResult filterResult = outputFilter.filter(response);
        String filteredResponse = filterResult.content();
        
        // 缓存结果
        if (cacheEnabled && cacheManager.shouldCache(sanitizedMessage)) {
            cacheManager.cacheResponse(sanitizedMessage, "default", filteredResponse);
        }
        
        // 记录指标
        metricsService.recordAICall("default", true, latency, 
                estimateTokens(sanitizedMessage), 
                estimateTokens(filteredResponse));
        
        return filteredResponse;
    }

    /**
     * 带上下文的对话（支持微型元能力）
     */
    @Override
    @CircuitBreaker(name = "aiChatContext", fallbackMethod = "fallbackChatWithContext")
    public String chatWithContext(String message, List<Map<String, String>> history) {
        return chatWithContext(message, history, null, null);
    }

    /**
     * 带上下文和 System Prompt 的对话（兼容旧接口）
     */
    @Override
    @CircuitBreaker(name = "aiChatContext", fallbackMethod = "fallbackChatWithContext")
    public String chatWithContext(String message, List<Map<String, String>> history, String systemPrompt) {
        return chatWithContext(message, history, systemPrompt, null);
    }

    /**
     * 带上下文、System Prompt 和用户ID的对话（支持微型元能力）
     */
    @CircuitBreaker(name = "aiChatContext", fallbackMethod = "fallbackChatWithContext")
    public String chatWithContext(String message, List<Map<String, String>> history, 
                                   String systemPrompt, Long userId) {
        // 输入过滤
        String sanitizedMessage = inputSanitizer.sanitize(message);
        
        // 构建增强 System Prompt（使用微型元能力）
        String enhancedSystemPrompt;
        if (userId != null) {
            // 使用元能力层构建增强 Prompt
            enhancedSystemPrompt = metaOrchestrator.buildEnhancedPrompt(userId, sanitizedMessage);
            log.debug("使用微型元能力构建增强 Prompt, userId={}", userId);
        } else if (systemPrompt != null) {
            enhancedSystemPrompt = systemPrompt;
        } else {
            enhancedSystemPrompt = "";
        }
        
        // 构建消息列表
        List<ChatMessage> messages = buildMessages(sanitizedMessage, history, enhancedSystemPrompt);
        
        // Token 预算检查
        if (estimateTokens(messages) > tokenBudget) {
            messages = compressMessages(messages);
        }
        
        // 调用模型
        Instant start = Instant.now();
        ChatLanguageModel model = getUserModel(userId);
        Response<AiMessage> response = model.generate(messages);
        long latency = Duration.between(start, Instant.now()).toMillis();

        // 增加使用次数统计
        if (userId != null) {
            userApiKeyService.incrementUsage(userId);
        }

        String content = response.content().text();
        
        // 输出过滤
        OutputFilter.FilterResult filterResult = outputFilter.filter(content);
        String filteredContent = filterResult.content();
        
        // 记录指标
        metricsService.recordAICall("default", true, latency,
                estimateTokens(messages),
                estimateTokens(content));
        
        // 记录反馈用于学习（异步）
        if (userId != null) {
            recordFeedbackAsync(userId, sanitizedMessage, true);
        }
        
        return filteredContent;
    }

    /**
     * RAG 增强对话
     */
    @Override
    @CircuitBreaker(name = "aiChatRag", fallbackMethod = "fallbackChatWithRag")
    public String chatWithRag(String message) {
        return chatWithRagAndContext(message, null);
    }

    @Override
    @CircuitBreaker(name = "aiChatRag", fallbackMethod = "fallbackChatWithRag")
    public String chatWithRagAndContext(String message, List<Map<String, String>> history) {
        // 输入过滤
        String sanitizedMessage = inputSanitizer.sanitize(message);
        
        // 检索知识库
        Instant kbStart = Instant.now();
        String kbContext = knowledgeBaseService.search(sanitizedMessage, 3);
        long kbLatency = Duration.between(kbStart, Instant.now()).toMillis();
        
        metricsService.recordKnowledgeSearch("hybrid", !kbContext.isEmpty(), kbLatency, 3);
        
        // 构建增强提示
        String enhancedPrompt = buildRagPrompt(sanitizedMessage, kbContext);
        
        return chatWithContext(enhancedPrompt, history);
    }

    /**
     * 带网络搜索的对话
     */
    @Override
    @CircuitBreaker(name = "aiChatWeb", fallbackMethod = "fallbackChatWithContext")
    public String chatWithWebSearch(String message, List<Map<String, String>> history) {
        // 简化实现，直接调用上下文对话
        return chatWithContext(message, history);
    }

    /**
     * 使用用户 API Key 对话
     */
    @Override
    public String chatWithUserApiKey(Long userId, String message) {
        return chatWithUserApiKeyAndPrompt(userId, message, null);
    }

    /**
     * 使用用户 API Key 对话（带系统提示词）
     */
    @Override
    public String chatWithUserApiKeyAndPrompt(Long userId, String message, String systemPrompt) {
        // 简化实现，调用普通对话
        return chatWithContext(message, null, systemPrompt, userId);
    }

    /**
     * 流式对话（返回 Map）
     */
    @Override
    @CircuitBreaker(name = "aiStream", fallbackMethod = "fallbackStreamMap")
    public Map<String, Object> streamChat(String message, Consumer<String> onChunk) {
        // TODO: 实现流式对话
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "流式对话功能开发中");
        return result;
    }

    /**
     * 流式对话 SSE
     */
    @Override
    public void streamChatSSE(String message, SseEmitter emitter) {
        // TODO: 实现 SSE 流式对话
        try {
            emitter.send(SseEmitter.event().data("功能开发中").name("message"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * 索引文章
     */
    @Override
    public void indexArticle(Long articleId, String title, String content) {
        if (knowledgeBaseService != null) {
            knowledgeBaseService.addDocument(articleId, title, content);
        }
    }

    /**
     * 索引所有文章
     */
    @Override
    public void indexAllArticles() {
        // TODO: 实现索引所有文章
        log.info("索引所有文章 - 待实现");
    }

    /**
     * 清空知识库
     */
    @Override
    public void clearKnowledgeBase() {
        if (knowledgeBaseService != null) {
            knowledgeBaseService.clearAll();
        }
    }

    // ========== Fallback 方法 ==========

    private String fallbackChat(String message, Exception ex) {
        log.warn("AI 服务降级，使用 fallback 模型: {}", ex.getMessage());
        try {
            ChatLanguageModel fallbackModel = modelRouter.getFallback();
            return fallbackModel.generate(message);
        } catch (Exception e) {
            log.error("Fallback 也失败了", e);
            return "AI 服务暂时不可用，请稍后重试。";
        }
    }

    private String fallbackChatWithContext(String message, List<Map<String, String>> history, Exception ex) {
        log.warn("AI 上下文对话降级: {}", ex.getMessage());
        return "服务繁忙，请简化您的问题后重试。";
    }

    private String fallbackChatWithContext(String message, List<Map<String, String>> history, String systemPrompt, Exception ex) {
        return fallbackChatWithContext(message, history, ex);
    }

    private String fallbackChatWithRag(String message, List<Map<String, String>> history, Exception ex) {
        log.warn("AI RAG 对话降级: {}", ex.getMessage());
        return chatWithContext(message, history);
    }

    private Map<String, Object> fallbackStreamMap(String message, Consumer<String> onChunk, Exception ex) {
        log.warn("AI 流式对话降级 (Map): {}", ex.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", "服务暂时不可用，请稍后重试");
        return result;
    }

    // ========== 私有方法 ==========

    private List<ChatMessage> buildMessages(String message, List<Map<String, String>> history, String systemPrompt) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // System Prompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        
        // 历史消息
        if (history != null) {
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AiMessage(content));
                }
            }
        }
        
        // 当前消息
        messages.add(new UserMessage(message));
        
        return messages;
    }

    private String buildRagPrompt(String message, String kbContext) {
        if (kbContext == null || kbContext.isEmpty()) {
            return message;
        }
        return String.format("""
            基于以下知识库内容回答问题：
            
            知识库内容：
            %s
            
            用户问题：%s
            
            请结合知识库内容回答，如果知识库中没有相关信息，请基于你的知识回答。
            """, kbContext, message);
    }

    private int estimateTokens(String text) {
        // 简单估算：每个字符约 0.5 个 token
        return text.length() / 2;
    }

    private int estimateTokens(List<ChatMessage> messages) {
        int totalChars = messages.stream()
                .mapToInt(m -> m.text().length())
                .sum();
        return totalChars / 2;
    }

    private List<ChatMessage> compressMessages(List<ChatMessage> messages) {
        List<ChatMessage> compressed = new ArrayList<>();
        compressed.add(messages.get(0)); // System prompt
        
        int start = Math.max(1, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            compressed.add(messages.get(i));
        }
        
        return compressed;
    }
    
    /**
     * 异步记录反馈
     */
    private void recordFeedbackAsync(Long userId, String userInput, boolean positive) {
        new Thread(() -> {
            try {
                metaOrchestrator.recordFeedback(userId, userInput, positive);
            } catch (Exception e) {
                log.warn("记录反馈失败", e);
            }
        }).start();
    }
}