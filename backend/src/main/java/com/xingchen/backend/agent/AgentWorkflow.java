package com.xingchen.backend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 工作流管理
 * 处理 Agent 相关的工作流程
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentWorkflow {

    private final AgentSessionManager sessionManager;
    private final StatePersistenceService statePersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 任务步骤类
     */
    @Data
    public static class TaskStep {
        private int index;
        private String description;
        private int total;

        public TaskStep(int index, String description) {
            this.index = index;
            this.description = description;
        }
    }

    /**
     * 初始化工作流
     */
    public AgentState initializeWorkflow(Long userId) {
        AgentState state = sessionManager.createSession(userId);
        state.markExecuting("初始化工作流");
        sessionManager.updateSession(state);
        return state;
    }

    /**
     * 执行工作流
     */
    public AgentState executeWorkflow(String sessionId, Long userId, String task) {
        AgentState state = sessionManager.getOrCreateSession(sessionId, userId);
        state.markExecuting(task);
        sessionManager.updateSession(state);
        
        // 这里可以添加具体的工作流执行逻辑
        
        state.markCompleted();
        sessionManager.updateSession(state);
        return state;
    }

    /**
     * 终止工作流
     */
    public boolean terminateWorkflow(String sessionId, Long userId) {
        AgentState state = sessionManager.getSession(sessionId, userId)
                .orElse(null);
        
        if (state != null) {
            state.markFailed("工作流被手动终止");
            sessionManager.updateSession(state);
            return sessionManager.endSession(sessionId, userId);
        }
        
        return false;
    }

    /**
     * 解析步骤
     */
    protected List<TaskStep> parseSteps(String response) {
        List<TaskStep> steps = new ArrayList<>();
        
        if (response == null || response.trim().isEmpty()) {
            return steps;
        }

        // 尝试 JSON 格式解析
        if (response.contains("{") && response.contains("}")) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                if (rootNode.has("steps")) {
                    JsonNode stepsNode = rootNode.get("steps");
                    for (int i = 0; i < stepsNode.size(); i++) {
                        JsonNode stepNode = stepsNode.get(i);
                        if (stepNode.has("description")) {
                            steps.add(new TaskStep(i + 1, stepNode.get("description").asText()));
                        }
                    }
                }
                if (!steps.isEmpty()) {
                    setTotal(steps);
                    return steps;
                }
            } catch (Exception e) {
                // JSON 解析失败，尝试其他格式
            }
        }

        // 尝试简单的步骤解析
        String[] lines = response.split("\n");
        int index = 1;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            // 跳过空行和无意义的行
            if (line.length() < 3) {
                continue;
            }
            // 添加步骤
            steps.add(new TaskStep(index++, line));
        }
        if (!steps.isEmpty()) {
            setTotal(steps);
            return steps;
        }

        return steps;
    }

    /**
     * 设置总步骤数
     */
    private void setTotal(List<TaskStep> steps) {
        int total = steps.size();
        for (TaskStep step : steps) {
            step.setTotal(total);
        }
    }
}
