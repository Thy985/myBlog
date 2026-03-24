package com.xingchen.backend.agent;

import com.xingchen.backend.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 会话管理器
 * 
 * 管理多轮对话会话状态
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentSessionManager {

    private final CacheService cacheService;
    
    // 本地缓存（活跃会话）
    private final Map<String, AgentState> activeSessions = new ConcurrentHashMap<>();
    
    // 会话过期时间（30分钟）
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    
    /**
     * 创建新会话
     */
    public AgentState createSession(Long userId) {
        String sessionId = generateSessionId();
        AgentState state = new AgentState(sessionId, userId);
        
        activeSessions.put(sessionId, state);
        persistSession(state);
        
        log.info("创建 Agent 会话: sessionId={}, userId={}", sessionId, userId);
        return state;
    }
    
    /**
     * 获取会话
     */
    public Optional<AgentState> getSession(String sessionId) {
        // 1. 从本地缓存获取
        AgentState state = activeSessions.get(sessionId);
        if (state != null) {
            return Optional.of(state);
        }
        
        // 2. 从 Redis 获取
        Optional<AgentState> cached = cacheService.get(
                getSessionKey(sessionId), 
                AgentState.class
        );
        
        // 恢复到本地缓存
        cached.ifPresent(s -> activeSessions.put(sessionId, s));
        
        return cached;
    }
    
    /**
     * 获取或创建会话
     */
    public AgentState getOrCreateSession(String sessionId, Long userId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            Optional<AgentState> existing = getSession(sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return createSession(userId);
    }
    
    /**
     * 更新会话
     */
    public void updateSession(AgentState state) {
        activeSessions.put(state.getSessionId(), state);
        persistSession(state);
    }
    
    /**
     * 结束会话
     */
    public void endSession(String sessionId) {
        activeSessions.remove(sessionId);
        cacheService.delete(getSessionKey(sessionId));
        log.info("结束 Agent 会话: sessionId={}", sessionId);
    }
    
    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int count = 0;
        
        for (Map.Entry<String, AgentState> entry : activeSessions.entrySet()) {
            AgentState state = entry.getValue();
            long lastUpdate = java.time.Duration.between(
                    state.getLastUpdateTime(),
                    java.time.LocalDateTime.now()
            ).toMinutes();
            
            if (lastUpdate > SESSION_TTL.toMinutes()) {
                activeSessions.remove(entry.getKey());
                count++;
            }
        }
        
        if (count > 0) {
            log.info("清理过期会话: {} 个", count);
        }
    }
    
    /**
     * 获取用户活跃会话数
     */
    public int getActiveSessionCount(Long userId) {
        return (int) activeSessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .count();
    }
    
    /**
     * 获取所有活跃会话数
     */
    public int getTotalActiveSessions() {
        return activeSessions.size();
    }
    
    private String generateSessionId() {
        return "agent_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private String getSessionKey(String sessionId) {
        return "agent:session:" + sessionId;
    }
    
    private void persistSession(AgentState state) {
        cacheService.set(getSessionKey(state.getSessionId()), state, SESSION_TTL);
    }
}