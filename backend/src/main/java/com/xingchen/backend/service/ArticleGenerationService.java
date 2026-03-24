package com.xingchen.backend.service;

import com.xingchen.backend.entity.Article;

public interface ArticleGenerationService {

    Article generateArticle(Long userId, String topic, Integer wordCount, String style);

    Article generateArticleByTask(Long userId, Long taskId);

    String generateArticlePreview(Long userId, String topic);
}
