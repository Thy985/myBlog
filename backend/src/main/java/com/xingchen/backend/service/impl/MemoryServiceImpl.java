package com.xingchen.backend.service.impl;

import com.xingchen.backend.service.MemoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 记忆服务（对标 OpenClaw 三层记忆架构）
 * 第1层：SQLite 对话历史（会话级）
 * 第2层：MEMORY.md 长期记忆（跨会话，自动提取）
 * 第3层：知识库（由 KnowledgeBaseService 管理）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final ChatLanguageModel chatLanguageModel;

    @Value("${agent.workspace:./data/agent}")
    private String workspace;

    @Value("${agent.memory.max-rounds:50}")
    private int maxRounds;

    // 触发记忆提取的关键词
    private static final List<String> TRIGGER_KEYWORDS = List.of(
            "我喜欢", "我偏好", "我习惯", "我常用", "我是", "我叫", "我的名字",
            "我在", "我做", "我的工作", "不要用", "别用", "不喜欢",
            "项目", "技术栈", "框架", "决定", "选择", "确定",
            "踩坑", "教训", "发现", "原来", "注意", "记住"
    );

    private static final String MEMORY_EXTRACT_PROMPT = """
            分析以下对话，提取值得长期记住的用户信息。
            类别：用户信息、用户偏好、项目信息、重要决策、经验教训
            已有记忆：%s
            输出 Markdown 列表，格式 "- [类别] 内容"。无则输出"无"。
            对话：%s
            """;

    @Override
    public void initUserMemory(Long userId) {
        try {
            Path userDir = getUserDir(userId);
            Files.createDirectories(userDir);
            Path memFile = userDir.resolve("MEMORY.md");
            if (!Files.exists(memFile)) {
                Files.writeString(memFile, """
                        # 用户记忆
                        > 由系统自动维护
                        ## 用户信息
                        ## 用户偏好
                        ## 项目信息
                        ## 重要决策
                        ## 经验教训
                        """);
            }
            try (Connection conn = getConnection(userId)) {
                initSqlite(conn);
            }
        } catch (Exception e) {
            log.error("初始化记忆失败: {}", userId, e);
        }
    }

    @Override
    public void saveConversation(Long userId, String role, String content) {
        String sql = "INSERT INTO conversation (role, content, created_at) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setString(2, content);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.executeUpdate();
            cleanupOld(userId);
        } catch (Exception e) {
            log.error("保存对话失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存对话并自动提取记忆
     */
    public void saveConversationAndExtract(Long userId, String userMsg, String assistantMsg) {
        saveConversation(userId, "user", userMsg);
        saveConversation(userId, "assistant", assistantMsg);
        if (shouldExtract(userMsg, assistantMsg)) {
            extractMemoryAsync(userId, userMsg, assistantMsg);
        }
    }

    @Override
    public List<Map<String, Object>> getRecentConversations(Long userId, int limit) {
        String sql = "SELECT role, content, created_at FROM conversation ORDER BY id DESC LIMIT ?";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(Map.of(
                        "role", rs.getString("role"),
                        "content", rs.getString("content"),
                        "created_at", rs.getString("created_at")
                ));
            }
        } catch (Exception e) {
            log.error("获取对话失败: {}", e.getMessage(), e);
        }
        Collections.reverse(results);
        return results;
    }

    @Override
    public void saveEntity(Long userId, String name, String type, String content) {
        String sql = "INSERT OR REPLACE INTO entity (name, type, content, updated_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, type);
            stmt.setString(3, content);
            stmt.setString(4, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error("保存实体失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> searchEntities(Long userId, String keyword, int limit) {
        String sql = "SELECT name, type, content FROM entity WHERE name LIKE ? OR content LIKE ? LIMIT ?";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(Map.of("name", rs.getString("name"), "type", rs.getString("type"), "content", rs.getString("content")));
            }
        } catch (Exception e) {
            log.error("搜索实体失败: {}", e.getMessage(), e);
        }
        return results;
    }

    @Override
    public void savePreference(Long userId, String key, String value) {
        String sql = "INSERT OR REPLACE INTO preference (key, value, updated_at) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error("保存偏好失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getPreferences(Long userId) {
        Map<String, Object> prefs = new HashMap<>();
        try (Connection conn = getConnection(userId);
             ResultSet rs = conn.createStatement().executeQuery("SELECT key, value FROM preference")) {
            while (rs.next()) prefs.put(rs.getString("key"), rs.getString("value"));
        } catch (Exception e) {
            log.error("获取偏好失败: {}", e.getMessage(), e);
        }
        return prefs;
    }

    @Override
    public String getUserMemory(Long userId) {
        try {
            Path memFile = getUserDir(userId).resolve("MEMORY.md");
            return Files.exists(memFile) ? Files.readString(memFile) : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void appendToMemory(Long userId, String content) {
        appendToMemoryWithCategory(userId, "其他", content);
    }

    public void appendToMemoryWithCategory(Long userId, String category, String content) {
        try {
            Path memFile = getUserDir(userId).resolve("MEMORY.md");
            String existing = Files.exists(memFile) ? Files.readString(memFile) : "";
            if (existing.contains(content.trim())) return;

            String timestamp = LocalDateTime.now().toLocalDate().toString();
            String entry = "- " + content.trim() + " (" + timestamp + ")\n";
            String header = "## " + category;

            if (existing.contains(header)) {
                int idx = existing.indexOf(header) + header.length();
                int next = existing.indexOf("\n## ", idx);
                if (next == -1) next = existing.length();
                existing = existing.substring(0, next) + entry + existing.substring(next);
            } else {
                existing = existing + "\n" + header + "\n" + entry;
            }

            Files.writeString(memFile, existing);
            log.info("用户 {} 记忆[{}]更新: {}", userId, category, content.substring(0, Math.min(50, content.length())));
        } catch (Exception e) {
            log.error("追加记忆失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void clearUserMemory(Long userId) {
        try {
            initUserMemory(userId);
            try (Connection conn = getConnection(userId); Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM conversation");
                stmt.execute("DELETE FROM entity");
                stmt.execute("DELETE FROM preference");
            }
        } catch (Exception e) {
            log.error("清空记忆失败: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // 自动记忆提取
    // ============================================================

    private boolean shouldExtract(String userMsg, String assistantMsg) {
        if (userMsg == null || assistantMsg == null) return false;
        String combined = userMsg + " " + assistantMsg;
        if (combined.length() < 80) return false;
        for (String kw : TRIGGER_KEYWORDS) {
            if (combined.contains(kw)) return true;
        }
        return combined.length() > 300;
    }

    @Async
    public void extractMemoryAsync(Long userId, String userMsg, String assistantMsg) {
        try {
            String existing = getUserMemory(userId);
            String conversation = "用户: " + userMsg + "\n助手: " + assistantMsg;
            String prompt = String.format(MEMORY_EXTRACT_PROMPT, existing, conversation);

            List<ChatMessage> msgs = List.of(
                    SystemMessage.from("你是信息提取助手。只输出 Markdown 列表或无。"),
                    UserMessage.from(prompt)
            );
            Response<AiMessage> resp = chatLanguageModel.generate(msgs);
            String result = resp.content().text().trim();

            if ("无".equals(result) || result.isBlank()) return;

            for (String line : result.split("\n")) {
                line = line.trim();
                if (!line.startsWith("- ")) continue;
                String content = line.substring(2).trim();
                Matcher m = Pattern.compile("\\[(.+?)]\\s*(.+)").matcher(content);
                if (m.matches()) {
                    appendToMemoryWithCategory(userId, m.group(1).trim(), m.group(2).trim());
                } else {
                    appendToMemory(userId, content);
                }
            }
            log.info("用户 {} 记忆自动更新完成", userId);
        } catch (Exception e) {
            log.warn("记忆提取失败: {}", e.getMessage());
        }
    }

    // ============================================================
    // SQLite 基础设施
    // ============================================================

    private Path getUserDir(Long userId) {
        return Path.of(workspace.replace("~", System.getProperty("user.home")), "user_" + userId);
    }

    private Connection getConnection(Long userId) throws SQLException {
        Path dbPath = getUserDir(userId).resolve("memory.db");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        conn.createStatement().execute("PRAGMA journal_mode=WAL");
        initSqlite(conn);
        return conn;
    }

    private void initSqlite(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS conversation (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT, content TEXT, created_at TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS entity (name TEXT PRIMARY KEY, type TEXT, content TEXT, updated_at TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS preference (key TEXT PRIMARY KEY, value TEXT, updated_at TEXT)");
        } catch (Exception e) {
            log.error("初始化 SQLite 失败: {}", e.getMessage(), e);
        }
    }

    private void cleanupOld(Long userId) {
        // 1. 按数量清理 - 保留最近 maxRounds * 2 条
        String sqlCount = "DELETE FROM conversation WHERE id NOT IN (SELECT id FROM conversation ORDER BY id DESC LIMIT ?)";
        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(sqlCount)) {
            stmt.setInt(1, maxRounds * 2);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                log.debug("用户 {} 清理旧对话 {} 条（数量限制）", userId, deleted);
            }
        } catch (Exception e) {
            log.error("清理旧对话失败: {}", e.getMessage(), e);
        }

        // 2. 按时间清理 - 删除超过 90 天的对话
        String sqlTime = "DELETE FROM conversation WHERE created_at < datetime('now', '-90 days')";
        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(sqlTime)) {
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                log.info("用户 {} 归档旧对话 {} 条（超过90天）", userId, deleted);
            }
        } catch (Exception e) {
            log.error("按时间清理对话失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 归档旧记忆（超过保留期的数据）
     * 由定时任务调用
     */
    @Override
    public void archiveOldMemories(Long userId, int retentionDays) {
        String cutoffDate = java.time.LocalDateTime.now()
                .minusDays(retentionDays)
                .toString();

        // 1. 归档对话历史到文件
        String selectSql = "SELECT role, content, created_at FROM conversation WHERE created_at < ? ORDER BY created_at";
        String archiveFileName = "conversation_archive_" + userId + "_" + java.time.LocalDate.now() + ".json";

        try (Connection conn = getConnection(userId);
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, cutoffDate);
            ResultSet rs = stmt.executeQuery();

            java.util.List<java.util.Map<String, Object>> archivedConversations = new java.util.ArrayList<>();
            while (rs.next()) {
                archivedConversations.add(java.util.Map.of(
                        "role", rs.getString("role"),
                        "content", rs.getString("content"),
                        "created_at", rs.getString("created_at")
                ));
            }

            if (!archivedConversations.isEmpty()) {
                // 写入归档文件
                Path archiveDir = getUserDir(userId).resolve("archives");
                Files.createDirectories(archiveDir);
                Path archiveFile = archiveDir.resolve(archiveFileName);

                String json = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(archivedConversations);
                Files.writeString(archiveFile, json);

                // 删除已归档的数据
                String deleteSql = "DELETE FROM conversation WHERE created_at < ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, cutoffDate);
                    int deleted = deleteStmt.executeUpdate();
                    log.info("用户 {} 归档 {} 条对话到 {}", userId, deleted, archiveFileName);
                }
            }
        } catch (Exception e) {
            log.error("归档记忆失败: {}", e.getMessage(), e);
        }

        // 2. 清理 MEMORY.md 中过期的条目（可选）
        cleanupOldMemoryEntries(userId, retentionDays);
    }

    /**
     * 清理 MEMORY.md 中过期的条目
     */
    private void cleanupOldMemoryEntries(Long userId, int retentionDays) {
        try {
            Path memFile = getUserDir(userId).resolve("MEMORY.md");
            if (!Files.exists(memFile)) return;

            String content = Files.readString(memFile);
            java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusDays(retentionDays);

            // 简单清理：删除包含过期日期的行（格式: (YYYY-MM-DD)）
            String[] lines = content.split("\n");
            StringBuilder cleaned = new StringBuilder();
            int removedCount = 0;

            for (String line : lines) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\((\\d{4}-\\d{2}-\\d{2})\\)").matcher(line);
                if (matcher.find()) {
                    java.time.LocalDate entryDate = java.time.LocalDate.parse(matcher.group(1));
                    if (entryDate.isBefore(cutoffDate)) {
                        removedCount++;
                        continue; // 跳过过期条目
                    }
                }
                cleaned.append(line).append("\n");
            }

            if (removedCount > 0) {
                Files.writeString(memFile, cleaned.toString());
                log.info("用户 {} 清理 MEMORY.md 中 {} 条过期条目", userId, removedCount);
            }
        } catch (Exception e) {
            log.warn("清理 MEMORY.md 失败: {}", e.getMessage());
        }
    }

    @Override
    public int getConversationCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM conversation";
        try (Connection conn = getConnection(userId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("获取对话数量失败: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public int getEntityCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM entity";
        try (Connection conn = getConnection(userId);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            log.error("获取实体数量失败: {}", e.getMessage());
        }
        return 0;
    }
}
