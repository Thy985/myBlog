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
                            ObjectNode doneNode = objectMapper.createObjectNode();
                            doneNode.put("done", true);
                            emitter.send(SseEmitter.event().name("done").data(toJsonString(doneNode)));
                            emitter.complete();
                        } else if (response.getType() == AIResponse.ResponseType.ERROR) {
                            ObjectNode errorNode = objectMapper.createObjectNode();
                            errorNode.put("error", response.getContent());
                            emitter.send(SseEmitter.event().name("error").data(toJsonString(errorNode)));
                            emitter.completeWithError(new RuntimeException(response.getContent()));
                        } else {
                            ObjectNode messageNode = objectMapper.createObjectNode();
                            messageNode.put("content", response.getContent());
                            emitter.send(SseEmitter.event().name("message").data(toJsonString(messageNode)));
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
                    ObjectNode errorNode = objectMapper.createObjectNode();
                    errorNode.put("error", e.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(toJsonString(errorNode)));
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
        ObjectNode rootNode = objectMapper.createObjectNode();
        ObjectNode intentNode = rootNode.putObject("intent");

        intentNode.put("type", intentType);
        intentNode.put("confidence", confidence);
        intentNode.put("requiresTool", requiresTool);

        ArrayNode entitiesArray = intentNode.putArray("entities");
        for (Map<String, String> entity : entities) {
            ObjectNode entityNode = entitiesArray.addObject();
            entityNode.put("name", entity.get("name"));
            entityNode.put("value", entity.get("value"));
        }

        ArrayNode toolsArray = intentNode.putArray("possibleTools");
        for (String tool : possibleTools) {
            toolsArray.add(tool);
        }

        emitter.send(SseEmitter.event().name("intent").data(toJsonString(rootNode)));
    }

    /**
     * 发送工具调用事件
     */
    private void sendToolCallEvent(SseEmitter emitter, String toolName, Map<String, Object> parameters) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("toolName", toolName);
        rootNode.put("status", "executing");

        ObjectNode paramsNode = rootNode.putObject("parameters");
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            putValue(paramsNode, entry.getKey(), entry.getValue());
        }

        emitter.send(SseEmitter.event().name("tool_call").data(toJsonString(rootNode)));
    }

    /**
     * 发送工具调用结果事件
     */
    private void sendToolResultEvent(SseEmitter emitter, String toolName, boolean success,
                                     Map<String, Object> result, String message, long executionTime) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("toolName", toolName);
        rootNode.put("success", success);
        rootNode.put("message", message);
        rootNode.put("executionTime", executionTime);

        ObjectNode resultNode = rootNode.putObject("result");
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            putValue(resultNode, entry.getKey(), entry.getValue());
        }

        emitter.send(SseEmitter.event().name("tool_result").data(toJsonString(rootNode)));
    }

    /**
     * 发送 RAG 来源事件
     */
    private void sendRagEvent(SseEmitter emitter, String query, List<WebSearchService.SearchResult> sources) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("query", query);

        ArrayNode sourcesArray = rootNode.putArray("sources");
        for (int i = 0; i < sources.size(); i++) {
            WebSearchService.SearchResult source = sources.get(i);
            String cleanContent = cleanHtml(source.content());
            if (cleanContent.length() > 200) {
                cleanContent = cleanContent.substring(0, 200) + "...";
            }

            ObjectNode sourceNode = sourcesArray.addObject();
            sourceNode.put("title", source.title());
            sourceNode.put("url", source.url());
            sourceNode.put("snippet", cleanContent);
            sourceNode.put("relevance", 0.9 - (i * 0.1));
        }

        emitter.send(SseEmitter.event().name("rag").data(toJsonString(rootNode)));
    }

    private void sendStep(SseEmitter emitter, int currentStep, int totalSteps, String stepName, String status, String description, Long executionTime) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("currentStep", currentStep);
        rootNode.put("totalSteps", totalSteps);
        rootNode.put("stepName", stepName);
        rootNode.put("status", status);
        rootNode.put("description", description);

        if (executionTime != null) {
            rootNode.put("executionTime", executionTime);
        }

        emitter.send(SseEmitter.event().name("step").data(toJsonString(rootNode)));
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

    private String toJsonString(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            return "{}";
        }
    }

    private void putValue(ObjectNode node, String key, Object value) {
        if (value instanceof String) {
            node.put(key, (String) value);
        } else if (value instanceof Number) {
            if (value instanceof Integer) {
                node.put(key, ((Number) value).intValue());
            } else if (value instanceof Long) {
                node.put(key, ((Number) value).longValue());
            } else {
                node.put(key, ((Number) value).doubleValue());
            }
        } else if (value instanceof Boolean) {
            node.put(key, (Boolean) value);
        } else if (value != null) {
            node.put(key, value.toString());
        }
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
