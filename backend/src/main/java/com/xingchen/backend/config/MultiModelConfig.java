package com.xingchen.backend.config;

import com.xingchen.backend.security.ApiKeyEncryptionService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模型配置（重构版）
 * 支持多模型路由、动态参数、密钥加密
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MultiModelConfig {

    private final ApiKeyEncryptionService encryptionService;

    @Value("${ai.models.default:glm-4-flash}")
    private String defaultModel;

    @Value("${ai.models.fallback:glm-4-air}")
    private String fallbackModel;

    @Value("${ai.models.premium:glm-4-plus}")
    private String premiumModel;

    @Value("${glm.api.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String baseUrl;

    @Value("${glm.api.key:}")
    private String encryptedApiKey;

    // 模型参数配置
    @Value("${ai.params.default.temperature:0.7}")
    private double defaultTemperature;

    @Value("${ai.params.default.max-tokens:4096}")
    private int defaultMaxTokens;

    @Value("${ai.params.code.temperature:0.2}")
    private double codeTemperature;

    @Value("${ai.params.creative.temperature:0.8}")
    private double creativeTemperature;

    private boolean apiKeyConfigured = false;

    /**
     * 解密 API Key
     */
    private String getApiKey() {
        if (encryptedApiKey == null || encryptedApiKey.isEmpty()) {
            return null;
        }
        // 如果已经是明文（未加密格式），直接返回
        if (!encryptionService.isEncrypted(encryptedApiKey)) {
            log.warn("API Key 未加密，建议加密存储");
            return encryptedApiKey;
        }
        return encryptionService.decrypt(encryptedApiKey);
    }

    /**
     * 检查 API Key 是否已配置
     */
    private boolean isApiKeyConfigured() {
        return getApiKey() != null && !getApiKey().isEmpty();
    }

    /**
     * 创建模型实例
     */
    private ChatLanguageModel createModel(String modelName, double temperature, int maxTokens, int timeoutSeconds) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key 未配置，使用 MockChatModel");
            return new MockChatLanguageModel();
        }
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(2)
                .build();
    }

    /**
     * 创建流式模型实例
     */
    private StreamingChatLanguageModel createStreamingModel(String modelName, double temperature, int timeoutSeconds) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API Key 未配置，使用 MockStreamingChatModel");
            return new MockStreamingChatLanguageModel();
        }
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Bean
    @Primary
    public ChatLanguageModel chatLanguageModel() {
        return createModel(defaultModel, defaultTemperature, defaultMaxTokens, 60);
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return createStreamingModel(defaultModel, defaultTemperature, 120);
    }

    /**
     * 模型注册表 - 支持动态路由
     */
    @Bean
    public ModelRegistry modelRegistry() {
        ModelRegistry registry = new ModelRegistry();

        // 默认模型 - 平衡性能与成本
        registry.register("default", createModel(defaultModel, defaultTemperature, defaultMaxTokens, 60));
        registry.register("default-streaming", createStreamingModel(defaultModel, defaultTemperature, 120));

        // 轻量模型 - 快速响应
        registry.register("fast", createModel("glm-4-flash", 0.5, 2048, 30));
        registry.register("fast-streaming", createStreamingModel("glm-4-flash", 0.5, 30));

        // 代码模型 - 低温度，高精度
        registry.register("code", createModel("glm-4-air", codeTemperature, 4096, 60));
        registry.register("code-streaming", createStreamingModel("glm-4-air", codeTemperature, 60));

        // 创意模型 - 高温度
        registry.register("creative", createModel("glm-4-air", creativeTemperature, 4096, 60));
        registry.register("creative-streaming", createStreamingModel("glm-4-air", creativeTemperature, 60));

        // 高级模型 - 复杂任务
        registry.register("premium", createModel(premiumModel, 0.7, 8192, 120));
        registry.register("premium-streaming", createStreamingModel(premiumModel, 0.7, 120));

        // 降级模型
        registry.register("fallback", createModel(fallbackModel, defaultTemperature, 4096, 60));

        log.info("模型注册表初始化完成，共 {} 个模型", registry.size());
        return registry;
    }

    /**
     * 模型路由器
     */
    @Bean
    public ModelRouter modelRouter(ModelRegistry registry) {
        return new ModelRouter(registry);
    }

    /**
     * Mock ChatLanguageModel（当 API Key 未配置时使用）
     */
    public static class MockChatLanguageModel implements ChatLanguageModel {
        @Override
        public String generate(String userMessage) {
            log.warn("MockChatLanguageModel.generate 被调用");
            return "[Mock] 这是一个模拟响应。请配置 GLM_API_KEY 以使用真实的 AI 服务。";
        }

        @Override
        public Response<AiMessage> generate(ChatMessage... messages) {
            log.warn("MockChatLanguageModel.generate(messages) 被调用");
            return Response.from(AiMessage.from("[Mock] 这是一个模拟响应。请配置 GLM_API_KEY 以使用真实的 AI 服务。"));
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            log.warn("MockChatLanguageModel.generate(List) 被调用");
            return Response.from(AiMessage.from("[Mock] 这是一个模拟响应。请配置 GLM_API_KEY 以使用真实的 AI 服务。"));
        }
    }

    /**
     * Mock StreamingChatLanguageModel（当 API Key 未配置时使用）
     */
    public static class MockStreamingChatLanguageModel implements StreamingChatLanguageModel {
        @Override
        public void generate(String userMessage, StreamingResponseHandler<AiMessage> handler) {
            log.warn("MockStreamingChatLanguageModel.generate 被调用");
            handler.onNext("[Mock] 这是一个模拟响应。请配置 GLM_API_KEY 以使用真实的 AI 服务。");
            handler.onComplete(Response.from(AiMessage.from("[Mock] 这是一个模拟响应。请配置 GLM_API_KEY 以使用真实的 AI 服务。")));
        }

        @Override
        public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
            log.warn("MockStreamingChatLanguageModel.generate(List) 被调用");
            handler.onNext("[Mock] 这是一个模拟响应。请配置 GLM_API_KEY 以使用真实的 AI 服务。");
            handler.onComplete(Response.from(AiMessage.from("[Mock] 这是一个模拟响应。请配置 GLM_API_KEY 以使用真实的 AI 服务。")));
        }
    }

    /**
     * 模型注册表
     */
    public static class ModelRegistry {
        private final Map<String, ChatLanguageModel> models = new HashMap<>();
        private final Map<String, StreamingChatLanguageModel> streamingModels = new HashMap<>();

        public void register(String name, ChatLanguageModel model) {
            models.put(name, model);
        }

        public void register(String name, StreamingChatLanguageModel model) {
            streamingModels.put(name, model);
        }

        public ChatLanguageModel get(String name) {
            ChatLanguageModel model = models.get(name);
            if (model == null) {
                throw new IllegalArgumentException("未知模型: " + name);
            }
            return model;
        }

        public StreamingChatLanguageModel getStreaming(String name) {
            StreamingChatLanguageModel model = streamingModels.get(name);
            if (model == null) {
                throw new IllegalArgumentException("未知流式模型: " + name);
            }
            return model;
        }

        public boolean hasModel(String name) {
            return models.containsKey(name) || streamingModels.containsKey(name);
        }

        public int size() {
            return models.size() + streamingModels.size();
        }
    }

    /**
     * 模型路由器
     */
    public static class ModelRouter {
        private final ModelRegistry registry;

        public ModelRouter(ModelRegistry registry) {
            this.registry = registry;
        }

        /**
         * 根据任务类型选择模型
         */
        public String route(String taskType) {
            return switch (taskType.toLowerCase()) {
                case "code", "programming", "debug" -> "code";
                case "creative", "write", "story" -> "creative";
                case "complex", "analyze", "research" -> "premium";
                case "fast", "quick", "simple" -> "fast";
                default -> "default";
            };
        }

        /**
         * 获取模型实例
         */
        public ChatLanguageModel getModel(String taskType) {
            String modelName = route(taskType);
            return registry.get(modelName);
        }

        /**
         * 获取降级模型
         */
        public ChatLanguageModel getFallback() {
            return registry.get("fallback");
        }
    }
}
