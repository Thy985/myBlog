package com.xingchen.backend.meta.lightweight;

import com.xingchen.backend.cache.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户偏好学习器
 * 
 * 从用户历史行为学习偏好，微调 prompt（< 60 行核心逻辑）
 */
@Component
@RequiredArgsConstructor
public class UserPreferenceLearner {
    
    private final CacheService cacheService;
    
    private static final String PREF_KEY_PREFIX = "user:pref:";
    private static final int MAX_HISTORY = 20;
    
    /**
     * 学习用户偏好
     */
    public void learn(Long userId, String templateKey, boolean positive) {
        String key = PREF_KEY_PREFIX + userId;
        
        UserPreference pref = cacheService.get(key, UserPreference.class)
                .orElse(new UserPreference());
        
        // 更新偏好分数
        pref.getTemplateScores().merge(templateKey, 
                positive ? 1 : -1, Integer::sum);
        
        // 记录历史
        pref.getRecentTemplates().add(templateKey);
        if (pref.getRecentTemplates().size() > MAX_HISTORY) {
            pref.getRecentTemplates().poll();
        }
        
        cacheService.set(key, pref, Duration.ofDays(30));
    }
    
    /**
     * 获取个性化调整
     */
    public Personalization getPersonalization(Long userId, String baseTemplate) {
        String key = PREF_KEY_PREFIX + userId;
        
        Optional<UserPreference> prefOpt = cacheService.get(key, UserPreference.class);
        if (prefOpt.isEmpty()) {
            return Personalization.builder()
                    .tone("neutral")
                    .detailLevel("normal")
                    .build();
        }
        
        UserPreference pref = prefOpt.get();
        
        // 分析偏好
        String tone = analyzeTone(pref);
        String detailLevel = analyzeDetailLevel(pref);
        
        return Personalization.builder()
                .tone(tone)
                .detailLevel(detailLevel)
                .preferredTemplates(getTopTemplates(pref, 3))
                .build();
    }
    
    private String analyzeTone(UserPreference pref) {
        // 简化：根据最近使用频率判断
        Map<String, Long> freq = pref.getRecentTemplates().stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        
        if (freq.getOrDefault("code", 0L) > 5) return "technical";
        if (freq.getOrDefault("blog", 0L) > 5) return "creative";
        return "neutral";
    }
    
    private String analyzeDetailLevel(UserPreference pref) {
        int score = pref.getTemplateScores().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        return score > 10 ? "detailed" : score < -5 ? "concise" : "normal";
    }
    
    private List<String> getTopTemplates(UserPreference pref, int n) {
        return pref.getTemplateScores().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * 应用个性化
     */
    public String applyPersonalization(String prompt, Personalization p) {
        StringBuilder sb = new StringBuilder();
        
        // 添加语气调整
        switch (p.getTone()) {
            case "technical" -> sb.append("使用技术术语，提供详细实现。\n");
            case "creative" -> sb.append("使用生动语言，注重可读性。\n");
            default -> sb.append("保持专业友好的语气。\n");
        }
        
        // 添加详细程度
        switch (p.getDetailLevel()) {
            case "detailed" -> sb.append("提供详细解释和示例。\n");
            case "concise" -> sb.append("简洁回答，突出重点。\n");
            default -> sb.append("适度详细，平衡全面性和简洁性。\n");
        }
        
        sb.append(prompt);
        return sb.toString();
    }
    
    // ========== 数据类 ==========
    
    public static class UserPreference {
        private final Map<String, Integer> templateScores = new HashMap<>();
        private final Queue<String> recentTemplates = new LinkedList<>();
        
        public Map<String, Integer> getTemplateScores() { return templateScores; }
        public Queue<String> getRecentTemplates() { return recentTemplates; }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Personalization {
        private String tone;           // neutral, technical, creative
        private String detailLevel;    // concise, normal, detailed
        private List<String> preferredTemplates;
    }
}