package com.xingchen.backend.vector;

import com.xingchen.backend.knowledge.SemanticChunker;
import com.xingchen.backend.observability.MetricsService;
import com.xingchen.backend.repository.ArticleSearchRepository;
import com.xingchen.backend.repository.ArticleSearchRepository.HybridSearchResult;
import com.xingchen.backend.repository.ArticleVectorRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务 - PostgreSQL 版本
 * 结合向量检索 + 全文搜索 + 模糊匹配
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HybridSearchService {

    private final ArticleVectorRepository articleVectorRepository;
    private final ArticleSearchRepository articleSearchRepository;
    private final EmbeddingModel embeddingModel;
    private final MetricsService metricsService;

    // 混合搜索权重
    private static final float VECTOR_WEIGHT = 0.5f;
    private static final float TEXT_WEIGHT = 0.3f;
    private static final float TRGM_WEIGHT = 0.2f;

    /**
     * 混合检索
     */
    public List<HybridSearchResult> hybridSearch(String query, int topK) {
        return hybridSearch(query, topK, null, null);
    }

    /**
     * 带过滤条件的混合检索（目前忽略过滤条件）
     */
    public List<HybridSearchResult> hybridSearch(String query, int topK, String filterField, String filterValue) {
        Instant start = Instant.now();
        boolean success = false;

        try {
            // 1. 获取查询向量
            float[] queryVector = embeddingModel.embed(query).content().vector();

            // 2. 使用 PostgreSQL hybrid_search 函数
            List<HybridSearchResult> results = articleSearchRepository.hybridSearch(query, queryVector, topK);

            success = true;
            return results;

        } catch (Exception e) {
            log.error("混合检索失败: {}", query, e);
            // 降级为纯全文搜索
            return fallbackToFullTextSearch(query, topK);
        } finally {
            long latency = Duration.between(start, Instant.now()).toMillis();
            metricsService.recordKnowledgeSearch("hybrid_postgresql", success, latency, topK);
        }
    }

    /**
     * 降级为纯全文检索
     */
    private List<HybridSearchResult> fallbackToFullTextSearch(String query, int topK) {
        log.warn("混合检索失败，降级为纯全文检索: {}", query);

        try {
            var textResults = articleSearchRepository.fullTextSearch(query, topK);

            return textResults.stream()
                    .map(r -> new HybridSearchResult(
                            r.articleId(),
                            r.title(),
                            r.snippet(),
                            0f,
                            r.rank(),
                            0f,
                            r.rank()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("全文检索也失败了: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 索引文档（向量 + 全文）
     */
    public void indexDocument(Long articleId, String title, String content, String category) {
        try {
            // 1. 语义分块
            List<SemanticChunker.Chunk> chunks = new SemanticChunker().chunk(content);

            // 2. 准备向量和文本数据
            List<String> chunkContents = new ArrayList<>();
            List<float[]> chunkEmbeddings = new ArrayList<>();

            for (SemanticChunker.Chunk chunk : chunks) {
                chunkContents.add(chunk.content());
                float[] vector = embeddingModel.embed(chunk.content()).content().vector();
                chunkEmbeddings.add(vector);
            }

            // 3. 写入向量表
            articleVectorRepository.batchInsert(articleId, chunkContents, chunkEmbeddings);

            // 4. 写入全文搜索表
            articleSearchRepository.upsert(articleId, title, content);

            log.info("文档索引成功: articleId={}, chunks={}", articleId, chunks.size());

        } catch (Exception e) {
            log.error("文档索引失败: articleId={}", articleId, e);
            throw new RuntimeException("文档索引失败", e);
        }
    }

    /**
     * 删除文档
     */
    public void deleteDocument(Long articleId) {
        try {
            articleVectorRepository.deleteByArticleId(articleId);
            articleSearchRepository.deleteByArticleId(articleId);
            log.info("文档删除成功: articleId={}", articleId);
        } catch (Exception e) {
            log.error("文档删除失败: articleId={}", articleId, e);
            throw new RuntimeException("文档删除失败", e);
        }
    }

    /**
     * 更新文档
     */
    public void updateDocument(Long articleId, String title, String content, String category) {
        deleteDocument(articleId);
        indexDocument(articleId, title, content, category);
    }
}
