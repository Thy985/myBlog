package com.xingchen.backend.service.impl;

import com.xingchen.backend.messaging.EmbeddingProducer;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.vector.HybridSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务 V2（基于混合检索）
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class
KnowledgeBaseServiceImplV2 implements KnowledgeBaseService {

    private final HybridSearchService hybridSearchService;
    private final EmbeddingProducer embeddingProducer;

    @Override
    public void addDocument(Long articleId, String title, String content) {
        addDocument(articleId, title, content, "default");
    }

    @Override
    public void addDocument(Long articleId, String title, String content, String category) {
        // 异步发送索引任务
        embeddingProducer.sendIndexTask(articleId, title, content, category);
        log.info("知识库添加文档（异步）: articleId={}", articleId);
    }

    @Override
    public void deleteDocument(Long articleId) {
        // 异步发送删除任务
        embeddingProducer.sendDeleteTask(articleId);
        log.info("知识库删除文档（异步）: articleId={}", articleId);
    }

    @Override
    public String search(String query) {
        return search(query, 5);
    }

    @Override
    public String search(String query, int topK) {
        List<HybridSearchService.HybridSearchResult> results = hybridSearchService.hybridSearch(query, topK);

        if (results.isEmpty()) {
            return "";
        }

        // 拼接搜索结果
        StringBuilder sb = new StringBuilder();
        sb.append("根据知识库检索到以下相关信息：\n\n");

        for (int i = 0; i < results.size(); i++) {
            HybridSearchService.HybridSearchResult result = results.get(i);
            sb.append("[").append(i + 1).append("] ")
              .append(result.getTitle())
              .append(" (相关度: ").append(String.format("%.2f", result.getRrfScore())).append(")\n");
            sb.append(result.getSummary()).append("\n\n");
        }

        return sb.toString();
    }

    @Override
    public void clearAll() {
        // 清空所有索引
        log.warn("清空知识库索引");
        // 这里可以添加清空逻辑
    }

    @Override
    public long getDocumentCount() {
        // 返回文档数量
        return 0; // 待实现
    }
}