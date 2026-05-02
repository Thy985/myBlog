package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.content.ContentAuditAgent;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.config.AgentLLMProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/audit")
@Slf4j
@RequiredArgsConstructor
public class ContentAuditController {

    private final ContentAuditAgent contentAuditAgent;
    private final AgentLLMProviderAdapter agentLLMProviderAdapter;

    @PostMapping("/article/{articleId}")
    @SaCheckLogin
    public Result<AuditResponse> auditArticle(@PathVariable Long articleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .params(Map.of("articleId", articleId))
                    .build();

            BaseAgent.AgentResult result = contentAuditAgent.execute(context);

            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();
                return Result.success(AuditResponse.fromMap(data));
            } else {
                return Result.fail(400, result.getError());
            }

        } catch (Exception e) {
            log.error("内容审核失败: userId={}, articleId={}", userId, articleId, e);
            return Result.fail(500, "审核失败: " + e.getMessage());
        }
    }

    @PostMapping("/all")
    @SaCheckLogin
    public Result<AuditSummaryResponse> auditAllArticles(
            @RequestParam(defaultValue = "100") Integer limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .params(Map.of("limit", limit))
                    .build();

            BaseAgent.AgentResult result = contentAuditAgent.execute(context);

            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();
                return Result.success(AuditSummaryResponse.fromMap(data));
            } else {
                return Result.fail(400, result.getError());
            }

        } catch (Exception e) {
            log.error("批量内容审核失败: userId={}", userId, e);
            return Result.fail(500, "审核失败: " + e.getMessage());
        }
    }

    public record AuditResponse(
            Long articleId,
            String title,
            String finalStatus,
            boolean needHumanReview,
            boolean layer1Passed,
            boolean layer2Passed,
            Object layerResults,
            Object allIssues
    ) {
        public static AuditResponse fromMap(Map<String, Object> data) {
            return new AuditResponse(
                    (Long) data.get("articleId"),
                    (String) data.get("title"),
                    (String) data.get("finalStatus"),
                    (Boolean) data.get("needHumanReview"),
                    (Boolean) data.get("layer1Passed"),
                    (Boolean) data.get("layer2Passed"),
                    data.get("layerResults"),
                    data.get("allIssues")
            );
        }
    }

    public record AuditSummaryResponse(
            int totalArticles,
            int passCount,
            int warningCount,
            int failCount,
            int needHumanCount,
            int healthScore,
            Object auditResults
    ) {
        public static AuditSummaryResponse fromMap(Map<String, Object> data) {
            return new AuditSummaryResponse(
                    (Integer) data.get("totalArticles"),
                    (Integer) data.get("passCount"),
                    (Integer) data.get("warningCount"),
                    (Integer) data.get("failCount"),
                    (Integer) data.get("needHumanCount"),
                    (Integer) data.get("healthScore"),
                    data.get("auditResults")
            );
        }
    }
}
