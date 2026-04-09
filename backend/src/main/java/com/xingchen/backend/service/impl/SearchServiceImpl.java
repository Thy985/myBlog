package com.xingchen.backend.service.impl;

import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.SearchHistory;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.SearchHistoryMapper;
import com.xingchen.backend.service.SearchService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.SearchResultVO;
import cn.hutool.http.HtmlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ArticleMapper articleMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String HOT_SEARCH_CACHE_KEY = "search:hot:keywords:";
    private static final long HOT_SEARCH_CACHE_MINUTES = 10;

    @Override
    public PageResult<ArticleListVO> searchArticles(String keyword, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Article> articles = articleMapper.searchByKeyword(keyword, offset, size);
        List<ArticleListVO> voList = articles.stream()
                .map(this::convertToListVO)
                .map(this::escapeSearchResult)
                .collect(Collectors.toList());

        Long total = articleMapper.countByKeyword(keyword);
        int pages = (int) Math.ceil((double) total / size);
        return PageResult.of(voList, total, pages, page, size);
    }

    @Override
    public PageResult<SearchResultVO> globalSearch(String keyword, String type, Integer page, Integer size) {
        List<SearchResultVO> results = new ArrayList<>();

        if (type == null || "article".equals(type)) {
            int offset = (page - 1) * size;
            List<Article> articles = articleMapper.searchByKeyword(keyword, offset, size);

            for (Article article : articles) {
                SearchResultVO vo = new SearchResultVO();
                vo.setType("article");
                vo.setId(article.getId());
                // XSS 防护：HTML 转义搜索结果中的标题和描述
                vo.setTitle(HtmlUtil.escape(article.getTitle() != null ? article.getTitle() : ""));
                vo.setContent(HtmlUtil.escape(article.getDescription() != null ? article.getDescription() : ""));
                results.add(vo);
            }
        }

        Long total = articleMapper.countByKeyword(keyword);
        int pages = (int) Math.ceil((double) total / size);
        return PageResult.of(results, total, pages, page, size);
    }

    @Override
    public List<String> getSearchSuggestions(String keyword, Integer limit) {
        if (keyword == null || keyword.isEmpty()) {
            return Collections.emptyList();
        }
        return articleMapper.selectSuggestionTitles(keyword, limit);
    }

    @Override
    public List<String> getHotSearchKeywords(Integer limit) {
        String cacheKey = HOT_SEARCH_CACHE_KEY + limit;
        List<String> cached = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Map<String, Object>> hotKeywords = searchHistoryMapper.selectHotKeywords(thirtyDaysAgo, limit);

        List<String> keywords = hotKeywords.stream()
                .map(map -> (String) map.get("keyword"))
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, keywords, HOT_SEARCH_CACHE_MINUTES, TimeUnit.MINUTES);
        return keywords;
    }

    @Override
    @Async
    public void saveSearchHistoryAsync(Long userId, String keyword, Integer resultCount) {
        if (keyword == null || keyword.isEmpty()) {
            return;
        }
        try {
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(keyword.trim());
            history.setResultCount(resultCount);
            history.setCreateTime(LocalDateTime.now());
            searchHistoryMapper.insert(history);
            log.debug("搜索历史已保存: keyword={}, userId={}", keyword, userId);
        } catch (Exception e) {
            log.warn("保存搜索历史失败: keyword={}, userId={}", keyword, userId, e);
        }
    }

    private ArticleListVO convertToListVO(Article article) {
        ArticleListVO vo = new ArticleListVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }

    /**
     * XSS 防护：对搜索结果中的标题和摘要进行 HTML 转义
     */
    private ArticleListVO escapeSearchResult(ArticleListVO vo) {
        if (vo.getTitle() != null) {
            vo.setTitle(HtmlUtil.escape(vo.getTitle()));
        }
        if (vo.getDescription() != null) {
            vo.setDescription(HtmlUtil.escape(vo.getDescription()));
        }
        return vo;
    }
}
