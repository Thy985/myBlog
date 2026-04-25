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
 * 用户级记忆管理器
 * 每个用户的记忆完全隔离
 * @deprecated 请使用 {@link UnifiedMemoryServiceImpl}
 */
@Deprecated
@Component
@RequiredArgsConstructor
@Slf4j
public class UserMemoryManager implements MemoryManager {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Key格式: ai:user:{userId}:session:{sessionId}:working
    private static final String WORKING_MEMORY_KEY = "ai:user:%s:session:%s:working";
    private static final String SHORT_TERM_MEMORY_KEY = "ai:user:%s:short";
    private static final String USER_SESSIONS_KEY = "ai:user:%s:sessions";
    
    private static final long WORKING_MEMORY_TTL = 30; // 30分钟
    private static final long SHORT_TERM_MEMORY_TTL = 7; // 7天
    
    @Override
    public MemoryContext load(Long userId, String sessionId, MemoryRequirements requirements) {
        if (userId == null) {
            log.warn("用户ID为空，无法加载记忆");
            return MemoryContext.builder().build();
        }
        
        MemoryContext.MemoryContextBuilder contextBuilder = MemoryContext.builder();
        
        try {
            // 加载工作记忆（会话级）
            if (requirements.isNeedWorking() && sessionId != null) {
                List<MemoryContext.MemoryItem> working = loadWorkingMemory(userId, sessionId);
                contextBuilder.workingMemory(working);
            }
            
            // 加载短期记忆（用户级）
            if (requirements.isNeedShortTerm()) {
                List<MemoryContext.MemoryItem> shortTerm = loadShortTermMemory(userId, requirements.getShortTermRounds());
                contextBuilder.shortTermMemory(shortTerm);
            }
            
            // 记录用户会话
            if (sessionId != null) {
                recordUserSession(userId, sessionId);
            }
            
        } catch (Exception e) {
            log.error("加载用户记忆失败: userId={}, sessionId={}", userId, sessionId, e);
        }
        
        return contextBuilder.build();
    }
    
    @Override
    public void save(Long userId, List<MemoryContext.MemoryItem> memories) {
        if (userId == null || memories == null || memories.isEmpty()) {
            return;
        }
        
        String key = String.format(SHORT_TERM_MEMORY_KEY, userId);
        
        try {
            for (MemoryContext.MemoryItem memory : memories) {
                String value = objectMapper.writeValueAsString(memory);
                redisTemplate.opsForList().leftPush(key, value);
            }
            
            // 限制长度
            redisTemplate.opsForList().trim(key, 0, 99);
            redisTemplate.expire(key, SHORT_TERM_MEMORY_TTL, TimeUnit.DAYS);
            
            log.debug("保存用户短期记忆: userId={}, count={}", userId, memories.size());
            
        } catch (Exception e) {
            log.error("保存用户短期记忆失败: userId={}", userId, e);
        }
    }
    
    @Override
    public void addWorkingMemory(String sessionId, MemoryContext.MemoryItem item) {
        // 需要从item中提取userId，这里简化处理
        // 实际应该在item中包含userId
        log.warn("addWorkingMemory需要userId，请使用addUserWorkingMemory方法");
    }
    
    /**
     * 添加用户工作记忆
     */
    public void addUserWorkingMemory(Long userId, String sessionId, MemoryContext.MemoryItem item) {
        if (userId == null || sessionId == null || item == null) {
            return;
        }
        
        String key = String.format(WORKING_MEMORY_KEY, userId, sessionId);
        
        try {
            String value = objectMapper.writeValueAsString(item);
            redisTemplate.opsForList().rightPush(key, value);
            redisTemplate.expire(key, WORKING_MEMORY_TTL, TimeUnit.MINUTES);
            
            log.debug("添加用户工作记忆: userId={}, sessionId={}", userId, sessionId);
            
        } catch (Exception e) {
            log.error("添加用户工作记忆失败: userId={}, sessionId={}", userId, sessionId, e);
        }
    }
    
    @Override
    public void clearUserMemory(Long userId) {
        if (userId == null) {
            return;
        }

        // 清除短期记忆
        String shortTermKey = String.format(SHORT_TERM_MEMORY_KEY, userId);
        redisTemplate.delete(shortTermKey);

        // 清除所有会话的工作记忆
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        var sessions = redisTemplate.opsForSet().members(sessionsKey);
        if (sessions != null) {
            for (String sessionId : sessions) {
                String workingKey = String.format(WORKING_MEMORY_KEY, userId, sessionId);
                redisTemplate.delete(workingKey);
            }
        }
        redisTemplate.delete(sessionsKey);

        log.info("清除用户所有记忆: userId={}", userId);
    }
    
    @Override
    public void clearSessionMemory(String sessionId) {
        // 需要userId才能清除，这里简化处理
        log.warn("clearSessionMemory需要userId，请使用clearUserSessionMemory方法");
    }
    
    /**
     * 清除用户特定会话的记忆
     */
    public void clearUserSessionMemory(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        
        String workingKey = String.format(WORKING_MEMORY_KEY, userId, sessionId);
        redisTemplate.delete(workingKey);
        
        // 从用户会话集合中移除
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        redisTemplate.opsForSet().remove(sessionsKey, sessionId);
        
        log.info("清除用户会话记忆: userId={}, sessionId={}", userId, sessionId);
    }
    
    /**
     * 获取用户的所有会话
     */
    public List<String> getUserSessions(Long userId) {
        if (userId == null) {
            return List.of();
        }

        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        var sessions = redisTemplate.opsForSet().members(sessionsKey);
        return sessions != null ? new ArrayList<>(sessions) : List.of();
    }
    
    /**
     * 获取用户记忆统计
     */
    public MemoryStats getUserMemoryStats(Long userId) {
        if (userId == null) {
            return MemoryStats.empty();
        }
        
        String shortTermKey = String.format(SHORT_TERM_MEMORY_KEY, userId);
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        
        Long shortTermCount = redisTemplate.opsForList().size(shortTermKey);
        Long sessionCount = redisTemplate.opsForSet().size(sessionsKey);
        
        return MemoryStats.builder()
                .userId(userId)
                .shortTermMemoryCount(shortTermCount != null ? shortTermCount.intValue() : 0)
                .activeSessions(sessionCount != null ? sessionCount.intValue() : 0)
                .build();
    }
    
    private List<MemoryContext.MemoryItem> loadWorkingMemory(Long userId, String sessionId) {
        String key = String.format(WORKING_MEMORY_KEY, userId, sessionId);
        List<String> values = redisTemplate.opsForList().range(key, 0, -1);
        return parseMemoryItems(values);
    }
    
    private List<MemoryContext.MemoryItem> loadShortTermMemory(Long userId, int rounds) {
        String key = String.format(SHORT_TERM_MEMORY_KEY, userId);
        List<String> values = redisTemplate.opsForList().range(key, 0, rounds - 1);
        return parseMemoryItems(values);
    }
    
    private void recordUserSession(Long userId, String sessionId) {
        String sessionsKey = String.format(USER_SESSIONS_KEY, userId);
        redisTemplate.opsForSet().add(sessionsKey, sessionId);
        redisTemplate.expire(sessionsKey, SHORT_TERM_MEMORY_TTL, TimeUnit.DAYS);
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
    
    /**
     * 记忆统计
     */
    public record MemoryStats(Long userId, int shortTermMemoryCount, int activeSessions) {
        public static MemoryStats empty() {
            return new MemoryStats(null, 0, 0);
        }
        
        public static MemoryStatsBuilder builder() {
            return new MemoryStatsBuilder();
        }
        
        public static class MemoryStatsBuilder {
            private Long userId;
            private int shortTermMemoryCount;
            private int activeSessions;
            
            public MemoryStatsBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }
            
            public MemoryStatsBuilder shortTermMemoryCount(int count) {
                this.shortTermMemoryCount = count;
                return this;
            }
            
            public MemoryStatsBuilder activeSessions(int count) {
                this.activeSessions = count;
                return this;
            }
            
            public MemoryStats build() {
                return new MemoryStats(userId, shortTermMemoryCount, activeSessions);
            }
        }
    }
}