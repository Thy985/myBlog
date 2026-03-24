package com.xingchen.backend.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.indices.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenSearch 全文检索服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenSearchService {

    private final OpenSearchClient openSearchClient;

    @Value("${opensearch.index:myblog_articles}")
    private String indexName;

    @PostConstruct
    public void init() {
        try {
            createIndexIfNotExists();
        } catch (Exception e) {
            log.error("初始化 OpenSearch Index 失败", e);
        }
    }

    /**
     * 创建索引（如果不存在）
     */
    private void createIndexIfNotExists() throws IOException {
        boolean exists = openSearchClient.indices().exists(
                org.opensearch.client.opensearch.indices.ExistsRequest.of(e -> e.index(indexName))
        ).value();

        if (!exists) {
            log.info("创建 OpenSearch Index: {}", indexName);

            try {
                // 尝试使用 IK 分词器创建索引
                CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                        .index(indexName)
                        .settings(s -> s
                                .numberOfShards("1")
                                .numberOfReplicas("0")
                                .analysis(a -> a
                                        .analyzer("ik_max_word", ik -> ik
                                                .custom(c -> c
                                                        .tokenizer("ik_max_word")
                                                        .filter("lowercase")
                                                )
                                        )
                                        .analyzer("ik_smart", ik -> ik
                                                .custom(c -> c
                                                        .tokenizer("ik_smart")
                                                        .filter("lowercase")
                                                )
                                        )
                                )
                        )
                        .mappings(m -> m
                                .properties("article_id", p -> p.keyword(k -> k))
                                .properties("title", p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                        .fields("keyword", k -> k.keyword(kw -> kw.ignoreAbove(256)))
                                ))
                                .properties("content", p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                ))
                                .properties("summary", p -> p.text(t -> t
                                        .analyzer("ik_smart")
                                ))
                                .properties("category", p -> p.keyword(k -> k))
                                .properties("tags", p -> p.keyword(k -> k))
                                .properties("author", p -> p.keyword(k -> k))
                                .properties("publish_time", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
                                .properties("chunk_index", p -> p.integer(i -> i))
                        )
                );

                openSearchClient.indices().create(request);
                log.info("Index 创建成功（使用 IK 分词器）");
            } catch (Exception e) {
                log.warn("IK 分词器不可用，使用标准分词器创建索引: {}", e.getMessage());
                createIndexWithStandardAnalyzer();
            }
        } else {
            log.info("Index 已存在: {}", indexName);
        }
    }

    /**
     * 使用标准分词器创建索引（当 IK 分词器不可用时）
     */
    private void createIndexWithStandardAnalyzer() throws IOException {
        CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                .index(indexName)
                .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                )
                .mappings(m -> m
                        .properties("article_id", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t
                                .analyzer("standard")
                                .fields("keyword", k -> k.keyword(kw -> kw.ignoreAbove(256)))
                        ))
                        .properties("content", p -> p.text(t -> t.analyzer("standard")))
                        .properties("summary", p -> p.text(t -> t.analyzer("standard")))
                        .properties("category", p -> p.keyword(k -> k))
                        .properties("tags", p -> p.keyword(k -> k))
                        .properties("author", p -> p.keyword(k -> k))
                        .properties("publish_time", p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
                        .properties("chunk_index", p -> p.integer(i -> i))
                )
        );

        openSearchClient.indices().create(request);
        log.info("Index 创建成功（使用标准分词器）");
    }

    /**
     * 添加文档
     */
    public void addDocument(SearchDocument document) {
        try {
            IndexRequest<SearchDocument> request = IndexRequest.of(builder -> builder
                    .index(indexName)
                    .id(document.getId())
                    .document(document)
                    .refresh(Refresh.True)
            );

            openSearchClient.index(request);
            log.debug("文档添加成功: {}", document.getId());
        } catch (IOException e) {
            log.error("添加文档失败: {}", document.getId(), e);
            throw new RuntimeException("添加文档失败", e);
        }
    }

    /**
     * 批量添加文档
     */
    public void bulkAddDocuments(List<SearchDocument> documents) {
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

            for (SearchDocument doc : documents) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(doc.getId())
                                .document(doc)
                        )
                );
            }

            BulkResponse response = openSearchClient.bulk(bulkBuilder.build());

            if (response.errors()) {
                log.error("批量添加文档部分失败");
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        log.error("文档 {} 失败: {}", item.id(), item.error().reason());
                    }
                }
            } else {
                log.info("批量添加文档成功: {} 个", documents.size());
            }
        } catch (IOException e) {
            log.error("批量添加文档失败", e);
            throw new RuntimeException("批量添加文档失败", e);
        }
    }

    /**
     * 全文搜索（BM25）
     */
    public List<SearchResult> search(String query, int topK) {
        return search(query, topK, null, null);
    }

    /**
     * 带过滤条件的全文搜索
     */
    public List<SearchResult> search(String query, int topK, String filterField, String filterValue) {
        try {
            SearchRequest searchRequest = SearchRequest.of(builder -> {
                builder.index(indexName)
                        .query(q -> q
                                .multiMatch(mm -> mm
                                        .query(query)
                                        .fields("title^3", "content^2", "summary^1.5", "tags")
                                        .type(org.opensearch.client.opensearch._types.query_dsl.TextQueryType.BestFields)
                                        .fuzziness("AUTO")
                                )
                        )
                        .size(topK)
                        .highlight(h -> h
                                .fields("title", f -> f)
                                .fields("content", f -> f.fragmentSize(150).numberOfFragments(3))
                                .preTags("<mark>")
                                .postTags("</mark>")
                        )
                        .source(s -> s.filter(f -> f.excludes("content"))); // 不返回完整内容

                // 添加过滤条件
                if (filterField != null && filterValue != null) {
                    builder.postFilter(f -> f
                            .term(t -> t
                                    .field(filterField)
                                    .value(FieldValue.of(filterValue))
                            )
                    );
                }

                return builder;
            });

            SearchResponse<SearchDocument> response = openSearchClient.search(
                    searchRequest,
                    SearchDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> new SearchResult(
                            hit.id(),
                            hit.score() != null ? hit.score().floatValue() : 0f,
                            hit.source() != null ? hit.source().getArticleId() : "",
                            hit.source() != null ? hit.source().getTitle() : "",
                            hit.source() != null ? hit.source().getSummary() : "",
                            hit.highlight() != null ? hit.highlight().get("content") : null
                    ))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("全文搜索失败: {}", query, e);
            throw new RuntimeException("全文搜索失败", e);
        }
    }

    /**
     * 删除文档
     */
    public void deleteDocument(String id) {
        try {
            DeleteRequest request = DeleteRequest.of(builder -> builder
                    .index(indexName)
                    .id(id)
            );

            openSearchClient.delete(request);
            log.debug("文档删除成功: {}", id);
        } catch (IOException e) {
            log.error("删除文档失败: {}", id, e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    /**
     * 根据 articleId 删除所有相关文档
     */
    public void deleteByArticleId(String articleId) {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(builder -> builder
                    .index(indexName)
                    .query(q -> q
                            .term(t -> t
                                    .field("article_id")
                                    .value(FieldValue.of(articleId))
                            )
                    )
            );

            openSearchClient.deleteByQuery(request);
            log.info("删除文章文档成功: {}", articleId);
        } catch (IOException e) {
            log.error("删除文章文档失败: {}", articleId, e);
            throw new RuntimeException("删除文章文档失败", e);
        }
    }

    /**
     * 清空索引
     */
    public void clearIndex() {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(builder -> builder
                    .index(indexName)
                    .query(q -> q.matchAll(m -> m))
                    .refresh(true)
            );

            openSearchClient.deleteByQuery(request);
            log.info("Index 已清空: {}", indexName);
        } catch (IOException e) {
            log.error("清空 Index 失败", e);
            throw new RuntimeException("清空 Index 失败", e);
        }
    }

    /**
     * 获取文档数量
     */
    public long getDocumentCount() {
        try {
            CountRequest request = CountRequest.of(builder -> builder.index(indexName));
            return openSearchClient.count(request).count();
        } catch (IOException e) {
            log.error("获取文档数量失败", e);
            return 0;
        }
    }

    /**
     * 搜索文档模型
     */
    public static class SearchDocument {
        private String id;
        private String articleId;
        private String title;
        private String content;
        private String summary;
        private String category;
        private List<String> tags;
        private String author;
        private String publishTime;
        private Integer chunkIndex;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getArticleId() { return articleId; }
        public void setArticleId(String articleId) { this.articleId = articleId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getPublishTime() { return publishTime; }
        public void setPublishTime(String publishTime) { this.publishTime = publishTime; }
        public Integer getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    }

    /**
     * 搜索结果
     */
    public record SearchResult(
            String id,
            float score,
            String articleId,
            String title,
            String summary,
            List<String> highlights
    ) {}
}