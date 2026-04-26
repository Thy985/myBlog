package com.xingchen.backend.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 百度文心一言 Provider
 * 使用百度千帆大模型API
 * 
 * 参考文档: https://cloud.baidu.com/doc/WENXINWORKSHOP/index.html
 */
@Component
@ConditionalOnProperty(prefix = "baidu.wenxin", name = "api-key")
@Slf4j
public class BaiduProvider implements LLMProvider {
    
    @Value("${baidu.wenxin.api-key}")
    private String apiKey;
    
    @Value("${baidu.wenxin.secret-key}")
    private String secretKey;
    
    @Value("${baidu.wenxin.model:ernie-bot-4}")
    private String defaultModel;
    
    @Value("${ai.agent.llm.timeout-seconds:60}")
    private int timeoutSeconds;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;
    private long tokenExpireTime;
    
    @Override
    public String getProviderName() {
        return "Baidu";
    }
    
    @Override
    public String getModelName() {
        return defaultModel;
    }
    /**
     * 消息
     */
    @Override
    public AIResponse chat(AIRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取access token
            String token = getAccessToken();
            
            String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/" 
                    + defaultModel + "?access_token=" + token;
            
            Map<String, Object> body = new HashMap<>();
            body.put("messages", buildMessages(request));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);// 设置请求头
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (response.getBody() != null) {
                String result = (String) response.getBody().get("result");
                
                log.debug("百度文心调用完成: model={}, elapsed={}ms", defaultModel, elapsed);
                
                return AIResponse.builder()
                        .type(AIResponse.ResponseType.SUCCESS)
                        .content(result)
                        .model(defaultModel)
                        .executionTime(elapsed)
                        .build();
            }
            
            return AIResponse.error("百度文心返回空响应");
            
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("百度文心调用失败: model={}, elapsed={}ms", defaultModel, elapsed, e);
            return AIResponse.error("百度文心调用失败: " + e.getMessage());
        }
    }
    /**
     * 流式消息
     */
    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        long startTime = System.currentTimeMillis();
        StringBuilder fullContent = new StringBuilder();
        
        try {
            String token = getAccessToken();
            String model = request.getPreferredModel() != null ? request.getPreferredModel() : defaultModel;
            String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/" 
                    + model + "?access_token=" + token;
            
            Map<String, Object> body = new HashMap<>();
            body.put("messages", buildMessages(request));
            body.put("stream", true);
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();// 创建连接
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);//允许输出（POST)
            conn.setConnectTimeout(timeoutSeconds * 1000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setRequestProperty("Content-Type", "application/json");
            
            String jsonBody = objectMapper.writeValueAsString(body);// 构建请求体
            conn.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
            
            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);

                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        try {
                            Map<String, Object> chunkData = objectMapper.readValue(data, Map.class);
                            String result = (String) chunkData.get("result");

                            if (result != null && !result.isEmpty()) {
                                fullContent.append(result);
                                onChunk.accept(AIResponse.chunk(result));
                            }

                            boolean isEnd = Boolean.TRUE.equals(chunkData.get("is_end"));
                            if (isEnd) {
                                break;
                            }
                        } catch (Exception e) {
                            log.warn("解析流式数据块失败: {}", data, e);
                        }
                    }
                }
            } finally {
                reader.close();
                inputStream.close();
                conn.disconnect();
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("百度文心流式调用完成: model={}, elapsed={}ms", model, elapsed);
            
            onChunk.accept(AIResponse.builder()
                    .type(AIResponse.ResponseType.STREAM_END)
                    .content(fullContent.toString())
                    .model(model)
                    .executionTime(elapsed)
                    .build());
            
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("百度文心流式调用失败: elapsed={}ms", elapsed, e);
            onChunk.accept(AIResponse.error("流式输出失败: " + e.getMessage()));
        }
    }
    
    @Override
    public boolean supportsStreaming() {
        return true;
    }
    
    @Override
    public boolean supportsToolCalling() {
        return false;
    }
    
    @Override
    public List<String> getAvailableModels() {
        return List.of(
                "ernie-bot-4",      // 文心一言4.0
                "ernie-bot",        // 文心一言
                "ernie-bot-turbo",  // 文心一言Turbo
                "ernie-bot-8k",     // 文心一言8K
                "ernie-bot-128k"    // 文心一言128K
        );
    }
    
    /**
     * 获取百度Access Token
     */
    private synchronized String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }
        
        try {
            String url = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" 
                    + apiKey + "&client_secret=" + secretKey;
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            
            if (response.getBody() != null) {
                accessToken = (String) response.getBody().get("access_token");
                // token有效期30天，提前1天刷新
                tokenExpireTime = System.currentTimeMillis() + 29 * 24 * 60 * 60 * 1000;
                return accessToken;
            }
            
            throw new RuntimeException("获取百度Access Token失败");
            
        } catch (Exception e) {
            log.error("获取百度Access Token失败", e);
            throw new RuntimeException("获取百度Access Token失败: " + e.getMessage());
        }
    }
    
    private List<Map<String, String>> buildMessages(AIRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 百度文心不支持system角色，将system prompt合并到user消息
        StringBuilder userContent = new StringBuilder();
        
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            userContent.append(request.getSystemPrompt()).append("\n\n");
        }
        
        if (request.getHistory() != null) {
            for (var entry : request.getHistory()) {
                Map<String, String> msg = new HashMap<>();
                String role = entry.get("role");
                // 百度文心只支持user和assistant
                if ("user".equals(role)) {
                    msg.put("role", "user");
                    msg.put("content", entry.get("content"));
                    messages.add(msg);
                } else if ("assistant".equals(role)) {
                    msg.put("role", "assistant");
                    msg.put("content", entry.get("content"));
                    messages.add(msg);
                }
            }
        }
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userContent.append(request.getMessage());
        userMsg.put("content", userContent.toString());
        messages.add(userMsg);
        
        return messages;
    }
}