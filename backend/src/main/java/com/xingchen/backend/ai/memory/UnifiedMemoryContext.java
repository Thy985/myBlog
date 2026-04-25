package com.xingchen.backend.ai.memory;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UnifiedMemoryContext {

    private List<MemoryItem> workingMemory;
    private List<MemoryItem> shortTermMemory;
    private List<MemoryItem> longTermMemory;
    private UserProfile userProfile;

    public boolean isEmpty() {
        return (workingMemory == null || workingMemory.isEmpty())
                && (shortTermMemory == null || shortTermMemory.isEmpty())
                && (longTermMemory == null || longTermMemory.isEmpty());
    }

    public String toPromptText() {
        StringBuilder sb = new StringBuilder();

        if (longTermMemory != null && !longTermMemory.isEmpty()) {
            sb.append("【长期记忆】\n");
            longTermMemory.forEach(m -> sb.append("- ").append(m.getContent()).append("\n"));
            sb.append("\n");
        }

        if (shortTermMemory != null && !shortTermMemory.isEmpty()) {
            sb.append("【近期对话】\n");
            shortTermMemory.forEach(m -> sb.append(m.getRole()).append(": ").append(m.getContent()).append("\n"));
            sb.append("\n");
        }

        if (workingMemory != null && !workingMemory.isEmpty()) {
            sb.append("【当前任务上下文】\n");
            workingMemory.forEach(m -> sb.append(m.getRole()).append(": ").append(m.getContent()).append("\n"));
            sb.append("\n");
        }

        return sb.toString();
    }

    @Data
    @Builder
    public static class MemoryItem {
        private String id;
        private String role;
        private String content;
        private long timestamp;
        private double importance;
        private MemoryType type;
        private String category;
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
        FACT,
        PREFERENCE,
        EVENT,
        DECISION,
        CONVERSATION
    }
}