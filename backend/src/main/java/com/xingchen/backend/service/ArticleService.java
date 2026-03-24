package com.xingchen.backend.service;

import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.dto.ArticleUpdateDTO;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;

import java.util.List;
import java.util.Map;

public interface ArticleService {
    ArticleVO createArticle(Long userId, ArticleCreateDTO dto);

    ArticleVO updateArticle(Long userId, Long id, ArticleUpdateDTO dto);

    void deleteArticle(Long userId, Long id);

    ArticleVO getArticleById(Long id, Long userId);

    PageResult<ArticleListVO> getArticleList(Integer page, Integer size, String keyword, Long categoryId, Long tagId);

    PageResult<ArticleListVO> getArticleList(Integer page, Integer size, String keyword, Long categoryId, Long tagId, Long userId);

    PageResult<ArticleListVO> getUserArticles(Long userId, Integer page, Integer size);
    PageResult<ArticleListVO> getUserArticles(Long userId, Integer page, Integer size, String keyword);

    List<ArticleVO> getHotArticles(Integer limit);

    void publishArticle(Long userId, Long id);

    void offlineArticle(Long userId, Long id);

    void topArticle(Long userId, Long id, Boolean isTop);

    void likeArticle(Long userId, Long id);

    void unlikeArticle(Long userId, Long id);

    void collectArticle(Long userId, Long id);

    void uncollectArticle(Long userId, Long id);

    PageResult<ArticleListVO> getUserCollects(Long userId, Integer page, Integer size);

    Map<String, Object> getArticleArchive(Integer page, Integer size);

    Map<String, Object> getUserArticleArchive(Long userId, Integer page, Integer size);

    void updateReadNum(Long id);

    Map<String, Object> getArticleReadStats(Long id);

    List<ArticleListVO> getRelatedArticles(Long articleId, Integer limit);
}
