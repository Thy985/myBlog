package com.xingchen.backend.agent;

import com.xingchen.backend.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentSessionManager 单元测试
 * 重点测试权限隔离功能
 */
class AgentSessionManagerTest {

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private AgentSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("创建会话 - 应该成功创建并返回会话状态")
    void createSession_shouldCreateAndReturnState() {
        // Given
        Long userId = 1L;

        // When
        AgentState state = sessionManager.createSession(userId);

        // Then
        assertNotNull(state);
        assertEquals(userId, state.getUserId());
        assertNotNull(state.getSessionId());
        assertTrue(state.getSessionId().startsWith("agent_"));
        assertEquals(AgentState.TaskStatus.IDLE, state.getStatus());
    }

    @Test
    @DisplayName("获取会话 - 用户应该能访问自己的会话")
    void getSession_withCorrectUser_shouldReturnSession() {
        // Given
        Long userId = 1L;
        AgentState createdState = sessionManager.createSession(userId);
        String sessionId = createdState.getSessionId();

        // When
        Optional<AgentState> result = sessionManager.getSession(sessionId, userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    @DisplayName("获取会话 - 用户不应该能访问其他用户的会话")
    void getSession_withWrongUser_shouldReturnEmpty() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        AgentState createdState = sessionManager.createSession(userId1);
        String sessionId = createdState.getSessionId();

        // When - 用户2尝试访问用户1的会话
        Optional<AgentState> result = sessionManager.getSession(sessionId, userId2);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("获取会话 - 不存在的会话应该返回空")
    void getSession_nonExistent_shouldReturnEmpty() {
        // Given
        Long userId = 1L;
        String nonExistentSessionId = "agent_nonexistent123";

        // When
        Optional<AgentState> result = sessionManager.getSession(nonExistentSessionId, userId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("获取或创建会话 - 存在且属于用户的会话应该返回现有会话")
    void getOrCreateSession_existingOwnedSession_shouldReturnExisting() {
        // Given
        Long userId = 1L;
        AgentState createdState = sessionManager.createSession(userId);
        String sessionId = createdState.getSessionId();

        // When
        AgentState result = sessionManager.getOrCreateSession(sessionId, userId);

        // Then
        assertEquals(sessionId, result.getSessionId());
        assertEquals(userId, result.getUserId());
    }

    @Test
    @DisplayName("获取或创建会话 - 存在但不属于用户的会话应该创建新会话")
    void getOrCreateSession_existingNotOwnedSession_shouldCreateNew() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        AgentState createdState = sessionManager.createSession(userId1);
        String sessionId = createdState.getSessionId();

        // When - 用户2尝试获取用户1的会话
        AgentState result = sessionManager.getOrCreateSession(sessionId, userId2);

        // Then - 应该创建新会话
        assertNotEquals(sessionId, result.getSessionId());
        assertEquals(userId2, result.getUserId());
    }

    @Test
    @DisplayName("结束会话 - 用户应该能结束自己的会话")
    void endSession_withCorrectUser_shouldSucceed() {
        // Given
        Long userId = 1L;
        AgentState createdState = sessionManager.createSession(userId);
        String sessionId = createdState.getSessionId();

        // When
        boolean result = sessionManager.endSession(sessionId, userId);

        // Then
        assertTrue(result);
        assertFalse(sessionManager.getSession(sessionId, userId).isPresent());
    }

    @Test
    @DisplayName("结束会话 - 用户不应该能结束其他用户的会话")
    void endSession_withWrongUser_shouldFail() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        AgentState createdState = sessionManager.createSession(userId1);
        String sessionId = createdState.getSessionId();

        // When - 用户2尝试结束用户1的会话
        boolean result = sessionManager.endSession(sessionId, userId2);

        // Then
        assertFalse(result);
        // 用户1的会话应该仍然存在
        assertTrue(sessionManager.getSession(sessionId, userId1).isPresent());
    }

    @Test
    @DisplayName("活跃会话计数 - 应该只计算该用户的会话")
    void getActiveSessionCount_shouldCountOnlyUserSessions() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;

        // 为用户1创建2个会话
        sessionManager.createSession(userId1);
        sessionManager.createSession(userId1);

        // 为用户2创建1个会话
        sessionManager.createSession(userId2);

        // When
        int countUser1 = sessionManager.getActiveSessionCount(userId1);
        int countUser2 = sessionManager.getActiveSessionCount(userId2);

        // Then
        assertEquals(2, countUser1);
        assertEquals(1, countUser2);
    }

    @Test
    @DisplayName("更新会话 - 应该更新本地缓存和持久化")
    void updateSession_shouldUpdateCacheAndPersist() {
        // Given
        Long userId = 1L;
        AgentState state = sessionManager.createSession(userId);
        state.setCurrentTask("测试任务");
        state.updateStatus(AgentState.TaskStatus.EXECUTING);

        // When
        sessionManager.updateSession(state);

        // Then
        Optional<AgentState> retrieved = sessionManager.getSession(state.getSessionId(), userId);
        assertTrue(retrieved.isPresent());
        assertEquals("测试任务", retrieved.get().getCurrentTask());
        assertEquals(AgentState.TaskStatus.EXECUTING, retrieved.get().getStatus());
    }
}