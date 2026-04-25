package com.xingchen.backend.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnifiedMemoryServiceImpl implements UnifiedMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String WORKING_KEY_PREFIX = "unified:memory:working:%s:%s";
    private static final String SHORT_TERM_KEY_PREFIX = "unified:memory:short:%s";
    private static final String LONG_TERM_KEY_PREFIX = "unified:memory:long:%s";
    private static final String SESSION_INDEX_KEY_PREFIX = "unified:memory:sessions:%s";

    private static final Duration WORKING_TTL = Duration.ofMinutes(30);
    private static final Duration SHORT_TERM_TTL = Duration.ofDays(7);
    private static final Duration LONG_TERM_TTL = Duration.ofDays(90);

    private static final int MAX_WORKING_ITEMS = 50;
    private static final int MAX_SHORT_TERM_ITEMS = 100;
    private static final int MAX_LONG_TERM_ITEMS = 500;

    private static final double MIN_IMPORTANCE_THRESHOLD = 0.3;

    private static final Set<String> PREFERENCE_TRIGGERS = Set.of(
        "喜欢", "偏好", "习惯", "常用", "不要", "别用", "不喜欢",
        "讨厌", "想要", "希望", "推荐", "倾向于"
    );

    private static final Set<String> DECISION_TRIGGERS = Set.of(
        "决定", "选择", "确定", "采用", "使用", "不用", "放弃"
    );

    private static final Set<String> FACT_TRIGGERS = Set.of(
        "我叫", "我的名字", "我在", "我做", "我的工作", "我是"
    );

    private final Map<String, List<UnifiedMemoryContext.MemoryItem>> localWorkingCache = new ConcurrentHashMap<>();

    @Override
    public UnifiedMemoryContext loadContext(Long userId, String sessionId, MemoryRequirements requirements) {
        UnifiedMemoryContext.UnifiedMemoryContextBuilder builder = UnifiedMemoryContext.builder();

        if (requirements.needWorking() && sessionId != null) {
            List<UnifiedMemoryContext.MemoryItem> working = loadWorkingMemory(userId, sessionId);
            builder.workingMemory(working);
        }

        if (requirements.needShortTerm() && userId != null) {
            List<UnifiedMemoryContext.MemoryItem> shortTerm = loadShortTermMemory(userId, requirements.shortTermRounds());
            builder.shortTermMemory(shortTerm);
        }

        if (requirements.needLongTerm() && userId != null && requirements.queryForLongTerm() != null) {
            List<UnifiedMemoryContext.MemoryItem> longTerm = loadLongTermMemory(userId, requirements.queryForLongTerm());
            builder.longTermMemory(longTerm);
        }

        return builder.build();
    }

    @Override
    public void saveMemories(Long userId, String sessionId, List<UnifiedMemoryContext.MemoryItem> memories) {
        if (userId == null || memories == null || memories.isEmpty()) {
            return;
        }

        List<UnifiedMemoryContext.MemoryItem> filtered = memories.stream()
            .filter(m -> m.getImportance() >= MIN_IMPORTANCE_THRESHOLD)
            .filter(this::isNotDuplicateLongTerm)
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.debug("没有值得保存的记忆");
            return;
        }

        for (UnifiedMemoryContext.MemoryItem item : filtered) {
            saveToLongTermStorage(userId, item);
        }

        log.info("保存了 {} 条长期记忆", filtered.size());
    }

    @Override
    public void addWorkingMemory(Long userId, String sessionId, UnifiedMemoryContext.MemoryItem item) {
        if (userId == null || sessionId == null || item == null) {
            return;
        }

        String cacheKey = buildCacheKey(userId, sessionId);
        localWorkingCache.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(item);

        List<UnifiedMemoryContext.MemoryItem> items = localWorkingCache.get(cacheKey);
        if (items.size() > MAX_WORKING_ITEMS) {
            List<UnifiedMemoryContext.MemoryItem> trimmed = items.subList(items.size() - MAX_WORKING_ITEMS, items.size());
            localWorkingCache.put(cacheKey, new ArrayList<>(trimmed));
        }

        String redisKey = String.format(WORKING_KEY_PREFIX, userId, sessionId);
        try {
            String value = objectMapper.writeValueAsString(item);
            redisTemplate.opsForList().rightPush(redisKey, value);
            redisTemplate.expire(redisKey, WORKING_TTL);
        } catch (JsonProcessingException e) {
            log.error("序列化记忆失败", e);
        }

        indexSession(userId, sessionId);
        log.debug("添加工作记忆: userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    public void clearSessionMemory(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }

        String cacheKey = buildCacheKey(userId, sessionId);
        localWorkingCache.remove(cacheKey);

        String workingKey = String.format(WORKING_KEY_PREFIX, userId, sessionId);
        redisTemplate.delete(workingKey);

        String sessionIndexKey = String.format(SESSION_INDEX_KEY_PREFIX, userId);
        redisTemplate.opsForSet().remove(sessionIndexKey, sessionId);

        log.info("清除会话记忆: userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    public void clearUserMemory(Long userId) {
        if (userId == null) {
            return;
        }

        String shortTermKey = String.format(SHORT_TERM_KEY_PREFIX, userId);
        String longTermKey = String.format(LONG_TERM_KEY_PREFIX, userId);
        String sessionIndexKey = String.format(SESSION_INDEX_KEY_PREFIX, userId);

        redisTemplate.delete(shortTermKey);
        redisTemplate.delete(longTermKey);
        redisTemplate.delete(sessionIndexKey);

        localWorkingCache.entrySet().removeIf(e -> e.getKey().startsWith("unified:memory:working:" + userId + ":"));

        log.info("清除用户所有记忆: userId={}", userId);
    }

    @Override
    public String retrieveMemoryText(Long userId, String query, int limit) {
        if (userId == null || query == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        List<UnifiedMemoryContext.MemoryItem> shortTerm = loadShortTermMemory(userId, limit);
        if (!shortTerm.isEmpty()) {
            result.append("【短期记忆】\n");
            shortTerm.forEach(m -> result.append("- ").append(m.getContent()).append("\n"));
            result.append("\n");
        }

        List<UnifiedMemoryContext.MemoryItem> longTerm = loadLongTermMemory(userId, query);
        if (!longTerm.isEmpty()) {
            result.append("【长期记忆】\n");
            longTerm.forEach(m -> result.append("- [").append(m.getCategory()).append("] ")
                .append(m.getContent()).append("\n"));
            result.append("\n");
        }

        return result.toString();
    }

    @Override
    public MemoryAnalysis analyzeAndSave(Long userId, String userMessage, String assistantMessage) {
        if (userId == null || userMessage == null || userMessage.isBlank()) {
            return new MemoryAnalysis(false, List.of(), List.of(), "");
        }

        List<UnifiedMemoryContext.MemoryItem> extracted = new ArrayList<>();
        List<String> categories = new ArrayList<>();

        UnifiedMemoryContext.MemoryType type = classifyMemoryType(userMessage);
        double importance = assessImportance(userMessage, assistantMessage);

        if (importance >= MIN_IMPORTANCE_THRESHOLD) {
            UnifiedMemoryContext.MemoryItem item = UnifiedMemoryContext.MemoryItem.builder()
                .id(UUID.randomUUID().toString())
                .role("user")
                .content(userMessage)
                .timestamp(Instant.now().toEpochMilli())
                .importance(importance)
                .type(type)
                .category(type.name())
                .build();

            extracted.add(item);
            categories.add(type.name());

            if (assistantMessage != null && !assistantMessage.isBlank()) {
                UnifiedMemoryContext.MemoryItem responseItem = UnifiedMemoryContext.MemoryItem.builder()
                    .id(UUID.randomUUID().toString())
                    .role("assistant")
                    .content(assistantMessage)
                    .timestamp(Instant.now().toEpochMilli())
                    .importance(Math.min(importance, 0.8))
                    .type(UnifiedMemoryContext.MemoryType.CONVERSATION)
                    .category("CONVERSATION")
                    .build();
                extracted.add(responseItem);
            }

            saveMemories(userId, null, extracted);
        }

        String summary = String.format("提取了 %d 条记忆，类型: %s，重要性: %.2f",
            extracted.size(), String.join(", ", categories), importance);

        return new MemoryAnalysis(!extracted.isEmpty(), extracted, categories, summary);
    }

    @Override
    public void persistWorkingToShortTerm(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }

        String cacheKey = buildCacheKey(userId, sessionId);
        List<UnifiedMemoryContext.MemoryItem> workingItems = localWorkingCache.get(cacheKey);

        if (workingItems == null || workingItems.isEmpty()) {
            String redisKey = String.format(WORKING_KEY_PREFIX, userId, sessionId);
            List<String> rawItems = redisTemplate.opsForList().range(redisKey, 0, -1);
            if (rawItems == null) return;

            workingItems = rawItems.stream().map(this::parseMemoryItem).filter(Objects::nonNull).toList();
        }

        if (workingItems.isEmpty()) return;

        String shortTermKey = String.format(SHORT_TERM_KEY_PREFIX, userId);
        for (UnifiedMemoryContext.MemoryItem item : workingItems) {
            try {
                String value = objectMapper.writeValueAsString(item);
                redisTemplate.opsForList().leftPush(shortTermKey, value);
            } catch (JsonProcessingException e) {
                log.error("序列化记忆失败", e);
            }
        }

        redisTemplate.opsForList().trim(shortTermKey, 0, MAX_SHORT_TERM_ITEMS - 1);
        redisTemplate.expire(shortTermKey, SHORT_TERM_TTL);

        clearSessionMemory(userId, sessionId);
        log.info("工作记忆转存短期记忆: userId={}, sessionId={}, count={}", userId, sessionId, workingItems.size());
    }

    @Override
    public MemoryStats getMemoryStats(Long userId) {
        if (userId == null) {
            return MemoryStats.empty(null);
        }

        String shortTermKey = String.format(SHORT_TERM_KEY_PREFIX, userId);
        String longTermKey = String.format(LONG_TERM_KEY_PREFIX, userId);

        Long shortTermCount = redisTemplate.opsForList().size(shortTermKey);
        Long longTermCount = redisTemplate.opsForList().size(longTermKey);

        int workingCount = (int) localWorkingCache.entrySet().stream()
            .filter(e -> e.getKey().contains(":" + userId + ":"))
            .mapToInt(e -> e.getValue().size())
            .sum();

        return new MemoryStats(userId,
            shortTermCount != null ? shortTermCount.intValue() : 0,
            longTermCount != null ? longTermCount.intValue() : 0,
            workingCount);
    }

    @Override
    public List<String> getUserSessions(Long userId) {
        if (userId == null) {
            return List.of();
        }

        String indexKey = String.format(SESSION_INDEX_KEY_PREFIX, userId);
        var sessions = redisTemplate.opsForSet().members(indexKey);
        return sessions != null ? new ArrayList<>(sessions) : List.of();
    }

    private List<UnifiedMemoryContext.MemoryItem> loadWorkingMemory(Long userId, String sessionId) {
        String cacheKey = buildCacheKey(userId, sessionId);
        List<UnifiedMemoryContext.MemoryItem> cached = localWorkingCache.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        String redisKey = String.format(WORKING_KEY_PREFIX, userId, sessionId);
        List<String> rawItems = redisTemplate.opsForList().range(redisKey, 0, -1);

        if (rawItems == null || rawItems.isEmpty()) {
            return new ArrayList<>();
        }

        List<UnifiedMemoryContext.MemoryItem> items = rawItems.stream()
            .map(this::parseMemoryItem)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (!items.isEmpty()) {
            localWorkingCache.put(cacheKey, new ArrayList<>(items));
        }

        return items;
    }

    private List<UnifiedMemoryContext.MemoryItem> loadShortTermMemory(Long userId, int limit) {
        String key = String.format(SHORT_TERM_KEY_PREFIX, userId);
        int fetchLimit = limit > 0 ? limit : MAX_SHORT_TERM_ITEMS;

        List<String> rawItems = redisTemplate.opsForList().range(key, 0, fetchLimit - 1);
        if (rawItems == null) {
            return new ArrayList<>();
        }

        return rawItems.stream()
            .map(this::parseMemoryItem)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<UnifiedMemoryContext.MemoryItem> loadLongTermMemory(Long userId, String query) {
        String key = String.format(LONG_TERM_KEY_PREFIX, userId);
        List<String> rawItems = redisTemplate.opsForList().range(key, 0, MAX_LONG_TERM_ITEMS - 1);

        if (rawItems == null || rawItems.isEmpty()) {
            return new ArrayList<>();
        }

        List<UnifiedMemoryContext.MemoryItem> allItems = rawItems.stream()
            .map(this::parseMemoryItem)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (query == null || query.isBlank()) {
            return allItems;
        }

        String queryLower = query.toLowerCase();
        return allItems.stream()
            .filter(item -> {
                String content = item.getContent() != null ? item.getContent().toLowerCase() : "";
                String category = item.getCategory() != null ? item.getCategory().toLowerCase() : "";
                return content.contains(queryLower) || category.contains(queryLower) ||
                       calculateRelevance(item.getContent(), query) > 0.3;
            })
            .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
            .limit(20)
            .collect(Collectors.toList());
    }

    private void saveToLongTermStorage(Long userId, UnifiedMemoryContext.MemoryItem item) {
        String key = String.format(LONG_TERM_KEY_PREFIX, userId);

        if (isDuplicateContent(userId, item.getContent())) {
            log.debug("跳过重复记忆: {}", item.getContent().substring(0, Math.min(30, item.getContent().length())));
            return;
        }

        try {
            String value = objectMapper.writeValueAsString(item);
            redisTemplate.opsForList().leftPush(key, value);
            redisTemplate.opsForList().trim(key, 0, MAX_LONG_TERM_ITEMS - 1);
            redisTemplate.expire(key, LONG_TERM_TTL);
        } catch (JsonProcessingException e) {
            log.error("保存长期记忆失败", e);
        }
    }

    private boolean isDuplicateContent(Long userId, String content) {
        String key = String.format(LONG_TERM_KEY_PREFIX, userId);
        List<String> rawItems = redisTemplate.opsForList().range(key, 0, -1);

        if (rawItems == null) return false;

        String normalizedContent = normalizeForComparison(content);
        for (String raw : rawItems) {
            UnifiedMemoryContext.MemoryItem existing = parseMemoryItem(raw);
            if (existing != null && existing.getContent() != null) {
                String normalizedExisting = normalizeForComparison(existing.getContent());
                if (normalizedContent.equals(normalizedExisting)) {
                    return true;
                }
                if (calculateRelevance(content, existing.getContent()) > 0.9) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNotDuplicateLongTerm(UnifiedMemoryContext.MemoryItem item) {
        return true;
    }

    private UnifiedMemoryContext.MemoryType classifyMemoryType(String content) {
        if (content == null) return UnifiedMemoryContext.MemoryType.FACT;

        String lower = content.toLowerCase();
        int prefScore = 0, decisionScore = 0, factScore = 0;

        for (String trigger : PREFERENCE_TRIGGERS) {
            if (lower.contains(trigger)) prefScore++;
        }
        for (String trigger : DECISION_TRIGGERS) {
            if (lower.contains(trigger)) decisionScore++;
        }
        for (String trigger : FACT_TRIGGERS) {
            if (lower.contains(trigger)) factScore++;
        }

        int maxScore = Math.max(Math.max(prefScore, decisionScore), factScore);
        if (maxScore == 0) return UnifiedMemoryContext.MemoryType.CONVERSATION;
        if (prefScore == maxScore) return UnifiedMemoryContext.MemoryType.PREFERENCE;
        if (decisionScore == maxScore) return UnifiedMemoryContext.MemoryType.DECISION;
        return UnifiedMemoryContext.MemoryType.FACT;
    }

    private double assessImportance(String userMessage, String assistantMessage) {
        double importance = 0.5;

        if (userMessage == null) return importance;

        int length = userMessage.length();
        if (length > 100) importance += 0.1;
        if (length > 300) importance += 0.1;

        UnifiedMemoryContext.MemoryType type = classifyMemoryType(userMessage);
        switch (type) {
            case PREFERENCE -> importance += 0.2;
            case DECISION -> importance += 0.25;
            case FACT -> importance += 0.15;
            default -> {}
        }

        if (PREFERENCE_TRIGGERS.stream().anyMatch(userMessage::contains)) importance += 0.1;
        if (DECISION_TRIGGERS.stream().anyMatch(userMessage::contains)) importance += 0.15;
        if (userMessage.contains("不要") || userMessage.contains("别")) importance += 0.1;

        if (assistantMessage != null && assistantMessage.contains("我会记住")) importance += 0.1;

        return Math.min(importance, 1.0);
    }

    private double calculateRelevance(String content1, String content2) {
        if (content1 == null || content2 == null) return 0.0;

        Set<String> words1 = new HashSet<>(List.of(content1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(List.of(content2.toLowerCase().split("\\s+")));

        words1.removeIf(w -> w.length() < 2);
        words2.removeIf(w -> w.length() < 2);

        if (words1.isEmpty() || words2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        return (double) intersection.size() / Math.min(words1.size(), words2.size());
    }

    private String normalizeForComparison(String text) {
        if (text == null) return "";
        return text.toLowerCase()
            .replaceAll("\\s+", "")
            .replaceAll("[，。！？、]", "");
    }

    private void indexSession(Long userId, String sessionId) {
        String indexKey = String.format(SESSION_INDEX_KEY_PREFIX, userId);
        redisTemplate.opsForSet().add(indexKey, sessionId);
        redisTemplate.expire(indexKey, SHORT_TERM_TTL);
    }

    private String buildCacheKey(Long userId, String sessionId) {
        return String.format("unified:memory:working:%s:%s", userId, sessionId);
    }

    private UnifiedMemoryContext.MemoryItem parseMemoryItem(String json) {
        try {
            return objectMapper.readValue(json, UnifiedMemoryContext.MemoryItem.class);
        } catch (JsonProcessingException e) {
            log.error("解析记忆失败: {}", json, e);
            return null;
        }
    }
}