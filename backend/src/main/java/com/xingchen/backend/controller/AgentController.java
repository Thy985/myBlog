package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.ab.ABTestService;
import com.xingchen.backend.agent.AgentExecutor;
import com.xingchen.backend.agent.AgentSessionManager;
import com.xingchen.backend.agent.AgentState;
import com.xingchen.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent 控制器
 * 
 * 支持多轮对话和复杂任务执行
 */
@RestController
@RequestMapping("/api/agent")
@Slf4j
@RequiredArgsConstructor
public class AgentController {

    private final AgentSessionManager sessionManager;
    private final AgentExecutor agentExecutor;
    private final ABTestService abTestService;

    /**
     * 开始新会话
     */
    @PostMapping("/session/start")
    @SaCheckLogin
    public Result<StartSessionResponse> startSession() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 检查活跃会话数
        int activeCount = sessionManager.getActiveSessionCount(userId);
        if (activeCount >= 5) {
            return Result.fail(429, "活跃会话过多，请先结束其他会话");
        }
        
        AgentState state = sessionManager.createSession(userId);
        
        return Result.success(new StartSessionResponse(
                state.getSessionId(),
                "会话创建成功，请开始对话"
        ));
    }

    /**
     * Agent 对话
     */
    @PostMapping("/chat")
    @SaCheckLogin
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 获取或创建会话
        AgentState state = sessionManager.getOrCreateSession(
                request.getSessionId(), 
                userId
        );
        
        // 检查是否需要规划
        if (state.getStatus() == AgentState.TaskStatus.IDLE) {
            // 首次对话，判断是否需要 Agent 模式
            if (isComplexTask(request.getMessage())) {
                // 启动 Agent 工作流
                AgentExecutor.AgentResult result = agentExecutor.execute(
                        request.getMessage(), 
                        state
                );
                
                sessionManager.updateSession(state);
                
                if (result.success()) {
                    return Result.success(new ChatResponse(
                            state.getSessionId(),
                            result.result(),
                            state.getStatus().name(),
                            state.getCurrentStep(),
                            state.getTotalSteps()
                    ));
                } else {
                    return Result.fail(500, result.error());
                }
            }
        }
        
        // 普通对话模式
        state.addTurn(request.getMessage(), "收到，正在处理...");
        sessionManager.updateSession(state);
        
        return Result.success(new ChatResponse(
                state.getSessionId(),
                "这是一个普通回复",
                state.getStatus().name(),
                state.getCurrentStep(),
                state.getTotalSteps()
        ));
    }

    /**
     * 获取会话状态
     */
    @GetMapping("/session/{sessionId}")
    @SaCheckLogin
    public Result<SessionStatusResponse> getSessionStatus(@PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        return sessionManager.getSession(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .map(state -> Result.success(new SessionStatusResponse(
                        state.getSessionId(),
                        state.getStatus().name(),
                        state.getCurrentTask(),
                        state.getCurrentStep(),
                        state.getTotalSteps(),
                        state.getHistoryAsText(10)
                )))
                .orElse(Result.fail(404, "会话不存在"));
    }

    /**
     * 结束会话
     */
    @PostMapping("/session/{sessionId}/end")
    @SaCheckLogin
    public Result<Void> endSession(@PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        sessionManager.getSession(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .ifPresent(s -> sessionManager.endSession(sessionId));
        
        return Result.success();
    }

    /**
     * 列出活跃会话
     */
    @GetMapping("/sessions")
    @SaCheckLogin
    public Result<List<SessionInfo>> listSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 这里简化实现，实际应该查询用户的所有会话
        int count = sessionManager.getActiveSessionCount(userId);
        
        return Result.success(List.of(new SessionInfo(
                "summary",
                count + " 个活跃会话"
        )));
    }

    // ========== A/B 测试接口 ==========

    /**
     * 获取 A/B 分组
     */
    @GetMapping("/ab/assign")
    @SaCheckLogin
    public Result<ABAssignmentResponse> getABAssignment(
            @RequestParam String experimentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        ABTestService.Variant variant = abTestService.assignVariant(experimentId, userId);
        
        if (variant == null) {
            return Result.fail(404, "实验不存在");
        }
        
        return Result.success(new ABAssignmentResponse(
                experimentId,
                variant.getId(),
                variant.getName(),
                variant.getConfig()
        ));
    }

    /**
     * 记录 A/B 指标
     */
    @PostMapping("/ab/metric")
    @SaCheckLogin
    public Result<Void> recordABMetric(@RequestBody ABMetricRequest request) {
        abTestService.recordMetric(
                request.getExperimentId(),
                request.getVariantId(),
                request.getMetricName(),
                request.getValue()
        );
        return Result.success();
    }

    /**
     * 获取实验统计
     */
    @GetMapping("/ab/stats/{experimentId}")
    @SaCheckLogin
    public Result<ABTestService.ExperimentStats> getABStats(
            @PathVariable String experimentId) {
        ABTestService.ExperimentStats stats = abTestService.getStats(experimentId);
        
        if (stats == null) {
            return Result.fail(404, "实验不存在");
        }
        
        return Result.success(stats);
    }

    /**
     * 列出所有实验
     */
    @GetMapping("/ab/experiments")
    @SaCheckLogin
    public Result<List<ABTestService.Experiment>> listExperiments() {
        return Result.success(abTestService.listExperiments());
    }

    // ========== 私有方法 ==========

    private boolean isComplexTask(String message) {
        // 判断是否为复杂任务
        String lower = message.toLowerCase();
        return lower.contains("步骤") || 
               lower.contains("流程") || 
               lower.contains("规划") ||
               lower.contains("计划") ||
               lower.contains("分析") ||
               message.length() > 100;
    }

    // ========== 请求/响应类 ==========

    @lombok.Data
    public static class StartSessionResponse {
        private String sessionId;
        private String message;
        
        public StartSessionResponse(String sessionId, String message) {
            this.sessionId = sessionId;
            this.message = message;
        }
    }

    @lombok.Data
    public static class ChatRequest {
        private String sessionId;
        private String message;
    }

    @lombok.Data
    public static class ChatResponse {
        private String sessionId;
        private String response;
        private String status;
        private int currentStep;
        private int totalSteps;
        
        public ChatResponse(String sessionId, String response, String status,
                           int currentStep, int totalSteps) {
            this.sessionId = sessionId;
            this.response = response;
            this.status = status;
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
        }
    }

    @lombok.Data
    public static class SessionStatusResponse {
        private String sessionId;
        private String status;
        private String currentTask;
        private int currentStep;
        private int totalSteps;
        private String recentHistory;
        
        public SessionStatusResponse(String sessionId, String status, String currentTask,
                                     int currentStep, int totalSteps, String recentHistory) {
            this.sessionId = sessionId;
            this.status = status;
            this.currentTask = currentTask;
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
            this.recentHistory = recentHistory;
        }
    }

    @lombok.Data
    public static class SessionInfo {
        private String sessionId;
        private String info;
        
        public SessionInfo(String sessionId, String info) {
            this.sessionId = sessionId;
            this.info = info;
        }
    }

    @lombok.Data
    public static class ABAssignmentResponse {
        private String experimentId;
        private String variantId;
        private String variantName;
        private Map<String, Object> config;
        
        public ABAssignmentResponse(String experimentId, String variantId, 
                                    String variantName, Map<String, Object> config) {
            this.experimentId = experimentId;
            this.variantId = variantId;
            this.variantName = variantName;
            this.config = config;
        }
    }

    @lombok.Data
    public static class ABMetricRequest {
        private String experimentId;
        private String variantId;
        private String metricName;
        private double value;
    }
}