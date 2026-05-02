package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.content.ReflectionAgent;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.config.AgentLLMProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/reflection")
@Slf4j
@RequiredArgsConstructor
public class ReflectionController {

    private final ReflectionAgent reflectionAgent;
    private final AgentLLMProviderAdapter agentLLMProviderAdapter;

    @PostMapping("/analyze")
    @SaCheckLogin
    public Result<ReflectionResponse> analyzeTask(@RequestBody ReflectionRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .params(Map.of(
                            "taskType", request.taskType() != null ? request.taskType() : "content_generation",
                            "taskResult", request.taskResult() != null ? request.taskResult() : "",
                            "success", request.success() != null ? request.success() : true,
                            "history", request.history() != null ? request.history() : List.of()
                    ))
                    .build();

            BaseAgent.AgentResult result = reflectionAgent.execute(context);

            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();
                return Result.success(ReflectionResponse.fromMap(data));
            } else {
                return Result.fail(400, result.getError());
            }

        } catch (Exception e) {
            log.error("Reflection analysis failed: userId={}", userId, e);
            return Result.fail(500, "反思分析失败: " + e.getMessage());
        }
    }

    public record ReflectionRequest(
            String taskType,
            String taskResult,
            Boolean success,
            List<Map<String, Object>> history
    ) {}

    public record ReflectionResponse(
            String taskType,
            boolean success,
            String summary,
            List<String> successFactors,
            List<String> failureReasons,
            List<String> improvements,
            List<String> recommendations,
            double confidence
    ) {
        public static ReflectionResponse fromMap(Map<String, Object> data) {
            @SuppressWarnings("unchecked")
            List<String> successFactors = (List<String>) data.getOrDefault("successFactors", List.of());
            @SuppressWarnings("unchecked")
            List<String> failureReasons = (List<String>) data.getOrDefault("failureReasons", List.of());
            @SuppressWarnings("unchecked")
            List<String> improvements = (List<String>) data.getOrDefault("improvements", List.of());
            @SuppressWarnings("unchecked")
            List<String> recommendations = (List<String>) data.getOrDefault("recommendations", List.of());

            return new ReflectionResponse(
                    (String) data.get("taskType"),
                    (Boolean) data.get("success"),
                    (String) data.get("summary"),
                    successFactors,
                    failureReasons,
                    improvements,
                    recommendations,
                    ((Number) data.getOrDefault("confidence", 0.8)).doubleValue()
            );
        }
    }
}
