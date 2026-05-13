package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.agent.growth.GrowthWorkflowOrchestrator;
import com.xingchen.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/growth")
@RequiredArgsConstructor
@Slf4j
public class GrowthController {

    private final GrowthWorkflowOrchestrator growthOrchestrator;

    @PostMapping("/闭环/execute")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> executeGrowth闭环(
            @RequestParam(defaultValue = "content_growth") String taskType) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用户 {} 发起增长闭环任务: taskType={}", userId, taskType);

        GrowthWorkflowOrchestrator.GrowthResult result = growthOrchestrator.executeGrowth闭环(userId, taskType);

        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.error(500, result.getMessage());
        }
    }

    @PostMapping("/闭环/step/{step}")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> executeStep(
            @RequestParam String sessionId,
            @PathVariable String step) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用户 {} 执行增长步骤: sessionId={}, step={}", userId, sessionId, step);

        GrowthWorkflowOrchestrator.GrowthResult result = growthOrchestrator.executeStep(sessionId, step);

        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.error(500, result.getMessage());
        }
    }

    @GetMapping("/闭环/session/{sessionId}")
    public Result<GrowthWorkflowOrchestrator.GrowthSession> getSession(@PathVariable String sessionId) {
        GrowthWorkflowOrchestrator.GrowthSession session = growthOrchestrator.getSession(sessionId);

        if (session != null) {
            return Result.success(session);
        } else {
            return Result.error(404, "会话不存在");
        }
    }

    @PostMapping("/step/analyze")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> analyzeBehavior(@RequestParam String sessionId) {
        return Result.success(growthOrchestrator.executeStep(sessionId, "analyze"));
    }

    @PostMapping("/step/discover")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> discoverOpportunities(@RequestParam String sessionId) {
        return Result.success(growthOrchestrator.executeStep(sessionId, "discover"));
    }

    @PostMapping("/step/generate")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> generateContent(@RequestParam String sessionId) {
        return Result.success(growthOrchestrator.executeStep(sessionId, "generate"));
    }

    @PostMapping("/step/optimize")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> optimizeSEO(@RequestParam String sessionId) {
        return Result.success(growthOrchestrator.executeStep(sessionId, "optimize"));
    }

    @PostMapping("/step/critic")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> criticReview(@RequestParam String sessionId) {
        return Result.success(growthOrchestrator.executeStep(sessionId, "critic"));
    }

    @PostMapping("/step/reflect")
    public Result<GrowthWorkflowOrchestrator.GrowthResult> reflect(@RequestParam String sessionId) {
        return Result.success(growthOrchestrator.executeStep(sessionId, "reflect"));
    }

    @GetMapping("/report")
    public Result<?> getGrowthReport(
            @RequestParam(defaultValue = "daily") String period) {
        return Result.success(growthOrchestrator.executeGrowth闭环(
                StpUtil.getLoginIdAsLong(), "report_" + period));
    }
}