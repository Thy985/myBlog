package com.xingchen.backend.agent.react;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@Slf4j
public class ReActParser {

    private static final Pattern THOUGHT_PATTERN = Pattern.compile(
        "(?i)(?:思考|thought|reasoning)[:：]?\\s*(.+?)(?=\\n(?:行动|action)[:：]|\\n(?:最终答案|final answer)|$)",
        Pattern.DOTALL
    );

    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "(?i)(?:行动|action)[:：]?\\s*(\\w+)(?:\\(|（)(.*?)(?:\\)|）)?",
        Pattern.DOTALL
    );

    private static final Pattern ACTION_PARAMS_PATTERN = Pattern.compile(
        "(?i)(?:行动参数|action parameters?|parameters?)[:：]?\\s*(\\{.+?\\}|.+?)(?=\\n|$)",
        Pattern.DOTALL
    );

    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile(
        "(?i)(?:最终答案|final answer)[:：]?\\s*(.+?)(?=\\n\\s*(?:\\[|$))",
        Pattern.DOTALL
    );

    private static final Pattern JSON_PARAMS_PATTERN = Pattern.compile(
        "\\{[^{}]*\"(\\w+)\"\\s*:\\s*[^{}]*\\}",
        Pattern.DOTALL
    );

    public ParsedReActResponse parse(String llmResponse) {
        ParsedReActResponse.ParsedReActResponseBuilder builder = ParsedReActResponse.builder()
            .rawResponse(llmResponse);

        String thought = extractThought(llmResponse);
        builder.thought(thought);

        String finalAnswer = extractFinalAnswer(llmResponse);
        if (finalAnswer != null) {
            builder.isFinalAnswer(true)
                   .finalAnswer(finalAnswer);
            return builder.build();
        }

        builder.isFinalAnswer(false);

        ParsedReActResponse.ActionInfo actionInfo = extractAction(llmResponse);
        if (actionInfo != null) {
            builder.actionName(actionInfo.getActionName())
                   .actionParameters(actionInfo.getParameters());
        }

        return builder.build();
    }

    private String extractThought(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        Matcher matcher = THOUGHT_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int finalAnswerIndex = findFinalAnswerIndex(response);
        if (finalAnswerIndex > 0) {
            String beforeFinal = response.substring(0, finalAnswerIndex);
            String[] lines = beforeFinal.split("\n");
            StringBuilder thought = new StringBuilder();
            for (String line : lines) {
                if (!line.trim().startsWith("步骤") &&
                    !line.trim().startsWith("观察") &&
                    !line.trim().startsWith("行动") &&
                    !line.trim().startsWith("结果")) {
                    thought.append(line).append(" ");
                }
            }
            String result = thought.toString().trim();
            return result.isEmpty() ? null : result;
        }

        return null;
    }

    private String extractFinalAnswer(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        Matcher matcher = FINAL_ANSWER_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        if (response.contains("[FINAL ANSWER]") || response.contains("【最终答案】")) {
            String[] parts = response.split("(?:\\[FINAL ANSWER\\]|【最终答案】)");
            if (parts.length > 1) {
                return parts[parts.length - 1].trim();
            }
        }

        return null;
    }

    private int findFinalAnswerIndex(String response) {
        int index = response.indexOf("[FINAL ANSWER]");
        if (index == -1) {
            index = response.indexOf("【最终答案】");
        }
        if (index == -1) {
            index = response.indexOf("最终答案");
        }
        return index;
    }

    private ParsedReActResponse.ActionInfo extractAction(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        if (actionMatcher.find()) {
            String actionName = actionMatcher.group(1).trim();
            String paramsStr = actionMatcher.group(2);

            if (paramsStr == null || paramsStr.isBlank()) {
                Matcher paramsMatcher = ACTION_PARAMS_PATTERN.matcher(response);
                if (paramsMatcher.find()) {
                    paramsStr = paramsMatcher.group(1).trim();
                }
            }

            Map<String, Object> params = parseParameters(paramsStr);

            return ParsedReActResponse.ActionInfo.builder()
                .actionName(actionName)
                .parameters(params)
                .rawParams(paramsStr)
                .build();
        }

        return tryExtractJsonAction(response);
    }

    private ParsedReActResponse.ActionInfo tryExtractJsonAction(String response) {
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd > jsonStart) {
            String jsonStr = response.substring(jsonStart, jsonEnd + 1);
            try {
                Map<String, Object> params = parseJsonParameters(jsonStr);
                if (params.containsKey("tool") || params.containsKey("action") || params.containsKey("tool_name")) {
                    String toolName = params.getOrDefault("tool",
                                          params.getOrDefault("action",
                                          params.get("tool_name"))).toString();
                    params.remove("tool");
                    params.remove("action");
                    params.remove("tool_name");
                    return ParsedReActResponse.ActionInfo.builder()
                        .actionName(toolName)
                        .parameters(params)
                        .rawParams(jsonStr)
                        .build();
                }
            } catch (Exception e) {
                log.debug("JSON action parsing failed: {}", e.getMessage());
            }
        }

        return null;
    }

    private Map<String, Object> parseParameters(String paramsStr) {
        if (paramsStr == null || paramsStr.isBlank()) {
            return Map.of();
        }

        paramsStr = paramsStr.trim();

        if (paramsStr.startsWith("{")) {
            return parseJsonParameters(paramsStr);
        }

        return parseNaturalLanguageParameters(paramsStr);
    }

    private Map<String, Object> parseJsonParameters(String jsonStr) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON parameters: {}", jsonStr, e);
            return Map.of();
        }
    }

    private Map<String, Object> parseNaturalLanguageParameters(String paramsStr) {
        Map<String, Object> params = new java.util.HashMap<>();

        paramsStr = paramsStr.replaceAll("[\"\"'']", "");

        String[] keyValuePairs = paramsStr.split("[,，]");

        for (String pair : keyValuePairs) {
            String[] kv = pair.split("[:：=]");
            if (kv.length >= 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                params.put(key, value);
            }
        }

        return params;
    }

    @lombok.Data
    @Builder
    public static class ParsedReActResponse {
        private String rawResponse;
        private String thought;
        private boolean isFinalAnswer;
        private String finalAnswer;
        private String actionName;
        private Map<String, Object> actionParameters;

        @lombok.Data
        @Builder
        public static class ActionInfo {
            private String actionName;
            private Map<String, Object> parameters;
            private String rawParams;
        }
    }
}
