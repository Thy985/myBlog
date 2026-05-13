package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.agent.human.ApprovalRequest;
import com.xingchen.backend.agent.human.HumanReviewService;
import com.xingchen.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/human")
@RequiredArgsConstructor
@Slf4j
public class HumanReviewController {

    private final HumanReviewService humanReviewService;

    @PostMapping("/approval/request")
    public Result<ApprovalRequest> requestApproval(
            @RequestParam String agentType,
            @RequestParam String actionType,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String payloadJson) {

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用户 {} 请求审批: agentType={}, actionType={}, title={}",
                userId, agentType, actionType, title);

        Map<String, Object> payload = parsePayload(payloadJson);
        String riskLevel = humanReviewService.getRiskLevel(agentType, actionType, payload);

        ApprovalRequest request = humanReviewService.requestApproval(
                userId,
                agentType,
                actionType,
                riskLevel,
                title,
                description != null ? description : "",
                payload
        );

        return Result.success(request);
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            log.warn("解析payload失败: {}", payloadJson);
            return Map.of();
        }
    }

    @GetMapping("/approval/pending")
    public Result<List<ApprovalRequest>> getPendingApprovals() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ApprovalRequest> pending = humanReviewService.getPendingApprovals(userId);
        return Result.success(pending);
    }

    @GetMapping("/approval/{requestId}")
    public Result<ApprovalRequest> getApprovalRequest(@PathVariable String requestId) {
        ApprovalRequest request = humanReviewService.getApprovalRequest(requestId);

        if (request != null) {
            return Result.success(request);
        } else {
            return Result.error(404, "审批请求不存在");
        }
    }

    @PostMapping("/approval/{requestId}/approve")
    public Result<ApprovalRequest> approve(
            @PathVariable String requestId,
            @RequestParam(required = false) String comment) {

        Long reviewerId = StpUtil.getLoginIdAsLong();
        log.info("用户 {} 批准审批请求: requestId={}", reviewerId, requestId);

        ApprovalRequest request = humanReviewService.approve(requestId, reviewerId, comment);

        if (request != null) {
            return Result.success(request);
        } else {
            return Result.error(404, "审批请求不存在");
        }
    }

    @PostMapping("/approval/{requestId}/reject")
    public Result<ApprovalRequest> reject(
            @PathVariable String requestId,
            @RequestParam(required = false) String comment) {

        Long reviewerId = StpUtil.getLoginIdAsLong();
        log.info("用户 {} 拒绝审批请求: requestId={}, comment={}", reviewerId, requestId, comment);

        ApprovalRequest request = humanReviewService.reject(requestId, reviewerId, comment);

        if (request != null) {
            return Result.success(request);
        } else {
            return Result.error(404, "审批请求不存在");
        }
    }

    @PostMapping("/approval/{requestId}/check")
    public Result<Map<String, Object>> checkApprovalStatus(@PathVariable String requestId) {
        ApprovalRequest request = humanReviewService.getApprovalRequest(requestId);

        if (request == null) {
            return Result.error(404, "审批请求不存在");
        }

        Map<String, Object> status = Map.of(
                "requestId", request.getRequestId(),
                "status", request.getStatus().name(),
                "isPending", request.isPending(),
                "isExpired", request.isExpired(),
                "isHighRisk", request.isHighRisk()
        );

        return Result.success(status);
    }
}