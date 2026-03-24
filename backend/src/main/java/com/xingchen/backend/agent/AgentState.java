package com.xingchen.backend.agent;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 状态
 * 
 * 管理多轮对话中的状态信息
 */
@Data
public class AgentState {
    
    private String sessionId;
    private Long userId;
    private String currentTask;
    private TaskStatus status;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;
    
    // 对话历史
    private List<DialogueTurn> dialogueHistory;
    
    // 任务上下文
    private Map<String, Object> context;
    
    // 中间结果
    private Map<String, Object> intermediateResults;
    
    // 当前步骤
    private int currentStep;
    private int totalSteps;
    
    public AgentState(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = TaskStatus.IDLE;
        this.startTime = LocalDateTime.now();
        this.lastUpdateTime = LocalDateTime.now();
        this.dialogueHistory = new ArrayList<>();
        this.context = new HashMap<>();
        this.intermediateResults = new HashMap<>();
        this.currentStep = 0;
        this.totalSteps = 0;
    }
    
    /**
     * 添加对话轮次
     */
    public void addTurn(String userInput, String agentResponse) {
        dialogueHistory.add(new DialogueTurn(userInput, agentResponse, LocalDateTime.now()));
        lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 更新状态
     */
    public void updateStatus(TaskStatus status) {
        this.status = status;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 设置上下文
     */
    public void setContext(String key, Object value) {
        context.put(key, value);
    }
    
    /**
     * 获取上下文
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        return (T) context.get(key);
    }
    
    /**
     * 设置中间结果
     */
    public void setIntermediateResult(String key, Object value) {
        intermediateResults.put(key, value);
    }
    
    /**
     * 获取中间结果
     */
    @SuppressWarnings("unchecked")
    public <T> T getIntermediateResult(String key) {
        return (T) intermediateResults.get(key);
    }
    
    /**
     * 前进一步
     */
    public void nextStep() {
        currentStep++;
    }
    
    /**
     * 获取对话历史文本
     */
    public String getHistoryAsText(int lastN) {
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, dialogueHistory.size() - lastN);
        
        for (int i = start; i < dialogueHistory.size(); i++) {
            DialogueTurn turn = dialogueHistory.get(i);
            sb.append("用户: ").append(turn.getUserInput()).append("\n");
            sb.append("助手: ").append(turn.getAgentResponse()).append("\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 任务状态
     */
    public enum TaskStatus {
        IDLE,           // 空闲
        PLANNING,       // 规划中
        EXECUTING,      // 执行中
        WAITING_INPUT,  // 等待用户输入
        COMPLETED,      // 已完成
        FAILED          // 失败
    }
    
    /**
     * 对话轮次
     */
    @Data
    public static class DialogueTurn {
        private String userInput;
        private String agentResponse;
        private LocalDateTime timestamp;
        
        public DialogueTurn(String userInput, String agentResponse, LocalDateTime timestamp) {
            this.userInput = userInput;
            this.agentResponse = agentResponse;
            this.timestamp = timestamp;
        }
    }
}