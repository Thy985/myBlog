package com.xingchen.backend.ai.memory;

import com.xingchen.backend.ai.model.Intent;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 记忆上下文
 * 包含多层记忆数据
 */
@Data
@Builder
public class MemoryContext {
    
    /** 工作记忆 - 当前会话 */
    private List<MemoryItem> workingMemory;
    
    /** 短期记忆 - 最近N轮 */
    private List<MemoryItem> shortTermMemory;
    
    /** 长期记忆 - 语义相关 */
    private List<MemoryItem> longTermMemory;
    
    /** 用户画像 */
    private UserProfile userProfile;
    
    /**
     * 构建Prompt用的记忆文本
     */
    public String toPromptText() {
        StringBuilder sb = new StringBuilder();
        
        // 长期记忆
        if (longTermMemory != null && !longTermMemory.isEmpty()) {
            sb.append("【长期记忆】\n");
            longTermMemory.forEach(m -> sb.append("- ").append(m.getContent()).append("\n"));
            sb.append("\n");
        }
        
        // 短期记忆
        if (shortTermMemory != null && !shortTermMemory.isEmpty()) {
            sb.append("【近期对话】\n");
            shortTermMemory.forEach(m -> {
                sb.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
            });
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return (workingMemory == null || workingMemory.isEmpty())
                && (shortTermMemory == null || shortTermMemory.isEmpty())
                && (longTermMemory == null || longTermMemory.isEmpty());
    }
    
    @Data
    @Builder
    public static class MemoryItem {
        private String id;
        private String role;        // user / assistant / system
        private String content;
        private long timestamp;
        private double importance;  // 重要性评分
        private MemoryType type;
    }
    
    @Data
    @Builder
    public static class UserProfile {
        private Long userId;
        private String preferredStyle;
        private List<String> interests;
        private List<String> commonTopics;
    }
    
    public enum MemoryType {
        FACT,       // 事实
        PREFERENCE, // 偏好
        EVENT,      // 事件
        DECISION    // 决策
    }
}