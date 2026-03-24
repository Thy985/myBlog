package com.xingchen.backend.meta;

import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Skill 存储仓库
 */
@Repository
public class SkillRepository {

    private final Connection connection;

    public SkillRepository(Connection connection) {
        this.connection = connection;
        initTable();
    }

    private void initTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS skills (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT,
                    trigger_type TEXT,
                    trigger_pattern TEXT,
                    steps TEXT,
                    tools TEXT,
                    code TEXT,
                    code_language TEXT,
                    version TEXT,
                    author TEXT,
                    source TEXT,
                    status TEXT,
                    tags TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    metadata TEXT
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("初始化 Skill 表失败", e);
        }
    }

    public void save(Skill skill) {
        String sql = """
            INSERT INTO skills (id, name, description, trigger_type, trigger_pattern, 
                               steps, tools, code, code_language, version, author, 
                               source, status, tags, create_time, update_time, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                description = excluded.description,
                trigger_type = excluded.trigger_type,
                trigger_pattern = excluded.trigger_pattern,
                steps = excluded.steps,
                tools = excluded.tools,
                code = excluded.code,
                code_language = excluded.code_language,
                version = excluded.version,
                author = excluded.author,
                source = excluded.source,
                status = excluded.status,
                tags = excluded.tags,
                update_time = excluded.update_time,
                metadata = excluded.metadata
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, skill.getId());
            ps.setString(2, skill.getName());
            ps.setString(3, skill.getDescription());
            ps.setString(4, skill.getTrigger() != null ? skill.getTrigger().getType().name() : null);
            ps.setString(5, skill.getTrigger() != null ? skill.getTrigger().getPattern() : null);
            ps.setString(6, serializeSteps(skill.getSteps()));
            ps.setString(7, serializeList(skill.getTools()));
            ps.setString(8, skill.getCode());
            ps.setString(9, skill.getCodeLanguage());
            ps.setString(10, skill.getVersion());
            ps.setString(11, skill.getAuthor());
            ps.setString(12, skill.getSource() != null ? skill.getSource().name() : null);
            ps.setString(13, skill.getStatus() != null ? skill.getStatus().name() : null);
            ps.setString(14, serializeList(skill.getTags()));
            ps.setTimestamp(15, Timestamp.valueOf(skill.getCreateTime() != null ? skill.getCreateTime() : LocalDateTime.now()));
            ps.setTimestamp(16, Timestamp.valueOf(skill.getUpdateTime() != null ? skill.getUpdateTime() : LocalDateTime.now()));
            ps.setString(17, serializeMetadata(skill.getMetadata()));

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("保存 Skill 失败", e);
        }
    }

    public Skill findById(String id) {
        String sql = "SELECT * FROM skills WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 Skill 失败", e);
        }
        return null;
    }

    public List<Skill> findAll() {
        String sql = "SELECT * FROM skills";
        List<Skill> skills = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                skills.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询所有 Skill 失败", e);
        }
        return skills;
    }

    public void delete(String id) {
        String sql = "DELETE FROM skills WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("删除 Skill 失败", e);
        }
    }

    private Skill mapResultSet(ResultSet rs) throws SQLException {
        return Skill.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .code(rs.getString("code"))
                .codeLanguage(rs.getString("code_language"))
                .version(rs.getString("version"))
                .author(rs.getString("author"))
                .createTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null)
                .updateTime(rs.getTimestamp("update_time") != null ? rs.getTimestamp("update_time").toLocalDateTime() : null)
                .build();
    }

    private String serializeSteps(List<Skill.SkillStep> steps) {
        if (steps == null) return null;
        // 简化实现，实际应该使用 JSON 序列化
        return steps.toString();
    }

    private String serializeList(List<String> list) {
        if (list == null) return null;
        return String.join(",", list);
    }

    private String serializeMetadata(java.util.Map<String, Object> metadata) {
        if (metadata == null) return null;
        return metadata.toString();
    }
}
