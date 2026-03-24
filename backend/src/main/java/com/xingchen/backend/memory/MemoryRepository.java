package com.xingchen.backend.memory;

import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 记忆存储仓库
 * 使用 SQLite 存储记忆和向量
 */
@Repository
public class MemoryRepository {

    private final Connection connection;

    public MemoryRepository(Connection connection) {
        this.connection = connection;
        initTable();
    }

    private void initTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS memories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    embedding BLOB,
                    category TEXT DEFAULT 'GENERAL',
                    confidence INTEGER DEFAULT 1,
                    ttl_days INTEGER DEFAULT 30,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expire_time TIMESTAMP
                )
            """);
            
            // 创建索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_memories_user_id ON memories(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_memories_category ON memories(category)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_memories_expire ON memories(expire_time)");
        } catch (SQLException e) {
            throw new RuntimeException("初始化记忆表失败", e);
        }
    }

    public void save(Memory memory) {
        String sql = """
            INSERT INTO memories (user_id, content, embedding, category, confidence, ttl_days, create_time, update_time, expire_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                content = excluded.content,
                embedding = excluded.embedding,
                category = excluded.category,
                confidence = excluded.confidence,
                ttl_days = excluded.ttl_days,
                update_time = excluded.update_time,
                expire_time = excluded.expire_time
        """;
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, memory.getUserId());
            ps.setString(2, memory.getContent());
            ps.setBytes(3, floatArrayToBytes(memory.getEmbedding()));
            ps.setString(4, memory.getCategory());
            ps.setInt(5, memory.getConfidence());
            ps.setInt(6, memory.getTtlDays());
            ps.setTimestamp(7, Timestamp.valueOf(memory.getCreateTime()));
            ps.setTimestamp(8, Timestamp.valueOf(memory.getUpdateTime()));
            
            LocalDateTime expireTime = memory.getCreateTime().plusDays(memory.getTtlDays());
            ps.setTimestamp(9, Timestamp.valueOf(expireTime));
            
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("保存记忆失败", e);
        }
    }

    public List<Memory> findByUserId(Long userId) {
        String sql = "SELECT * FROM memories WHERE user_id = ? AND (expire_time IS NULL OR expire_time > CURRENT_TIMESTAMP)";
        List<Memory> memories = new ArrayList<>();
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                memories.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询记忆失败", e);
        }
        
        return memories;
    }

    public void deleteExpired() {
        String sql = "DELETE FROM memories WHERE expire_time < CURRENT_TIMESTAMP";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("清理过期记忆失败", e);
        }
    }

    public Memory findById(Long id) {
        String sql = "SELECT * FROM memories WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询记忆失败", e);
        }
        return null;
    }

    public List<Memory> findAll() {
        String sql = "SELECT * FROM memories";
        List<Memory> memories = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                memories.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询所有记忆失败", e);
        }
        return memories;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM memories WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("删除记忆失败", e);
        }
    }

    private Memory mapResultSet(ResultSet rs) throws SQLException {
        Memory memory = new Memory();
        memory.setId(rs.getLong("id"));
        memory.setUserId(rs.getLong("user_id"));
        memory.setContent(rs.getString("content"));
        memory.setEmbedding(bytesToFloatArray(rs.getBytes("embedding")));
        memory.setCategory(rs.getString("category"));
        memory.setConfidence(rs.getInt("confidence"));
        memory.setTtlDays(rs.getInt("ttl_days"));
        memory.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        memory.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        
        Timestamp expireTime = rs.getTimestamp("expire_time");
        if (expireTime != null) {
            memory.setExpireTime(expireTime.toLocalDateTime());
        }
        
        return memory;
    }

    private byte[] floatArrayToBytes(float[] floats) {
        if (floats == null) return null;
        byte[] bytes = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = Float.floatToIntBits(floats[i]);
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return bytes;
    }

    private float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null) return null;
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            int bits = ((bytes[i * 4] & 0xFF) << 24) |
                      ((bytes[i * 4 + 1] & 0xFF) << 16) |
                      ((bytes[i * 4 + 2] & 0xFF) << 8) |
                      (bytes[i * 4 + 3] & 0xFF);
            floats[i] = Float.intBitsToFloat(bits);
        }
        return floats;
    }
}