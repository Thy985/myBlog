package com.xingchen.backend.agent;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 会话状态
 * 
 * 注意：
 * 1. context Map 只存储可序列化的简单类型（String、Number、Boolean）
 * 2. 时间使用 Instant (UTC) 避免时区问题
 * 3. 线程安全由 AgentSessionManager 的 ConcurrentHashMap 保证
 */
@Data
@NoArgsConstructor
public class AgentState implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        IDLE,        // 空闲
        EXECUTING,   // 执行中
        COMPLETED,   // 完成
        FAILED       // 失败
    }

    private String sessionId;          // 会话ID
    private Long userId;               // 用户ID
    private TaskStatus status;         // 任务状态
    private String currentTask;        // 当前任务
    private String lastError;          // 最后错误信息
    private Instant createdAt;         // 创建时间 (UTC)
    private Instant updatedAt;         // 更新时间 (UTC)
    
    /**
     * 上下文信息
     * 重要：只存储可序列化的简单类型
     * - String, Number (Integer, Long, Double), Boolean
     * - 避免存储复杂对象，如需存储请先转为 JSON 字符串
     */
    private Map<String, Object> context;

    public AgentState(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = TaskStatus.IDLE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.context = new HashMap<>();
    }

    /**
     * 更新状态
     * 注意：AgentSessionManager 保证同一会话的串行访问，此处无需额外同步
     */
    public void updateStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /**
     * 标记任务完成
     */
    public void markCompleted() {
        updateStatus(TaskStatus.COMPLETED);
    }

    /**
     * 标记任务失败
     */
    public void markFailed(String error) {
        this.lastError = error;
        updateStatus(TaskStatus.FAILED);
    }

    /**
     * 标记任务执行中
     */
    public void markExecuting(String task) {
        this.currentTask = task;
        updateStatus(TaskStatus.EXECUTING);
    }
    
    /**
     * 安全地添加上下文数据
     * 只允许存储基本类型和 String
     */
    public void putContext(String key, Object value) {
        if (value == null) {
            context.remove(key);
            return;
        }
        
        // 验证类型安全性
        if (!(value instanceof String || 
              value instanceof Number || 
              value instanceof Boolean)) {
            throw new IllegalArgumentException(
                "Context 只支持 String、Number、Boolean 类型，实际类型: " + 
                value.getClass().getName() + 
                "。如需存储复杂对象，请先序列化为 JSON 字符串"
            );
        }
        
        context.put(key, value);
        this.updatedAt = Instant.now();
    }
    
    /**
     * 获取上下文数据（带类型转换）
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key, Class<T> type) {
        Object value = context.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        throw new ClassCastException(
            "无法将 " + value.getClass().getName() + " 转换为 " + type.getName()
        );
    }
}
