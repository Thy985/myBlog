package com.xingchen.backend.ai.intent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 PostgreSQL pgvector 的意图向量存储库
 * 使用向量索引加速相似度搜索，从 O(N) 优化到 O(log N)
 */
@Repository
@Slf4j
public class PgVectorIntentRepository {

    private final JdbcTemplate jdbcTemplate;

    @Value("${ai.intent.embedding-dimensions:512}")
    private int embeddingDimensions;

    /**
     * 使用 PostgreSQL 数据源构造
     */
    public PgVectorIntentRepository(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 初始化表结构（如果不存在）
     */
    public void initializeTable() {
        try {
            // 启用 pgvector 扩展
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            
            // 创建表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS intent_embeddings (
                    id BIGSERIAL PRIMARY KEY,
                    intent_type VARCHAR(50) NOT NULL,
                    example_text TEXT NOT NULL,
                    embedding vector(?) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.replace("?", String.valueOf(embeddingDimensions)));
            
            // 创建向量索引
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_intent_embedding 
                ON intent_embeddings 
                USING ivfflat (embedding vector_cosine_ops) 
                WITH (lists = 100)
                """);
            
            // 创建意图类型索引
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_intent_type 
                ON intent_embeddings (intent_type)
                """);
            
            log.info("意图向量表初始化完成，向量维度: {}", embeddingDimensions);
        } catch (Exception e) {
            log.error("初始化意图向量表失败", e);
            throw new RuntimeException("初始化意图向量表失败", e);
        }
    }

    /**
     * 清空所有意图向量数据
     */
    public void clearAll() {
        jdbcTemplate.execute("TRUNCATE TABLE intent_embeddings");
        log.info("已清空所有意图向量数据");
    }

    /**
     * 批量插入意图向量
     *
     * @param intentType 意图类型
     * @param examples   示例文本列表
     * @param embeddings 对应的向量嵌入列表
     */
    public void batchInsert(String intentType, List<String> examples, List<float[]> embeddings) {
        if (examples == null || embeddings == null || examples.size() != embeddings.size()) {
            throw new IllegalArgumentException("示例和向量数量不匹配");
        }

        String sql = "INSERT INTO intent_embeddings (intent_type, example_text, embedding) VALUES (?, ?, ?)";
        
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 0; i < examples.size(); i++) {
            batchArgs.add(new Object[]{
                intentType,
                examples.get(i),
                createVector(embeddings.get(i))
            });
        }
        
        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("批量插入意图向量: type={}, count={}", intentType, examples.size());
    }

    /**
     * 查找最相似的意图
     * 使用 pgvector 的余弦相似度操作符 <=>
     *
     * @param queryEmbedding 查询向量
     * @param topK           返回前 K 个结果
     * @return 意图类型和相似度得分列表
     */
    public List<Map<String, Object>> findMostSimilar(float[] queryEmbedding, int topK) {
        String sql = """
            SELECT intent_type, example_text, 
                   1 - (embedding <=> ?) AS similarity
            FROM intent_embeddings
            ORDER BY embedding <=> ?
            LIMIT ?
            """;
        
        return jdbcTemplate.queryForList(sql, 
            createVector(queryEmbedding),
            createVector(queryEmbedding),
            topK);
    }

    /**
     * 按意图类型分组查找最高相似度
     *
     * @param queryEmbedding 查询向量
     * @return 每个意图类型的最高相似度
     */
    public Map<String, Double> findMaxSimilarityByIntentType(float[] queryEmbedding) {
        String sql = """
            SELECT intent_type, MAX(1 - (embedding <=> ?)) AS max_similarity
            FROM intent_embeddings
            GROUP BY intent_type
            ORDER BY max_similarity DESC
            """;
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, createVector(queryEmbedding));
        
        Map<String, Double> similarityMap = new java.util.HashMap<>();
        for (Map<String, Object> row : results) {
            String intentType = (String) row.get("intent_type");
            Double similarity = ((Number) row.get("max_similarity")).doubleValue();
            similarityMap.put(intentType, similarity);
        }
        
        return similarityMap;
    }

    /**
     * 检查表是否为空
     */
    public boolean isEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intent_embeddings", Integer.class);
        return count == null || count == 0;
    }

    /**
     * 获取总记录数
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intent_embeddings", Long.class);
        return count != null ? count : 0;
    }

    /**
     * 将 float 数组转换为 PostgreSQL vector 类型
     */
    private Array createVector(float[] vector) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            // 将 float[] 转换为 Float[]
            Float[] boxedArray = new Float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                boxedArray[i] = vector[i];
            }
            return conn.createArrayOf("FLOAT", boxedArray);
        } catch (Exception e) {
            throw new RuntimeException("创建向量数组失败", e);
        }
    }
}
