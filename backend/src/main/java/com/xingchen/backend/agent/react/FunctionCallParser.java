package com.xingchen.backend.agent.react;

import com.xingchen.backend.ai.model.AIResponse;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@Slf4j
public class FunctionCallParser {

    @Data
    @Builder
    public static class FunctionCall {
        private String callId;
        private String functionName;
        private Map<String, Object> arguments;
        private String rawJson;
    }

    public FunctionCall parseToolCalls(AIResponse response) {
        if (response == null) {
            return null;
        }

        Object toolCallsObj = response.getExtraData() != null ? response.getExtraData().get("toolCalls") : null;

        if (toolCallsObj instanceof java.util.List) {
            java.util.List<?> toolCallsList = (java.util.List<?>) toolCallsObj;
            if (!toolCallsList.isEmpty()) {
                Object firstCall = toolCallsList.get(0);
                if (firstCall instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> callMap = (Map<String, Object>) firstCall;
                    String name = callMap.get("name") != null ? callMap.get("name").toString() : null;
                    String args = callMap.get("arguments") != null ? callMap.get("arguments").toString() : "{}";
                    String id = callMap.get("id") != null ? callMap.get("id").toString() : null;

                    if (name != null) {
                        return FunctionCall.builder()
                                .callId(id)
                                .functionName(name)
                                .rawJson(args)
                                .arguments(parseJsonArguments(args))
                                .build();
                    }
                }
            }
        }

        String content = response.getContent();
        if (content != null && content.contains("function_call")) {
            return parseFromContent(content);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonArguments(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) {
            return Map.of();
        }

        try {
            jsonStr = jsonStr.trim();

            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(jsonStr, Map.class);
            }

            return parseNaturalLanguageArgs(jsonStr);
        } catch (Exception e) {
            log.warn("Failed to parse JSON arguments: {}", jsonStr, e);
            return parseNaturalLanguageArgs(jsonStr);
        }
    }

    private Map<String, Object> parseNaturalLanguageArgs(String argsStr) {
        Map<String, Object> args = new java.util.HashMap<>();

        argsStr = argsStr.trim();

        String[] keyValuePairs = argsStr.split("[,，\\n]");

        for (String pair : keyValuePairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            String[] kv = pair.split("[:：=]", 2);
            if (kv.length >= 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
                String value = kv[1].trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");

                if (value.matches("\\d+")) {
                    args.put(key, Long.parseLong(value));
                } else if (value.matches("\\d+\\.\\d+")) {
                    args.put(key, Double.parseDouble(value));
                } else if (value.matches("true|false")) {
                    args.put(key, Boolean.parseBoolean(value));
                } else {
                    args.put(key, value);
                }
            }
        }

        return args;
    }

    private FunctionCall parseFromContent(String content) {
        Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern argsPattern = Pattern.compile("\"arguments\"\\s*:\\s*(\\{.+?\\}|\\[[^\\]]+\\])",
                Pattern.DOTALL);

        Matcher nameMatcher = namePattern.matcher(content);
        Matcher argsMatcher = argsPattern.matcher(content);

        if (nameMatcher.find() && argsMatcher.find()) {
            String name = nameMatcher.group(1);
            String argsJson = argsMatcher.group(1);

            return FunctionCall.builder()
                    .functionName(name)
                    .rawJson(argsJson)
                    .arguments(parseJsonArguments(argsJson))
                    .build();
        }

        return null;
    }

    public static boolean isFunctionCallResponse(AIResponse response) {
        if (response == null || response.getExtraData() == null) {
            return false;
        }

        Object toolCalls = response.getExtraData().get("toolCalls");
        if (toolCalls instanceof java.util.List) {
            return !((java.util.List<?>) toolCalls).isEmpty();
        }

        String content = response.getContent();
        return content != null && content.contains("\"function_call\"");
    }
}
