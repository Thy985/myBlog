package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xingchen.backend.ab.ABTestService;
import com.xingchen.backend.ai.gateway.AIGateway;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * Agent 控制器
 * 使用新架构 AIGateway
 */
@RestController
@RequestMapping("/api/agent")
@Slf4j
@RequiredArgsConstructor
public class AgentController {

    private final AIGateway aiGateway;
    private final ABTestService abTestService;
    private final ArticleService articleService;
    private final WebSearchService webSearchService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 开始新会话
     */
    @PostMapping("/session/start")
    @SaCheckLogin
    public Result<StartSessionResponse> startSession() {
        String sessionId = "agent_" + UUID.randomUUID().toString().replace("-", "");
        return Result.success(new StartSessionResponse(sessionId, "会话创建成功"));
    }

    /**
     * Agent 对话（SSE 流式版本）
     */
    @PostMapping("/chat/stream")
    @SaCheckLogin
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        emitter.onCompletion(() -> log.debug("Agent SSE 完成: sessionId={}", request.sessionId()));
        emitter.onTimeout(() -> log.warn("Agent SSE 超时: sessionId={}", request.sessionId()));
        emitter.onError(e -> log.error("Agent SSE 错误: sessionId={}", request.sessionId(), e));

        // 异步执行，避免阻塞 HTTP 线程
        new Thread(() -> {
            try {
                // 构建 AI 请求
                AIRequest aiRequest = AIRequest.builder()
                        .userId(userId)
                        .sessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString())
                        .message(request.message())
                        .stream(true)
                        .build();

                // 使用 AIGateway 流式处理
                StringBuilder fullResponse = new StringBuilder();
                aiGateway.processStream(aiRequest, response -> {
                    try {
                        if (response.getType() == AIResponse.ResponseType.STREAM_END) {
                            emitter.send(SseEmitter.event().name("done").data("{\"done\": true}"));
                            emitter.complete();
                        } else if (response.getType() == AIResponse.ResponseType.ERROR) {
                            emitter.send(SseEmitter.event().name("error").data("{\"error\": \"" + escapeJson(response.getContent()) + "\"}"));
                            emitter.completeWithError(new RuntimeException(response.getContent()));
                        } else {
                            String sseData = "{\"content\": \"" + escapeJson(response.getContent()) + "\"}";
                            emitter.send(SseEmitter.event().name("message").data(sseData));
                            fullResponse.append(response.getContent());
                        }
                    } catch (Exception e) {
                        log.error("发送SSE消息失败", e);
                        emitter.completeWithError(e);
                    }
                });

            } catch (Exception e) {
                log.error("Agent SSE 执行异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\": \"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 发送意图识别事件
     */
    private void sendIntentEvent(SseEmitter emitter, String intentType, double confidence,
                                 List<Map<String, String>> entities, boolean requiresTool,
                                 List<String> possibleTools) throws Exception {
        StringBuilder entitiesJson = new StringBuilder("[");
        for (int i = 0; i < entities.size(); i++) {
            Map<String, String> entity = entities.get(i);
            entitiesJson.append(String.format("{\"name\": \"%s\", \"value\": \"%s\"}",
                escapeJson(entity.get("name")), escapeJson(entity.get("value"))));
            if (i < entities.size() - 1) entitiesJson.append(", ");
        }
        entitiesJson.append("]");

        StringBuilder toolsJson = new StringBuilder("[");
        for (int i = 0; i < possibleTools.size(); i++) {
            toolsJson.append("\"").append(escapeJson(possibleTools.get(i))).append("\"");
            if (i < possibleTools.size() - 1) toolsJson.append(", ");
        }
        toolsJson.append("]");

        String data = String.format(
            "{\"intent\": {\"type\": \"%s\", \"confidence\": %.2f, \"entities\": %s, \"requiresTool\": %b, \"possibleTools\": %s}}",
            intentType, confidence, entitiesJson, requiresTool, toolsJson
        );
        emitter.send(SseEmitter.event().name("intent").data(data));
    }

    /**
     * 发送工具调用事件
     */
    private void sendToolCallEvent(SseEmitter emitter, String toolName, Map<String, Object> parameters) throws Exception {
        StringBuilder paramsJson = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            paramsJson.append(String.format("\"%s\": ", escapeJson(entry.getKey())));
            Object value = entry.getValue();
            if (value instanceof String) {
                paramsJson.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                paramsJson.append(value);
            } else if (value instanceof Boolean) {
                paramsJson.append(value);
            } else {
                paramsJson.append("\"").append(escapeJson(value.toString())).append("\"");
            }
            if (i < parameters.size() - 1) paramsJson.append(", ");
            i++;
        }
        paramsJson.append("}");

        String data = String.format(
            "{\"toolName\": \"%s\", \"parameters\": %s, \"status\": \"executing\"}",
            escapeJson(toolName), paramsJson
        );
        emitter.send(SseEmitter.event().name("tool_call").data(data));
    }

    /**
     * 发送工具调用结果事件
     */
    private void sendToolResultEvent(SseEmitter emitter, String toolName, boolean success,
                                     Map<String, Object> result, String message, long executionTime) throws Exception {
        StringBuilder resultJson = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            resultJson.append(String.format("\"%s\": ", escapeJson(entry.getKey())));
            Object value = entry.getValue();
            if (value instanceof String) {
                resultJson.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                resultJson.append(value);
            } else if (value instanceof Boolean) {
                resultJson.append(value);
            } else {
                resultJson.append("\"").append(escapeJson(value.toString())).append("\"");
            }
            if (i < result.size() - 1) resultJson.append(", ");
            i++;
        }
        resultJson.append("}");

        String data = String.format(
            "{\"toolName\": \"%s\", \"success\": %b, \"result\": %s, \"message\": \"%s\", \"executionTime\": %d}",
            escapeJson(toolName), success, resultJson, escapeJson(message), executionTime
        );
        emitter.send(SseEmitter.event().name("tool_result").data(data));
    }

    /**
     * 发送 RAG 来源事件
     */
    private void sendRagEvent(SseEmitter emitter, String query, List<WebSearchService.SearchResult> sources) throws Exception {
        StringBuilder sourcesJson = new StringBuilder("[");
        for (int i = 0; i < sources.size(); i++) {
            WebSearchService.SearchResult source = sources.get(i);
            String cleanContent = cleanHtml(source.content());
            if (cleanContent.length() > 200) {
                cleanContent = cleanContent.substring(0, 200) + "...";
            }
            // 模拟相关度评分（实际应该由 RAG 系统提供）
            double relevance = 0.9 - (i * 0.1);
            sourcesJson.append(String.format(
                "{\"title\": \"%s\", \"url\": \"%s\", \"snippet\": \"%s\", \"relevance\": %.2f}",
                escapeJson(source.title()),
                escapeJson(source.url()),
                escapeJson(cleanContent),
                relevance
            ));
            if (i < sources.size() - 1) sourcesJson.append(", ");
        }
        sourcesJson.append("]");

        String data = String.format(
            "{\"query\": \"%s\", \"sources\": %s}",
            escapeJson(query), sourcesJson
        );
        emitter.send(SseEmitter.event().name("rag").data(data));
    }

    private void sendStep(SseEmitter emitter, int currentStep, int totalSteps, String stepName, String status, String description, Long executionTime) throws Exception {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append(String.format(
            "{\"currentStep\": %d, \"totalSteps\": %d, \"stepName\": \"%s\", \"status\": \"%s\", \"description\": \"%s\"",
            currentStep, totalSteps, escapeJson(stepName), status, escapeJson(description)
        ));
        if (executionTime != null) {
            dataBuilder.append(String.format(", \"executionTime\": %d", executionTime));
        }
        dataBuilder.append("}");
        emitter.send(SseEmitter.event().name("step").data(dataBuilder.toString()));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    /**
     * Agent 对话（非流式）
     */
    @PostMapping("/chat")
    @SaCheckLogin
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 使用 AIGateway 处理
        AIRequest aiRequest = AIRequest.builder()
                .userId(userId)
                .sessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString())
                .message(request.message())
                .stream(false)
                .build();

        AIResponse response = aiGateway.process(aiRequest);

        return Result.success(new ChatResponse(
                aiRequest.getSessionId(),
                response.getContent(),
                response.isSuccess() ? "COMPLETED" : "ERROR",
                0,
                0
        ));
    }


    /**
     * 清理 HTML 标签
     */
    private String cleanHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#\\d+;", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ========== A/B 测试接口 ==========

    @GetMapping("/ab/assign")
    @SaCheckLogin
    public Result<ABAssignmentResponse> getABAssignment(@RequestParam String experimentId) {
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

    @GetMapping("/ab/stats/{experimentId}")
    @SaCheckLogin
    public Result<ABTestService.ExperimentStats> getABStats(@PathVariable String experimentId) {
        ABTestService.ExperimentStats stats = abTestService.getStats(experimentId);
        if (stats == null) {
            return Result.fail(404, "实验不存在");
        }
        return Result.success(stats);
    }

    @GetMapping("/ab/experiments")
    @SaCheckLogin
    public Result<List<ABTestService.Experiment>> listExperiments() {
        return Result.success(abTestService.listExperiments());
    }

    // ========== 请求/响应类 ==========

    public record StartSessionResponse(String sessionId, String message) {}
    public record ChatRequest(String sessionId, String message) {}
    public record ChatResponse(String sessionId, String response, String status, int currentStep, int totalSteps) {}

    public record ABAssignmentResponse(
            String experimentId,
            String variantId,
            String variantName,
            Map<String, Object> config
    ) {}

    @lombok.Data
    public static class ABMetricRequest {
        private String experimentId;
        private String variantId;
        private String metricName;
        private double value;
    }
}
