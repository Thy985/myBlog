package com.xingchen.backend.vector;

import com.xingchen.backend.knowledge.SemanticChunker;
import com.xingchen.backend.observability.MetricsService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 * 结合 BM25 + 向量检索 + RRF 重排序
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HybridSearchService {

    private final OpenSearchService openSearchService;
    private final QdrantVectorService qdrantVectorService;
    private final EmbeddingModel embeddingModel;
    private final MetricsService metricsService;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    // RRF 参数
    private static final int RRF_K = 60;
    // 向量检索权重
    private static final float VECTOR_WEIGHT = 0.5f;
    // 全文检索权重
    private static final float TEXT_WEIGHT = 0.5f;

    /**
     * 混合检索
     */
    public List<HybridSearchResult> hybridSearch(String query, int topK) {
        return hybridSearch(query, topK, null, null);
    }

    /**
     * 带过滤条件的混合检索
     */
    public List<HybridSearchResult> hybridSearch(String query, int topK, String filterField, String filterValue) {
        Instant start = Instant.now();
        boolean success = false;

        try {
            // 1. 并行执行两种检索
            CompletableFuture<List<OpenSearchService.SearchResult>> textFuture =
                    CompletableFuture.supplyAsync(() ->
                            openSearchService.search(query, topK * 2, filterField, filterValue), executor);

            CompletableFuture<List<QdrantVectorService.SearchResult>> vectorFuture =
                    CompletableFuture.supplyAsync(() -> {
                        float[] queryVector = embeddingModel.embed(query).content().vector();
                        return qdrantVectorService.search(queryVector, topK * 2, filterField, filterValue);
                    }, executor);

            // 2. 等待结果
            List<OpenSearchService.SearchResult> textResults = textFuture.get();
            List<QdrantVectorService.SearchResult> vectorResults = vectorFuture.get();

            // 3. RRF 融合
            List<HybridSearchResult> merged = reciprocalRankFusion(textResults, vectorResults, topK);

            success = true;
            return merged;

        } catch (Exception e) {
            log.error("混合检索失败: {}", query, e);
            // 降级为纯文本检索
            return fallbackToTextSearch(query, topK);
        } finally {
            long latency = Duration.between(start, Instant.now()).toMillis();
            metricsService.recordKnowledgeSearch("hybrid", success, latency, topK);
        }
    }

    /**
     * RRF (Reciprocal Rank Fusion)
     */
    private List<HybridSearchResult> reciprocalRankFusion(
            List<OpenSearchService.SearchResult> textResults,
            List<QdrantVectorService.SearchResult> vectorResults,
            int topK) {

        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, HybridSearchResult> resultMap = new HashMap<>();

        // 处理文本检索结果
        for (int i = 0; i < textResults.size(); i++) {
            OpenSearchService.SearchResult result = textResults.get(i);
            String id = result.articleId();
            double rrfScore = TEXT_WEIGHT * (1.0 / (RRF_K + i + 1));

            rrfScores.merge(id, rrfScore, Double::sum);
            resultMap.computeIfAbsent(id, k -> new HybridSearchResult(
                    id,
                    result.title(),
                    result.summary(),
                    result.highlights(),
                    0f,
                    0f,
                    0.0,
                    ""
            )).setTextScore(result.score());
        }

        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            QdrantVectorService.SearchResult result = vectorResults.get(i);
            String id = result.payload().getOrDefault("article_id", result.id());
            double rrfScore = VECTOR_WEIGHT * (1.0 / (RRF_K + i + 1));

            rrfScores.merge(id, rrfScore, Double::sum);
            resultMap.computeIfAbsent(id, k -> new HybridSearchResult(
                    id,
                    result.payload().getOrDefault("title", ""),
                    result.payload().getOrDefault("summary", ""),
                    null,
                    0f,
                    0f,
                    0.0,
                    ""
            )).setVectorScore(result.score());
        }

        // 按 RRF 分数排序
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    HybridSearchResult result = resultMap.get(entry.getKey());
                    result.setRrfScore(entry.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 降级为纯文本检索
     */
    private List<HybridSearchResult> fallbackToTextSearch(String query, int topK) {
        log.warn("混合检索失败，降级为纯文本检索: {}", query);
        List<OpenSearchService.SearchResult> textResults = openSearchService.search(query, topK);

        return textResults.stream()
                .map(r -> new HybridSearchResult(
                        r.articleId(),
                        r.title(),
                        r.summary(),
                        r.highlights(),
                        r.score(),
                        0f,
                        (double) r.score(),
                        "text_fallback"
                ))
                .collect(Collectors.toList());
    }

    /**
     * 索引文档（全文 + 向量）
     */
    public void indexDocument(Long articleId, String title, String content, String category) {
        try {
            // 1. 语义分块
            List<SemanticChunker.Chunk> chunks = new SemanticChunker().chunk(content);

            // 2. 准备 OpenSearch 文档
            List<OpenSearchService.SearchDocument> textDocs = new ArrayList<>();
            List<QdrantVectorService.VectorDocument> vectorDocs = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                SemanticChunker.Chunk chunk = chunks.get(i);
                String docId = articleId + "_" + i;

                // OpenSearch 文档
                OpenSearchService.SearchDocument textDoc = new OpenSearchService.SearchDocument();
                textDoc.setId(docId);
                textDoc.setArticleId(String.valueOf(articleId));
                textDoc.setTitle(title);
                textDoc.setContent(chunk.content());
                textDoc.setSummary(chunk.content().substring(0, Math.min(200, chunk.content().length())));
                textDoc.setCategory(category);
                textDoc.setChunkIndex(i);
                textDocs.add(textDoc);

                // 向量文档
                float[] vector = embeddingModel.embed(chunk.content()).content().vector();
                Map<String, String> payload = new HashMap<>();
                payload.put("article_id", String.valueOf(articleId));
                payload.put("title", title);
                payload.put("chunk_index", String.valueOf(i));
                payload.put("category", category);
                vectorDocs.add(new QdrantVectorService.VectorDocument(docId, vector, payload));
            }

            // 3. 并行写入
            CompletableFuture<Void> textFuture = CompletableFuture.runAsync(() ->
                    openSearchService.bulkAddDocuments(textDocs), executor);

            CompletableFuture<Void> vectorFuture = CompletableFuture.runAsync(() ->
                    qdrantVectorService.addVectors(vectorDocs), executor);

            CompletableFuture.allOf(textFuture, vectorFuture).join();

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
            String id = String.valueOf(articleId);
            CompletableFuture<Void> textFuture = CompletableFuture.runAsync(() ->
                    openSearchService.deleteByArticleId(id), executor);
            CompletableFuture<Void> vectorFuture = CompletableFuture.runAsync(() ->
                    qdrantVectorService.deleteByArticleId(id), executor);

            CompletableFuture.allOf(textFuture, vectorFuture).join();
            log.info("文档删除成功: articleId={}", articleId);
        } catch (Exception e) {
            log.error("文档删除失败: articleId={}", articleId, e);
            throw new RuntimeException("文档删除失败", e);
        }
    }

    /**
     * 混合搜索结果
     */
    public static class HybridSearchResult {
        private String articleId;
        private String title;
        private String summary;
        private List<String> highlights;
        private float textScore;
        private float vectorScore;
        private double rrfScore;
        private String source;

        public HybridSearchResult(String articleId, String title, String summary,
                                  List<String> highlights, float textScore,
                                  float vectorScore, double rrfScore, String source) {
            this.articleId = articleId;
            this.title = title;
            this.summary = summary;
            this.highlights = highlights;
            this.textScore = textScore;
            this.vectorScore = vectorScore;
            this.rrfScore = rrfScore;
            this.source = source;
        }

        // Getters and Setters
        public String getArticleId() { return articleId; }
        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public List<String> getHighlights() { return highlights; }
        public float getTextScore() { return textScore; }
        public void setTextScore(float textScore) { this.textScore = textScore; }
        public float getVectorScore() { return vectorScore; }
        public void setVectorScore(float vectorScore) { this.vectorScore = vectorScore; }
        public double getRrfScore() { return rrfScore; }
        public void setRrfScore(double rrfScore) { this.rrfScore = rrfScore; }
        public String getSource() { return source; }
    }
}