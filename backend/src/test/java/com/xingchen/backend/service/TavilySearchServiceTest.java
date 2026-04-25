package com.xingchen.backend.service;

import com.xingchen.backend.config.TavilyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TavilySearchServiceTest {

    @Autowired
    private TavilySearchService tavilySearchService;

    @Autowired
    private TavilyConfig tavilyConfig;

    @Test
    public void testTavilyConfig() {
        System.out.println("Tavily API Key configured: " + tavilyConfig.isConfigured());
        System.out.println("Tavily API Key: " + (tavilyConfig.getApiKey() != null ? "set" : "null"));
        assertTrue(tavilyConfig.isConfigured(), "Tavily API should be configured");
    }

    @Test
    public void testSearch() {
        if (!tavilyConfig.isConfigured()) {
            System.out.println("Skipping test - Tavily not configured");
            return;
        }

        TavilySearchService.TavilySearchResponse response = tavilySearchService.search("人工智能", 3);

        System.out.println("Query: " + response.getQuery());
        System.out.println("Total results: " + response.getTotalResults());
        System.out.println("Response time: " + response.getResponseTime() + "ms");

        for (TavilySearchService.TavilySearchResult result : response.getResults()) {
            System.out.println("Title: " + result.getTitle());
            System.out.println("URL: " + result.getUrl());
            System.out.println("Snippet: " + result.getSnippet());
            System.out.println("---");
        }

        assertNotNull(response.getResults());
    }

    @Test
    public void testSearchWithContext() {
        if (!tavilyConfig.isConfigured()) {
            System.out.println("Skipping test - Tavily not configured");
            return;
        }

        String[] context = {"技术", "最新"};
        TavilySearchService.TavilySearchResponse response =
            tavilySearchService.searchWithContext("AI发展", context);

        System.out.println("Query with context: " + response.getQuery());
        System.out.println("Results: " + response.getResults().size());

        assertNotNull(response.getResults());
    }
}