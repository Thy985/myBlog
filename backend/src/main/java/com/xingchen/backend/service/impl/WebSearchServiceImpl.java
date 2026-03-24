package com.xingchen.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.service.WebSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WebSearchServiceImpl implements WebSearchService {

    @Value("${tavily.api.key}")
    private String tavilyApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        try {
            URL url = new URL("https://api.tavily.com/search");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // 构建正确的请求体
            String requestBody = String.format(
                "{\"api_key\":\"%s\",\"query\":\"%s\",\"max_results\":%d,\"search_depth\":\"basic\",\"include_answer\":false}",
                tavilyApiKey,
                query.replace("\"", "\\\""),
                maxResults
            );

            log.debug("Tavily 请求体: {}", requestBody);

            try (var os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            log.debug("Tavily API 响应码: {}", responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                log.debug("Tavily 响应: {}", response.toString());

                JsonNode root = objectMapper.readTree(response.toString());
                JsonNode resultsNode = root.path("results");

                if (resultsNode.isArray()) {
                    for (JsonNode item : resultsNode) {
                        String title = item.path("title").asText("");
                        String url_result = item.path("url").asText("");
                        String content = item.path("content").asText("");

                        if (!title.isEmpty() || !url_result.isEmpty()) {
                            results.add(new SearchResult(title, url_result, content));
                        }
                    }
                }

                log.info("Tavily 搜索 '{}' 获得 {} 条结果", query, results.size());
            } else {
                // 读取错误响应
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                log.error("Tavily API 请求失败: {} - {}", responseCode, errorResponse.toString());
            }

            conn.disconnect();

        } catch (Exception e) {
            log.error("搜索失败: {}", e.getMessage(), e);
        }

        return results;
    }
}
