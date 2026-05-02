package com.xingchen.backend.agent.react;

import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.memory.UnifiedMemoryService;
import com.xingchen.backend.ai.memory.UnifiedMemoryContext;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.tool.AIToolRegistry;
import com.xingchen.backend.ai.tool.Tool;
import com.xingchen.backend.service.KnowledgeBaseService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

@Service
@Slf4j
public class ReActAgent {

    private final LLMProvider llmProvider;
    private final AIToolRegistry toolRegistry;
    private final UnifiedMemoryService memoryService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;

    public ReActAgent(
            @Qualifier("systemDefaultProvider") LLMProvider llmProvider,
            AIToolRegistry toolRegistry,
            UnifiedMemoryService memoryService,
            KnowledgeBaseService knowledgeBaseService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.memoryService = memoryService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistry = meterRegistry;
    }

    private static final int DEFAULT_MAX_ITERATIONS = 10;
    private static final int DEFAULT_MAX_TOKENS = 4096;

    public AIResponse execute(ReActRequest request) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);

        ReActState state = ReActState.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .maxIterations(request.getMaxIterations() > 0 ? request.getMaxIterations() : DEFAULT_MAX_ITERATIONS)
                .startedAt(Instant.now())
                .build();

        try {
            log.info("开始 ReAct 执行: userId={}, sessionId={}, message={}",
                    request.getUserId(), request.getSessionId(),
                    request.getMessage().substring(0, Math.min(50, request.getMessage().length())));

            while (state.hasMoreSteps()) {
                ReActState.ReActStep step = executeNextStep(state, request);
                state.addStep(step);

                if (state.isTerminated()) {
                    break;
                }

                if (step.getResult() != null && step.getResult().isSuccess()) {
                    Object observation = step.getResult().getData();
                    if (observation instanceof String) {
                        step.setObservation((String) observation);
                    } else if (observation != null) {
                        step.setObservation(observation.toString());
                    }
                }

                log.debug("ReAct 步骤 {} 完成: type={}, action={}",
                        state.getCurrentStep(), step.getType(),
                        step.getAction() != null ? step.getAction().getToolName() : "none");
            }

            if (!state.isTerminated()) {
                state.terminate(ReActState.TerminationReason.MAX_ITERATIONS_REACHED,
                        "达到最大迭代次数 " + state.getMaxIterations() + "，未能生成最终答案");
            }

            saveExecutionTrace(state, request);

            long elapsed = System.currentTimeMillis() - startTime;
            sample.stop(meterRegistry.timer("react.agent.execution",
                    "status", state.isTerminated() ? "success" : "max_iterations"));

            log.info("ReAct 执行完成: steps={}, elapsed={}ms, termination={}",
                    state.getSteps().size(), elapsed, state.getTerminationReason());

            return buildResponse(state, elapsed);

        } catch (Exception e) {
            log.error("ReAct 执行失败", e);
            sample.stop(meterRegistry.timer("react.agent.execution", "status", "error"));
            return AIResponse.error("ReAct 执行失败: " + e.getMessage());
        }
    }

    public void executeStream(ReActRequest request, Consumer<AIResponse> onChunk) {
        ReActState state = ReActState.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .maxIterations(request.getMaxIterations() > 0 ? request.getMaxIterations() : DEFAULT_MAX_ITERATIONS)
                .startedAt(Instant.now())
                .build();

        try {
            onChunk.accept(AIResponse.chunk("[开始 ReAct 推理]\n\n"));

            while (state.hasMoreSteps()) {
                StringBuilder stepOutput = new StringBuilder();

                ReActState.ReActStep step = executeNextStep(state, request);
                state.addStep(step);

                stepOutput.append("【步骤 ").append(step.getStepNumber()).append("】\n\n");

                if (step.getThought() != null) {
                    stepOutput.append("思考: ").append(step.getThought()).append("\n\n");
                }

                if (step.getAction() != null) {
                    stepOutput.append("行动: ").append(step.getAction().getToolName()).append("\n");
                    if (step.getAction().getParameters() != null && !step.getAction().getParameters().isEmpty()) {
                        stepOutput.append("参数: ").append(step.getAction().getParameters()).append("\n");
                    }
                    stepOutput.append("\n");
                }

                if (step.getResult() != null) {
                    stepOutput.append("结果: ").append(step.getResult().getMessage()).append("\n\n");
                    if (step.getResult().getData() != null) {
                        String dataStr = step.getResult().getData().toString();
                        if (dataStr.length() > 500) {
                            dataStr = dataStr.substring(0, 500) + "...(省略)";
                        }
                        stepOutput.append("返回: ").append(dataStr).append("\n\n");
                    }
                }

                onChunk.accept(AIResponse.chunk(stepOutput.toString()));

                if (state.isTerminated()) {
                    break;
                }

                if (step.getResult() != null && step.getResult().isSuccess()) {
                    Object observation = step.getResult().getData();
                    step.setObservation(observation instanceof String ? (String) observation : observation.toString());
                }
            }

            if (!state.isTerminated()) {
                state.terminate(ReActState.TerminationReason.MAX_ITERATIONS_REACHED,
                        "达到最大迭代次数");
            }

            StringBuilder finalOutput = new StringBuilder();
            finalOutput.append("\n").append("═".repeat(50)).append("\n\n");
            finalOutput.append("推理完成！\n");
            finalOutput.append("总步骤: ").append(state.getSteps().size()).append("\n");
            finalOutput.append("终止原因: ").append(state.getTerminationReason()).append("\n\n");
            finalOutput.append("最终答案:\n").append(state.getFinalAnswer());

            onChunk.accept(AIResponse.chunk(finalOutput.toString()));
            onChunk.accept(AIResponse.builder().type(AIResponse.ResponseType.STREAM_END).build());

            saveExecutionTrace(state, request);

        } catch (Exception e) {
            log.error("ReAct 流式执行失败", e);
            onChunk.accept(AIResponse.error("推理失败: " + e.getMessage()));
        }
    }

    private ReActState.ReActStep executeNextStep(ReActState state, ReActRequest request) {
        long stepStart = System.currentTimeMillis();

        ReActState.ReActStep.ReActStepBuilder stepBuilder = ReActState.ReActStep.builder()
                .stepNumber(state.getCurrentStep() + 1)
                .timestamp(Instant.now());

        try {
            ReActPrompt prompt = buildPrompt(state, request);

            AIRequest llmRequest = AIRequest.builder()
                    .userId(request.getUserId())
                    .sessionId(request.getSessionId())
                    .message(prompt.buildUserMessage())
                    .systemPrompt(prompt.buildSystemPrompt())
                    .history(request.getHistory())
                    .stream(false)
                    .extraParams(Map.of("temperature", 0.7, "max_tokens", DEFAULT_MAX_TOKENS))
                    .build();

            AIResponse llmResponse = llmProvider.chat(llmRequest);

            if (!llmResponse.isSuccess()) {
                return stepBuilder
                        .type(ReActState.ReActStep.StepType.REASONING)
                        .success(false)
                        .errorMessage(llmResponse.getContent())
                        .executionTimeMs(System.currentTimeMillis() - stepStart)
                        .build();
            }

            String responseContent = llmResponse.getContent();
            if (responseContent == null || responseContent.isBlank()) {
                return stepBuilder
                        .type(ReActState.ReActStep.StepType.REASONING)
                        .thought("")
                        .success(false)
                        .errorMessage("LLM 返回为空")
                        .executionTimeMs(System.currentTimeMillis() - stepStart)
                        .build();
            }

            ReActParser parser = new ReActParser();
            ReActParser.ParsedReActResponse parsed = parser.parse(responseContent);

            stepBuilder.thought(parsed.getThought())
                      .type(ReActState.ReActStep.StepType.REASONING);

            if (parsed.isFinalAnswer()) {
                state.terminate(ReActState.TerminationReason.FINAL_ANSWER_GENERATED, parsed.getFinalAnswer());
                return stepBuilder
                        .type(ReActState.ReActStep.StepType.FINAL_ANSWER)
                        .success(true)
                        .executionTimeMs(System.currentTimeMillis() - stepStart)
                        .build();
            }

            if (parsed.getActionName() != null) {
                ReActState.Action action = ReActState.Action.builder()
                        .toolName(parsed.getActionName())
                        .parameters(parsed.getActionParameters() != null ? parsed.getActionParameters() : Map.of())
                        .rawResponse(parsed.getRawResponse())
                        .build();
                stepBuilder.type(ReActState.ReActStep.StepType.ACTION)
                           .action(action);

                ReActState.ActionResult actionResult = executeTool(action, request);
                stepBuilder.result(actionResult)
                           .success(actionResult.isSuccess())
                           .observation(actionResult.getMessage());

                if (!actionResult.isSuccess()) {
                    log.warn("工具执行失败: tool={}, error={}",
                            action.getToolName(), actionResult.getMessage());
                }
            } else {
                stepBuilder.success(true)
                          .observation("未检测到工具调用，请继续推理");
            }

            stepBuilder.executionTimeMs(System.currentTimeMillis() - stepStart);

            return stepBuilder.build();

        } catch (Exception e) {
            log.error("步骤执行异常", e);
            state.terminate(ReActState.TerminationReason.LLM_ERROR, null);
            return stepBuilder
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - stepStart)
                    .build();
        }
    }

    private ReActState.ActionResult executeTool(ReActState.Action action, ReActRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            String toolName = action.getToolName();
            Map<String, Object> params = action.getParameters();

            Tool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                return ReActState.ActionResult.builder()
                        .success(false)
                        .message("工具不存在: " + toolName)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            if (params == null) {
                params = new HashMap<>();
            }
            params.put("userId", request.getUserId());
            params.put("sessionId", request.getSessionId());

            Tool.ToolResult result = tool.execute(params);

            return ReActState.ActionResult.builder()
                    .success(result.success())
                    .data(result.data())
                    .message(result.success() ? "工具执行成功" : result.message())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("工具执行异常: tool={}", action.getToolName(), e);
            return ReActState.ActionResult.builder()
                    .success(false)
                    .message("工具执行异常: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ReActPrompt buildPrompt(ReActState state, ReActRequest request) {
        List<ReActState.ReActStep> previousSteps = state.getCurrentStep() > 0 ? state.getSteps() : List.of();

        String memoryContext = null;
        if (request.isUseMemory() && request.getUserId() != null) {
            UnifiedMemoryService.MemoryRequirements requirements = new UnifiedMemoryService.MemoryRequirements(
                    true, true, false, request.getMessage(), 10);
            UnifiedMemoryContext context = memoryService.loadContext(
                    request.getUserId(), request.getSessionId(), requirements);
            if (context != null && !context.isEmpty()) {
                memoryContext = context.toPromptText();
            }
        }

        String ragContext = null;
        if (request.isUseRag()) {
            ragContext = knowledgeBaseService.search(request.getMessage(), 5);
        }

        return ReActPrompt.builder()
                .userMessage(request.getMessage())
                .availableTools(request.getTools() != null ? request.getTools() : toolRegistry.getAvailableTools())
                .previousSteps(previousSteps)
                .memoryContext(memoryContext)
                .ragContext(ragContext)
                .chatHistory(buildChatHistory(request.getHistory()))
                .currentStep(state.getCurrentStep() + 1)
                .maxSteps(state.getMaxIterations())
                .build();
    }

    private String buildChatHistory(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : history) {
            String role = msg.getOrDefault("role", "user");
            String content = msg.getOrDefault("content", "");
            sb.append(role).append(": ").append(content).append("\n\n");
        }
        return sb.toString();
    }

    private void saveExecutionTrace(ReActState state, ReActRequest request) {
        if (request.getUserId() == null) {
            return;
        }

        try {
            String trace = state.toDetailedTrace();

            UnifiedMemoryContext.MemoryItem traceItem = UnifiedMemoryContext.MemoryItem.builder()
                    .id(UUID.randomUUID().toString())
                    .role("system")
                    .content("ReAct执行轨迹: " + trace)
                    .timestamp(System.currentTimeMillis())
                    .importance(0.3)
                    .type(UnifiedMemoryContext.MemoryType.CONVERSATION)
                    .category("REACT_TRACE")
                    .build();

            memoryService.addWorkingMemory(request.getUserId(), request.getSessionId(), traceItem);

            log.debug("保存 ReAct 执行轨迹到记忆");
        } catch (Exception e) {
            log.warn("保存执行轨迹失败", e);
        }
    }

    private AIResponse buildResponse(ReActState state, long elapsed) {
        AIResponse.AIResponseBuilder builder = AIResponse.builder()
                .executionTime(elapsed)
                .timestamp(java.time.LocalDateTime.now());

        if (state.isTerminated() && state.getFinalAnswer() != null) {
            builder.type(AIResponse.ResponseType.SUCCESS)
                   .content(state.getFinalAnswer());
        } else {
            builder.type(AIResponse.ResponseType.ERROR)
                   .content(state.getFinalAnswer() != null ? state.getFinalAnswer() : "执行未完成");
        }

        List<AIResponse.ToolCallRecord> toolCallRecords = new ArrayList<>();
        for (ReActState.ReActStep step : state.getSteps()) {
            if (step.getAction() != null) {
                AIResponse.ToolCallRecord record = AIResponse.ToolCallRecord.builder()
                        .toolName(step.getAction().getToolName())
                        .parameters(step.getAction().getParameters())
                        .result(step.getResult() != null ? step.getResult().getMessage() : null)
                        .executionTime(step.getExecutionTimeMs())
                        .build();
                toolCallRecords.add(record);
            }
        }
        builder.toolCalls(toolCallRecords);

        builder.extraData(Map.of(
                "reactSteps", state.getSteps().size(),
                "terminationReason", state.getTerminationReason() != null ? state.getTerminationReason().name() : "NONE",
                "trace", state.toDetailedTrace()
        ));

        return builder.build();
    }
}
