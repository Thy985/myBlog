package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.ai.gateway.AIGateway;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AI智能体V2控制器
 * 新的AI接口，使用重构后的架构
 */
@RestController
@RequestMapping("/api/v2/ai")
@RequiredArgsConstructor
@Slf4j
public class AIV2Controller {
    
    private final AIGateway aiGateway;
    
    /**
     * 普通对话
     */
    @SaCheckLogin
    @PostMapping("/chat")
    public Result<AIResponse> chat(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        AIRequest aiRequest = AIRequest.builder()
                .userId(userId)
                .sessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString())
                .message(request.message())
                .preferredModel(request.model())
                .systemPrompt(request.systemPrompt())
                .history(request.history())
                .extraParams(request.extraParams())
                .build();

        AIResponse response = aiGateway.process(aiRequest);

        if (response.isSuccess()) {
            return Result.success(response);
        } else {
            return Result.error(500, response.getContent());
        }
    }
    
    /**
     * 流式对话
     */
    @SaCheckLogin
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(5));

        AIRequest aiRequest = AIRequest.builder()
                .userId(userId)
                .sessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString())
                .message(request.message())
                .preferredModel(request.model())
                .systemPrompt(request.systemPrompt())
                .history(request.history())
                .stream(true)
                .extraParams(request.extraParams())
                .build();
        
        aiGateway.processStream(aiRequest, response -> {
            try {
                if (response.getType() == AIResponse.ResponseType.STREAM_END) {
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data(response));
                    emitter.complete();
                } else if (response.getType() == AIResponse.ResponseType.ERROR) {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(response));
                    emitter.completeWithError(new RuntimeException(response.getContent()));
                } else {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(response));
                }
            } catch (IOException e) {
                log.error("SSE发送失败", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * 获取可用模型列表
     */
    @SaCheckLogin
    @GetMapping("/models")
    public Result<List<String>> getModels() {
        return Result.success(List.of(
                "openai/gpt-4",
                "openai/gpt-4-turbo",
                "openai/gpt-3.5-turbo",
                "anthropic/claude-3-opus",
                "anthropic/claude-3-sonnet"
        ));
    }
    
    /**
     * 请求DTO
     */
    public record ChatRequest(
            String message,
            String sessionId,
            String model,
            String systemPrompt,
            List<Map<String, String>> history,
            Map<String, Object> extraParams
    ) {}
}