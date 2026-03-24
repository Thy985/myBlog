package com.xingchen.backend.agent;

import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 执行器
 * 
 * 执行多步骤任务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentExecutor {

    private final AgentWorkflow workflow;
    private final AIService aiService;
    
    // 最大重试次数
    private static final int MAX_RETRIES = 2;
    // 最大步骤数
    private static final int MAX_STEPS = 10;
    
    /**
     * 执行任务
     */
    public AgentResult execute(String task, AgentState state) {
        log.info("开始执行任务: sessionId={}, task={}", state.getSessionId(), task);
        
        state.setCurrentTask(task);
        state.updateStatus(AgentState.TaskStatus.PLANNING);
        
        try {
            // 1. 规划步骤
            List<AgentWorkflow.TaskStep> steps = workflow.planSteps(task, state);
            
            if (steps.isEmpty()) {
                return AgentResult.failure("任务规划失败，无法分解步骤");
            }
            
            state.setTotalSteps(steps.size());
            log.info("任务规划完成: {} 个步骤", steps.size());
            
            // 2. 执行步骤
            state.updateStatus(AgentState.TaskStatus.EXECUTING);
            
            for (AgentWorkflow.TaskStep step : steps) {
                if (state.getCurrentStep() >= MAX_STEPS) {
                    log.warn("达到最大步骤数限制");
                    break;
                }
                
                AgentWorkflow.StepResult result = executeStepWithRetry(step, state);
                
                if (result.success()) {
                    state.setIntermediateResult("step_" + step.getIndex(), result.result());
                    state.addTurn("执行步骤 " + step.getIndex(), result.result());
                } else {
                    state.updateStatus(AgentState.TaskStatus.FAILED);
                    return AgentResult.failure("步骤 " + step.getIndex() + " 执行失败: " + result.error());
                }
                
                state.nextStep();
            }
            
            // 3. 反思结果
            state.updateStatus(AgentState.TaskStatus.COMPLETED);
            boolean success = workflow.reflect(state);
            
            if (success) {
                String finalResult = generateFinalResult(state);
                return AgentResult.success(finalResult, state);
            } else {
                return AgentResult.failure("任务执行结果不符合预期");
            }
            
        } catch (Exception e) {
            log.error("任务执行异常", e);
            state.updateStatus(AgentState.TaskStatus.FAILED);
            return AgentResult.failure("执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 带重试的步骤执行
     */
    private AgentWorkflow.StepResult executeStepWithRetry(AgentWorkflow.TaskStep step, AgentState state) {
        int retries = 0;
        
        while (retries <= MAX_RETRIES) {
            AgentWorkflow.StepResult result = workflow.executeStep(step, state);
            
            if (result.success()) {
                return result;
            }
            
            retries++;
            if (retries <= MAX_RETRIES) {
                log.warn("步骤 {} 执行失败，第 {} 次重试", step.getIndex(), retries);
            }
        }
        
        return new AgentWorkflow.StepResult(false, null, "达到最大重试次数");
    }
    
    /**
     * 生成最终结果
     */
    private String generateFinalResult(AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 任务执行结果\n\n");
        sb.append("任务：").append(state.getCurrentTask()).append("\n\n");
        
        sb.append("### 执行步骤\n");
        for (int i = 1; i <= state.getCurrentStep(); i++) {
            Object result = state.getIntermediateResult("step_" + i);
            if (result != null) {
                sb.append("**步骤 ").append(i).append("**\n");
                sb.append(result.toString()).append("\n\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 执行结果
     */
    public record AgentResult(boolean success, String result, String error, AgentState finalState) {
        public static AgentResult success(String result, AgentState state) {
            return new AgentResult(true, result, null, state);
        }
        
        public static AgentResult failure(String error) {
            return new AgentResult(false, null, error, null);
        }
    }
}