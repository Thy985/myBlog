package com.xingchen.backend.ai.memory;

import java.util.List;

public interface UnifiedMemoryService {

    UnifiedMemoryContext loadContext(Long userId, String sessionId, MemoryRequirements requirements);

    void saveMemories(Long userId, String sessionId, List<UnifiedMemoryContext.MemoryItem> memories);

    void addWorkingMemory(Long userId, String sessionId, UnifiedMemoryContext.MemoryItem item);

    void clearSessionMemory(Long userId, String sessionId);

    void clearUserMemory(Long userId);

    String retrieveMemoryText(Long userId, String query, int limit);

    MemoryAnalysis analyzeAndSave(Long userId, String userMessage, String assistantMessage);

    void persistWorkingToShortTerm(Long userId, String sessionId);

    MemoryStats getMemoryStats(Long userId);

    List<String> getUserSessions(Long userId);

    record MemoryRequirements(
        boolean needWorking,
        boolean needShortTerm,
        boolean needLongTerm,
        String queryForLongTerm,
        int shortTermRounds
    ) {
        public static MemoryRequirements minimal() {
            return new MemoryRequirements(true, false, false, null, 0);
        }

        public static MemoryRequirements all() {
            return new MemoryRequirements(true, true, true, null, 10);
        }

        public static MemoryRequirements forQuery(String query, int shortTermRounds) {
            return new MemoryRequirements(true, true, true, query, shortTermRounds);
        }
    }

    record MemoryAnalysis(
        boolean saved,
        List<UnifiedMemoryContext.MemoryItem> extractedMemories,
        List<String> categories,
        String summary
    ) {}

    record MemoryStats(
        Long userId,
        int shortTermCount,
        int longTermCount,
        int workingCount
    ) {
        public static MemoryStats empty(Long userId) {
            return new MemoryStats(userId, 0, 0, 0);
        }
    }
}