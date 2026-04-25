package com.xingchen.backend.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * PostgreSQL 文章向量存储库
 * 使用 pgvector 进行向量存储和相似度搜索
 */
@Repository
@Slf4j
public class ArticleVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public ArticleVectorRepository(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 初始化表结构
     */
    public void initializeTable() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("PostgreSQL vector 扩展启用成功");
        } catch (Exception e) {
            log.warn("vector 扩展可能已存在: {}", e.getMessage());
        }
    }

    /**
     * 插入文章向量
     */
    public void insert(Long articleId, int chunkIndex, String chunkContent, float[] embedding) {
        String sql = """
            INSERT INTO article_vectors (article_id, chunk_index, chunk_content, embedding)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (article_id, chunk_index) DO UPDATE
            SET chunk_content = EXCLUDED.chunk_content, embedding = EXCLUDED.embedding
            """;

        jdbcTemplate.update(sql, articleId, chunkIndex, chunkContent, createVector(embedding));
        log.debug("插入文章向量: articleId={}, chunkIndex={}", articleId, chunkIndex);
    }

    /**
     * 批量插入文章向量
     */
    public void batchInsert(Long articleId, List<String> contents, List<float[]> embeddings) {
        String sql = """
            INSERT INTO article_vectors (article_id, chunk_index, chunk_content, embedding)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (article_id, chunk_index) DO UPDATE
            SET chunk_content = EXCLUDED.chunk_content, embedding = EXCLUDED.embedding
            """;

        for (int i = 0; i < contents.size(); i++) {
            jdbcTemplate.update(sql, articleId, i, contents.get(i), createVector(embeddings.get(i)));
        }
        log.info("批量插入文章向量: articleId={}, count={}", articleId, contents.size());
    }

    /**
     * 删除文章的所有向量
     */
    public void deleteByArticleId(Long articleId) {
        String sql = "DELETE FROM article_vectors WHERE article_id = ?";
        int deleted = jdbcTemplate.update(sql, articleId);
        log.info("删除文章向量: articleId={}, deleted={}", articleId, deleted);
    }

    /**
     * 向量相似度搜索
     */
    public List<VectorSearchResult> searchByVector(float[] queryEmbedding, int topK) {
        String sql = """
            SELECT article_id, chunk_index, chunk_content,
                   1 - (embedding <=> ?) AS similarity
            FROM article_vectors
            ORDER BY embedding <=> ?
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new VectorSearchResultRowMapper(),
                createVector(queryEmbedding), createVector(queryEmbedding), topK);
    }

    /**
     * 按文章ID搜索向量
     */
    public List<VectorSearchResult> findByArticleId(Long articleId) {
        String sql = """
            SELECT article_id, chunk_index, chunk_content,
                   1.0 AS similarity
            FROM article_vectors
            WHERE article_id = ?
            ORDER BY chunk_index
            """;

        return jdbcTemplate.query(sql, new VectorSearchResultRowMapper(), articleId);
    }

    /**
     * 获取文章向量数量
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM article_vectors", Long.class);
        return count != null ? count : 0;
    }

    /**
     * 获取文章向量数量
     */
    public long countByArticleId(Long articleId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM article_vectors WHERE article_id = ?", Long.class, articleId);
        return count != null ? count : 0;
    }

    private Array createVector(float[] vector) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            Float[] boxedArray = new Float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                boxedArray[i] = vector[i];
            }
            return conn.createArrayOf("FLOAT", boxedArray);
        } catch (SQLException e) {
            throw new RuntimeException("创建向量数组失败", e);
        }
    }

    public record VectorSearchResult(
            Long articleId,
            int chunkIndex,
            String chunkContent,
            float similarity
    ) {}

    private static class VectorSearchResultRowMapper implements RowMapper<VectorSearchResult> {
        @Override
        public VectorSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VectorSearchResult(
                    rs.getLong("article_id"),
                    rs.getInt("chunk_index"),
                    rs.getString("chunk_content"),
                    rs.getFloat("similarity")
            );
        }
    }
}
