package com.xingchen.backend.agent.react;

import com.xingchen.backend.ai.tool.Tool;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReActPrompt {

    private String systemPromptTemplate;

    private List<Tool> availableTools;

    private String userMessage;

    private String chatHistory;

    private String memoryContext;

    private String ragContext;

    private int currentStep;

    private int maxSteps;

    private List<ReActState.ReActStep> previousSteps;

    public static ReActPromptBuilder builder() {
        return new ReActPromptBuilder();
    }

    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append(systemPromptTemplate != null ? systemPromptTemplate : getDefaultSystemPrompt());

        if (availableTools != null && !availableTools.isEmpty()) {
            prompt.append("\n\n## 可用工具\n");
            prompt.append(buildToolsDescription());
        }

        if (memoryContext != null && !memoryContext.isBlank()) {
            prompt.append("\n\n## 相关记忆\n");
            prompt.append(memoryContext);
        }

        if (ragContext != null && !ragContext.isBlank()) {
            prompt.append("\n\n## 知识库检索结果\n");
            prompt.append(ragContext);
        }

        prompt.append("\n\n## 推理指南\n");
        prompt.append(getReActGuidelines());

        return prompt.toString();
    }

    private String getDefaultSystemPrompt() {
        return """
            你是一个智能助手，擅长通过结构化推理来回答问题和完成任务。

            你的推理过程遵循 ReAct (Reasoning + Acting) 模式：
            1. 思考（Thought）：分析当前情况，决定下一步行动
            2. 行动（Action）：选择并执行合适的工具
            3. 观察（Observation）：分析工具返回的结果
            4. 循环直到得到最终答案

            重要规则：
            - 每一步都必须包含清晰的思考过程
            - 只有在确定最终答案时才输出 [FINAL ANSWER]
            - 如果需要外部信息，优先使用工具获取
            - 工具调用要准确，参数要完整
            """;
    }

    private String buildToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("你可以使用以下工具来帮助你完成任务：\n\n");

        for (Tool tool : availableTools) {
            sb.append("### ").append(tool.getName()).append("\n");
            sb.append(tool.getDescription()).append("\n");

            Tool.ToolParameter[] params = tool.getParameters();
            if (params != null && params.length > 0) {
                sb.append("参数：\n");
                for (Tool.ToolParameter param : params) {
                    sb.append("- ").append(param.name())
                      .append(" (").append(param.type()).append(")")
                      .append(param.required() ? " [必填]" : " [可选]");
                    if (param.defaultValue() != null) {
                        sb.append(" = ").append(param.defaultValue());
                    }
                    sb.append(": ").append(param.description()).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String getReActGuidelines() {
        return """
            ## 输出格式

            请严格按照以下格式输出你的推理过程：

            步骤 N：
            思考: <分析当前情况，描述你的推理过程>
            行动: <选择工具名称>
            行动参数: <JSON格式的工具参数>

            然后等待工具执行结果，再继续下一步推理。

            当你准备好给出最终答案时，使用以下格式：
            思考: <最终分析>
            最终答案: <你的完整回答>
            """;
    }

    public String buildUserMessage() {
        StringBuilder msg = new StringBuilder();

        if (previousSteps != null && !previousSteps.isEmpty()) {
            msg.append("## 之前的推理步骤\n\n");
            for (ReActState.ReActStep step : previousSteps) {
                msg.append("步骤 ").append(step.getStepNumber()).append("：\n");
                if (step.getThought() != null) {
                    msg.append("思考：").append(step.getThought()).append("\n");
                }
                if (step.getAction() != null) {
                    msg.append("行动：").append(step.getAction().getToolName());
                    if (step.getAction().getParameters() != null) {
                        msg.append("(").append(step.getAction().getParameters()).append(")");
                    }
                    msg.append("\n");
                }
                if (step.getObservation() != null) {
                    msg.append("观察：").append(step.getObservation()).append("\n");
                }
                if (step.getResult() != null) {
                    msg.append("结果：").append(step.getResult().getMessage()).append("\n");
                }
                msg.append("\n");
            }
            msg.append("请基于以上步骤继续推理，或给出最终答案。\n\n");
        }

        msg.append("## 当前问题\n\n");
        msg.append(userMessage);

        if (chatHistory != null && !chatHistory.isBlank()) {
            msg.append("\n\n## 对话历史\n\n").append(chatHistory);
        }

        return msg.toString();
    }

    public String buildSystemPromptWithFunctionCalling(FunctionCallingConfig config) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个智能助手，擅长使用工具来回答问题和完成任务。\n\n");

        prompt.append("你可以通过调用函数工具来获取信息、执行操作。每次调用函数后，系统会返回结果，你可以根据结果继续推理或调用更多工具。\n\n");

        if (memoryContext != null && !memoryContext.isBlank()) {
            prompt.append("## 相关记忆\n").append(memoryContext).append("\n\n");
        }

        if (ragContext != null && !ragContext.isBlank()) {
            prompt.append("## 知识库检索结果\n").append(ragContext).append("\n\n");
        }

        prompt.append("当你准备好给出最终答案时，请在回复中包含：\n");
        prompt.append("[FINAL ANSWER] 你的完整回答内容\n\n");

        prompt.append("可用工具通过 Function Calling 机制提供，你可以在回复中指定要调用的函数。\n");

        return prompt.toString();
    }
}
