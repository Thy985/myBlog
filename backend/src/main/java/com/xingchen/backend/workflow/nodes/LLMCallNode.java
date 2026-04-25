package com.xingchen.backend.workflow.nodes;

import com.xingchen.backend.ai.intent.IntentClassifierInterface;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.Intent;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.llm.UserLLMProviderManager;
import com.xingchen.backend.workflow.TaskNode;
import com.xingchen.backend.workflow.WorkflowDefinition;
import com.xingchen.backend.workflow.WorkflowExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class LLMCallNode implements TaskNode {

    private final IntentClassifierInterface intentClassifier;
    private final UserLLMProviderManager userProviderManager;

    private static final String NODE_ID = "llm_call";
    private static final String NODE_NAME = "LLM调用节点";

    @Override
    public String getNodeId() {
        return NODE_ID;
    }

    @Override
    public String getName() {
        return NODE_NAME;
    }

    @Override
    public WorkflowDefinition.WorkflowNode.NodeType getNodeType() {
        return WorkflowDefinition.WorkflowNode.NodeType.LLM_CALL;
    }

    @Override
    public CompletableFuture<NodeResult> execute(WorkflowExecution execution, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long userId = execution.getUserId();
                String message = extractString(input, "message");

                if (message == null || message.isEmpty()) {
                    return NodeResult.failure(getNodeId(), "消息内容为空");
                }

                Intent intent = intentClassifier.classify(message);
                log.info("LLM调用意图分类: type={}, confidence={}", intent.getType(), intent.getConfidence());

                execution.putContext("intent", intent.getType().name());
                execution.putContext("intent_confidence", intent.getConfidence());

                LLMProvider provider = userProviderManager.getUserProvider(userId);
                if (provider == null) {
                    return NodeResult.failure(getNodeId(), "用户未配置AI API Key");
                }

                AIRequest request = AIRequest.builder()
                        .userId(userId)
                        .sessionId(execution.getSessionId())
                        .message(message)
                        .systemPrompt(extractString(input, "systemPrompt"))
                        .build();

                var response = provider.chat(request);

                if (response.isSuccess()) {
                    execution.putContext("llm_response", response.getContent());
                    return NodeResult.success(getNodeId(), Map.of(
                            "content", response.getContent(),
                            "model", response.getModel(),
                            "intent", intent.getType().name()
                    ));
                } else {
                    return NodeResult.failure(getNodeId(), response.getContent());
                }

            } catch (Exception e) {
                log.error("LLM调用失败", e);
                return NodeResult.failure(getNodeId(), e.getMessage());
            }
        });
    }

    private String extractString(Map<String, Object> input, String key) {
        Object value = input.get(key);
        return value != null ? value.toString() : null;
    }
}