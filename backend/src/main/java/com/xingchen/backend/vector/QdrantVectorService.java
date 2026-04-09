package com.xingchen.backend.vector;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Qdrant 向量存储服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QdrantVectorService {

    private final QdrantClient qdrantClient;

    @Value("${qdrant.collection:myblog_articles}")
    private String collectionName;

    @Value("${qdrant.vector-size:384}")
    private int vectorSize;

    @PostConstruct
    public void init() {
        try {
            createCollectionIfNotExists();
        } catch (Exception e) {
            log.error("初始化 Qdrant Collection 失败", e);
        }
    }

    /**
     * 创建 Collection（如果不存在）
     */
    public void createCollectionIfNotExists() throws ExecutionException, InterruptedException {
        createCollectionIfNotExists(collectionName, vectorSize);
    }

    /**
     * 创建 Collection（如果不存在）- 指定名称和向量大小
     */
    public void createCollectionIfNotExists(String collectionName, int vectorSize) throws ExecutionException, InterruptedException {
        boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();

        if (!exists) {
            log.info("创建 Qdrant Collection: {}", collectionName);

            Collections.Distance distance = Collections.Distance.Cosine;

            qdrantClient.createCollectionAsync(collectionName,
                    Collections.VectorParams.newBuilder()
                            .setSize(vectorSize)
                            .setDistance(distance)
                            .build()
            ).get();

            log.info("Collection 创建成功");
        } else {
            log.info("Collection 已存在: {}", collectionName);
        }
    }

    /**
     * 添加向量
     */
    public void addVector(String id, float[] vector, Map<String, String> payload) {
        addVector(collectionName, id, vector, payload);
    }

    /**
     * 添加向量 - 指定集合名称
     */
    public void addVector(String collectionName, String idStr, float[] vector, Map<String, String> payload) {
        try {
            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(id(UUID.fromString(idStr)))
                    .setVectors(vectors(vector))
                    .putAllPayload(payload.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> value(e.getValue())
                            )))
                    .build();

            qdrantClient.upsertAsync(collectionName, List.of(point)).get();
            log.debug("向量添加成功: {}", idStr);
        } catch (Exception e) {
            log.error("添加向量失败: {}", idStr, e);
            throw new RuntimeException("添加向量失败", e);
        }
    }

    /**
     * 批量添加向量
     */
    public void addVectors(List<VectorDocument> documents) {
        try {
            List<Points.PointStruct> points = documents.stream()
                    .map(doc -> Points.PointStruct.newBuilder()
                            .setId(id(UUID.fromString(doc.getId())))
                            .setVectors(vectors(doc.getVector()))
                            .putAllPayload(doc.getPayload().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> value(e.getValue())
                                    )))
                            .build())
                    .collect(Collectors.toList());

            qdrantClient.upsertAsync(collectionName, points).get();
            log.info("批量添加向量成功: {} 个", documents.size());
        } catch (Exception e) {
            log.error("批量添加向量失败", e);
            throw new RuntimeException("批量添加向量失败", e);
        }
    }

    /**
     * 语义搜索
     */
    public List<SearchResult> search(float[] queryVector, int topK) {
        return search(collectionName, queryVector, topK, null, null);
    }

    /**
     * 带过滤条件的语义搜索
     */
    public List<SearchResult> search(float[] queryVector, int topK, String filterKey, String filterValue) {
        return search(collectionName, queryVector, topK, filterKey, filterValue);
    }

    /**
     * 带过滤条件的语义搜索 - 指定集合名称
     */
    public List<SearchResult> search(String collectionName, float[] queryVector, int topK, String filterKey, String filterValue) {
        try {
            List<Float> vectorList = wrapFloatArray(queryVector);

            Points.SearchPoints.Builder searchBuilder = Points.SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(vectorList)
                    .setLimit(topK)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(false).build());

            // 添加过滤条件
            if (filterKey != null && filterValue != null) {
                Points.Condition condition = Points.Condition.newBuilder()
                        .setField(Points.FieldCondition.newBuilder()
                                .setKey(filterKey)
                                .setMatch(Points.Match.newBuilder().setKeyword(filterValue).build())
                                .build())
                        .build();
                Points.Filter filter = Points.Filter.newBuilder()
                        .addMust(condition)
                        .build();
                searchBuilder.setFilter(filter);
            }

            List<Points.ScoredPoint> results = qdrantClient.searchAsync(searchBuilder.build()).get();

            return results.stream()
                    .map(point -> new SearchResult(
                            point.getId().getUuid(),
                            point.getScore(),
                            point.getPayloadMap().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> e.getValue().getStringValue()
                                    ))
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            throw new RuntimeException("向量搜索失败", e);
        }
    }

    /**
     * 删除向量
     */
    public void deleteVector(String id) {
        deleteVector(collectionName, id);
    }

    /**
     * 删除向量 - 指定集合名称
     */
    public void deleteVector(String collectionName, String idStr) {
        try {
            qdrantClient.deleteAsync(collectionName, List.of(id(UUID.fromString(idStr)))).get();
            log.debug("向量删除成功: {}", idStr);
        } catch (Exception e) {
            log.error("删除向量失败: {}", idStr, e);
            throw new RuntimeException("删除向量失败", e);
        }
    }

    /**
     * 根据 articleId 删除所有相关向量
     */
    public void deleteByArticleId(String articleId) {
        try {
            Points.Condition condition = Points.Condition.newBuilder()
                    .setField(Points.FieldCondition.newBuilder()
                            .setKey("article_id")
                            .setMatch(Points.Match.newBuilder().setKeyword(articleId).build())
                            .build())
                    .build();
            Points.Filter filter = Points.Filter.newBuilder()
                    .addMust(condition)
                    .build();

            qdrantClient.deleteAsync(collectionName, filter).get();
            log.info("删除文章向量成功: {}", articleId);
        } catch (Exception e) {
            log.error("删除文章向量失败: {}", articleId, e);
            throw new RuntimeException("删除文章向量失败", e);
        }
    }

    /**
     * 清空 Collection
     */
    public void clearCollection() {
        try {
            Points.Filter filter = Points.Filter.newBuilder().build();
            qdrantClient.deleteAsync(collectionName, filter).get();
            log.info("Collection 已清空: {}", collectionName);
        } catch (Exception e) {
            log.error("清空 Collection 失败", e);
            throw new RuntimeException("清空 Collection 失败", e);
        }
    }

    /**
     * 删除 Collection
     */
    public void deleteCollection() {
        deleteCollection(collectionName);
    }

    /**
     * 删除 Collection - 指定集合名称
     */
    public void deleteCollection(String collectionName) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (exists) {
                qdrantClient.deleteCollectionAsync(collectionName).get();
                log.info("Collection 已删除: {}", collectionName);
            } else {
                log.info("Collection 不存在，无需删除: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("删除 Collection 失败: {}", collectionName, e);
            throw new RuntimeException("删除 Collection 失败", e);
        }
    }

    /**
     * 获取 Collection 信息
     */
    public Map<String, Object> getCollectionInfo() {
        return getCollectionInfo(collectionName);
    }

    /**
     * 获取 Collection 信息 - 指定集合名称
     */
    public Map<String, Object> getCollectionInfo(String collectionName) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (!exists) {
                return Map.of("exists", false, "name", collectionName);
            }
            return Map.of(
                    "exists", true,
                    "name", collectionName,
                    "vectorSize", vectorSize
            );
        } catch (Exception e) {
            log.error("获取 Collection 信息失败: {}", collectionName, e);
            throw new RuntimeException("获取 Collection 信息失败", e);
        }
    }

    private List<Float> wrapFloatArray(float[] array) {
        List<Float> list = new java.util.ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    /**
     * 向量文档
     */
    public static class VectorDocument {
        private String id;
        private float[] vector;
        private Map<String, String> payload;

        public VectorDocument(String id, float[] vector, Map<String, String> payload) {
            this.id = id;
            this.vector = vector;
            this.payload = payload;
        }

        public String getId() { return id; }
        public float[] getVector() { return vector; }
        public Map<String, String> getPayload() { return payload; }
    }

    /**
     * 搜索结果
     */
    public record SearchResult(String id, float score, Map<String, String> payload) {}
}
