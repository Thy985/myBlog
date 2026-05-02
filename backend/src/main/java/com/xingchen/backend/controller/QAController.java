package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.content.QAAgent;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.config.AgentLLMProviderAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/qa")
@Slf4j
@RequiredArgsConstructor
public class QAController {

    private final QAAgent qaAgent;
    private final AgentLLMProviderAdapter agentLLMProviderAdapter;

    @PostMapping("/answer")
    @SaCheckLogin
    public Result<QAAnswerResponse> answerQuestion(@RequestBody QARequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentLLMProviderAdapter.setCurrentUserId(userId);

        try {
            BaseAgent.AgentContext context = BaseAgent.AgentContext.builder()
                    .userId(userId)
                    .sessionId(request.sessionId())
                    .input(request.question())
                    .params(Map.of(
                            "question", request.question(),
                            "topK", request.topK() != null ? request.topK() : 5,
                            "sessionId", request.sessionId() != null ? request.sessionId() : ""
                    ))
                    .build();

            BaseAgent.AgentResult result = qaAgent.execute(context);

            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sources = (List<Map<String, Object>>) data.get("sources");
                @SuppressWarnings("unchecked")
                List<String> suggestedQuestions = (List<String>) data.get("suggestedQuestions");

                return Result.success(new QAAnswerResponse(
                        true,
                        "回答成功",
                        (String) data.get("answer"),
                        sources != null ? sources.stream()
                                .map(s -> new SourceInfo(
                                        ((Number) s.get("articleId")).longValue(),
                                        (String) s.get("title"),
                                        (String) s.get("content"),
                                        ((Number) s.get("similarity")).doubleValue()
                                )).toList() : List.of(),
                        suggestedQuestions != null ? suggestedQuestions : List.of()
                ));
            } else {
                return Result.fail(400, result.getError());
            }

        } catch (Exception e) {
            log.error("智能问答失败: userId={}, question={}", userId, request.question(), e);
            return Result.fail(500, "问答失败: " + e.getMessage());
        }
    }

    public record QARequest(
            String question,
            String sessionId,
            Integer topK
    ) {}

    public record QAAnswerResponse(
            boolean success,
            String message,
            String answer,
            List<SourceInfo> sources,
            List<String> suggestedQuestions
    ) {}

    public record SourceInfo(
            Long articleId,
            String title,
            String content,
            double similarity
    ) {}
}
