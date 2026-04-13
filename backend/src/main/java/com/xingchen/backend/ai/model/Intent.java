package com.xingchen.backend.ai.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 意图识别结果
 */
@Data
@Builder
public class Intent {
    
    /** 意图类型 */
    private IntentType type;
    
    /** 置信度 0-1 */
    private double confidence;
    
    /** 原始消息 */
    private String originalMessage;
    
    /** 提取的实体 */
    private List<Entity> entities;
    
    /** 是否需要记忆 */
    private boolean requiresMemory;
    
    /** 需要的记忆类型 */
    private List<MemoryType> requiredMemoryTypes;
    
    /** 是否需要工具 */
    private boolean requiresTool;
    
    /** 可能需要的工具 */
    private List<String> possibleTools;
    
    public enum IntentType {
        UNKNOWN,            // 未知意图（低置信度）
        CHAT,               // 普通聊天
        CREATE_ARTICLE,     // 创建文章
        EDIT_ARTICLE,       // 编辑文章
        PUBLISH_ARTICLE,    // 发布文章
        SCHEDULE_TASK,      // 定时任务
        LIST_TASKS,         // 列出任务
        CANCEL_TASK,        // 取消任务
        SEARCH,             // 搜索
        SUMMARIZE,          // 总结
        TRANSLATE,          // 翻译
        CODE_GENERATE,      // 代码生成
        KNOWLEDGE_QUERY,    // 知识库查询
        SYSTEM_COMMAND      // 系统命令
    }
    
    public enum MemoryType {
        SHORT_TERM,     // 短期记忆
        LONG_TERM,      // 长期记忆
        WORKING         // 工作记忆
    }
    
    @Data
    @Builder
    public static class Entity {
        private String name;
        private String value;
        private int start;
        private int end;
    }
    
    /**
     * 创建聊天意图
     */
    public static Intent chat(String message) {
        return Intent.builder()
                .type(IntentType.CHAT)
                .confidence(1.0)
                .originalMessage(message)
                .requiresMemory(false)
                .requiresTool(false)
                .build();
    }

    /**
     * 创建未知意图（低置信度）
     */
    public static Intent unknown(String message, double confidence) {
        return Intent.builder()
                .type(IntentType.UNKNOWN)
                .confidence(confidence)
                .originalMessage(message)
                .requiresMemory(false)
                .requiresTool(false)
                .build();
    }
}