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
     * 获取会话（带用户权限校验）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 会话状态，如果会话不存在或用户无权访问则返回 empty
     */
    public Optional<AgentState> getSession(String sessionId, Long userId) {
        Optional<AgentState> state = getSessionInternal(sessionId);
        return state.filter(s -> s.getUserId().equals(userId));
    }

    /**
     * 获取会话（内部方法，不带权限校验）
     * 仅在服务层内部使用，不对外暴露
     */
    private Optional<AgentState> getSessionInternal(String sessionId) {
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
     * 获取会话（已废弃，请使用带 userId 的版本）
     * @deprecated 使用 {@link #getSession(String, Long)} 替代
     */
    @Deprecated
    public Optional<AgentState> getSession(String sessionId) {
        return getSessionInternal(sessionId);
    }
    
    /**
     * 获取或创建会话（带用户权限校验）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 现有会话（如果存在且属于该用户）或新创建的会话
     */
    public AgentState getOrCreateSession(String sessionId, Long userId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            Optional<AgentState> existing = getSession(sessionId, userId);
            if (existing.isPresent()) {
                return existing.get();
            }
            // 会话存在但不属于该用户，记录警告
            Optional<AgentState> unauthorizedSession = getSessionInternal(sessionId);
            if (unauthorizedSession.isPresent()) {
                log.warn("用户 {} 尝试访问不属于自己的会话 {}", userId, sessionId);
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
     * 结束会话（带用户权限校验）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return true 如果成功结束，false 如果会话不存在或用户无权访问
     */
    public boolean endSession(String sessionId, Long userId) {
        Optional<AgentState> state = getSession(sessionId, userId);
        if (state.isEmpty()) {
            log.warn("用户 {} 尝试结束不属于自己的会话 {}", userId, sessionId);
            return false;
        }

        activeSessions.remove(sessionId);
        cacheService.delete(getSessionKey(sessionId));
        log.info("结束 Agent 会话: sessionId={}, userId={}", sessionId, userId);
        return true;
    }

    /**
     * 结束会话（已废弃，请使用带 userId 的版本）
     * @deprecated 使用 {@link #endSession(String, Long)} 替代
     */
    @Deprecated
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