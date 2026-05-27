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
 * 从用户历史行为学习偏好，微调 prompt
 * 特性：
 * - 自适应学习速率（正反馈 +1，负反馈 -1.5）
 * - 遗忘机制（30天不活跃重置）
 * - 多维度偏好追踪（语气、详细度、模板偏好）
 */
@Component
@RequiredArgsConstructor
public class UserPreferenceLearner {

    private final CacheService cacheService;

    private static final String PREF_KEY_PREFIX = "user:pref:";
    private static final int MAX_HISTORY = 30;
    private static final double POSITIVE_WEIGHT = 1.0;
    private static final double NEGATIVE_WEIGHT = 1.5;
    private static final int TONE_THRESHOLD = 5;
    private static final int DECAY_DAYS = 30;

    public void learn(Long userId, String templateKey, boolean positive) {
        String key = PREF_KEY_PREFIX + userId;

        UserPreference pref = cacheService.get(key, UserPreference.class)
                .orElse(new UserPreference());

        double delta = positive ? POSITIVE_WEIGHT : -NEGATIVE_WEIGHT;
        pref.getTemplateScores().merge(templateKey, (int) delta, Integer::sum);

        // 清理负分模板
        pref.getTemplateScores().entrySet().removeIf(e -> e.getValue() <= -5);

        pref.getRecentTemplates().add(templateKey);
        if (pref.getRecentTemplates().size() > MAX_HISTORY) {
            pref.getRecentTemplates().poll();
        }

        pref.setLastActiveTime(System.currentTimeMillis());
        cacheService.set(key, pref, Duration.ofDays(30));
    }

    public Personalization getPersonalization(Long userId, String baseTemplate) {
        String key = PREF_KEY_PREFIX + userId;

        Optional<UserPreference> prefOpt = cacheService.get(key, UserPreference.class);
        if (prefOpt.isEmpty()) {
            return defaultPersonalization(baseTemplate);
        }

        UserPreference pref = prefOpt.get();

        // 遗忘机制：30天不活跃
        if (shouldDecay(pref)) {
            return defaultPersonalization(baseTemplate);
        }

        return Personalization.builder()
                .tone(analyzeTone(pref))
                .detailLevel(analyzeDetailLevel(pref))
                .preferredTemplates(getTopTemplates(pref, 3))
                .build();
    }

    private Personalization defaultPersonalization(String baseTemplate) {
        return Personalization.builder()
                .tone("neutral")
                .detailLevel("normal")
                .preferredTemplates(List.of(baseTemplate))
                .build();
    }

    private boolean shouldDecay(UserPreference pref) {
        long inactiveDays = (System.currentTimeMillis() - pref.getLastActiveTime()) / (1000 * 60 * 60 * 24);
        return inactiveDays > DECAY_DAYS;
    }

    private String analyzeTone(UserPreference pref) {
        Map<String, Long> freq = pref.getRecentTemplates().stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        if (freq.getOrDefault("code", 0L) >= TONE_THRESHOLD) return "technical";
        if (freq.getOrDefault("blog", 0L) >= TONE_THRESHOLD) return "creative";
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

    public String applyPersonalization(String prompt, Personalization p) {
        StringBuilder sb = new StringBuilder();

        switch (p.getTone()) {
            case "technical" -> sb.append("使用技术术语，提供详细实现。\n");
            case "creative" -> sb.append("使用生动语言，注重可读性。\n");
            default -> sb.append("保持专业友好的语气。\n");
        }

        switch (p.getDetailLevel()) {
            case "detailed" -> sb.append("提供详细解释和示例。\n");
            case "concise" -> sb.append("简洁回答，突出重点。\n");
            default -> sb.append("适度详细，平衡全面性和简洁性。\n");
        }

        sb.append(prompt);
        return sb.toString();
    }

    public static class UserPreference {
        private final Map<String, Integer> templateScores = new HashMap<>();
        private final Queue<String> recentTemplates = new LinkedList<>();
        private long lastActiveTime = System.currentTimeMillis();

        public Map<String, Integer> getTemplateScores() { return templateScores; }
        public Queue<String> getRecentTemplates() { return recentTemplates; }
        public long getLastActiveTime() { return lastActiveTime; }
        public void setLastActiveTime(long t) { this.lastActiveTime = t; }
    }

    @lombok.Data
    @lombok.Builder
    public static class Personalization {
        private String tone;
        private String detailLevel;
        private List<String> preferredTemplates;
    }
}
