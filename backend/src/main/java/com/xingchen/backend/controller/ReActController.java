package com.xingchen.backend.controller;

import com.xingchen.backend.agent.react.ReActAgent;
import com.xingchen.backend.agent.react.ReActAgentWithFunctionCalling;
import com.xingchen.backend.agent.react.ReActProperties;
import com.xingchen.backend.agent.react.ReActRequest;
import com.xingchen.backend.agent.react.ReActState;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.tool.AIToolRegistry;
import com.xingchen.backend.ai.tool.Tool;
import com.xingchen.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai/react")
@RequiredArgsConstructor
@Slf4j
public class ReActController {

    private final ReActAgent reactAgent;
    private final ReActAgentWithFunctionCalling reactAgentWithFunctionCalling;
    private final AIToolRegistry toolRegistry;
    private final ReActProperties reactProperties;

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @PostMapping("/chat")
    public Result<AIResponse> chat(
            @RequestParam Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestBody Map<String, Object> request) {

        String message = (String) request.get("message");
        if (message == null || message.isBlank()) {
            return Result.error(400, "消息内容不能为空");
        }

        String sid = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString();

        Boolean useMemory = request.get("useMemory") != null
                ? (Boolean) request.get("useMemory")
                : reactProperties.isUseMemory();

        Boolean useRag = request.get("useRag") != null
                ? (Boolean) request.get("useRag")
                : reactProperties.isUseRag();

        Integer maxIterations = request.get("maxIterations") != null
                ? (Integer) request.get("maxIterations")
                : reactProperties.getMaxIterations();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = request.get("history") != null
                ? (List<Map<String, String>>) request.get("history")
                : List.of();

        ReActRequest reactRequest = ReActRequest.builder()
                .userId(userId)
                .sessionId(sid)
                .message(message)
                .history(history)
                .useMemory(useMemory)
                .useRag(useRag)
                .maxIterations(maxIterations)
                .build();

        log.info("收到 ReAct 对话请求: userId={}, sessionId={}, message={}",
                userId, sid, message.substring(0, Math.min(50, message.length())));

        AIResponse response;
        if (reactProperties.isUseFunctionCalling()) {
            response = reactAgentWithFunctionCalling.execute(reactRequest);
        } else {
            response = reactAgent.execute(reactRequest);
        }

        return Result.success(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestBody Map<String, Object> request) {

        String message = (String) request.get("message");
        if (message == null || message.isBlank()) {
            return null;
        }

        String sid = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString();

        Boolean useMemory = request.get("useMemory") != null
                ? (Boolean) request.get("useMemory")
                : reactProperties.isUseMemory();

        Boolean useRag = request.get("useRag") != null
                ? (Boolean) request.get("useRag")
                : reactProperties.isUseRag();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = request.get("history") != null
                ? (List<Map<String, String>>) request.get("history")
                : List.of();

        SseEmitter emitter = new SseEmitter(300_000L);

        ReActRequest reactRequest = ReActRequest.builder()
                .userId(userId)
                .sessionId(sid)
                .message(message)
                .history(history)
                .useMemory(useMemory)
                .useRag(useRag)
                .maxIterations(reactProperties.getMaxIterations())
                .build();

        sseExecutor.execute(() -> {
            try {
                AIResponseConsumer consumer = new AIResponseConsumer(emitter);
                if (reactProperties.isUseFunctionCalling()) {
                    reactAgentWithFunctionCalling.executeStream(reactRequest, consumer);
                } else {
                    reactAgent.executeStream(reactRequest, consumer);
                }
            } catch (Exception e) {
                log.error("流式对话异常", e);
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.debug("SSE 连接完成"));
        emitter.onTimeout(() -> log.debug("SSE 连接超时"));
        emitter.onError(e -> log.debug("SSE 错误: {}", e.getMessage()));

        return emitter;
    }

    @GetMapping("/tools")
    public Result<List<Tool>> getTools() {
        return Result.success(toolRegistry.getAvailableTools());
    }

    @GetMapping("/trace/{sessionId}")
    public Result<String> getTrace(
            @PathVariable String sessionId,
            @RequestParam Long userId) {
        return Result.success("轨迹查询功能开发中");
    }

    @GetMapping("/status")
    public Result<ReActProperties> getStatus() {
        return Result.success(reactProperties);
    }

    @PostMapping("/config")
    public Result<Void> updateConfig(@RequestBody ReActProperties config) {
        if (config.getMaxIterations() > 0) {
            reactProperties.setMaxIterations(config.getMaxIterations());
        }
        if (config.getTemperature() > 0) {
            reactProperties.setTemperature(config.getTemperature());
        }
        if (config.getMaxTokens() > 0) {
            reactProperties.setMaxTokens(config.getMaxTokens());
        }
        reactProperties.setUseMemory(config.isUseMemory());
        reactProperties.setUseRag(config.isUseRag());
        reactProperties.setUseFunctionCalling(config.isUseFunctionCalling());
        return Result.success();
    }

    private static class AIResponseConsumer implements java.util.function.Consumer<AIResponse> {
        private final SseEmitter emitter;

        public AIResponseConsumer(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void accept(AIResponse response) {
            try {
                if (response.getType() == AIResponse.ResponseType.STREAM_END) {
                    emitter.complete();
                } else {
                    String data = response.getContent() != null ? response.getContent() : "";
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(data));
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }
}
