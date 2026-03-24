package com.xingchen.backend.service;

import java.util.List;

public interface WebSearchService {

    List<SearchResult> search(String query, int maxResults);

    record SearchResult(String title, String url, String content) {}
}
