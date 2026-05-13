package com.xingchen.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.cache.AICacheManager;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleContent;
import com.xingchen.backend.entity.UserApiKey;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.ArticleContentMapper;
import com.xingchen.backend.meta.lightweight.LightweightMetaOrchestrator;
import com.xingchen.backend.observability.MetricsService;
import com.xingchen.backend.security.InputSanitizer;
import com.xingchen.backend.security.OutputFilter;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.MemoryService;
import com.xingchen.backend.service.UserApiKeyService;
import com.xingchen.backend.config.MultiModelConfig;
import com.xingchen.backend.ai.util.MessageBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import okhttp3.*;
import okio.BufferedSource;

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
    private final Cache<Long, ChatLanguageModel> userModelCache;
    private final ArticleMapper articleMapper;
    private final ArticleContentMapper articleContentMapper;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${ai.token.budget:8000}")
    private int tokenBudget;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // 提供商基础URL映射
    private static final Map<String, String> PROVIDER_BASE_URLS = Map.of(
            "OPENAI", "https://api.openai.com/v1",
            "ANTHROPIC", "https://api.anthropic.com",
            "ZHIPU", "https://open.bigmodel.cn/api/paas/v4",
            "BAIDU", "https://qianfan.baidubce.com/v2",
            "AZURE", "https://api.openai.com/v1",
            "DEEPSEEK", "https://api.deepseek.com/v1",
            "CUSTOM", ""
    );

    /**
     * 获取用户的 ChatLanguageModel，如果用户没有配置则使用系统默认
     */
    private ChatLanguageModel getUserModel(Long userId) {
        if (userId == null) {
            return modelRouter.getModel("default");
        }

        return userModelCache.get(userId, id -> createUserModel(id));
    }

    /**
     * 为用户创建模型
     */
    private ChatLanguageModel createUserModel(Long userId) {
        UserApiKey userApiKey = userApiKeyService.getByUserId(userId);
        String effectiveApiKey = userApiKeyService.getEffectiveApiKey(userId);

        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            log.warn("用户 {} 没有配置 API Key，使用系统默认模型", userId);
            return modelRouter.getModel("default");
        }

        try {
            String provider = userApiKey != null ? userApiKey.getProvider() : "ZHIPU";
            String baseUrl = getBaseUrl(provider, userApiKey);
            String modelName = userApiKey != null && userApiKey.getDefaultModel() != null
                    ? userApiKey.getDefaultModel()
                    : "glm-4-flash";

            double temperature = userApiKey != null && userApiKey.getTemperature() != null
                    ? userApiKey.getTemperature()
                    : 0.7;
            Integer maxTokens = userApiKey != null && userApiKey.getMaxTokens() != null
                    ? userApiKey.getMaxTokens()
                    : 4096;

            ChatLanguageModel model = OpenAiChatModel.builder()
                    .apiKey(effectiveApiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(60))
                    .maxRetries(2)
                    .build();

            log.info("为用户 {} 创建模型成功，提供商: {}, 模型: {}, temperature: {}, maxTokens: {}",
                    userId, provider, modelName, temperature, maxTokens);

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
        userModelCache.invalidate(userId);
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
        Map<String, Object> result = new HashMap<>();

        try {
            String sanitizedMessage = inputSanitizer.sanitize(message);

            StringBuilder fullResponse = new StringBuilder();

            streamingChatLanguageModel.generate(
                    UserMessage.from(sanitizedMessage),
                    new StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            if (token != null && !token.isEmpty()) {
                                fullResponse.append(token);
                                onChunk.accept(token);
                            }
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            log.debug("流式响应完成");
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("流式响应错误: {}", error.getMessage());
                        }
                    });

            result.put("success", true);
            result.put("message", fullResponse.toString());
            result.put("tokens", estimateTokens(fullResponse.toString()));

            log.info("流式对话完成，响应长度: {} tokens", result.get("tokens"));
            return result;

        } catch (Exception e) {
            log.error("流式对话失败: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 使用用户 API Key 的流式对话（直接使用 OkHttp 实现）
     */
    @Override
    public Map<String, Object> streamChatWithUserApiKey(Long userId, String message, Consumer<String> onChunk) {
        Map<String, Object> result = new HashMap<>();

        try {
            UserApiKey userApiKey = userApiKeyService.getByUserId(userId);
            String effectiveApiKey = userApiKeyService.getEffectiveApiKey(userId);

            if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
                log.warn("用户 {} 没有有效的 API Key", userId);
                result.put("success", false);
                result.put("error", "未配置 API Key");
                return result;
            }

            String provider = userApiKey != null ? userApiKey.getProvider() : "DEEPSEEK";
            String modelName = userApiKey != null && userApiKey.getDefaultModel() != null
                    ? userApiKey.getDefaultModel() : "deepseek-chat";

            String baseUrl = PROVIDER_BASE_URLS.getOrDefault(provider, "https://api.deepseek.com/v1");

            StringBuilder fullResponse = new StringBuilder();

            // 直接使用 OkHttp 进行 SSE 流式调用
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .post(RequestBody.create(
                            "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" +
                            message.replace("\"", "\\\"") + "\"}],\"stream\":true}",
                            MediaType.get("application/json; charset=utf-8")))
                    .addHeader("Authorization", "Bearer " + effectiveApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("DeepSeek API 请求失败: {}", response);
                    result.put("success", false);
                    result.put("error", "API 请求失败: " + response);
                    return result;
                }

                if (response.body() == null) {
                    log.error("DeepSeek API 响应体为空");
                    result.put("success", false);
                    result.put("error", "响应体为空");
                    return result;
                }

                BufferedSource source = response.body().source();
                String line;

                while ((line = source.readUtf8Line()) != null) {
                    if (line.trim().isEmpty()) continue;
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        // 解析 SSE 数据 - DeepSeek/OpenAI 格式
                        // 格式: {"choices":[{"delta":{"content":"xxx"}}]}
                        try {
                            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(data);
                            com.fasterxml.jackson.databind.JsonNode delta = jsonNode.path("choices").get(0).path("delta");
                            String content = delta.path("content").asText();
                            if (content != null && !content.isEmpty()) {
                                fullResponse.append(content);
                                onChunk.accept(content);
                            }
                        } catch (Exception e) {
                            log.warn("解析 SSE 数据失败: {}", e.getMessage());
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("message", fullResponse.toString());
            result.put("tokens", estimateTokens(fullResponse.toString()));

            // 增加使用次数统计
            if (userId != null) {
                userApiKeyService.incrementUsage(userId);
            }

            log.info("用户流式对话完成，userId={}, 响应长度: {} tokens", userId, result.get("tokens"));
            return result;

        } catch (Exception e) {
            log.error("用户流式对话失败: {}, userId={}", e.getMessage(), userId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 获取用户的流式 ChatLanguageModel
     */
    private StreamingChatLanguageModel getUserStreamingModel(Long userId) {
        if (userId == null) {
            log.warn("userId 为空，使用系统默认流式模型");
            return streamingChatLanguageModel; // 使用系统默认
        }

        UserApiKey userApiKey = userApiKeyService.getByUserId(userId);
        String effectiveApiKey = userApiKeyService.getEffectiveApiKey(userId);

        log.info(">>>>>> getUserStreamingModel: userId={}, apiKey.provider={}, apiKey.model={}, effectiveApiKey={}",
                userId,
                userApiKey != null ? userApiKey.getProvider() : "null",
                userApiKey != null ? userApiKey.getDefaultModel() : "null",
                effectiveApiKey != null ? effectiveApiKey.substring(0, Math.min(10, effectiveApiKey.length())) + "..." : "null");

        if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
            log.warn("用户 {} 没有配置 API Key 或 API Key 为空，使用系统默认流式模型", userId);
            return streamingChatLanguageModel;
        }

        try {
            String provider = userApiKey != null ? userApiKey.getProvider() : "ZHIPU";
            String baseUrl = getBaseUrl(provider, userApiKey);
            String modelName = userApiKey != null && userApiKey.getDefaultModel() != null
                    ? userApiKey.getDefaultModel()
                    : "glm-4-flash";

            double temperature = userApiKey != null && userApiKey.getTemperature() != null
                    ? userApiKey.getTemperature()
                    : 0.7;

            // 直接创建用户流式模型
            return createUserStreamingModel(userId, effectiveApiKey, baseUrl, modelName, temperature);
        } catch (Exception e) {
            log.error("为用户 {} 创建流式模型失败: {}", userId, e.getMessage());
            return streamingChatLanguageModel;
        }
    }

    /**
     * 为用户创建流式模型
     */
    private StreamingChatLanguageModel createUserStreamingModel(Long userId, String apiKey, String baseUrl, String modelName, double temperature) {
        try {
            // 使用 OpenAI 兼容接口创建流式模型
            return dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(120))
                    .build();
        } catch (Exception e) {
            log.error("创建用户流式模型失败: {}, userId={}", e.getMessage(), userId);
            return streamingChatLanguageModel;
        }
    }

    /**
     * 流式对话 SSE
     */
    @Override
    public void streamChatSSE(String message, SseEmitter emitter) {
        try {
            String sanitizedMessage = inputSanitizer.sanitize(message);

            emitter.send(SseEmitter.event()
                    .name("start")
                    .data("开始生成回复..."));

            StringBuilder fullResponse = new StringBuilder();

            streamingChatLanguageModel.generate(
                    UserMessage.from(sanitizedMessage),
                    new StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            try {
                                if (token != null && !token.isEmpty()) {
                                    fullResponse.append(token);
                                    emitter.send(SseEmitter.event()
                                            .name("chunk")
                                            .data(token));
                                }
                            } catch (Exception e) {
                                log.warn("SSE 发送 chunk 失败: {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(""));
                                emitter.complete();
                            } catch (Exception e) {
                                log.warn("SSE 完成失败: {}", e.getMessage());
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("SSE 流式响应错误: {}", error.getMessage());
                            emitter.completeWithError(error);
                        }
                    });

            log.info("SSE 流式对话完成，响应长度: {}", fullResponse.length());

        } catch (Exception e) {
            log.error("SSE 流式对话失败: {}", e.getMessage());
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
        log.info("开始索引所有已发布文章...");
        try {
            List<Article> articles = articleMapper.selectPublicArticles();
            int count = 0;
            for (Article article : articles) {
                try {
                    ArticleContent content = articleContentMapper.selectByArticleId(article.getId());
                    if (content != null && content.getContent() != null) {
                        String fullContent = article.getTitle() + "\n\n" + content.getContent();
                        indexArticle(article.getId(), article.getTitle(), fullContent);
                        count++;
                    }
                } catch (Exception e) {
                    log.warn("索引文章失败: articleId={}, error={}", article.getId(), e.getMessage());
                }
            }
            log.info("索引完成，共索引 {} 篇文章", count);
        } catch (Exception e) {
            log.error("索引所有文章失败", e);
        }
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
        log.warn("AI 服务降级，使用 fallback 模型: {}, 消息长度: {}",
                ex.getMessage(), message != null ? message.length() : 0);

        metricsService.recordFallback("aiChat", ex.getClass().getSimpleName());

        try {
            ChatLanguageModel fallbackModel = modelRouter.getFallback();
            return fallbackModel.generate(message);
        } catch (Exception e) {
            log.error("Fallback 模型也失败了: {}, 错误类型: {}", e.getMessage(), e.getClass().getSimpleName());
            return "AI 服务暂时不可用，请稍后重试。";
        }
    }

    private String fallbackChatWithContext(String message, List<Map<String, String>> history, Exception ex) {
        log.warn("AI 上下文对话降级: {}, 消息长度: {}, 历史记录数: {}",
                ex.getMessage(), message != null ? message.length() : 0, history != null ? history.size() : 0);

        metricsService.recordFallback("aiChatContext", ex.getClass().getSimpleName());

        return "服务繁忙，请简化您的问题后重试。";
    }

    private String fallbackChatWithContext(String message, List<Map<String, String>> history, String systemPrompt, Exception ex) {
        log.warn("AI 上下文对话降级 (带systemPrompt): {}, 消息长度: {}, 历史记录数: {}, systemPrompt长度: {}",
                ex.getMessage(), message != null ? message.length() : 0,
                history != null ? history.size() : 0, systemPrompt != null ? systemPrompt.length() : 0);

        metricsService.recordFallback("aiChatContext", ex.getClass().getSimpleName());

        return "服务繁忙，请简化您的问题后重试。";
    }

    private String fallbackChatWithRag(String message, List<Map<String, String>> history, Exception ex) {
        log.warn("AI RAG 对话降级: {}, 消息长度: {}, 历史记录数: {}",
                ex.getMessage(), message != null ? message.length() : 0, history != null ? history.size() : 0);

        metricsService.recordFallback("aiChatRag", ex.getClass().getSimpleName());

        return "知识库服务暂时不可用，请稍后重试或尝试简化您的问题。";
    }

    private Map<String, Object> fallbackStreamMap(String message, Consumer<String> onChunk, Exception ex) {
        log.warn("AI 流式对话降级: {}, 消息长度: {}", ex.getMessage(),
                message != null ? message.length() : 0);

        metricsService.recordFallback("aiStream", ex.getClass().getSimpleName());

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", "服务暂时不可用，请稍后重试");
        return result;
    }

    // ========== 私有方法 ==========

    private List<ChatMessage> buildMessages(String message, List<Map<String, String>> history, String systemPrompt) {
        return MessageBuilder.buildMessages(systemPrompt, history, message);
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
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = 0;
        int englishWords = 0;
        int otherChars = 0;

        boolean inEnglishWord = false;
        StringBuilder currentWord = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                if (inEnglishWord && currentWord.length() > 0) {
                    englishWords++;
                    currentWord.setLength(0);
                }
                inEnglishWord = false;
                chineseChars++;
            } else if (Character.isLetterOrDigit(c)) {
                currentWord.append(c);
                inEnglishWord = true;
            } else {
                if (inEnglishWord && currentWord.length() > 0) {
                    englishWords++;
                    currentWord.setLength(0);
                }
                inEnglishWord = false;
                if (!Character.isWhitespace(c)) {
                    otherChars++;
                }
            }
        }

        if (inEnglishWord && currentWord.length() > 0) {
            englishWords++;
        }

        int chineseTokens = (int) Math.ceil(chineseChars * 1.5);
        int englishTokens = (int) Math.ceil(englishWords * 1.3);
        int otherTokens = (int) Math.ceil(otherChars * 0.25);

        int total = chineseTokens + englishTokens + otherTokens;
        int charBasedEstimate = text.length() / 2;

        return Math.max(total, (int) (charBasedEstimate * 0.8));
    }

    private int estimateTokens(List<ChatMessage> messages) {
        return messages.stream()
                .mapToInt(m -> estimateTokens(m.text()))
                .sum();
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