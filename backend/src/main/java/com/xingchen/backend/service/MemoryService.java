package com.xingchen.backend.service;

import java.util.List;
import java.util.Map;

public interface MemoryService {

    void initUserMemory(Long userId);

    void saveConversation(Long userId, String role, String content);

    List<Map<String, Object>> getRecentConversations(Long userId, int limit);

    void saveEntity(Long userId, String name, String type, String content);

    List<Map<String, Object>> searchEntities(Long userId, String keyword, int limit);

    void savePreference(Long userId, String key, String value);

    Map<String, Object> getPreferences(Long userId);

    String getUserMemory(Long userId);

    void appendToMemory(Long userId, String content);

    void clearUserMemory(Long userId);

    /**
     * 获取用户对话记录数量
     */
    int getConversationCount(Long userId);

    /**
     * 获取用户实体/知识点数量
     */
    int getEntityCount(Long userId);

    // ========== V2 版本新增方法 ==========

    /**
     * 获取用户记忆上下文
     */
    default String getUserMemoryContext(Long userId) {
        return getUserMemory(userId);
    }

    /**
     * 检索记忆
     */
    default String retrieveMemory(Long userId, String query, int limit) {
        return getUserMemory(userId);
    }

    /**
     * 添加记忆
     */
    default void addMemory(Long userId, String content, String type) {
        appendToMemory(userId, content);
    }

    /**
     * 归档旧记忆（超过保留期的数据）
     *
     * @param userId 用户ID
     * @param retentionDays 保留天数，超过此天数的数据将被归档
     */
    default void archiveOldMemories(Long userId, int retentionDays) {
        // 默认实现为空，由具体实现类覆盖
    }
}
