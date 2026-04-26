package com.xingchen.backend.ai.intent;

import com.xingchen.backend.ai.model.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 混合意图分类器
 * 组合多种分类器，按优先级和置信度选择最佳结果
 */
@Component
@Primary
@Slf4j
public class HybridIntentClassifier implements IntentClassifierInterface {

    private final RegexIntentClassifier regexClassifier;//正则分类器
    private final EmbeddingIntentClassifier embeddingClassifier;//嵌入分类器

    // 分类器链，按优先级排序
    private final List<IntentClassifierInterface> classifiers;

    /** 高置信度阈值，超过此值直接返回 */
    @Value("${ai.intent.high-confidence-threshold:0.95}")
    private double highConfidenceThreshold;

    /** 最低接受阈值，低于此值返回 UNKNOWN */
    @Value("${ai.intent.min-confidence-threshold:0.7}")
    private double minConfidenceThreshold;

    public HybridIntentClassifier(RegexIntentClassifier regexClassifier,
                                   EmbeddingIntentClassifier embeddingClassifier) {
        this.regexClassifier = regexClassifier;
        this.embeddingClassifier = embeddingClassifier;
        this.classifiers = List.of(regexClassifier, embeddingClassifier);
    }

    @Override
    public Intent classify(String message) {
        long startTime = System.currentTimeMillis();

        // 1. 首先尝试正则分类器（高精度，适合已知模式）
        if (regexClassifier.isAvailable()) {
            try {
                Intent regexIntent = regexClassifier.classify(message);
                if (regexIntent.getConfidence() >= highConfidenceThreshold) {
                    log.debug("正则高置信度匹配: type={}, confidence={}",
                            regexIntent.getType(), regexIntent.getConfidence());
                    return regexIntent;
                }
                // 如果正则匹配了工具类意图，直接返回（不与Embedding比较）
                if (regexIntent.getType() != Intent.IntentType.UNKNOWN
                        && regexIntent.getType() != Intent.IntentType.CHAT
                        && regexIntent.isRequiresTool()) {
                    log.debug("正则匹配工具意图: type={}, confidence={}",
                            regexIntent.getType(), regexIntent.getConfidence());
                    return regexIntent;
                }
            } catch (Exception e) {
                log.error("正则分类器执行失败", e);
            }
        }

        // 2. Embedding分类器作为后备
        Intent bestIntent = null;
        double bestConfidence = 0.0;

        if (embeddingClassifier.isAvailable()) {
            try {
                Intent embeddingIntent = embeddingClassifier.classify(message);
                if (embeddingIntent.getConfidence() >= highConfidenceThreshold) {
                    log.debug("Embedding高置信度匹配: type={}, confidence={}",
                            embeddingIntent.getType(), embeddingIntent.getConfidence());
                    return embeddingIntent;
                }
                if (embeddingIntent.getConfidence() > bestConfidence) {
                    bestConfidence = embeddingIntent.getConfidence();
                    bestIntent = embeddingIntent;
                }
            } catch (Exception e) {
                log.error("Embedding分类器执行失败", e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        if (bestIntent != null && bestConfidence >= minConfidenceThreshold) {
            log.debug("意图分类完成: source=EmbeddingClassifier, type={}, confidence={}, elapsed={}ms",
                    bestIntent.getType(), bestConfidence, elapsed);
            return bestIntent;
        }

        log.debug("意图分类置信度低({})，返回 UNKNOWN: elapsed={}ms", bestConfidence, elapsed);
        return Intent.unknown(message, bestConfidence);
    }

    @Override
    public String getName() {
        return "HybridClassifier";
    }

    @Override
    public boolean isAvailable() {
        return regexClassifier.isAvailable() || embeddingClassifier.isAvailable();
    }
}