package com.xingchen.backend.ai.tool;

import com.xingchen.backend.service.TavilySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TavilyTool implements Tool {

    private final TavilySearchService tavilySearchService;

    @Override
    public String getName() {
        return "tavily_search";
    }

    @Override
    public String getDescription() {
        return "Tavily 联网搜索工具：实时搜索互联网上的最新信息。支持搜索技术趋势、热门话题、新闻等。返回搜索结果列表，包含标题、URL、摘要和相关性评分。";
    }

    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
            new ToolParameter(
                    "query",
                    "搜索查询词（用英文效果更好）",
                    "string",
                    true,
                    null
            ),
            new ToolParameter(
                    "max_results",
                    "最大返回结果数（默认10）",
                    "integer",
                    false,
                    10
            )
        };
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        Integer maxResults = parameters.get("max_results") != null
                ? ((Number) parameters.get("max_results")).intValue()
                : 10;

        if (query == null || query.trim().isEmpty()) {
            return ToolResult.error("搜索查询词不能为空");
        }

        log.info("执行 Tavily 搜索: query={}, maxResults={}", query, maxResults);

        try {
            TavilySearchService.TavilySearchResponse response = tavilySearchService.search(query, maxResults);

            if (response.getResults().isEmpty()) {
                return ToolResult.success(
                        Map.of(
                                "query", query,
                                "totalResults", 0,
                                "results", List.of()
                        ),
                        String.format("Tavily 搜索完成，但未找到与 '%s' 相关的结果", query)
                );
            }

            List<Map<String, Object>> results = response.getResults().stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("title", r.getTitle());
                        map.put("url", r.getUrl());
                        map.put("snippet", r.getSnippet());
                        map.put("score", r.getScore());
                        map.put("publishedDate", r.getPublishedDate() != null ? r.getPublishedDate() : "");
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = Map.of(
                    "query", query,
                    "totalResults", response.getTotalResults(),
                    "responseTime", response.getResponseTime(),
                    "results", results
            );

            String message = String.format("Tavily 搜索完成：找到 %d 条相关结果（耗时 %dms）",
                    response.getTotalResults(), response.getResponseTime());

            log.info("Tavily 搜索成功: query={}, results={}, time={}ms",
                    query, response.getTotalResults(), response.getResponseTime());

            return ToolResult.success(data, message);

        } catch (Exception e) {
            log.error("Tavily 搜索失败: query={}", query, e);
            return ToolResult.error("Tavily 搜索失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeout() {
        return 30000;
    }
}
