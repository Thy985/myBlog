package com.xingchen.backend.service.impl;

import com.xingchen.backend.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务（重构版）
 * - 从 Redis 改为 SQLite FTS5 全文检索
 * - 支持中文分词
 * - 持久化存储，不会过期
 * - 性能优于 Redis 遍历
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Value("${agent.workspace:./data/knowledge}")
    private String knowledgePath;

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;
    private Connection connection;

    @PostConstruct
    public void init() {
        try {
            Path dbDir = Path.of(knowledgePath);
            Files.createDirectories(dbDir);
            Path dbPath = dbDir.resolve("knowledge.db");

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            // 启用 WAL 模式
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            // 创建 FTS5 虚拟表
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS kb_documents USING fts5(
                        content,
                        title,
                        article_id,
                        category,
                        chunk_index,
                        tokenize='porter'
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS kb_meta (
                        article_id INTEGER PRIMARY KEY,
                        title TEXT,
                        category TEXT,
                        chunks_count INTEGER,
                        indexed_at TEXT
                    )
                """);
            }

            log.info("知识库 SQLite 初始化完成: {}", dbPath);
        } catch (Exception e) {
            log.error("知识库初始化失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void addDocument(Long articleId, String title, String content) {
        addDocument(articleId, title, content, null);
    }

    @Override
    public void addDocument(Long articleId, String title, String content, String category) {
        try {
            // 先删除旧数据
            deleteDocument(articleId);

            // 分块
            List<String> chunks = splitText(content);

            // 插入 FTS5
            String insertSql = "INSERT INTO kb_documents(content, title, article_id, category, chunk_index) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                for (int i = 0; i < chunks.size(); i++) {
                    stmt.setString(1, chunks.get(i));
                    stmt.setString(2, title);
                    stmt.setString(3, String.valueOf(articleId));
                    stmt.setString(4, category != null ? category : "");
                    stmt.setString(5, String.valueOf(i));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // 更新元数据
            String metaSql = "INSERT OR REPLACE INTO kb_meta(article_id, title, category, chunks_count, indexed_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(metaSql)) {
                stmt.setLong(1, articleId);
                stmt.setString(2, title);
                stmt.setString(3, category != null ? category : "");
                stmt.setInt(4, chunks.size());
                stmt.setString(5, java.time.LocalDateTime.now().toString());
                stmt.executeUpdate();
            }

            log.info("文章 {} 已索引到知识库: {}（{} 个片段）", articleId, title, chunks.size());
        } catch (Exception e) {
            log.error("索引文章失败: {} - {}", articleId, e.getMessage(), e);
        }
    }

    @Override
    public void deleteDocument(Long articleId) {
        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM kb_documents WHERE article_id = ?")) {
                stmt.setString(1, String.valueOf(articleId));
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM kb_meta WHERE article_id = ?")) {
                stmt.setLong(1, articleId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public String search(String query) {
        return search(query, 5);
    }

    @Override
    public String search(String query, int topK) {
        if (query == null || query.isBlank()) return "";

        try {
            // 提取中文和英文关键词
            List<String> keywords = extractKeywords(query);
            if (keywords.isEmpty()) {
                keywords = List.of(query.trim());
            }

            // 构建 FTS5 查询
            String ftsQuery = keywords.stream()
                    .map(kw -> "\"" + kw + "\"")
                    .collect(Collectors.joining(" OR "));

            String sql = """
                SELECT content, title, article_id, rank
                FROM kb_documents
                WHERE kb_documents MATCH ?
                ORDER BY rank
                LIMIT ?
            """;

            List<String> results = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, ftsQuery);
                stmt.setInt(2, topK);

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String content = rs.getString("content");
                    String title = rs.getString("title");
                    results.add(String.format("[%s]\n%s", title, content));
                }
            }

            if (results.isEmpty()) {
                // 降级为 LIKE 模糊搜索
                return fallbackSearch(query, topK);
            }

            return String.join("\n\n---\n\n", results);

        } catch (Exception e) {
            log.error("知识库搜索失败: {}", e.getMessage(), e);
            return fallbackSearch(query, topK);
        }
    }

    @Override
    public void clearAll() {
        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM kb_documents");
                stmt.execute("DELETE FROM kb_meta");
            }
            log.info("知识库已清空");
        } catch (Exception e) {
            log.error("清空知识库失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public long getDocumentCount() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM kb_meta")) {
            return rs.getLong(1);
        } catch (Exception e) {
            return 0;
        }
    }

    // ============================================================
    // 内部方法
    // ============================================================

    /**
     * 智能文本分块（按段落 → 按句子）
     */
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        // 按段落分割
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (current.length() + para.length() < CHUNK_SIZE) {
                current.append(para).append("\n\n");
            } else {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                }
                current = new StringBuilder(para).append("\n\n");
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        // 过大的块再切分
        List<String> finalChunks = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.length() > CHUNK_SIZE * 2) {
                // 按句子切分
                String[] sentences = chunk.split("(?<=[。！？.!?])\\s*");
                StringBuilder sb = new StringBuilder();
                for (String sent : sentences) {
                    if (sb.length() + sent.length() < CHUNK_SIZE) {
                        sb.append(sent);
                    } else {
                        if (sb.length() > 0) finalChunks.add(sb.toString().trim());
                        sb = new StringBuilder(sent);
                    }
                }
                if (sb.length() > 0) finalChunks.add(sb.toString().trim());
            } else {
                finalChunks.add(chunk);
            }
        }

        return finalChunks;
    }

    /**
     * 提取中英文关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        // 中文连续字符（2字以上）
        var matcher = java.util.regex.Pattern.compile("[\\u4e00-\\u9fff]{2,}").matcher(text);
        while (matcher.find()) keywords.add(matcher.group());
        // 英文单词
        matcher = java.util.regex.Pattern.compile("[a-zA-Z]+").matcher(text.toLowerCase());
        while (matcher.find()) keywords.add(matcher.group());
        return keywords;
    }

    /**
     * 降级模糊搜索
     */
    private String fallbackSearch(String query, int topK) {
        try {
            String sql = "SELECT content, title FROM kb_documents WHERE content LIKE ? LIMIT ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, "%" + query + "%");
                stmt.setInt(2, topK);
                ResultSet rs = stmt.executeQuery();
                List<String> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(String.format("[%s]\n%s", rs.getString("title"), rs.getString("content")));
                }
                return String.join("\n\n---\n\n", results);
            }
        } catch (Exception e) {
            return "";
        }
    }
}
