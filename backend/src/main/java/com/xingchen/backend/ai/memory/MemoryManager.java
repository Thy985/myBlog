package com.xingchen.backend.ai.memory;

import com.xingchen.backend.ai.model.Intent;

import java.util.List;

/**
 * 记忆管理器接口
 */
public interface MemoryManager {
    
    /**
     * 加载记忆上下文
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param requirements 记忆需求
     * @return 记忆上下文
     */
    MemoryContext load(Long userId, String sessionId, MemoryRequirements requirements);
    
    /**
     * 保存记忆
     * @param userId 用户ID
     * @param memories 新的记忆项
     */
    void save(Long userId, List<MemoryContext.MemoryItem> memories);
    
    /**
     * 添加工作记忆
     * @param sessionId 会话ID
     * @param item 记忆项
     */
    void addWorkingMemory(String sessionId, MemoryContext.MemoryItem item);
    
    /**
     * 清除用户记忆
     * @param userId 用户ID
     */
    void clearUserMemory(Long userId);
    
    /**
     * 清除会话记忆
     * @param sessionId 会话ID
     */
    void clearSessionMemory(String sessionId);
    
    /**
     * 检索记忆上下文（简化版，用于AgentOrchestrator）
     * @param userInput 用户输入
     * @param sessionId 会话ID
     * @return 记忆上下文
     */
    String retrieveMemory(String userInput, String sessionId);
    
    /**
     * 更新记忆（简化版，用于AgentOrchestrator）
     * @param userInput 用户输入
     * @param response 响应
     * @param sessionId 会话ID
     */
    void updateMemory(String userInput, String response, String sessionId);
    
    /**
     * 记忆需求
     */
    class MemoryRequirements {
        private boolean needWorking = true;
        private boolean needShortTerm = true;
        private boolean needLongTerm = false;
        private String queryForLongTerm = null;
        private int shortTermRounds = 10;
        
        public static MemoryRequirements all() {
            return new MemoryRequirements()
                    .withWorking(true)
                    .withShortTerm(true)
                    .withLongTerm(true);
        }
        
        public static MemoryRequirements minimal() {
            return new MemoryRequirements()
                    .withWorking(true)
                    .withShortTerm(false)
                    .withLongTerm(false);
        }
        
        public static MemoryRequirements fromIntent(Intent intent) {
            if (intent == null || !intent.isRequiresMemory()) {
                return minimal();
            }
            
            MemoryRequirements req = new MemoryRequirements();
            req.needWorking = true;
            req.needShortTerm = true;
            
            // 根据意图类型决定是否需要长期记忆
            if (intent.getRequiredMemoryTypes() != null) {
                req.needLongTerm = intent.getRequiredMemoryTypes().contains(Intent.MemoryType.LONG_TERM);
            }
            
            if (req.needLongTerm) {
                req.queryForLongTerm = intent.getOriginalMessage();
            }
            
            return req;
        }
        
        // Getters and Setters
        public boolean isNeedWorking() { return needWorking; }
        public MemoryRequirements withWorking(boolean need) { this.needWorking = need; return this; }
        
        public boolean isNeedShortTerm() { return needShortTerm; }
        public MemoryRequirements withShortTerm(boolean need) { this.needShortTerm = need; return this; }
        
        public boolean isNeedLongTerm() { return needLongTerm; }
        public MemoryRequirements withLongTerm(boolean need) { this.needLongTerm = need; return this; }
        
        public String getQueryForLongTerm() { return queryForLongTerm; }
        public MemoryRequirements withQuery(String query) { this.queryForLongTerm = query; return this; }
        
        public int getShortTermRounds() { return shortTermRounds; }
        public MemoryRequirements withRounds(int rounds) { this.shortTermRounds = rounds; return this; }
    }
}