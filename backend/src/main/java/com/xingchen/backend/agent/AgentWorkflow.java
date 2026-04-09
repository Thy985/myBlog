package com.xingchen.backend.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 工作流引擎
 *
 * 支持多步骤任务规划和执行
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentWorkflow {

    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<ParsingStrategy> STRATEGIES = List.of(
            new JsonParsingStrategy(),
            new ChineseStepStrategy(),
            new NumberDotStrategy(),
            new NumberBracketStrategy(),
            new MarkdownListStrategy()
    );

    /**
     * 规划任务步骤
     */
    public List<TaskStep> planSteps(String task, AgentState state) {
        String prompt = buildPlanningPrompt(task, state);
        String response = aiService.chat(prompt);

        log.debug("LLM 步骤规划响应:\n{}", response);

        return parseSteps(response);
    }

    /**
     * 增强的步骤解析 - 支持多种格式
     */
    private List<TaskStep> parseSteps(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("步骤解析失败: 响应为空");
            return Collections.emptyList();
        }

        String cleanedResponse = response.trim();

        for (ParsingStrategy strategy : STRATEGIES) {
            try {
                List<TaskStep> steps = strategy.parse(cleanedResponse);
                if (steps != null && !steps.isEmpty()) {
                    log.info("使用解析策略 {} 成功解析出 {} 个步骤", strategy.getName(), steps.size());
                    return steps;
                }
            } catch (Exception e) {
                log.debug("策略 {} 解析失败: {}", strategy.getName(), e.getMessage());
            }
        }

        log.warn("所有解析策略均失败，返回空列表。响应内容: {}",
                cleanedResponse.length() > 200 ? cleanedResponse.substring(0, 200) + "..." : cleanedResponse);
        return Collections.emptyList();
    }

    /**
     * 执行单个步骤
     */
    public StepResult executeStep(TaskStep step, AgentState state) {
        log.info("执行步骤 {}: {}", step.getIndex(), step.getDescription());

        try {
            String prompt = buildExecutionPrompt(step, state);
            String result = aiService.chat(prompt);

            return new StepResult(true, result, null);
        } catch (Exception e) {
            log.error("步骤执行失败: {}", step.getIndex(), e);
            return new StepResult(false, null, e.getMessage());
        }
    }

    /**
     * 反思执行结果 - 结构化版本
     */
    public boolean reflect(AgentState state) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String prompt = buildReflectionPrompt(state);
                String response = aiService.chat(prompt);

                log.debug("反思结果 (尝试 {}/{}): {}", attempt, maxRetries, response);

                ReflectionResult result = parseReflectionResult(response);

                if (result != null) {
                    if (result.isCompleted()) {
                        log.info("任务反思完成: 成功");
                        return true;
                    } else {
                        log.info("任务反思: 需要继续 - {}", result.getReason());
                        return false;
                    }
                }
            } catch (Exception e) {
                log.warn("反思解析失败 (尝试 {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.warn("反思机制失效，降级到简单判断");
        return fallbackReflect(state);
    }

    /**
     * 降级反思方法
     */
    private boolean fallbackReflect(AgentState state) {
        String prompt = buildReflectionPrompt(state);
        String response = aiService.chat(prompt);
        return response.contains("成功") || response.contains("完成");
    }

    /**
     * 解析反思结果
     */
    private ReflectionResult parseReflectionResult(String response) {
        try {
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(jsonStr);

            boolean completed = false;
            String reason = "";

            if (root.has("completed")) {
                completed = root.get("completed").asBoolean();
            } else if (root.has("success")) {
                completed = root.get("success").asBoolean();
            } else if (root.has("is_completed")) {
                completed = root.get("is_completed").asBoolean();
            }

            if (root.has("reason")) {
                reason = root.get("reason").asText();
            } else if (root.has("message")) {
                reason = root.get("message").asText();
            } else if (root.has("next_steps")) {
                reason = root.get("next_steps").asText();
            }

            if (root.has("needs_continue") && root.get("needs_continue").asBoolean()) {
                completed = false;
                if (reason.isEmpty()) {
                    reason = "LLM 判断需要继续执行";
                }
            }

            return new ReflectionResult(completed, reason);
        } catch (Exception e) {
            log.debug("反思结果 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        start = response.indexOf("[");
        end = response.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return null;
    }

    /**
     * 反思结果
     */
    private record ReflectionResult(boolean completed, String reason) {
        public boolean isCompleted() {
            return completed;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 构建规划提示词
     */
    private String buildPlanningPrompt(String task, AgentState state) {
        return String.format("""
            你是一个任务规划专家。请将以下复杂任务分解为可执行的步骤。

            任务：%s

            要求：
            1. 将任务分解为 3-7 个清晰的步骤
            2. 每个步骤要具体、可执行
            3. 步骤之间要有逻辑顺序
            4. 输出格式：每行一个步骤，格式为 "步骤N: 描述"

            请输出步骤规划：
            """, task);
    }

    /**
     * 构建执行提示词
     */
    private String buildExecutionPrompt(TaskStep step, AgentState state) {
        String context = state.getHistoryAsText(3);

        return String.format("""
            你是一个任务执行专家。请执行以下步骤。

            当前步骤：步骤 %d / %d
            步骤描述：%s

            任务上下文：
            %s

            之前步骤的结果：
            %s

            请执行当前步骤并输出结果。
            """,
                step.getIndex(),
                step.getTotal(),
                step.getDescription(),
                state.getCurrentTask(),
                context,
                formatIntermediateResults(state)
        );
    }

    /**
     * 构建反思提示词
     */
    private String buildReflectionPrompt(AgentState state) {
        return String.format("""
            你是一个任务反思专家。请评估任务执行结果。

            任务：%s

            执行历史：
            %s

            请评估并以 JSON 格式输出结果：
            {
                "completed": true/false,  // 任务是否完成
                "success": true/false,    // 任务是否成功
                "reason": "原因说明",      // 判断原因
                "needs_continue": true/false,  // 是否需要继续
                "next_steps": "后续步骤建议"  // 建议的后续步骤
            }

            请输出 JSON 格式的评估结果：
            """,
                state.getCurrentTask(),
                state.getHistoryAsText(5)
        );
    }

    private String formatIntermediateResults(AgentState state) {
        if (state.getIntermediateResults().isEmpty()) {
            return "无";
        }

        StringBuilder sb = new StringBuilder();
        state.getIntermediateResults().forEach((key, value) -> {
            sb.append(key).append(": ").append(value).append("\n");
        });
        return sb.toString();
    }

    /**
     * 解析策略接口
     */
    private interface ParsingStrategy {
        String getName();
        List<TaskStep> parse(String response) throws Exception;
    }

    /**
     * JSON 解析策略
     */
    private static class JsonParsingStrategy implements ParsingStrategy {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public String getName() {
            return "JSON";
        }

        @Override
        public List<TaskStep> parse(String response) throws Exception {
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                return null;
            }

            JsonNode root = mapper.readTree(jsonStr);
            JsonNode stepsNode = findStepsNode(root);

            if (stepsNode != null && stepsNode.isArray()) {
                List<TaskStep> steps = new ArrayList<>();
                int index = 1;
                for (JsonNode stepNode : stepsNode) {
                    String description = extractDescription(stepNode);
                    if (description != null && !description.isEmpty()) {
                        steps.add(new TaskStep(index, description, 0));
                        index++;
                    }
                }

                if (!steps.isEmpty()) {
                    steps.forEach(s -> s.total = steps.size());
                    return steps;
                }
            }

            return null;
        }

        private String extractJson(String response) {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start >= 0 && end > start) {
                return response.substring(start, end + 1);
            }
            start = response.indexOf("[");
            end = response.lastIndexOf("]");
            if (start >= 0 && end > start) {
                return response.substring(start, end + 1);
            }
            return null;
        }

        private JsonNode findStepsNode(JsonNode root) {
            String[] keys = {"steps", "taskSteps", "items", "data", "result", "plan"};
            for (String key : keys) {
                if (root.has(key)) {
                    return root.get(key);
                }
            }
            if (root.isArray()) {
                return root;
            }
            return null;
        }

        private String extractDescription(JsonNode stepNode) {
            String[] descKeys = {"description", "desc", "title", "step", "name", "content", "text"};
            for (String key : descKeys) {
                if (stepNode.has(key)) {
                    return stepNode.get(key).asText().trim();
                }
            }
            if (stepNode.isTextual()) {
                return stepNode.asText().trim();
            }
            return null;
        }
    }

    /**
     * 中文步骤格式解析策略 - "步骤N: 描述"
     */
    private static class ChineseStepStrategy implements ParsingStrategy {
        private static final Pattern PATTERN = Pattern.compile("^步骤\\s*(\\d+)\\s*[:：]\\s*(.+)$");

        @Override
        public String getName() {
            return "中文步骤格式";
        }

        @Override
        public List<TaskStep> parse(String response) throws Exception {
            List<TaskStep> steps = new ArrayList<>();
            String[] lines = response.split("\n");

            for (String line : lines) {
                Matcher matcher = PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1));
                    String description = matcher.group(2).trim();
                    steps.add(new TaskStep(index, description, 0));
                }
            }

            if (!steps.isEmpty()) {
                steps.sort(Comparator.comparingInt(TaskStep::getIndex));
                steps.forEach(s -> s.total = steps.size());
                return steps;
            }
            return null;
        }
    }

    /**
     * 数字点号格式解析策略 - "1. 描述"
     */
    private static class NumberDotStrategy implements ParsingStrategy {
        private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\s*[\\.、]\\s*(.+)$");

        @Override
        public String getName() {
            return "数字点号格式";
        }

        @Override
        public List<TaskStep> parse(String response) throws Exception {
            List<TaskStep> steps = new ArrayList<>();
            String[] lines = response.split("\n");

            for (String line : lines) {
                Matcher matcher = PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1));
                    String description = matcher.group(2).trim();
                    steps.add(new TaskStep(index, description, 0));
                }
            }

            if (!steps.isEmpty()) {
                steps.sort(Comparator.comparingInt(TaskStep::getIndex));
                steps.forEach(s -> s.total = steps.size());
                return steps;
            }
            return null;
        }
    }

    /**
     * 数字方括号格式解析策略 - "[1] 描述"
     */
    private static class NumberBracketStrategy implements ParsingStrategy {
        private static final Pattern PATTERN = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\s*(.+)$");

        @Override
        public String getName() {
            return "数字方括号格式";
        }

        @Override
        public List<TaskStep> parse(String response) throws Exception {
            List<TaskStep> steps = new ArrayList<>();
            String[] lines = response.split("\n");

            for (String line : lines) {
                Matcher matcher = PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1));
                    String description = matcher.group(2).trim();
                    steps.add(new TaskStep(index, description, 0));
                }
            }

            if (!steps.isEmpty()) {
                steps.sort(Comparator.comparingInt(TaskStep::getIndex));
                steps.forEach(s -> s.total = steps.size());
                return steps;
            }
            return null;
        }
    }

    /**
     * Markdown 列表格式解析策略 - "- 描述" 或 "* 描述"
     */
    private static class MarkdownListStrategy implements ParsingStrategy {
        private static final Pattern PATTERN = Pattern.compile("^[-*]\\s+(.+)$");

        @Override
        public String getName() {
            return "Markdown列表格式";
        }

        @Override
        public List<TaskStep> parse(String response) throws Exception {
            List<TaskStep> steps = new ArrayList<>();
            String[] lines = response.split("\n");

            int index = 1;
            for (String line : lines) {
                Matcher matcher = PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    String description = matcher.group(1).trim();
                    if (!description.isEmpty()) {
                        steps.add(new TaskStep(index, description, 0));
                        index++;
                    }
                }
            }

            if (!steps.isEmpty()) {
                steps.forEach(s -> s.total = steps.size());
                return steps;
            }
            return null;
        }
    }

    /**
     * 任务步骤
     */
    public static class TaskStep {
        private int index;
        private String description;
        private int total;

        public TaskStep(int index, String description, int total) {
            this.index = index;
            this.description = description;
            this.total = total;
        }

        public int getIndex() {
            return index;
        }

        public String getDescription() {
            return description;
        }

        public int getTotal() {
            return total;
        }
    }

    /**
     * 步骤执行结果
     */
    public record StepResult(boolean success, String result, String error) {}
}
