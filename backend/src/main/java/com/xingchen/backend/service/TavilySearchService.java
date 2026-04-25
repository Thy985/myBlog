package com.xingchen.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.config.TavilyConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TavilySearchService {

    private final TavilyConfig tavilyConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public static class TavilySearchResult {
        private final String title;
        private final String url;
        private final String snippet;
        private final double score;
        private final String publishedDate;

        public TavilySearchResult(String title, String url, String snippet, double score, String publishedDate) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
            this.score = score;
            this.publishedDate = publishedDate;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getSnippet() { return snippet; }
        public double getScore() { return score; }
        public String getPublishedDate() { return publishedDate; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("title", title);
            map.put("url", url);
            map.put("snippet", snippet);
            map.put("score", score);
            map.put("publishedDate", publishedDate);
            return map;
        }
    }

    public static class TavilySearchResponse {
        private final String query;
        private final List<TavilySearchResult> results;
        private final long responseTime;
        private final int totalResults;

        public TavilySearchResponse(String query, List<TavilySearchResult> results, long responseTime, int totalResults) {
            this.query = query;
            this.results = results;
            this.responseTime = responseTime;
            this.totalResults = totalResults;
        }

        public String getQuery() { return query; }
        public List<TavilySearchResult> getResults() { return results; }
        public long getResponseTime() { return responseTime; }
        public int getTotalResults() { return totalResults; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("query", query);
            map.put("results", results.stream().map(TavilySearchResult::toMap).toList());
            map.put("responseTime", responseTime);
            map.put("totalResults", totalResults);
            return map;
        }
    }

    public TavilySearchResponse search(String query) {
        return search(query, tavilyConfig.getMaxResults());
    }

    public TavilySearchResponse search(String query, int maxResults) {
        long startTime = System.currentTimeMillis();

        if (!tavilyConfig.isConfigured()) {
            log.warn("Tavily API Key 未配置");
            return new TavilySearchResponse(query, List.of(), 0, 0);
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("max_results", maxResults);
            requestBody.put("include_answer", true);
            requestBody.put("include_raw_content", false);
            requestBody.put("include_images", false);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tavilyConfig.getBaseUrl() + "/search"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + tavilyConfig.getApiKey())
                    .timeout(Duration.ofSeconds(tavilyConfig.getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(query, response.body(), startTime);
            } else {
                log.error("Tavily API 调用失败: status={}, body={}", response.statusCode(), response.body());
                return new TavilySearchResponse(query, List.of(), System.currentTimeMillis() - startTime, 0);
            }
        } catch (Exception e) {
            log.error("Tavily API 调用异常", e);
            return new TavilySearchResponse(query, List.of(), System.currentTimeMillis() - startTime, 0);
        }
    }

    public TavilySearchResponse searchWithContext(String query, String[] contextKeywords) {
        String enhancedQuery = query;
        if (contextKeywords != null && contextKeywords.length > 0) {
            enhancedQuery = query + " " + String.join(" ", contextKeywords);
        }
        return search(enhancedQuery);
    }

    public List<String> extractTrendingTopics(String domain, int limit) {
        List<String> topics = new ArrayList<>();

        String[] searchQueries = {
            domain + " trends 2024",
            domain + " latest news",
            domain + " popular topics",
            "hot " + domain + " discussions"
        };

        for (String searchQuery : searchQueries) {
            TavilySearchResponse response = search(searchQuery, 5);
            for (TavilySearchResult result : response.getResults()) {
                String topic = extractTopicFromResult(result);
                if (topic != null && !topics.contains(topic)) {
                    topics.add(topic);
                    if (topics.size() >= limit) {
                        return topics;
                    }
                }
            }
        }

        return topics;
    }

    private String extractTopicFromResult(TavilySearchResult result) {
        String title = result.getTitle();
        if (title != null && title.length() > 5 && title.length() < 100) {
            title = title.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "")
                         .trim();
            return title;
        }
        return null;
    }

    private TavilySearchResponse parseResponse(String query, String responseBody, long startTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultsNode = root.get("results");

            List<TavilySearchResult> results = new ArrayList<>();
            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode node : resultsNode) {
                    String title = getTextOrEmpty(node, "title");
                    String url = getTextOrEmpty(node, "url");
                    String snippet = getTextOrEmpty(node, "snippet");
                    double score = node.has("score") ? node.get("score").asDouble() : 0.0;
                    String publishedDate = node.has("published_date") ? node.get("published_date").asText() : null;

                    results.add(new TavilySearchResult(title, url, snippet, score, publishedDate));
                }
            }

            long responseTime = System.currentTimeMillis() - startTime;
            int totalResults = root.has("total_results") ? root.get("total_results").asInt() : results.size();

            log.info("Tavily 搜索完成: query={}, results={}, time={}ms", query, results.size(), responseTime);

            return new TavilySearchResponse(query, results, responseTime, totalResults);

        } catch (Exception e) {
            log.error("解析 Tavily 响应失败", e);
            return new TavilySearchResponse(query, List.of(), System.currentTimeMillis() - startTime, 0);
        }
    }

    private String getTextOrEmpty(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }
}
