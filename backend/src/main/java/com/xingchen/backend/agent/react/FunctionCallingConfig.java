package com.xingchen.backend.agent.react;

import com.xingchen.backend.ai.tool.Tool;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class FunctionCallingConfig {

    private boolean enabled;

    private FunctionCallingMode mode;

    private List<Tool> availableTools;

    public enum FunctionCallingMode {
        AUTO,
        NONE,
        TOOL_CALL_ONLY
    }

    public static FunctionCallingConfig defaultConfig(List<Tool> tools) {
        return FunctionCallingConfig.builder()
                .enabled(true)
                .mode(FunctionCallingMode.AUTO)
                .availableTools(tools)
                .build();
    }

    public String toOpenAIFormat() {
        if (availableTools == null || availableTools.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (int i = 0; i < availableTools.size(); i++) {
            Tool tool = availableTools.get(i);
            if (i > 0) sb.append(",\n");

            sb.append("  {\n");
            sb.append("    \"type\": \"function\",\n");
            sb.append("    \"function\": {\n");
            sb.append("      \"name\": \"").append(escapeJson(tool.getName())).append("\",\n");
            sb.append("      \"description\": \"").append(escapeJson(tool.getDescription())).append("\",\n");
            sb.append("      \"parameters\": {\n");
            sb.append("        \"type\": \"object\",\n");
            sb.append("        \"properties\": {\n");

            Tool.ToolParameter[] params = tool.getParameters();
            if (params != null && params.length > 0) {
                for (int j = 0; j < params.length; j++) {
                    Tool.ToolParameter param = params[j];
                    if (j > 0) sb.append(",\n");
                    sb.append("          \"").append(escapeJson(param.name())).append("\": {\n");
                    sb.append("            \"type\": \"").append(escapeJson(param.type())).append("\",\n");
                    sb.append("            \"description\": \"").append(escapeJson(param.description())).append("\"");
                    if (param.defaultValue() != null) {
                        sb.append(",\n            \"default\": \"").append(escapeJson(param.defaultValue().toString())).append("\"");
                    }
                    sb.append("\n          }");
                }
            } else {
                sb.append("          \"_dummy\": {\n");
                sb.append("            \"type\": \"string\",\n");
                sb.append("            \"description\": \"placeholder\"\n");
                sb.append("          }");
            }

            sb.append("\n        },\n");
            sb.append("        \"required\": [");

            if (params != null && params.length > 0) {
                for (int j = 0; j < params.length; j++) {
                    Tool.ToolParameter param = params[j];
                    if (param.required()) {
                        if (j > 0) sb.append(", ");
                        sb.append("\"").append(escapeJson(param.name())).append("\"");
                    }
                }
            }

            sb.append("]\n");
            sb.append("      }\n");
            sb.append("    }\n");
            sb.append("  }");
        }

        sb.append("\n]");
        return sb.toString();
    }

    public List<Map<String, Object>> toFunctionCallList() {
        if (availableTools == null || availableTools.isEmpty()) {
            return List.of();
        }

        return availableTools.stream().map(tool -> {
            Tool.ToolParameter[] params = tool.getParameters();
            Map<String, Object> properties = new java.util.HashMap<>();
            List<String> required = new java.util.ArrayList<>();

            if (params != null) {
                for (Tool.ToolParameter param : params) {
                    Map<String, Object> paramDef = new java.util.HashMap<>();
                    paramDef.put("type", param.type());
                    paramDef.put("description", param.description());
                    if (param.defaultValue() != null) {
                        paramDef.put("default", param.defaultValue());
                    }
                    properties.put(param.name(), paramDef);
                    if (param.required()) {
                        required.add(param.name());
                    }
                }
            }

            Map<String, Object> func = new java.util.HashMap<>();
            func.put("name", tool.getName());
            func.put("description", tool.getDescription());
            func.put("parameters", Map.of(
                    "type", "object",
                    "properties", properties,
                    "required", required
            ));

            Map<String, Object> toolDef = new java.util.HashMap<>();
            toolDef.put("type", "function");
            toolDef.put("function", func);

            return toolDef;
        }).toList();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
