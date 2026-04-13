package com.xingchen.backend.agent;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent 会话状态
 */
@Data
@NoArgsConstructor
public class AgentState {

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
    private LocalDateTime createdAt;   // 创建时间
    private LocalDateTime updatedAt;   // 更新时间
    private Map<String, Object> context; // 上下文信息

    public AgentState(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = TaskStatus.IDLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新状态
     */
    public void updateStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
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
}
