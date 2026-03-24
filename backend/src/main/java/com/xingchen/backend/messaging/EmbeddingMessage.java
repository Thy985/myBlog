package com.xingchen.backend.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Embedding 任务消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务类型
     */
    public enum TaskType {
        INDEX_DOCUMENT,    // 索引文档
        DELETE_DOCUMENT,   // 删除文档
        UPDATE_DOCUMENT,   // 更新文档
        BATCH_INDEX        // 批量索引
    }
    
    private TaskType taskType;
    private Long articleId;
    private String title;
    private String content;
    private String category;
    private Long timestamp;
    private String requestId;
    
    public static EmbeddingMessage index(Long articleId, String title, String content, String category) {
        EmbeddingMessage msg = new EmbeddingMessage();
        msg.taskType = TaskType.INDEX_DOCUMENT;
        msg.articleId = articleId;
        msg.title = title;
        msg.content = content;
        msg.category = category;
        msg.timestamp = System.currentTimeMillis();
        msg.requestId = java.util.UUID.randomUUID().toString();
        return msg;
    }
    
    public static EmbeddingMessage delete(Long articleId) {
        EmbeddingMessage msg = new EmbeddingMessage();
        msg.taskType = TaskType.DELETE_DOCUMENT;
        msg.articleId = articleId;
        msg.timestamp = System.currentTimeMillis();
        msg.requestId = java.util.UUID.randomUUID().toString();
        return msg;
    }
}