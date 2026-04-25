package com.xingchen.backend.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PostgreSQL 记忆向量存储库
 * 使用 pgvector 存储用户记忆的嵌入向量
 */
@Repository
@Slf4j
public class MemoryVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemoryVectorRepository(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 插入记忆向量
     */
    public void insert(MemoryRecord record) {
        String sql = """
            INSERT INTO memory_vectors (user_id, content, embedding, category, confidence, ttl_days, expire_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        LocalDateTime expireTime = record.createTime()
                .plusDays(record.ttlDays() > 0 ? record.ttlDays() : 30);

        jdbcTemplate.update(sql,
                record.userId(),
                record.content(),
                createVector(record.embedding()),
                record.category(),
                record.confidence(),
                record.ttlDays(),
                Timestamp.valueOf(expireTime));

        log.debug("插入记忆向量: userId={}, content={}",
                record.userId(), record.content().substring(0, Math.min(30, record.content().length())));
    }

    /**
     * 更新记忆向量
     */
    public void update(Long id, String content, float[] embedding, String category, int confidence) {
        String sql = """
            UPDATE memory_vectors
            SET content = ?, embedding = ?, category = ?, confidence = ?,
                update_time = CURRENT_TIMESTAMP, expire_time = CURRENT_TIMESTAMP + INTERVAL '1 day' * ttl_days
            WHERE id = ?
            """;

        jdbcTemplate.update(sql, content, createVector(embedding), category, confidence, id);
        log.debug("更新记忆向量: id={}", id);
    }

    /**
     * 删除记忆向量
     */
    public void delete(Long id) {
        String sql = "DELETE FROM memory_vectors WHERE id = ?";
        jdbcTemplate.update(sql, id);
        log.debug("删除记忆向量: id={}", id);
    }

    /**
     * 按用户ID删除所有记忆
     */
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM memory_vectors WHERE user_id = ?";
        int deleted = jdbcTemplate.update(sql, userId);
        log.info("删除用户所有记忆: userId={}, deleted={}", userId, deleted);
    }

    /**
     * 向量相似度搜索
     */
    public List<MemorySearchResult> searchByVector(Long userId, float[] queryEmbedding, int topK) {
        String sql = """
            SELECT id, user_id, content, category, confidence,
                   1 - (embedding <=> ?) AS similarity
            FROM memory_vectors
            WHERE user_id = ?
              AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)
            ORDER BY embedding <=> ?
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new MemorySearchResultRowMapper(),
                createVector(queryEmbedding), userId, createVector(queryEmbedding), topK);
    }

    /**
     * 获取用户所有有效记忆
     */
    public List<MemoryRecord> findByUserId(Long userId) {
        String sql = """
            SELECT id, user_id, content, embedding, category, confidence,
                   ttl_days, create_time, update_time, expire_time
            FROM memory_vectors
            WHERE user_id = ?
              AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)
            ORDER BY create_time DESC
            """;

        return jdbcTemplate.query(sql, new MemoryRecordRowMapper(), userId);
    }

    /**
     * 根据 ID 查找记忆
     */
    public MemoryRecord findById(Long id) {
        String sql = """
            SELECT id, user_id, content, embedding, category, confidence,
                   ttl_days, create_time, update_time, expire_time
            FROM memory_vectors
            WHERE id = ?
            """;
        List<MemoryRecord> results = jdbcTemplate.query(sql, new MemoryRecordRowMapper(), id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 清理过期记忆
     */
    public int cleanupExpired() {
        String sql = "DELETE FROM memory_vectors WHERE expire_time IS NOT NULL AND expire_time < CURRENT_TIMESTAMP";
        int deleted = jdbcTemplate.update(sql);
        if (deleted > 0) {
            log.info("清理过期记忆: deleted={}", deleted);
        }
        return deleted;
    }

    /**
     * 获取用户记忆数量
     */
    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM memory_vectors WHERE user_id = ? AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)",
                Long.class, userId);
        return count != null ? count : 0;
    }

    /**
     * 获取记忆总数
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memory_vectors", Long.class);
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

    public record MemoryRecord(
            Long id,
            Long userId,
            String content,
            float[] embedding,
            String category,
            int confidence,
            int ttlDays,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            LocalDateTime expireTime
    ) {}

    public record MemorySearchResult(
            Long id,
            Long userId,
            String content,
            String category,
            int confidence,
            float similarity
    ) {}

    private static class MemoryRecordRowMapper implements RowMapper<MemoryRecord> {
        @Override
        public MemoryRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            float[] embedding = parseVectorArray(rs.getArray("embedding"));
            return new MemoryRecord(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("content"),
                    embedding,
                    rs.getString("category"),
                    rs.getInt("confidence"),
                    rs.getInt("ttl_days"),
                    rs.getTimestamp("create_time").toLocalDateTime(),
                    rs.getTimestamp("update_time").toLocalDateTime(),
                    rs.getTimestamp("expire_time") != null ? rs.getTimestamp("expire_time").toLocalDateTime() : null
            );
        }
    }

    private static class MemorySearchResultRowMapper implements RowMapper<MemorySearchResult> {
        @Override
        public MemorySearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MemorySearchResult(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("content"),
                    rs.getString("category"),
                    rs.getInt("confidence"),
                    rs.getFloat("similarity")
            );
        }
    }

    private static float[] parseVectorArray(Array array) {
        if (array == null) return new float[0];
        try {
            Float[] boxed = (Float[]) array.getArray();
            float[] result = new float[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                result[i] = boxed[i] != null ? boxed[i] : 0f;
            }
            return result;
        } catch (SQLException e) {
            log.error("解析向量数组失败", e);
            return new float[0];
        }
    }
}
