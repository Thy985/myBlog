package com.xingchen.backend.agent.growth;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.base.BaseAgent.AgentContext;
import com.xingchen.backend.agent.base.BaseAgent.AgentResult;
import com.xingchen.backend.agent.checkpoint.WorkflowCheckpoint;
import com.xingchen.backend.agent.checkpoint.WorkflowCheckpointService;
import com.xingchen.backend.agent.content.*;
import com.xingchen.backend.agent.human.ApprovalRequest;
import com.xingchen.backend.agent.human.HumanReviewService;
import com.xingchen.backend.agent.memory.ExperienceMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthWorkflowOrchestrator {

    private final BehaviorAnalyzerAgent behaviorAnalyzerAgent;
    private final OpportunityDiscoveryAgent opportunityDiscoveryAgent;
    private final ContentGenerationAgent contentGenerationAgent;
    private final SEOOptimizationAgent seoOptimizationAgent;
    private final ReflectionAgent reflectionAgent;
    private final ContentAuditAgent contentAuditAgent;
    private final CriticAgent criticAgent;
    private final HumanReviewService humanReviewService;
    private final WorkflowCheckpointService checkpointService;
    private final ExperienceMemoryService memoryService;

    private final Map<String, GrowthSession> sessions = new ConcurrentHashMap<>();

    public GrowthSession startGrowthSession(Long userId, String taskType) {
        GrowthSession session = new GrowthSession(
                UUID.randomUUID().toString(),
                userId,
                taskType,
                new Date()
        );
        sessions.put(session.getSessionId(), session);
        log.info("启动增长会话: sessionId={}, userId={}, taskType={}",
                session.getSessionId(), userId, taskType);
        return session;
    }

    public GrowthSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    @SuppressWarnings("unchecked")
    public GrowthResult executeGrowth闭环(Long userId, String taskType) {
        GrowthSession session = startGrowthSession(userId, taskType);

        try {
            session.setPhase(GrowthPhase.BEHAVIOR_ANALYSIS);
            log.info("开始增长闭环执行: sessionId={}", session.getSessionId());

            saveCheckpoint(session, "BEHAVIOR_ANALYSIS_STARTED");

            AgentContext behaviorContext = AgentContext.builder()
                    .userId(userId)
                    .params(Map.of("periodDays", 30, "topN", 10))
                    .build();
            AgentResult behaviorResult = behaviorAnalyzerAgent.execute(behaviorContext);
            session.setBehaviorData((Map<String, Object>) behaviorResult.getData());
            saveCheckpoint(session, "BEHAVIOR_ANALYSIS_COMPLETED");

            session.setPhase(GrowthPhase.OPPORTUNITY_DISCOVERY);
            saveCheckpoint(session, "OPPORTUNITY_DISCOVERY_STARTED");

            AgentContext oppContext = AgentContext.builder()
                    .userId(userId)
                    .params(Map.of("topN", 5))
                    .build();
            AgentResult oppResult = opportunityDiscoveryAgent.execute(oppContext);
            session.setOpportunityData((Map<String, Object>) oppResult.getData());
            saveCheckpoint(session, "OPPORTUNITY_DISCOVERY_COMPLETED");

            session.setPhase(GrowthPhase.CONTENT_GENERATION);
            saveCheckpoint(session, "CONTENT_GENERATION_STARTED");

            List<Map<String, Object>> generatedContents = new ArrayList<>();
            Map<String, Object> oppData = session.getOpportunityData();
            if (oppData != null && oppData.containsKey("opportunities")) {
                List<Map<String, Object>> opportunities = (List<Map<String, Object>>) oppData.get("opportunities");
                for (Map<String, Object> opp : opportunities) {
                    String title = opp.get("title") != null ? opp.get("title").toString() : "未命名主题";
                    try {
                        AgentContext genContext = AgentContext.builder()
                                .userId(userId)
                                .params(Map.of("topic", title))
                                .build();
                        AgentResult genResult = contentGenerationAgent.execute(genContext);
                        if (genResult.isSuccess() && genResult.getData() != null) {
                            generatedContents.add((Map<String, Object>) genResult.getData());
                        }
                    } catch (Exception e) {
                        log.warn("文章生成失败: title={}", title, e);
                    }
                }
            }
            session.setGeneratedContents(generatedContents);
            saveCheckpoint(session, "CONTENT_GENERATION_COMPLETED");

            session.setPhase(GrowthPhase.SEO_OPTIMIZATION);
            saveCheckpoint(session, "SEO_OPTIMIZATION_STARTED");

            List<Map<String, Object>> optimizedContents = new ArrayList<>();
            for (Map<String, Object> content : generatedContents) {
                try {
                    AgentContext optContext = AgentContext.builder()
                            .userId(userId)
                            .params(Map.of("content", content))
                            .build();
                    AgentResult optResult = seoOptimizationAgent.execute(optContext);
                    if (optResult.isSuccess() && optResult.getData() != null) {
                        optimizedContents.add((Map<String, Object>) optResult.getData());
                    } else {
                        optimizedContents.add(content);
                    }
                } catch (Exception e) {
                    log.warn("SEO优化失败", e);
                    optimizedContents.add(content);
                }
            }
            session.setOptimizedContents(optimizedContents);
            saveCheckpoint(session, "SEO_OPTIMIZATION_COMPLETED");

            session.setPhase(GrowthPhase.CRITIC_REVIEW);
            saveCheckpoint(session, "CRITIC_REVIEW_STARTED");

            List<Map<String, Object>> criticResults = new ArrayList<>();
            for (Map<String, Object> content : optimizedContents) {
                try {
                    AgentContext criticContext = AgentContext.builder()
                            .userId(userId)
                            .params(Map.of("content", content, "contentType", "article"))
                            .build();
                    AgentResult criticResult = criticAgent.execute(criticContext);
                    if (criticResult.isSuccess() && criticResult.getData() != null) {
                        Map<String, Object> result = (Map<String, Object>) criticResult.getData();
                        criticResults.add(result);
                        if ((boolean) result.getOrDefault("needsRevision", false)) {
                            log.info("内容需要修订: title={}", content.get("title"));
                        }
                    }
                } catch (Exception e) {
                    log.warn("内容审核失败", e);
                }
            }
            session.setCriticResults(criticResults);
            saveCheckpoint(session, "CRITIC_REVIEW_COMPLETED");

            session.setPhase(GrowthPhase.HUMAN_REVIEW);
            ApprovalRequest approvalRequest = null;
            if (!criticResults.isEmpty()) {
                int needsRevisionCount = 0;
                for (Map<String, Object> result : criticResults) {
                    if ((boolean) result.getOrDefault("needsRevision", false)) {
                        needsRevisionCount++;
                    }
                }

                double riskScore = (double) needsRevisionCount / criticResults.size();
                if (riskScore >= 0.3) {
                    String contentSummary = String.format("生成了%d篇文章，其中%d篇需要修订",
                            criticResults.size(), needsRevisionCount);

                    approvalRequest = humanReviewService.requestApprovalIfNeeded(
                            userId,
                            "GrowthWorkflowOrchestrator",
                            "publish",
                            Map.of("contents", criticResults.size()),
                            riskScore,
                            "内容发布审批",
                            contentSummary,
                            Map.of("criticResults", criticResults)
                    );

                    if (approvalRequest != null) {
                        session.setApprovalRequestId(approvalRequest.getRequestId());
                        session.setStatus(GrowthStatus.WAITING_APPROVAL);

                        return buildGrowthResult(session, false, "内容需要人工审批: " + approvalRequest.getRequestId());
                    }
                }
            }

            session.setPhase(GrowthPhase.REFLECTION);

            memoryService.storeGrowthExperience(
                    userId,
                    String.format("增长闭环完成：生成了%d篇文章，%d个机会",
                            generatedContents.size(),
                            session.getOpportunityData() != null ? ((List<?>)session.getOpportunityData().get("opportunities")) != null ? ((List<?>)session.getOpportunityData().get("opportunities")).size() : 0 : 0),
                    "COMPLETED",
                    true,
                    Map.of(
                            "generatedCount", generatedContents.size(),
                            "opportunityCount", session.getOpportunityData() != null ? ((List<?>)session.getOpportunityData().get("opportunities")) != null ? ((List<?>)session.getOpportunityData().get("opportunities")).size() : 0 : 0,
                            "criticResults", criticResults.size()
                    )
            );

            for (Map<String, Object> criticResult : criticResults) {
                String quality = (boolean) criticResult.getOrDefault("needsRevision", false) ? "REVISION" : "APPROVED";
                memoryService.storeContentFeedback(
                        userId,
                        "article",
                        String.format("文章质量评分: %.0f%%",
                                ((Number) criticResult.getOrDefault("overallScore", 0)).doubleValue() * 100),
                        quality,
                        criticResult
                );
            }

            AgentContext reflectContext = AgentContext.builder()
                    .userId(userId)
                    .params(Map.of(
                            "taskType", taskType,
                            "taskResult", String.format("生成了%d篇文章", generatedContents.size()),
                            "success", true
                    ))
                    .build();
            AgentResult reflectResult = reflectionAgent.execute(reflectContext);
            session.setReflectionData((Map<String, Object>) reflectResult.getData());

            Map<String, Object> reflectionData = session.getReflectionData();
            if (reflectionData != null) {
                String summary = reflectionData.get("summary") != null ?
                        reflectionData.get("summary").toString() : "反思完成";
                memoryService.storeReflectionInsight(
                        userId,
                        summary,
                        taskType,
                        Map.of(
                                "improvements", reflectionData.getOrDefault("improvements", List.of()),
                                "nextSteps", reflectionData.getOrDefault("nextSteps", List.of())
                        )
                );
            }

            session.setPhase(GrowthPhase.COMPLETED);
            session.setStatus(GrowthStatus.SUCCESS);

            return buildGrowthResult(session, true, "增长闭环执行成功");

        } catch (Exception e) {
            log.error("增长闭环执行失败: sessionId={}", session.getSessionId(), e);
            session.setStatus(GrowthStatus.FAILED);
            session.setErrorMessage(e.getMessage());
            session.setPhase(GrowthPhase.FAILED);
            checkpointService.markFailed(session.getSessionId(), e.getMessage());
            return buildGrowthResult(session, false, "执行失败: " + e.getMessage());
        }
    }

    private void saveCheckpoint(GrowthSession session, String phase) {
        try {
            List<String> completedPhases = new ArrayList<>();
            for (GrowthPhase p : GrowthPhase.values()) {
                if (p.ordinal() < session.getPhase().ordinal()) {
                    completedPhases.add(p.name());
                }
            }

            Map<String, Object> stateData = new HashMap<>();
            stateData.put("behaviorData", session.getBehaviorData());
            stateData.put("opportunityData", session.getOpportunityData());
            stateData.put("generatedContents", session.getGeneratedContents());
            stateData.put("optimizedContents", session.getOptimizedContents());
            stateData.put("criticResults", session.getCriticResults());
            stateData.put("approvalRequestId", session.getApprovalRequestId());

            checkpointService.saveCheckpoint(
                    session.getSessionId(),
                    "GrowthWorkflow",
                    session.getUserId(),
                    phase,
                    completedPhases,
                    session.getPhase().ordinal(),
                    stateData,
                    null
            );
        } catch (Exception e) {
            log.warn("保存检查点失败: sessionId={}, phase={}", session.getSessionId(), phase, e);
        }
    }

    @SuppressWarnings("unchecked")
    public GrowthResult resumeFromCheckpoint(String sessionId) {
        WorkflowCheckpoint checkpoint = checkpointService.getCheckpoint(sessionId);
        if (checkpoint == null) {
            return GrowthResult.builder()
                    .success(false)
                    .message("没有找到可恢复的检查点")
                    .build();
        }

        if (!checkpoint.canResume()) {
            return GrowthResult.builder()
                    .success(false)
                    .message("检查点无法恢复: " + (checkpoint.isExpired() ? "已过期" : "已完成或失败"))
                    .build();
        }

        GrowthSession session = new GrowthSession(
                sessionId,
                checkpoint.getUserId(),
                checkpoint.getWorkflowType(),
                Date.from(checkpoint.getCreatedAt())
        );

        sessions.put(sessionId, session);

        Map<String, Object> stateData = checkpoint.getStateData();
        if (stateData != null) {
            session.setBehaviorData((Map<String, Object>) stateData.get("behaviorData"));
            session.setOpportunityData((Map<String, Object>) stateData.get("opportunityData"));
            session.setGeneratedContents((List<Map<String, Object>>) stateData.get("generatedContents"));
            session.setOptimizedContents((List<Map<String, Object>>) stateData.get("optimizedContents"));
            session.setCriticResults((List<Map<String, Object>>) stateData.get("criticResults"));
            session.setApprovalRequestId((String) stateData.get("approvalRequestId"));
        }

        String lastPhase = checkpoint.getCurrentPhase();
        log.info("从检查点恢复: sessionId={}, lastPhase={}", sessionId, lastPhase);

        return GrowthResult.builder()
                .success(true)
                .message("已从检查点恢复，可以继续执行")
                .phase(GrowthPhase.valueOf(lastPhase))
                .status(GrowthStatus.RUNNING)
                .build();
    }

    public GrowthResult executeStep(String sessionId, String step) {
        GrowthSession session = getSession(sessionId);
        if (session == null) {
            return GrowthResult.builder()
                    .success(false)
                    .message("会话不存在")
                    .build();
        }

        try {
            return switch (step.toLowerCase()) {
                case "analyze" -> executeBehaviorAnalysis(session);
                case "discover" -> executeOpportunityDiscovery(session);
                case "generate" -> executeContentGeneration(session);
                case "optimize" -> executeSEOOptimization(session);
                case "critic" -> executeCriticReview(session);
                case "reflect" -> executeReflection(session);
                default -> GrowthResult.builder()
                        .success(false)
                        .message("未知步骤: " + step)
                        .build();
            };
        } catch (Exception e) {
            log.error("步骤执行失败: sessionId={}, step={}", sessionId, step, e);
            return GrowthResult.builder()
                    .success(false)
                    .message("步骤执行失败: " + e.getMessage())
                    .build();
        }
    }

    private GrowthResult executeBehaviorAnalysis(GrowthSession session) {
        session.setPhase(GrowthPhase.BEHAVIOR_ANALYSIS);
        AgentContext context = AgentContext.builder()
                .userId(session.getUserId())
                .params(Map.of("periodDays", 30, "topN", 10))
                .build();
        AgentResult result = behaviorAnalyzerAgent.execute(context);
        session.setBehaviorData((Map<String, Object>) result.getData());
        return buildGrowthResult(session, true, "行为分析完成");
    }

    private GrowthResult executeOpportunityDiscovery(GrowthSession session) {
        session.setPhase(GrowthPhase.OPPORTUNITY_DISCOVERY);
        AgentContext context = AgentContext.builder()
                .userId(session.getUserId())
                .params(Map.of("topN", 5))
                .build();
        AgentResult result = opportunityDiscoveryAgent.execute(context);
        session.setOpportunityData((Map<String, Object>) result.getData());
        return buildGrowthResult(session, true, "机会发现完成");
    }

    private GrowthResult executeContentGeneration(GrowthSession session) {
        session.setPhase(GrowthPhase.CONTENT_GENERATION);
        List<Map<String, Object>> generatedContents = new ArrayList<>();

        Map<String, Object> oppData = session.getOpportunityData();
        if (oppData != null && oppData.containsKey("opportunities")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> opportunities = (List<Map<String, Object>>) oppData.get("opportunities");
            for (Map<String, Object> opp : opportunities) {
                String title = opp.get("title") != null ? opp.get("title").toString() : "未命名主题";
                try {
                    AgentContext context = AgentContext.builder()
                            .userId(session.getUserId())
                            .params(Map.of("topic", title))
                            .build();
                    AgentResult result = contentGenerationAgent.execute(context);
                    if (result.isSuccess() && result.getData() != null) {
                        generatedContents.add((Map<String, Object>) result.getData());
                    }
                } catch (Exception e) {
                    log.warn("文章生成失败: title={}", title, e);
                }
            }
        }

        session.setGeneratedContents(generatedContents);
        return buildGrowthResult(session, true, "内容生成完成，共" + generatedContents.size() + "篇");
    }

    private GrowthResult executeSEOOptimization(GrowthSession session) {
        session.setPhase(GrowthPhase.SEO_OPTIMIZATION);
        List<Map<String, Object>> optimizedContents = new ArrayList<>();

        for (Map<String, Object> content : session.getGeneratedContents()) {
            try {
                AgentContext context = AgentContext.builder()
                        .userId(session.getUserId())
                        .params(Map.of("content", content))
                        .build();
                AgentResult result = seoOptimizationAgent.execute(context);
                if (result.isSuccess() && result.getData() != null) {
                    optimizedContents.add((Map<String, Object>) result.getData());
                } else {
                    optimizedContents.add(content);
                }
            } catch (Exception e) {
                log.warn("SEO优化失败", e);
                optimizedContents.add(content);
            }
        }

        session.setOptimizedContents(optimizedContents);
        return buildGrowthResult(session, true, "SEO优化完成");
    }

    private GrowthResult executeCriticReview(GrowthSession session) {
        session.setPhase(GrowthPhase.CRITIC_REVIEW);
        List<Map<String, Object>> criticResults = new ArrayList<>();

        if (session.getOptimizedContents() == null || session.getOptimizedContents().isEmpty()) {
            return GrowthResult.builder()
                    .success(false)
                    .message("没有可审核的内容，请先生成内容")
                    .build();
        }

        for (Map<String, Object> content : session.getOptimizedContents()) {
            try {
                AgentContext context = AgentContext.builder()
                        .userId(session.getUserId())
                        .params(Map.of("content", content, "contentType", "article"))
                        .build();
                AgentResult result = criticAgent.execute(context);
                if (result.isSuccess() && result.getData() != null) {
                    criticResults.add((Map<String, Object>) result.getData());
                }
            } catch (Exception e) {
                log.warn("内容审核失败", e);
            }
        }

        session.setCriticResults(criticResults);
        return buildGrowthResult(session, true, "评论家审核完成，共" + criticResults.size() + "篇");
    }

    private GrowthResult executeReflection(GrowthSession session) {
        session.setPhase(GrowthPhase.REFLECTION);
        AgentContext context = AgentContext.builder()
                .userId(session.getUserId())
                .params(Map.of(
                        "taskType", session.getTaskType(),
                        "taskResult", String.format("生成了%d篇文章", session.getGeneratedContents().size()),
                        "success", session.getStatus() == GrowthStatus.SUCCESS
                ))
                .build();
        AgentResult result = reflectionAgent.execute(context);
        session.setReflectionData((Map<String, Object>) result.getData());
        session.setPhase(GrowthPhase.COMPLETED);
        session.setStatus(GrowthStatus.SUCCESS);
        return buildGrowthResult(session, true, "反思完成");
    }

    private GrowthResult buildGrowthResult(GrowthSession session, boolean success, String message) {
        return GrowthResult.builder()
                .sessionId(session.getSessionId())
                .success(success)
                .message(message)
                .phase(session.getPhase())
                .status(session.getStatus())
                .approvalRequestId(session.getApprovalRequestId())
                .behaviorData(session.getBehaviorData())
                .opportunityData(session.getOpportunityData())
                .generatedContents(session.getGeneratedContents())
                .optimizedContents(session.getOptimizedContents())
                .criticResults(session.getCriticResults())
                .reflectionData(session.getReflectionData())
                .build();
    }

    public static class GrowthSession {
        private final String sessionId;
        private final Long userId;
        private final String taskType;
        private final Date createdAt;
        private GrowthPhase phase;
        private GrowthStatus status;
        private String errorMessage;
        private String approvalRequestId;
        private Map<String, Object> behaviorData;
        private Map<String, Object> opportunityData;
        private List<Map<String, Object>> generatedContents;
        private List<Map<String, Object>> optimizedContents;
        private List<Map<String, Object>> criticResults;
        private Map<String, Object> reflectionData;

        public GrowthSession(String sessionId, Long userId, String taskType, Date createdAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.taskType = taskType;
            this.createdAt = createdAt;
            this.phase = GrowthPhase.INITIALIZED;
            this.status = GrowthStatus.PENDING;
        }

        public String getSessionId() { return sessionId; }
        public Long getUserId() { return userId; }
        public String getTaskType() { return taskType; }
        public Date getCreatedAt() { return createdAt; }
        public GrowthPhase getPhase() { return phase; }
        public GrowthStatus getStatus() { return status; }
        public String getErrorMessage() { return errorMessage; }
        public Map<String, Object> getBehaviorData() { return behaviorData; }
        public Map<String, Object> getOpportunityData() { return opportunityData; }
        public List<Map<String, Object>> getGeneratedContents() { return generatedContents; }
        public List<Map<String, Object>> getOptimizedContents() { return optimizedContents; }
        public List<Map<String, Object>> getCriticResults() { return criticResults; }
        public Map<String, Object> getReflectionData() { return reflectionData; }

        public void setPhase(GrowthPhase phase) { this.phase = phase; }
        public void setStatus(GrowthStatus status) { this.status = status; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public void setBehaviorData(Map<String, Object> behaviorData) { this.behaviorData = behaviorData; }
        public void setOpportunityData(Map<String, Object> opportunityData) { this.opportunityData = opportunityData; }
        public void setGeneratedContents(List<Map<String, Object>> generatedContents) { this.generatedContents = generatedContents; }
        public void setOptimizedContents(List<Map<String, Object>> optimizedContents) { this.optimizedContents = optimizedContents; }
        public void setCriticResults(List<Map<String, Object>> criticResults) { this.criticResults = criticResults; }
        public void setReflectionData(Map<String, Object> reflectionData) { this.reflectionData = reflectionData; }
        public void setApprovalRequestId(String approvalRequestId) { this.approvalRequestId = approvalRequestId; }
        public String getApprovalRequestId() { return approvalRequestId; }
    }

    public enum GrowthPhase {
        INITIALIZED,
        BEHAVIOR_ANALYSIS,
        OPPORTUNITY_DISCOVERY,
        CONTENT_GENERATION,
        SEO_OPTIMIZATION,
        CRITIC_REVIEW,
        HUMAN_REVIEW,
        REFLECTION,
        COMPLETED,
        FAILED
    }

    public enum GrowthStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        WAITING_APPROVAL
    }

    @lombok.Data
    @lombok.Builder
    public static class GrowthResult {
        private String sessionId;
        private boolean success;
        private String message;
        private GrowthPhase phase;
        private GrowthStatus status;
        private String approvalRequestId;
        private Map<String, Object> behaviorData;
        private Map<String, Object> opportunityData;
        private List<Map<String, Object>> generatedContents;
        private List<Map<String, Object>> optimizedContents;
        private List<Map<String, Object>> criticResults;
        private Map<String, Object> reflectionData;
    }
}