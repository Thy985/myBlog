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

    private final RegexIntentClassifier regexClassifier;
    private final EmbeddingIntentClassifier embeddingClassifier;

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

        Intent bestIntent = null;
        double bestConfidence = 0.0;
        String bestSource = "";

        // 依次尝试每个分类器
        for (IntentClassifierInterface classifier : classifiers) {
            if (!classifier.isAvailable()) {
                continue;
            }

            try {
                Intent intent = classifier.classify(message);

                // 高置信度直接返回
                if (intent.getConfidence() >= highConfidenceThreshold) {
                    log.debug("高置信度匹配: source={}, type={}, confidence={}",
                            classifier.getName(), intent.getType(), intent.getConfidence());
                    return intent;
                }

                // 记录最佳结果
                if (intent.getConfidence() > bestConfidence) {
                    bestConfidence = intent.getConfidence();
                    bestIntent = intent;
                    bestSource = classifier.getName();
                }

            } catch (Exception e) {
                log.error("分类器 {} 执行失败", classifier.getName(), e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        if (bestIntent != null && bestConfidence >= minConfidenceThreshold) {
            log.debug("意图分类完成: source={}, type={}, confidence={}, elapsed={}ms",
                    bestSource, bestIntent.getType(), bestConfidence, elapsed);
            return bestIntent;
        }

        // 置信度过低，返回 UNKNOWN 而非强行 CHAT
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