package com.xingchen.backend.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 状态持久化服务
 * 支持任务状态的保存和加载，实现断点续传
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StatePersistenceService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String TASK_STATE_KEY_PREFIX = "agent:task:state:";
    private static final long TASK_STATE_EXPIRE_HOURS = 24; // 任务状态过期时间
    
    /**
     * 保存任务状态
     * @param sessionId 会话ID
     * @param state 任务状态
     */
    public void saveTaskState(String sessionId, AgentState state) {
        try {
            String key = TASK_STATE_KEY_PREFIX + sessionId;// 任务状态的 Redis key
            String stateJson = objectMapper.writeValueAsString(state);// 任务状态转为 JSON
            redisTemplate.opsForValue().set(key, stateJson, Duration.ofHours(TASK_STATE_EXPIRE_HOURS));// 保存任务状态
            log.info("任务状态已保存: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("保存任务状态失败", e);
        }
    }
    
    /**
     * 加载任务状态
     * @param sessionId 会话ID
     * @return 任务状态
     */
    public Optional<AgentState> loadTaskState(String sessionId) {
        try {
            String key = TASK_STATE_KEY_PREFIX + sessionId;
            String stateJson = redisTemplate.opsForValue().get(key);
            if (stateJson != null) {
                AgentState state = objectMapper.readValue(stateJson, AgentState.class);
                log.info("任务状态已加载: sessionId={}", sessionId);
                return Optional.of(state);
            }
        } catch (Exception e) {
            log.error("加载任务状态失败", e);
        }
        return Optional.empty();
    }
    
    /**
     * 删除任务状态
     * @param sessionId 会话ID
     */
    public void deleteTaskState(String sessionId) {
        try {
            String key = TASK_STATE_KEY_PREFIX + sessionId;
            redisTemplate.delete(key);
            log.info("任务状态已删除: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("删除任务状态失败", e);
        }
    }
    
    /**
     * 检查任务状态是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean taskStateExists(String sessionId) {
        String key = TASK_STATE_KEY_PREFIX + sessionId;
        return redisTemplate.hasKey(key);
    }
    
    /**
     * 更新任务状态
     * @param sessionId 会话ID
     * @param state 任务状态
     */
    public void updateTaskState(String sessionId, AgentState state) {
        saveTaskState(sessionId, state);
    }
}
