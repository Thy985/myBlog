package com.xingchen.backend.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * MCP 客户端服务
 * 负责调用远程 MCP 服务（Whisper、Vision 等）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class McpClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mcp.whisper.url:http://localhost:8001}")
    private String whisperUrl;

    @Value("${mcp.vision.url:http://localhost:8002}")
    private String visionUrl;

    @Value("${mcp.timeout:30000}")
    private int timeout;

    /**
     * 调用 Whisper 语音识别服务
     *
     * @param audioBase64 Base64 编码的音频数据
     * @param language 语言代码（可选，如 "zh", "en"）
     * @return 识别结果
     */
    public WhisperResult transcribeAudio(String audioBase64, String language) {
        long startTime = System.currentTimeMillis();
        
        try {
            String url = whisperUrl + "/transcribe";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                "audio", audioBase64,
                "language", language != null ? language : "auto",
                "model", "base"
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (response.getBody() != null && response.getBody().containsKey("text")) {
                String text = (String) response.getBody().get("text");
                log.info("Whisper 识别成功: elapsed={}ms, textLength={}", elapsed, text.length());
                
                return WhisperResult.builder()
                    .success(true)
                    .text(text)
                    .language((String) response.getBody().getOrDefault("language", "unknown"))
                    .confidence(getDoubleValue(response.getBody(), "confidence", 0.0))
                    .executionTime(elapsed)
                    .build();
            }
            
            return WhisperResult.builder()
                .success(false)
                .error("Whisper 服务返回空结果")
                .executionTime(elapsed)
                .build();
                
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Whisper 调用失败: elapsed={}ms, error={}", elapsed, e.getMessage());
            
            return WhisperResult.builder()
                .success(false)
                .error("语音识别失败: " + e.getMessage())
                .executionTime(elapsed)
                .build();
        }
    }

    /**
     * 调用 Whisper 服务（从 URL）
     */
    public WhisperResult transcribeFromUrl(String audioUrl, String language) {
        try {
            // 下载音频文件
            byte[] audioData = restTemplate.getForObject(audioUrl, byte[].class);
            if (audioData == null) {
                return WhisperResult.builder()
                    .success(false)
                    .error("无法下载音频文件")
                    .build();
            }
            
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);
            return transcribeAudio(audioBase64, language);
            
        } catch (Exception e) {
            log.error("从 URL 识别音频失败: {}", e.getMessage());
            return WhisperResult.builder()
                .success(false)
                .error("音频下载或识别失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 调用 Vision 图像识别服务
     *
     * @param imageBase64 Base64 编码的图像数据
     * @param operation 操作类型（describe, ocr, detect）
     * @return 识别结果
     */
    public VisionResult analyzeImage(String imageBase64, String operation) {
        long startTime = System.currentTimeMillis();
        
        try {
            String url = visionUrl + "/analyze";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                "image", imageBase64,
                "operation", operation != null ? operation : "describe"
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (response.getBody() != null) {
                String result = (String) response.getBody().get("result");
                log.info("Vision 分析成功: elapsed={}ms, operation={}", elapsed, operation);
                
                return VisionResult.builder()
                    .success(true)
                    .result(result)
                    .operation(operation)
                    .executionTime(elapsed)
                    .build();
            }
            
            return VisionResult.builder()
                .success(false)
                .error("Vision 服务返回空结果")
                .executionTime(elapsed)
                .build();
                
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Vision 调用失败: elapsed={}ms, error={}", elapsed, e.getMessage());
            
            return VisionResult.builder()
                .success(false)
                .error("图像分析失败: " + e.getMessage())
                .executionTime(elapsed)
                .build();
        }
    }

    /**
     * 调用 Vision 服务（从 URL）
     */
    public VisionResult analyzeImageFromUrl(String imageUrl, String operation) {
        try {
            // 下载图像文件
            byte[] imageData = restTemplate.getForObject(imageUrl, byte[].class);
            if (imageData == null) {
                return VisionResult.builder()
                    .success(false)
                    .error("无法下载图像文件")
                    .build();
            }
            
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);
            return analyzeImage(imageBase64, operation);
            
        } catch (Exception e) {
            log.error("从 URL 分析图像失败: {}", e.getMessage());
            return VisionResult.builder()
                .success(false)
                .error("图像下载或分析失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 检查 Whisper 服务健康状态
     */
    public boolean checkWhisperHealth() {
        try {
            String url = whisperUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查 Vision 服务健康状态
     */
    public boolean checkVisionHealth() {
        try {
            String url = visionUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Whisper 识别结果
     */
    @lombok.Data
    @lombok.Builder
    public static class WhisperResult {
        private boolean success;
        private String text;
        private String language;
        private double confidence;
        private String error;
        private long executionTime;
    }

    /**
     * Vision 分析结果
     */
    @lombok.Data
    @lombok.Builder
    public static class VisionResult {
        private boolean success;
        private String result;
        private String operation;
        private String error;
        private long executionTime;
    }
}
