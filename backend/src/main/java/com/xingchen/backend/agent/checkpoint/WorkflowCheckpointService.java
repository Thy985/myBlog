package com.xingchen.backend.agent.checkpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowCheckpointService {

    private final StringRedisTemplate redisTemplate;// Redis 模板
    private final ObjectMapper objectMapper;// JSON 序列化和反序列化（由 JacksonConfig 全局配置）

    private static final String CHECKPOINT_PREFIX = "workflow:checkpoint:";// 检查点前缀
    private static final String SESSION_CHECKPOINT_PREFIX = "workflow:session:";// 会话检查点前缀
    private static final String USER_CHECKPOINTS_PREFIX = "workflow:user:";// 用户检查点前缀

    /**
     * 使用 Caffeine 本地缓存，支持自动过期和容量限制
     * - 基于写入时间过期（TTL）：10分钟
     * - 最大容量：10000个检查点
     * - 避免内存泄漏风险
     */
    private final Cache<String, WorkflowCheckpoint> localCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .recordStats()  // 记录统计信息，便于监控
            .build();

    public WorkflowCheckpoint saveCheckpoint(
            String sessionId,
            String workflowType,
            Long userId,
            String currentPhase,
            List<String> completedPhases,
            int stepNumber,
            Map<String, Object> stateData,
            Map<String, Object> metadata) {

        WorkflowCheckpoint checkpoint = WorkflowCheckpoint.builder()
                .checkpointId(sessionId + "_" + System.currentTimeMillis())
                .sessionId(sessionId)
                .workflowType(workflowType)
                .userId(userId)
                .currentPhase(currentPhase)
                .completedPhases(completedPhases != null ? completedPhases : new ArrayList<>())
                .stepNumber(stepNumber)
                .stateData(stateData)
                .metadata(metadata)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .isCompleted(false)
                .isFailed(false)
                .build();

        // 先持久化到 Redis，成功后再更新本地缓存（保证分布式一致性）
        boolean persisted = persistToRedis(checkpoint);
        if (persisted) {
            localCache.put(sessionId, checkpoint);
            log.info("检查点已保存: sessionId={}, phase={}, step={}",
                    sessionId, currentPhase, stepNumber);
        } else {
            log.error("检查点持久化失败: sessionId={}", sessionId);
            throw new RuntimeException("检查点持久化失败");
        }

        return checkpoint;
    }

    public WorkflowCheckpoint getCheckpoint(String sessionId) {
        // 先从本地缓存获取（Caffeine 会自动处理过期）
        WorkflowCheckpoint local = localCache.getIfPresent(sessionId);
        if (local != null && local.canResume()) {
            return local;
        }

        // 缓存未命中，从 Redis 获取
        WorkflowCheckpoint fromRedis = getFromRedis(sessionId);
        if (fromRedis != null) {
            // 更新本地缓存
            localCache.put(sessionId, fromRedis);
            return fromRedis;
        }

        return null;
    }

    public List<WorkflowCheckpoint> getUserCheckpoints(Long userId) {
        List<WorkflowCheckpoint> checkpoints = new ArrayList<>();

        try {
            String userKey = USER_CHECKPOINTS_PREFIX + userId;
            List<String> sessionIds = redisTemplate.opsForList().range(userKey, 0, -1);

            if (sessionIds != null) {
                for (String sessionId : sessionIds) {
                    WorkflowCheckpoint cp = getCheckpoint(sessionId);
                    if (cp != null && cp.canResume()) {
                        checkpoints.add(cp);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取用户检查点列表失败: userId={}", userId, e);
        }

        return checkpoints;
    }

    public boolean deleteCheckpoint(String sessionId) {
        // 删除本地缓存
        localCache.invalidate(sessionId);
        // 删除 Redis 数据
        return deleteFromRedis(sessionId);
    }

    public boolean markCompleted(String sessionId) {
        WorkflowCheckpoint checkpoint = getCheckpoint(sessionId);
        if (checkpoint != null) {
            checkpoint.markCompleted();
            boolean persisted = persistToRedis(checkpoint);
            if (persisted) {
                localCache.put(sessionId, checkpoint);
                log.info("检查点已标记为完成: sessionId={}", sessionId);
                return true;
            } else {
                log.error("标记检查点完成时持久化失败: sessionId={}", sessionId);
                return false;
            }
        }
        return false;
    }

    public boolean markFailed(String sessionId, String reason) {
        WorkflowCheckpoint checkpoint = getCheckpoint(sessionId);
        if (checkpoint != null) {
            checkpoint.markFailed(reason);
            boolean persisted = persistToRedis(checkpoint);
            if (persisted) {
                localCache.put(sessionId, checkpoint);
                log.info("检查点已标记为失败: sessionId={}, reason={}", sessionId, reason);
                return true;
            } else {
                log.error("标记检查点失败时持久化失败: sessionId={}", sessionId);
                return false;
            }
        }
        return false;
    }

    public WorkflowCheckpoint updatePhase(String sessionId, String newPhase, Map<String, Object> additionalState) {
        WorkflowCheckpoint checkpoint = getCheckpoint(sessionId);
        if (checkpoint == null) {
            log.warn("更新检查点失败，会话不存在: sessionId={}", sessionId);
            return null;
        }

        List<String> completed = new ArrayList<>(checkpoint.getCompletedPhases());
        if (!completed.contains(checkpoint.getCurrentPhase())) {
            completed.add(checkpoint.getCurrentPhase());
        }

        checkpoint.setCurrentPhase(newPhase);
        checkpoint.setCompletedPhases(completed);
        checkpoint.setStepNumber(checkpoint.getStepNumber() + 1);

        if (additionalState != null && checkpoint.getStateData() != null) {
            checkpoint.getStateData().putAll(additionalState);
        }

        // 先持久化到 Redis
        boolean persisted = persistToRedis(checkpoint);
        if (persisted) {
            // 成功后更新本地缓存
            localCache.put(sessionId, checkpoint);
            log.info("检查点已更新: sessionId={}, newPhase={}", sessionId, newPhase);
        } else {
            log.error("检查点更新持久化失败: sessionId={}", sessionId);
        }

        return checkpoint;
    }

    /**
     * 清理过期检查点（Caffeine 会自动处理，此方法主要用于手动触发或监控）
     * 注意：由于使用了 Caffeine 的自动过期机制，这个方法实际上不再需要定期调用
     */
    public void cleanupExpiredCheckpoints() {
        // Caffeine 会自动清理过期条目，这里只记录统计信息
        log.info("Caffeine 缓存统计 - 大小: {}, 命中率: {}%",
                localCache.estimatedSize(),
                localCache.stats().hitRate() * 100);
    }

    /**
     * 持久化检查点到 Redis（使用 JSON 序列化）
     * @return 是否持久化成功
     */
    private boolean persistToRedis(WorkflowCheckpoint checkpoint) {
        try {
            String key = CHECKPOINT_PREFIX + checkpoint.getSessionId();
            String sessionKey = SESSION_CHECKPOINT_PREFIX + checkpoint.getSessionId();
            String userKey = USER_CHECKPOINTS_PREFIX + checkpoint.getUserId();

            // 使用 JSON 序列化，兼容性更好
            String jsonValue = objectMapper.writeValueAsString(checkpoint);
            
            // 保存到 Redis，设置 7 天过期时间
            redisTemplate.opsForValue().set(key, jsonValue, Duration.ofDays(7));
            redisTemplate.opsForValue().set(sessionKey, checkpoint.getCheckpointId(), Duration.ofDays(7));

            // 维护用户检查点列表
            Boolean hasKey = redisTemplate.hasKey(userKey);
            if (hasKey == null || !hasKey) {
                redisTemplate.opsForList().rightPush(userKey, checkpoint.getSessionId());
                redisTemplate.expire(userKey, Duration.ofDays(30));
            }
            
            return true;
        } catch (JsonProcessingException e) {
            log.error("检查点序列化失败: sessionId={}", checkpoint.getSessionId(), e);
            return false;
        } catch (Exception e) {
            log.error("检查点持久化到Redis失败: sessionId={}", checkpoint.getSessionId(), e);
            return false;
        }
    }

    /**
     * 从 Redis 获取检查点（使用 JSON 反序列化）
     */
    private WorkflowCheckpoint getFromRedis(String sessionId) {
        try {
            String key = CHECKPOINT_PREFIX + sessionId;
            String jsonValue = redisTemplate.opsForValue().get(key);
            
            if (jsonValue != null) {
                // 使用 JSON 反序列化，兼容性更好
                return objectMapper.readValue(jsonValue, WorkflowCheckpoint.class);
            }
        } catch (JsonProcessingException e) {
            log.error("检查点反序列化失败: sessionId={}", sessionId, e);
        } catch (Exception e) {
            log.error("从Redis获取检查点失败: sessionId={}", sessionId, e);
        }
        return null;
    }

    /**
     * 从 Redis 删除检查点
     * @return 是否删除成功
     */
    private boolean deleteFromRedis(String sessionId) {
        try {
            String key = CHECKPOINT_PREFIX + sessionId;
            String sessionKey = SESSION_CHECKPOINT_PREFIX + sessionId;
            redisTemplate.delete(key);
            redisTemplate.delete(sessionKey);
            return true;
        } catch (Exception e) {
            log.error("从Redis删除检查点失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    /**
     * 检查是否可以恢复
     */
    public boolean canResume(String sessionId) {
        WorkflowCheckpoint checkpoint = getCheckpoint(sessionId);
        return checkpoint != null && checkpoint.canResume();
    }
    /**
     * 获取检查点状态数据
     */
    public Map<String, Object> getStateData(String sessionId) {
        WorkflowCheckpoint checkpoint = getCheckpoint(sessionId);
        return checkpoint != null ? checkpoint.getStateData() : null;
    }
}