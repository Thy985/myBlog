package com.xingchen.backend.ai.memory;

import com.xingchen.backend.ai.model.Intent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 记忆服务实现
 * 支持工作记忆、短期记忆和长期记忆
 * @deprecated 请使用 {@link UnifiedMemoryServiceImpl}
 */
@Deprecated
@Service
@Slf4j
@RequiredArgsConstructor
public class AIMemoryServiceImpl implements MemoryManager {

    private final StringRedisTemplate redisTemplate;
    private final RedisMemoryManager redisMemoryManager;
    private final UserMemoryManager userMemoryManager;
    
    // 本地缓存工作记忆（会话级别）
    private final Map<String, List<MemoryContext.MemoryItem>> workingMemoryCache = new ConcurrentHashMap<>();
    
    @Override
    public MemoryContext load(Long userId, String sessionId, MemoryRequirements requirements) {
        MemoryContext context = MemoryContext.builder()
                .workingMemory(new ArrayList<>())
                .shortTermMemory(new ArrayList<>())
                .longTermMemory(new ArrayList<>())
                .build();
        
        // 加载工作记忆
        if (requirements.isNeedWorking()) {
            loadWorkingMemory(sessionId, context);
        }
        
        // 加载短期记忆
        if (requirements.isNeedShortTerm()) {
            loadShortTermMemory(userId, sessionId, requirements.getShortTermRounds(), context);
        }
        
        // 加载长期记忆
        if (requirements.isNeedLongTerm() && requirements.getQueryForLongTerm() != null) {
            loadLongTermMemory(userId, requirements.getQueryForLongTerm(), context);
        }
        
        return context;
    }
    
    @Override
    public void save(Long userId, List<MemoryContext.MemoryItem> memories) {
        if (memories == null || memories.isEmpty()) {
            return;
        }
        
        // 保存到长期记忆
        userMemoryManager.save(userId, memories);
        log.info("保存了 {} 条长期记忆", memories.size());
    }
    
    @Override
    public void addWorkingMemory(String sessionId, MemoryContext.MemoryItem item) {
        if (sessionId == null || item == null) {
            return;
        }
        
        // 添加到本地缓存
        workingMemoryCache.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(item);
        
        // 限制工作记忆大小
        List<MemoryContext.MemoryItem> items = workingMemoryCache.get(sessionId);
        if (items.size() > 50) {
            items = items.subList(items.size() - 50, items.size());
            workingMemoryCache.put(sessionId, items);
        }
        
        // 保存到Redis
        redisMemoryManager.addWorkingMemory(sessionId, item);
        log.debug("添加工作记忆: sessionId={}, content={}", sessionId, item.getContent().substring(0, Math.min(50, item.getContent().length())));
    }
    
    @Override
    public void clearUserMemory(Long userId) {
        userMemoryManager.clearUserMemory(userId);
        log.info("清除用户 {} 的所有记忆", userId);
    }
    
    @Override
    public void clearSessionMemory(String sessionId) {
        workingMemoryCache.remove(sessionId);
        redisMemoryManager.clearSessionMemory(sessionId);
        log.info("清除会话 {} 的所有记忆", sessionId);
    }
    
    /**
     * 加载工作记忆
     */
    private void loadWorkingMemory(String sessionId, MemoryContext context) {
        // 从本地缓存加载
        List<MemoryContext.MemoryItem> items = workingMemoryCache.get(sessionId);
        if (items != null && !items.isEmpty()) {
            context.getWorkingMemory().addAll(items);
            return;
        }
        
        // 从Redis加载
        // 由于RedisMemoryManager.loadWorkingMemory是私有方法，我们使用完整的load方法
        MemoryContext redisContext = redisMemoryManager.load(null, sessionId, MemoryRequirements.minimal().withWorking(true));
        if (redisContext != null && redisContext.getWorkingMemory() != null && !redisContext.getWorkingMemory().isEmpty()) {
            context.getWorkingMemory().addAll(redisContext.getWorkingMemory());
            workingMemoryCache.put(sessionId, redisContext.getWorkingMemory());
        }
    }
    
    /**
     * 加载短期记忆
     */
    private void loadShortTermMemory(Long userId, String sessionId, int rounds, MemoryContext context) {
        // 由于RedisMemoryManager.loadShortTermMemory是私有方法，我们使用完整的load方法
        MemoryContext redisContext = redisMemoryManager.load(userId, sessionId, MemoryRequirements.minimal().withShortTerm(true).withRounds(rounds));
        if (redisContext != null && redisContext.getShortTermMemory() != null && !redisContext.getShortTermMemory().isEmpty()) {
            context.getShortTermMemory().addAll(redisContext.getShortTermMemory());
        }
    }
    
    /**
     * 加载长期记忆
     */
    private void loadLongTermMemory(Long userId, String query, MemoryContext context) {
        // 由于UserMemoryManager没有retrieveUserMemories方法，我们使用完整的load方法
        MemoryContext userContext = userMemoryManager.load(userId, null, MemoryRequirements.minimal().withShortTerm(true));
        if (userContext != null && userContext.getShortTermMemory() != null && !userContext.getShortTermMemory().isEmpty()) {
            context.getLongTermMemory().addAll(userContext.getShortTermMemory());
        }
    }
    
    /**
     * 检索记忆上下文（简化版，用于AgentOrchestrator）
     */
    public String retrieveMemory(String userInput, String sessionId) {
        MemoryRequirements requirements = MemoryRequirements.minimal()
                .withShortTerm(true)
                .withLongTerm(true)
                .withQuery(userInput);
        
        MemoryContext context = load(null, sessionId, requirements);
        
        StringBuilder memoryContext = new StringBuilder();
        
        // 添加短期记忆
        if (!context.getShortTermMemory().isEmpty()) {
            memoryContext.append("短期记忆:\n");
            for (MemoryContext.MemoryItem item : context.getShortTermMemory()) {
                memoryContext.append("- " + item.getContent() + "\n");
            }
            memoryContext.append("\n");
        }
        
        // 添加长期记忆
        if (!context.getLongTermMemory().isEmpty()) {
            memoryContext.append("长期记忆:\n");
            for (MemoryContext.MemoryItem item : context.getLongTermMemory()) {
                memoryContext.append("- " + item.getContent() + "\n");
            }
        }
        
        return memoryContext.toString();
    }
    
    /**
     * 更新记忆（简化版，用于AgentOrchestrator）
     */
    public void updateMemory(String userInput, String response, String sessionId) {
        MemoryContext.MemoryItem item = MemoryContext.MemoryItem.builder()
                .content("用户: " + userInput + "\n助手: " + response)
                .timestamp(System.currentTimeMillis())
                .build();
        
        addWorkingMemory(sessionId, item);
        
        // 保存到短期记忆
        // 由于RedisMemoryManager没有saveShortTermMemory方法，我们使用save方法
        List<MemoryContext.MemoryItem> items = new ArrayList<>();
        items.add(item);
        redisMemoryManager.save(null, items);
    }
}
