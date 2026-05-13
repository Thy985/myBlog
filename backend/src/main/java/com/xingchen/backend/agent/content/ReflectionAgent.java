package com.xingchen.backend.agent.content;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ReflectionAgent extends BaseAgent {

    private final LLMProvider llmProvider;
    private static final Logger AGENT_LOG = org.slf4j.LoggerFactory.getLogger(ReflectionAgent.class);

    public static final String REFLECTION_TYPE_SUCCESS = "success";
    public static final String REFLECTION_TYPE_FAILURE = "failure";
    public static final String REFLECTION_TYPE_IMPROVEMENT = "improvement";

    public ReflectionAgent(@Qualifier("agentLLMProvider") LLMProvider llmProvider) {
        super("ReflectionAgent", Map.of("maxTokens", 3000, "temperature", 0.5));
        this.llmProvider = llmProvider;
    }

    @Override
    protected AgentResult executeImpl(AgentContext context) throws AgentException {
        String taskType = (String) context.getParam("taskType", "content_generation");
        String taskResult = (String) context.getParam("taskResult");
        Boolean success = (Boolean) context.getParam("success", true);
        List<Map<String, Object>> history = (List<Map<String, Object>>) context.getParam("history");

        if (taskResult == null || taskResult.isBlank()) {
            taskResult = "No result provided";
        }

        ReflectionAnalysis analysis = performReflection(taskType, taskResult, success, history);

        Map<String, Object> data = Map.of(
                "taskType", taskType,
                "success", success,
                "summary", analysis.summary(),
                "successFactors", analysis.successFactors(),
                "failureReasons", analysis.failureReasons(),
                "improvements", analysis.improvements(),
                "recommendations", analysis.recommendations(),
                "confidence", analysis.confidence()
        );

        return AgentResult.success(data);
    }

    private ReflectionAnalysis performReflection(String taskType, String taskResult,
                                                 boolean success, List<Map<String, Object>> history) {
        String prompt = buildReflectionPrompt(taskType, taskResult, success, history);

        try {
            AIRequest request = AIRequest.builder()
                    .userId(0L)
                    .message(prompt)
                    .stream(false)
                    .build();

            AIResponse response = llmProvider.chat(request);

            if (response != null) {
                if (!response.isSuccess()) {
                    AGENT_LOG.warn("AI returned error: {}", response.getContent());
                    return new ReflectionAnalysis(
                            "反思分析失败",
                            List.of(),
                            List.of("AI返回错误: " + response.getContent()),
                            List.of("建议检查AI配置"),
                            List.of("建议稍后重试"),
                            0.0
                    );
                }
                if (response.getContent() != null && !response.getContent().isBlank()) {
                    return parseReflectionResponse(response.getContent(), success);
                }
            }
        } catch (Exception e) {
            AGENT_LOG.error("Reflection analysis failed", e);
        }

        return new ReflectionAnalysis(
                "反思分析完成",
                List.of(),
                success ? List.of("任务执行完成") : List.of("任务执行失败"),
                List.of("建议人工检查执行结果"),
                List.of("建议记录执行过程以便后续分析"),
                0.5
        );
    }

    private String buildReflectionPrompt(String taskType, String taskResult,
                                         boolean success, List<Map<String, Object>> history) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一位专业的AI执行反思分析师。你的任务是分析AI执行结果，\n");
        prompt.append("识别成功因素和失败原因，并提出改进建议。\n\n");

        prompt.append("【任务类型】\n").append(taskType).append("\n\n");

        prompt.append("【执行结果】\n").append(taskResult).append("\n\n");

        prompt.append("【执行状态】\n").append(success ? "成功" : "失败").append("\n\n");

        if (history != null && !history.isEmpty()) {
            prompt.append("【历史执行记录】\n");
            for (int i = 0; i < Math.min(history.size(), 5); i++) {
                Map<String, Object> item = history.get(i);
                prompt.append(String.format("- 任务%d: %s, 成功=%s\n",
                        i + 1,
                        item.get("taskType"),
                        item.get("success")));
            }
            prompt.append("\n");
        }

        prompt.append("""
                请分析上述执行结果，返回JSON格式的反思报告：

                {
                    "summary": "简要总结（50字以内）",
                    "successFactors": ["成功因素1", "成功因素2"],
                    "failureReasons": ["失败原因1", "失败原因2"],
                    "improvements": ["改进建议1", "改进建议2"],
                    "recommendations": ["后续行动建议1", "后续行动建议2"],
                    "confidence": 0.85
                }

                注意：
                - successFactors 在成功时应该详细，失败时应该为空
                - failureReasons 在失败时应该详细，成功时应该为空
                - confidence 表示对分析结果的置信度（0-1）
                """);

        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private ReflectionAnalysis parseReflectionResponse(String content, boolean success) {
        try {
            int jsonStart = content.indexOf('{');
            int jsonEnd = content.lastIndexOf('}');

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(jsonStr, Map.class);

                String summary = (String) parsed.getOrDefault("summary", "");
                List<String> successFactors = parseStringList(parsed.get("successFactors"));
                List<String> failureReasons = parseStringList(parsed.get("failureReasons"));
                List<String> improvements = parseStringList(parsed.get("improvements"));
                List<String> recommendations = parseStringList(parsed.get("recommendations"));
                double confidence = parseDouble(parsed.get("confidence"), 0.8);

                return new ReflectionAnalysis(summary, successFactors, failureReasons,
                        improvements, recommendations, confidence);
            }
        } catch (Exception e) {
            AGENT_LOG.debug("解析反思结果失败: {}", e.getMessage());
        }

        return new ReflectionAnalysis(
                "反思分析完成",
                success ? List.of("任务执行完成") : List.of("任务执行失败"),
                success ? List.of() : List.of("需要进一步分析"),
                List.of("建议检查执行日志"),
                List.of("建议记录更多执行细节"),
                0.5
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object obj) {
        if (obj instanceof List) {
            return new ArrayList<>((List<String>) obj);
        }
        return List.of();
    }

    private double parseDouble(Object obj, double defaultValue) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public record ReflectionAnalysis(
            String summary,
            List<String> successFactors,
            List<String> failureReasons,
            List<String> improvements,
            List<String> recommendations,
            double confidence
    ) {}
}
