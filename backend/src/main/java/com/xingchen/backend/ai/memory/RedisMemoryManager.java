package com.xingchen.backend.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的记忆管理器
 * 管理工作记忆和短期记忆
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMemoryManager implements MemoryManager {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String WORKING_MEMORY_KEY = "ai:memory:working:%s";
    private static final String SHORT_TERM_MEMORY_KEY = "ai:memory:short:%s";
    private static final long WORKING_MEMORY_TTL = 30; // 30分钟
    private static final long SHORT_TERM_MEMORY_TTL = 7; // 7天
    
    @Override
    public MemoryContext load(Long userId, String sessionId, MemoryRequirements requirements) {
        MemoryContext.MemoryContextBuilder contextBuilder = MemoryContext.builder();
        
        try {
            // 加载工作记忆
            if (requirements.isNeedWorking() && sessionId != null) {
                List<MemoryContext.MemoryItem> working = loadWorkingMemory(sessionId);
                contextBuilder.workingMemory(working);
            }
            
            // 加载短期记忆
            if (requirements.isNeedShortTerm() && userId != null) {
                List<MemoryContext.MemoryItem> shortTerm = loadShortTermMemory(userId, requirements.getShortTermRounds());
                contextBuilder.shortTermMemory(shortTerm);
            }
            
            // 长期记忆由HybridMemoryManager处理
            
        } catch (Exception e) {
            log.error("加载记忆失败: userId={}, sessionId={}", userId, sessionId, e);
        }
        
        return contextBuilder.build();
    }
    
    @Override
    public void save(Long userId, List<MemoryContext.MemoryItem> memories) {
        // 短期记忆保存
        if (userId != null && memories != null && !memories.isEmpty()) {
            String key = String.format(SHORT_TERM_MEMORY_KEY, userId);
            try {
                for (MemoryContext.MemoryItem memory : memories) {
                    String value = objectMapper.writeValueAsString(memory);
                    redisTemplate.opsForList().leftPush(key, value);
                }
                // 限制长度
                redisTemplate.opsForList().trim(key, 0, 99);
                redisTemplate.expire(key, SHORT_TERM_MEMORY_TTL, TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("保存短期记忆失败: userId={}", userId, e);
            }
        }
    }
    
    @Override
    public void addWorkingMemory(String sessionId, MemoryContext.MemoryItem item) {
        if (sessionId == null || item == null) {
            return;
        }
        
        String key = String.format(WORKING_MEMORY_KEY, sessionId);
        try {
            String value = objectMapper.writeValueAsString(item);
            redisTemplate.opsForList().rightPush(key, value);
            redisTemplate.expire(key, WORKING_MEMORY_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("添加工作记忆失败: sessionId={}", sessionId, e);
        }
    }
    
    @Override
    public void clearUserMemory(Long userId) {
        if (userId == null) {
            return;
        }
        
        String shortTermKey = String.format(SHORT_TERM_MEMORY_KEY, userId);
        redisTemplate.delete(shortTermKey);
        log.info("清除用户短期记忆: userId={}", userId);
    }
    
    @Override
    public void clearSessionMemory(String sessionId) {
        if (sessionId == null) {
            return;
        }
        
        String workingKey = String.format(WORKING_MEMORY_KEY, sessionId);
        redisTemplate.delete(workingKey);
        log.info("清除会话工作记忆: sessionId={}", sessionId);
    }
    
    private List<MemoryContext.MemoryItem> loadWorkingMemory(String sessionId) {
        String key = String.format(WORKING_MEMORY_KEY, sessionId);
        List<String> values = redisTemplate.opsForList().range(key, 0, -1);
        return parseMemoryItems(values);
    }
    
    private List<MemoryContext.MemoryItem> loadShortTermMemory(Long userId, int rounds) {
        String key = String.format(SHORT_TERM_MEMORY_KEY, userId);
        List<String> values = redisTemplate.opsForList().range(key, 0, rounds - 1);
        return parseMemoryItems(values);
    }
    
    private List<MemoryContext.MemoryItem> parseMemoryItems(List<String> values) {
        List<MemoryContext.MemoryItem> items = new ArrayList<>();
        if (values == null) {
            return items;
        }
        
        for (String value : values) {
            try {
                MemoryContext.MemoryItem item = objectMapper.readValue(value, MemoryContext.MemoryItem.class);
                items.add(item);
            } catch (Exception e) {
                log.error("解析记忆项失败: {}", value, e);
            }
        }
        
        return items;
    }
    
    @Override
    public String retrieveMemory(String userInput, String sessionId) {
        MemoryRequirements requirements = MemoryRequirements.minimal()
                .withShortTerm(true);
        
        MemoryContext context = load(null, sessionId, requirements);
        
        StringBuilder memoryContext = new StringBuilder();
        
        // 添加短期记忆
        if (context.getShortTermMemory() != null && !context.getShortTermMemory().isEmpty()) {
            memoryContext.append("短期记忆:\n");
            for (MemoryContext.MemoryItem item : context.getShortTermMemory()) {
                memoryContext.append("- " + item.getContent() + "\n");
            }
            memoryContext.append("\n");
        }
        
        return memoryContext.toString();
    }
    
    @Override
    public void updateMemory(String userInput, String response, String sessionId) {
        MemoryContext.MemoryItem item = MemoryContext.MemoryItem.builder()
                .content("用户: " + userInput + "\n助手: " + response)
                .timestamp(System.currentTimeMillis())
                .build();
        
        addWorkingMemory(sessionId, item);
        
        // 保存到短期记忆
        List<MemoryContext.MemoryItem> items = new ArrayList<>();
        items.add(item);
        save(null, items);
    }
}