package com.xingchen.backend.service;

public interface KnowledgeBaseService {

    void addDocument(Long articleId, String title, String content);

    void addDocument(Long articleId, String title, String content, String category);

    void deleteDocument(Long articleId);

    /**
     * 搜索知识库，返回最相关的文本拼接结果
     */
    String search(String query);

    /**
     * 搜索知识库，指定返回数量
     */
    String search(String query, int topK);

    void clearAll();

    long getDocumentCount();
}
