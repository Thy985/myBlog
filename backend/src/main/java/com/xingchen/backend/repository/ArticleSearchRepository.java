package com.xingchen.backend.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * PostgreSQL 文章全文搜索存储库
 * 使用 tsvector 进行全文搜索，pg_trgm 进行模糊匹配
 */
@Repository
@Slf4j
public class ArticleSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public ArticleSearchRepository(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 初始化表结构
     */
    public void initializeTable() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS unaccent");
            log.info("PostgreSQL pg_trgm 和 unaccent 扩展启用成功");
        } catch (Exception e) {
            log.warn("扩展可能已存在: {}", e.getMessage());
        }
    }

    /**
     * 插入或更新文章搜索记录
     */
    public void upsert(Long articleId, String title, String content) {
        String sql = """
            INSERT INTO article_search (article_id, title_text, content_text)
            VALUES (?, ?, ?)
            ON CONFLICT (article_id) DO UPDATE
            SET title_text = EXCLUDED.title_text,
                content_text = EXCLUDED.content_text
            """;

        jdbcTemplate.update(sql, articleId, title, content);
        log.debug("更新文章搜索记录: articleId={}", articleId);
    }

    /**
     * 删除文章搜索记录
     */
    public void deleteByArticleId(Long articleId) {
        String sql = "DELETE FROM article_search WHERE article_id = ?";
        int deleted = jdbcTemplate.update(sql, articleId);
        log.info("删除文章搜索记录: articleId={}, deleted={}", articleId, deleted);
    }

    /**
     * 全文搜索
     */
    public List<SearchResult> fullTextSearch(String query, int topK) {
        String sql = """
            SELECT article_id, title_text, substring(content_text, 1, 200) as snippet,
                   ts_rank(search_vector, plainto_tsquery('simple', ?)) AS rank
            FROM article_search
            WHERE search_vector @@ plainto_tsquery('simple', ?)
            ORDER BY rank DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new SearchResultRowMapper(), query, query, topK);
    }

    /**
     * 模糊搜索（标题）
     */
    public List<SearchResult> fuzzySearch(String query, int topK) {
        String sql = """
            SELECT article_id, title_text, substring(content_text, 1, 200) as snippet,
                   similarity(title_text, ?) AS rank
            FROM article_search
            WHERE similarity(title_text, ?) > 0.3 OR title_text % ?
            ORDER BY rank DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new SearchResultRowMapper(), query, query, query, topK);
    }

    /**
     * 混合搜索（向量 + 全文 + 模糊）
     */
    public List<HybridSearchResult> hybridSearch(String query, float[] queryEmbedding, int topK) {
        String sql = """
            SELECT * FROM hybrid_search(?, ?, ?)
            """;

        return jdbcTemplate.query(sql, new HybridSearchResultRowMapper(),
                query, createVector(queryEmbedding), topK);
    }

    /**
     * 搜索文章是否存在
     */
    public boolean exists(Long articleId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM article_search WHERE article_id = ?", Integer.class, articleId);
        return count != null && count > 0;
    }

    /**
     * 获取搜索记录总数
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM article_search", Long.class);
        return count != null ? count : 0;
    }

    private Array createVector(float[] vector) {
        try (java.sql.Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            Float[] boxedArray = new Float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                boxedArray[i] = vector[i];
            }
            return conn.createArrayOf("FLOAT", boxedArray);
        } catch (SQLException e) {
            throw new RuntimeException("创建向量数组失败", e);
        }
    }

    public record SearchResult(
            Long articleId,
            String title,
            String snippet,
            float rank
    ) {}

    public record HybridSearchResult(
            Long articleId,
            String title,
            String snippet,
            float vectorScore,
            float textScore,
            float trgmScore,
            float finalScore
    ) {
        // 兼容旧接口
        public String getSummary() { return snippet; }
        public double getRrfScore() { return finalScore; }
        public String getSource() { return "postgresql"; }
    }

    private static class SearchResultRowMapper implements RowMapper<SearchResult> {
        @Override
        public SearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SearchResult(
                    rs.getLong("article_id"),
                    rs.getString("title_text"),
                    rs.getString("snippet"),
                    rs.getFloat("rank")
            );
        }
    }

    private static class HybridSearchResultRowMapper implements RowMapper<HybridSearchResult> {
        @Override
        public HybridSearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new HybridSearchResult(
                    rs.getLong("article_id"),
                    rs.getString("title"),
                    rs.getString("snippet"),
                    rs.getFloat("vector_score"),
                    rs.getFloat("text_score"),
                    rs.getFloat("trgm_score"),
                    rs.getFloat("final_score")
            );
        }
    }
}
