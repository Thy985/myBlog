package com.xingchen.backend.service;

import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.SearchResultVO;

import java.util.List;

public interface SearchService {
    PageResult<ArticleListVO> searchArticles(String keyword, Integer page, Integer size);

    PageResult<SearchResultVO> globalSearch(String keyword, String type, Integer page, Integer size);

    List<String> getSearchSuggestions(String keyword, Integer limit);

    List<String> getHotSearchKeywords(Integer limit);

    void saveSearchHistoryAsync(Long userId, String keyword, Integer resultCount);
}
