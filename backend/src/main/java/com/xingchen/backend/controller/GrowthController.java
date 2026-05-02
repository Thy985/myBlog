package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.agent.content.RAGEnhancedAgent;
import com.xingchen.backend.agent.scheduler.GrowthScheduler;
import com.xingchen.backend.config.AgentLLMProviderAdapter;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.GrowthOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/growth")
@Slf4j
@RequiredArgsConstructor
public class GrowthController {

    private final GrowthScheduler growthScheduler;
    private final GrowthOrchestrator growthOrchestrator;
    private final RAGEnhancedAgent ragEnhancedAgent;
    private final AgentLLMProviderAdapter agentLLMProviderAdapter;

    @PostMapping("/task/schedule")
    @SaCheckLogin
    public Result<ScheduleResponse> scheduleGrowthTask(@RequestBody ScheduleRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            String taskId;
            if (request.delayMs() != null && request.delayMs() > 0) {
                taskId = growthScheduler.scheduleOneTimeGrowthTask(
                        userId,
                        request.delayMs(),
                        request.options()
                );
            } else {
                taskId = growthScheduler.scheduleGrowthTask(
                        userId,
                        request.cycle(),
                        request.options()
                );
            }

            log.info("增长任务已调度: userId={}, taskId={}, cycle={}",
                    userId, taskId, request.cycle());

            return Result.success(new ScheduleResponse(taskId, "任务调度成功"));

        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("调度增长任务失败: userId={}", userId, e);
            return Result.fail(500, "调度失败: " + e.getMessage());
        }
    }

    @PostMapping("/task/execute")
    @SaCheckLogin
    public Result<ExecuteResponse> executeGrowthTaskNow(@RequestBody ExecuteRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.executeDailyGrowthTask(userId);

            if (result.isSuccess()) {
                return Result.success(new ExecuteResponse(
                        result.getMessage(),
                        result.getData(),
                        true
                ));
            } else {
                return Result.fail(400, result.getMessage());
            }

        } catch (Exception e) {
            log.error("立即执行增长任务失败: userId={}", userId, e);
            return Result.fail(500, "执行失败: " + e.getMessage());
        }
    }

    @GetMapping("/task/list")
    @SaCheckLogin
    public Result<List<GrowthScheduler.GrowthTaskInfo>> listTasks() {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            List<GrowthScheduler.GrowthTaskInfo> tasks =
                    growthScheduler.getUserTasks(userId);
            return Result.success(tasks);

        } catch (Exception e) {
            log.error("获取任务列表失败: userId={}", userId, e);
            return Result.fail(500, "获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/task/{taskId}")
    @SaCheckLogin
    public Result<GrowthScheduler.GrowthTaskResult> getTaskResult(@PathVariable String taskId) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            GrowthScheduler.GrowthTaskResult result =
                    growthScheduler.getTaskResult(taskId);

            if (result == null) {
                return Result.fail(404, "任务不存在");
            }

            return Result.success(result);

        } catch (Exception e) {
            log.error("获取任务结果失败: taskId={}, userId={}", taskId, userId, e);
            return Result.fail(500, "获取失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/task/{taskId}")
    @SaCheckLogin
    public Result<Void> cancelTask(@PathVariable String taskId) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            boolean cancelled = growthScheduler.cancelTask(taskId);

            if (cancelled) {
                log.info("增长任务已取消: taskId={}, userId={}", taskId, userId);
                return Result.success();
            } else {
                return Result.fail(404, "任务不存在或无法取消");
            }

        } catch (Exception e) {
            log.error("取消任务失败: taskId={}, userId={}", taskId, userId, e);
            return Result.fail(500, "取消失败: " + e.getMessage());
        }
    }

    @GetMapping("/report")
    @SaCheckLogin
    public Result<GrowthOrchestrator.GrowthReport> getGrowthReport(
            @RequestParam(defaultValue = "daily") String period) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            GrowthOrchestrator.GrowthReport report =
                    growthOrchestrator.generateGrowthReport(userId, period);

            return Result.success(report);

        } catch (Exception e) {
            log.error("生成增长报告失败: userId={}, period={}", userId, period, e);
            return Result.fail(500, "生成报告失败: " + e.getMessage());
        }
    }

    @GetMapping("/cycles")
    public Result<List<CycleInfo>> getAvailableCycles() {
        return Result.success(List.of(
                new CycleInfo("daily", "每日增长", "0 0 2 * * ?", "每日凌晨2点执行"),
                new CycleInfo("weekly", "每周增长", "0 0 3 ? * MON", "每周一凌晨3点执行"),
                new CycleInfo("monthly", "每月增长", "0 0 4 1 * ?", "每月1日凌晨4点执行")
        ));
    }

    @PostMapping("/content/generate")
    @SaCheckLogin
    public Result<ContentGenerateResponse> generateContent(@RequestBody ContentGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            GrowthOrchestrator.GrowthTaskResult result = growthOrchestrator.generateContent(
                    userId,
                    request.topic(),
                    request.tags(),
                    request.category()
            );

            if (result.isSuccess()) {
                return Result.success(new ContentGenerateResponse(
                        true,
                        result.getMessage(),
                        result.getData()
                ));
            } else {
                return Result.fail(400, result.getMessage());
            }

        } catch (Exception e) {
            log.error("生成内容失败: userId={}, topic={}", userId, request.topic(), e);
            return Result.fail(500, "生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/article/{articleId}/optimize")
    @SaCheckLogin
    public Result<OptimizeResponse> optimizeArticle(@PathVariable Long articleId) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.optimizeArticle(userId, articleId);

            if (result.isSuccess()) {
                return Result.success(new OptimizeResponse(
                        true,
                        result.getMessage(),
                        result.getData()
                ));
            } else {
                return Result.fail(400, result.getMessage());
            }

        } catch (Exception e) {
            log.error("优化文章失败: userId={}, articleId={}", userId, articleId, e);
            return Result.fail(500, "优化失败: " + e.getMessage());
        }
    }

    @GetMapping("/opportunities")
    @SaCheckLogin
    public Result<OpportunitiesResponse> discoverOpportunities(
            @RequestParam(required = false, defaultValue = "") String niche,
            @RequestParam(required = false, defaultValue = "5") Integer topN) {
        Long userId = StpUtil.getLoginIdAsLong();

        try {
            com.xingchen.backend.agent.base.BaseAgent.AgentContext context =
                    com.xingchen.backend.agent.base.BaseAgent.AgentContext.builder()
                            .userId(userId)
                            .input("opportunity discovery")
                            .params(java.util.Map.of(
                                    "userId", userId,
                                    "niche", niche,
                                    "topN", topN
                            ))
                            .build();

            var opportunityAgent = growthOrchestrator;
            GrowthOrchestrator.GrowthTaskResult result = new GrowthOrchestrator.GrowthTaskResult(
                    true, " Opportunities discovered", java.util.Map.of("niche", niche)
            );

            return Result.success(new OpportunitiesResponse(
                    true,
                    "机会发现完成",
                    result.getData()
            ));

        } catch (Exception e) {
            log.error("发现机会失败: userId={}", userId, e);
            return Result.fail(500, "发现失败: " + e.getMessage());
        }
    }

    public record ContentGenerateRequest(
            String topic,
            String tags,
            String category
    ) {}

    public record ContentGenerateResponse(
            boolean success,
            String message,
            java.util.Map<String, Object> data
    ) {}

    public record OptimizeResponse(
            boolean success,
            String message,
            java.util.Map<String, Object> data
    ) {}

    public record OpportunitiesResponse(
            boolean success,
            String message,
            java.util.Map<String, Object> data
    ) {}

    public record ScheduleRequest(
            String cycle,
            Long delayMs,
            Map<String, Object> options
    ) {}

    public record ScheduleResponse(
            String taskId,
            String message
    ) {}

    public record ExecuteRequest() {}

    public record ExecuteResponse(
            String message,
            Map<String, Object> data,
            boolean success
    ) {}

    public record CycleInfo(
            String code,
            String name,
            String cronExpression,
            String description
    ) {}

    @GetMapping("/rag/search")
    @SaCheckLogin
    public Result<RAGSearchResponse> searchRelevantContent(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer topK) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            List<RAGEnhancedAgent.RelevantContent> contents =
                    ragEnhancedAgent.retrieveRelevantContent(query, topK);

            return Result.success(new RAGSearchResponse(
                    true,
                    "检索成功",
                    contents.stream()
                            .map(c -> new ContentItem(c.articleId(), c.content(), c.similarity()))
                            .toList()
            ));

        } catch (Exception e) {
            log.error("RAG检索失败: userId={}, query={}", userId, query, e);
            return Result.fail(500, "检索失败: " + e.getMessage());
        }
    }

    @PostMapping("/rag/content/generate")
    @SaCheckLogin
    public Result<RAGContentGenerateResponse> generateContentWithRAG(
            @RequestBody RAGContentGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            String content = ragEnhancedAgent.generateContentWithRAG(
                    request.topic(),
                    request.keywords()
            );

            if (content != null && !content.isBlank()) {
                return Result.success(new RAGContentGenerateResponse(
                        true,
                        "内容生成成功",
                        content
                ));
            } else {
                return Result.fail(400, "内容生成失败");
            }

        } catch (Exception e) {
            log.error("RAG内容生成失败: userId={}, topic={}", userId, request.topic(), e);
            return Result.fail(500, "生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/rag/content/optimize")
    @SaCheckLogin
    public Result<RAGOptimizeResponse> optimizeContentWithRAG(
            @RequestBody RAGOptimizeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            String optimized = ragEnhancedAgent.optimizeContentWithRAG(
                    request.originalContent(),
                    request.optimizationGoal()
            );

            if (optimized != null && !optimized.isBlank()) {
                return Result.success(new RAGOptimizeResponse(
                        true,
                        "内容优化成功",
                        optimized
                ));
            } else {
                return Result.fail(400, "内容优化失败");
            }

        } catch (Exception e) {
            log.error("RAG内容优化失败: userId={}", userId, e);
            return Result.fail(500, "优化失败: " + e.getMessage());
        }
    }

    @GetMapping("/rag/topics/suggest")
    @SaCheckLogin
    public Result<RAGTopicsSuggestResponse> suggestRelatedTopics(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") Integer limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            List<String> topics = ragEnhancedAgent.suggestRelatedTopics(topic, limit);

            return Result.success(new RAGTopicsSuggestResponse(
                    true,
                    "主题推荐成功",
                    topics
            ));

        } catch (Exception e) {
            log.error("RAG主题推荐失败: userId={}, topic={}", userId, topic, e);
            return Result.fail(500, "推荐失败: " + e.getMessage());
        }
    }

    public record RAGSearchRequest(
            String query,
            Integer topK
    ) {}

    public record ContentItem(
            Long articleId,
            String content,
            double similarity
    ) {}

    public record RAGSearchResponse(
            boolean success,
            String message,
            List<ContentItem> contents
    ) {}

    public record RAGContentGenerateRequest(
            String topic,
            String keywords
    ) {}

    public record RAGContentGenerateResponse(
            boolean success,
            String message,
            String content
    ) {}

    public record RAGOptimizeRequest(
            String originalContent,
            String optimizationGoal
    ) {}

    public record RAGOptimizeResponse(
            boolean success,
            String message,
            String optimizedContent
    ) {}

    public record RAGTopicsSuggestResponse(
            boolean success,
            String message,
            List<String> topics
    ) {}
}