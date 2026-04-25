package com.xingchen.backend.agent;

import com.xingchen.backend.cache.CacheService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Agent 会话管理器
 * 管理用户的 Agent 会话，确保权限隔离
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentSessionManager {

    private final CacheService cacheService;

    // 本地会话缓存，用于快速访问（带过期时间）
    private final Map<String, SessionEntry> sessionCache = new ConcurrentHashMap<>();
    // 用户会话映射，用于统计用户的活跃会话
    private final Map<Long, Set<String>> userSessionMap = new ConcurrentHashMap<>();

    private static final long SESSION_TTL_MINUTES = 60;
    private static final long CLEANUP_INTERVAL_MINUTES = 10;

    /**
     * 会话条目（包含过期时间）
     */
    private record SessionEntry(AgentState state, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }// 判断是否过期
    }

    @PostConstruct
    public void init() {
        log.info("AgentSessionManager 初始化，SESSION_TTL={}分钟", SESSION_TTL_MINUTES);
    }

    @PreDestroy
    public void destroy() {
        sessionCache.clear();
        userSessionMap.clear();
        log.info("AgentSessionManager 已清理");
    }

    /**
     * 定时清理过期会话
     */
    @Scheduled(fixedRate = CLEANUP_INTERVAL_MINUTES * 60 * 1000)// 每10分钟执行一次
    public void cleanupExpiredSessions() {
        int before = sessionCache.size();
        long now = System.currentTimeMillis();

        var expiredKeys = sessionCache.entrySet().stream()
                .filter(e -> e.getValue().isExpired())
                .map(Map.Entry::getKey)
                .toList();// 获取过期的会话ID

        for (String sessionId : expiredKeys) {
            SessionEntry entry = sessionCache.remove(sessionId);
            if (entry != null) {
                Long userId = entry.state().getUserId();// 获取用户ID,以便删除
                Set<String> userSessions = userSessionMap.get(userId);
                if (userSessions != null) {
                    userSessions.remove(sessionId);
                    if (userSessions.isEmpty()) {
                        userSessionMap.remove(userId);
                    }
                }
            }
        }

        int cleaned = expiredKeys.size();
        if (cleaned > 0) {
            log.info("清理过期会话: 清理 {} 个，剩余 {} 个", cleaned, sessionCache.size());
        }
    }

    /**
     * 创建新会话
     */
    public AgentState createSession(Long userId) {
        // 生成唯一的会话ID，格式为 "agent_" + UUID
        String sessionId = "agent_" + UUID.randomUUID().toString().replace("-", "");
        // 创建新的 AgentState 对象
        AgentState state = new AgentState(sessionId, userId);

        // 计算会话过期时间（当前时间 + TTL）
        long expiresAt = System.currentTimeMillis() + SESSION_TTL_MINUTES * 60 * 1000L;
        // 将新会话存入本地缓存
        sessionCache.put(sessionId, new SessionEntry(state, expiresAt));
        // 维护用户与会话的映射关系
        userSessionMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // 记录创建会话的日志
        log.info("创建新会话: sessionId={}, userId={}, expiresAt={}", sessionId, userId,
                Instant.ofEpochMilli(expiresAt));
        return state;
    }

    /**
     * 获取会话（带权限检查），同时刷新 TTL
     */
    public Optional<AgentState> getSession(String sessionId, Long userId) {
        SessionEntry entry = sessionCache.get(sessionId);

        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                sessionCache.remove(sessionId);//当会话过期时，从缓存中删除
            }
            return Optional.empty();// 会话不存在或者已过期
        }

        AgentState state = entry.state();//获取会话状态
        if (!Objects.equals(state.getUserId(), userId)) {
            return Optional.empty();//权限检查
        }

        // 刷新 TTL
        long newExpiresAt = System.currentTimeMillis() + SESSION_TTL_MINUTES * 60 * 1000L;
        sessionCache.put(sessionId, new SessionEntry(state, newExpiresAt));//更新过期时间

        return Optional.of(state);
    }

    /**
     * 获取或创建会话
     */
    public AgentState getOrCreateSession(String sessionId, Long userId) {
        Optional<AgentState> existingSession = getSession(sessionId, userId);//获取会话
        if (existingSession.isPresent()) {//存在则返回
            return existingSession.get();
        }
        return createSession(userId);
    }

    /**
     * 结束会话
     */
    public boolean endSession(String sessionId, Long userId) {
        SessionEntry entry = sessionCache.get(sessionId);//获取会话

        if (entry == null || entry.isExpired()) {//不存在或者已过期
            sessionCache.remove(sessionId);
            return false;
        }

        AgentState state = entry.state();
        if (!Objects.equals(state.getUserId(), userId)) {
            return false;
        }

        sessionCache.remove(sessionId);
        Set<String> userSessions = userSessionMap.get(userId);//维护反向索引
        if (userSessions != null) {
            userSessions.remove(sessionId);
            if (userSessions.isEmpty()) {
                userSessionMap.remove(userId);
            }
        }

        log.info("结束会话: sessionId={}, userId={}", sessionId, userId);
        return true;
    }

    /**
     * 获取用户的活跃会话计数
     */
    public int getActiveSessionCount(Long userId) {
        Set<String> userSessions = userSessionMap.get(userId);
        return userSessions != null ? userSessions.size() : 0;//返回用户活跃会话数
    }

    /**
     * 更新会话状态并刷新 TTL
     */
    public void updateSession(AgentState state) {
        if (state == null || state.getSessionId() == null) {
            return;
        }

        // 如果会话已过期，不更新
        SessionEntry existing = sessionCache.get(state.getSessionId());
        if (existing != null && existing.isExpired()) {
            sessionCache.remove(state.getSessionId());
            return;
        }

        long expiresAt = System.currentTimeMillis() + SESSION_TTL_MINUTES * 60 * 1000L;
        sessionCache.put(state.getSessionId(), new SessionEntry(state, expiresAt));//更新过期时间
        log.debug("更新会话状态: sessionId={}, status={}", state.getSessionId(), state.getStatus());
    }

    /**
     * 获取缓存会话数（用于监控）
     */
    public int getCacheSize() {
        return sessionCache.size();
    }
}
