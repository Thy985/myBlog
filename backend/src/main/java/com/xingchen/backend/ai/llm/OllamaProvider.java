package com.xingchen.backend.ai.llm;

import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ollama本地模型 Provider
 * 使用本地部署的开源模型
 * 
 * 需要先安装Ollama: https://ollama.com/
 * 然后拉取模型: ollama pull llama2
 */
@Component
@ConditionalOnProperty(prefix = "ollama", name = "enabled", havingValue = "true")
@Slf4j
public class OllamaProvider implements LLMProvider {
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;
    
    @Value("${ollama.model:llama2}")
    private String defaultModel;
    
    @Value("${ai.agent.llm.timeout-seconds:120}")
    private int timeoutSeconds;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    public String getProviderName() {
        return "Ollama";
    }
    
    @Override
    public String getModelName() {
        return defaultModel;
    }
    
    @Override
    public AIResponse chat(AIRequest request) {
        long startTime = System.currentTimeMillis();
        String model = request.getPreferredModel() != null ? request.getPreferredModel() : defaultModel;
        
        try {
            String url = baseUrl + "/api/chat";
            
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", buildMessages(request));
            body.put("stream", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (response.getBody() != null) {
                Map message = (Map) response.getBody().get("message");
                String content = (String) message.get("content");
                
                log.debug("Ollama调用完成: model={}, elapsed={}ms", model, elapsed);
                
                return AIResponse.builder()
                        .type(AIResponse.ResponseType.SUCCESS)
                        .content(content)
                        .model(model)
                        .executionTime(elapsed)
                        .build();
            }
            
            return AIResponse.error("Ollama返回空响应");
            
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Ollama调用失败: model={}, elapsed={}ms", model, elapsed, e);
            return AIResponse.error("本地模型调用失败: " + e.getMessage() + 
                    "\n请确保Ollama已启动: ollama serve");
        }
    }
    
    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        // Ollama流式输出需要特殊处理
        // 简化实现：先普通调用，然后模拟流式
        AIResponse response = chat(request);
        
        if (response.isSuccess()) {
            String content = response.getContent();
            // 模拟流式：每10个字符发送一次
            for (int i = 0; i < content.length(); i += 10) {
                int end = Math.min(i + 10, content.length());
                String chunk = content.substring(i, end);
                onChunk.accept(AIResponse.chunk(chunk));
            }
            onChunk.accept(AIResponse.builder()
                    .type(AIResponse.ResponseType.STREAM_END)
                    .content(content)
                    .build());
        } else {
            onChunk.accept(response);
        }
    }
    
    @Override
    public boolean supportsStreaming() {
        return true;
    }
    
    @Override
    public boolean supportsToolCalling() {
        return false; // 本地模型通常不支持工具调用
    }
    
    @Override
    public List<String> getAvailableModels() {
        try {
            String url = baseUrl + "/api/tags";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getBody() != null) {
                List<Map> models = (List<Map>) response.getBody().get("models");
                return models.stream()
                        .map(m -> (String) m.get("name"))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("获取Ollama模型列表失败", e);
        }
        
        return List.of(defaultModel);
    }
    
    private List<Map<String, String>> buildMessages(AIRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messages.add(systemMsg);
        }
        
        if (request.getHistory() != null) {
            for (var entry : request.getHistory()) {
                Map<String, String> msg = new HashMap<>();
                msg.put("role", entry.get("role"));
                msg.put("content", entry.get("content"));
                messages.add(msg);
            }
        }
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getMessage());
        messages.add(userMsg);
        
        return messages;
    }
}