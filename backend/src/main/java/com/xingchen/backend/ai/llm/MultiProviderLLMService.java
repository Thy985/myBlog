package com.xingchen.backend.ai.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 多提供商LLM服务
 * 支持自动降级、负载均衡
 * 自动检测可用的Provider
 */
@Service
@Slf4j
public class MultiProviderLLMService implements LLMService {

    private final List<LLMProvider> providers;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // 模型优先级链（从高到低）
    private static final List<String> MODEL_CHAIN = List.of(
            "gpt-4o",
            "gpt-4",
            "glm-4-plus",
            "glm-4",
            "gpt-3.5-turbo",
            "glm-4-air",
            "glm-4-flash"
    );

    public MultiProviderLLMService(List<LLMProvider> providers,
                                    CircuitBreakerRegistry circuitBreakerRegistry) {
        this.providers = providers.stream()
                .filter(LLMProvider::isAvailable)
                .sorted(Comparator.comparing(LLMProvider::getProviderName))
                .toList();
        this.circuitBreakerRegistry = circuitBreakerRegistry;

        log.info("多提供商LLM服务初始化完成，共 {} 个提供商", this.providers.size());
        if (this.providers.isEmpty()) {
            log.error("警告: 没有可用的LLM提供商！请检查配置。");
        } else {
            this.providers.forEach(p -> log.info("  - {} (model={})",
                    p.getProviderName(), p.getModelName()));
        }
    }

    @Override
    public AIResponse chat(AIRequest request) {
        long startTime = System.currentTimeMillis();

        if (providers.isEmpty()) {
            return AIResponse.error("没有可用的AI服务提供商，请检查配置");
        }

        // 如果有指定模型，尝试找到对应的Provider
        if (request.getPreferredModel() != null && !request.getPreferredModel().isEmpty()) {
            LLMProvider provider = findProviderForModel(request.getPreferredModel());
            if (provider != null) {
                try {
                    return tryChatWithProvider(provider, request);
                } catch (Exception e) {
                    log.warn("首选Provider {} 调用失败，尝试其他Provider", provider.getProviderName(), e);
                }
            }
        }

        // 依次尝试每个Provider
        for (LLMProvider provider : providers) {
            try {
                return tryChatWithProvider(provider, request);
            } catch (Exception e) {
                log.warn("Provider {} 调用失败，尝试下一个", provider.getProviderName(), e);
            }
        }

        // 所有Provider都失败
        long elapsed = System.currentTimeMillis() - startTime;
        log.error("所有LLM提供商都不可用，elapsed={}ms", elapsed);

        return AIResponse.error("AI服务暂时不可用，请稍后重试");
    }

    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        if (providers.isEmpty()) {
            onChunk.accept(AIResponse.error("没有可用的AI服务提供商"));
            return;
        }

        for (LLMProvider provider : providers) {
            try {
                if (provider.supportsStreaming()) {
                    provider.streamChat(request, onChunk);
                    return;
                }
            } catch (Exception e) {
                log.warn("Provider {} 流式调用失败", provider.getProviderName(), e);
            }
        }

        onChunk.accept(AIResponse.error("流式AI服务暂时不可用"));
    }

    @Override
    public String getName() {
        return "MultiProviderLLMService";
    }

    @Override
    public List<String> getAvailableModels() {
        return MODEL_CHAIN;
    }

    /**
     * 使用指定Provider进行对话
     */
    private AIResponse tryChatWithProvider(LLMProvider provider, AIRequest request) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(provider.getProviderName());
        return cb.executeSupplier(() -> provider.chat(request));
    }

    /**
     * 根据模型名称查找Provider
     */
    private LLMProvider findProviderForModel(String model) {
        // 根据模型前缀判断Provider
        if (model == null) {
            return providers.isEmpty() ? null : providers.get(0);
        }

        if (model.startsWith("gpt-") || model.startsWith("openai/")) {
            return providers.stream()
                    .filter(p -> p.getProviderName().equals("OpenAI"))
                    .findFirst()
                    .orElse(null);
        } else if (model.startsWith("glm-")) {
            return providers.stream()
                    .filter(p -> p.getProviderName().equals("GLM"))
                    .findFirst()
                    .orElse(null);
        } else if (model.startsWith("ernie-")) {
            return providers.stream()
                    .filter(p -> p.getProviderName().equals("Baidu"))
                    .findFirst()
                    .orElse(null);
        } else if (model.startsWith("deepseek")) {
            return providers.stream()
                    .filter(p -> p.getProviderName().equals("DeepSeek"))
                    .findFirst()
                    .orElse(null);
        }

        // 默认返回第一个可用Provider
        return providers.isEmpty() ? null : providers.get(0);
    }
}
