package com.xingchen.backend.meta.lightweight;

import com.xingchen.backend.prompt.PromptTemplateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 微型元能力编排器
 * 
 * 整合三个轻量级元能力（< 40 行核心逻辑）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LightweightMetaOrchestrator {
    
    private final IntentTemplateMatcher intentMatcher;
    private final UserPreferenceLearner preferenceLearner;
    private final ContextAugmenter contextAugmenter;
    private final PromptTemplateManager templateManager;
    
    /**
     * 构建增强后的 Prompt
     */
    public String buildEnhancedPrompt(Long userId, String userInput) {
        // 1. 意图匹配 → 选择模板
        IntentTemplateMatcher.MatchResult match = intentMatcher.match(userInput);
        String templateKey = match.getTemplateKey();
        
        log.debug("意图匹配: template={}, confidence={}", templateKey, match.getConfidence());
        
        // 2. 获取基础 Prompt
        String basePrompt = templateManager.getTemplate(templateKey);
        
        // 3. 个性化调整 → 微调 Prompt
        UserPreferenceLearner.Personalization personalization = 
                preferenceLearner.getPersonalization(userId, templateKey);
        String personalizedPrompt = preferenceLearner.applyPersonalization(basePrompt, personalization);
        
        // 4. 上下文增强 → 扩展 RAG
        ContextAugmenter.AugmentedContext augmented = 
                contextAugmenter.smartAugment(userInput);
        
        // 5. 组装最终 Prompt
        StringBuilder finalPrompt = new StringBuilder();
        
        // 添加个性化前缀
        finalPrompt.append(personalizedPrompt).append("\n\n");
        
        // 添加上下文
        if (augmented.augmented()) {
            finalPrompt.append("## 参考资料\n").append(augmented.context()).append("\n\n");
        }
        
        // 添加用户问题
        finalPrompt.append("## 用户问题\n").append(userInput);
        
        return finalPrompt.toString();
    }
    
    /**
     * 记录反馈用于学习
     */
    public void recordFeedback(Long userId, String userInput, boolean positive) {
        IntentTemplateMatcher.MatchResult match = intentMatcher.match(userInput);
        preferenceLearner.learn(userId, match.getTemplateKey(), positive);
    }
    
    /**
     * 获取元能力执行统计
     */
    public MetaStats getStats() {
        return MetaStats.builder()
                .intentMatcherActive(true)
                .preferenceLearnerActive(true)
                .contextAugmenterActive(true)
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class MetaStats {
        private boolean intentMatcherActive;
        private boolean preferenceLearnerActive;
        private boolean contextAugmenterActive;
    }
}