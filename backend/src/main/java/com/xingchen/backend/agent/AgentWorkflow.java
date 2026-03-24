package com.xingchen.backend.agent;

import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent 工作流引擎
 * 
 * 支持多步骤任务规划和执行
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentWorkflow {

    private final AIService aiService;
    
    /**
     * 规划任务步骤
     */
    public List<TaskStep> planSteps(String task, AgentState state) {
        String prompt = buildPlanningPrompt(task, state);
        String response = aiService.chat(prompt);
        
        return parseSteps(response);
    }
    
    /**
     * 执行单个步骤
     */
    public StepResult executeStep(TaskStep step, AgentState state) {
        log.info("执行步骤 {}: {}", step.getIndex(), step.getDescription());
        
        try {
            String prompt = buildExecutionPrompt(step, state);
            String result = aiService.chat(prompt);
            
            return new StepResult(true, result, null);
        } catch (Exception e) {
            log.error("步骤执行失败: {}", step.getIndex(), e);
            return new StepResult(false, null, e.getMessage());
        }
    }
    
    /**
     * 反思执行结果
     */
    public boolean reflect(AgentState state) {
        String prompt = buildReflectionPrompt(state);
        String response = aiService.chat(prompt);
        
        // 解析反思结果
        return response.contains("成功") || response.contains("完成");
    }
    
    /**
     * 构建规划提示词
     */
    private String buildPlanningPrompt(String task, AgentState state) {
        return String.format("""
            你是一个任务规划专家。请将以下复杂任务分解为可执行的步骤。
            
            任务：%s
            
            要求：
            1. 将任务分解为 3-7 个清晰的步骤
            2. 每个步骤要具体、可执行
            3. 步骤之间要有逻辑顺序
            4. 输出格式：每行一个步骤，格式为 "步骤N: 描述"
            
            请输出步骤规划：
            """, task);
    }
    
    /**
     * 构建执行提示词
     */
    private String buildExecutionPrompt(TaskStep step, AgentState state) {
        String context = state.getHistoryAsText(3);
        
        return String.format("""
            你是一个任务执行专家。请执行以下步骤。
            
            当前步骤：步骤 %d / %d
            步骤描述：%s
            
            任务上下文：
            %s
            
            之前步骤的结果：
            %s
            
            请执行当前步骤并输出结果。
            """,
            step.getIndex(),
            step.getTotal(),
            step.getDescription(),
            state.getCurrentTask(),
            context,
            formatIntermediateResults(state)
        );
    }
    
    /**
     * 构建反思提示词
     */
    private String buildReflectionPrompt(AgentState state) {
        return String.format("""
            你是一个任务反思专家。请评估任务执行结果。
            
            任务：%s
            
            执行历史：
            %s
            
            请评估：
            1. 任务是否成功完成？
            2. 如果未完成，还需要哪些步骤？
            3. 输出 "成功" 或 "需要继续: 原因"
            """,
            state.getCurrentTask(),
            state.getHistoryAsText(5)
        );
    }
    
    /**
     * 解析步骤
     */
    private List<TaskStep> parseSteps(String response) {
        List<TaskStep> steps = new ArrayList<>();
        String[] lines = response.split("\n");
        
        int index = 1;
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^步骤\\d+[:：].+")) {
                String description = line.replaceAll("^步骤\\d+[:：]\\s*", "");
                steps.add(new TaskStep(index, description, steps.size() + 1));
                index++;
            }
        }
        
        return steps;
    }
    
    private String formatIntermediateResults(AgentState state) {
        if (state.getIntermediateResults().isEmpty()) {
            return "无";
        }
        
        StringBuilder sb = new StringBuilder();
        state.getIntermediateResults().forEach((key, value) -> {
            sb.append(key).append(": ").append(value).append("\n");
        });
        return sb.toString();
    }
    
    /**
     * 任务步骤
     */
    public static class TaskStep {
        private int index;
        private String description;
        private int total;
        
        public TaskStep(int index, String description, int total) {
            this.index = index;
            this.description = description;
            this.total = total;
        }
        
        public int getIndex() { return index; }
        public String getDescription() { return description; }
        public int getTotal() { return total; }
    }
    
    /**
     * 步骤执行结果
     */
    public record StepResult(boolean success, String result, String error) {}
}