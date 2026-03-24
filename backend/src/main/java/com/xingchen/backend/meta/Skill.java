package com.xingchen.backend.meta;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Skill - 智能体能力单元
 * 
 * Skill 是用户可感知的功能单元，由元能力引擎动态生成
 */
@Data
@Builder
public class Skill {
    
    /**
     * Skill ID
     */
    private String id;
    
    /**
     * Skill 名称
     */
    private String name;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 触发条件
     */
    private TriggerCondition trigger;
    
    /**
     * 执行步骤
     */
    private List<SkillStep> steps;
    
    /**
     * 使用的工具
     */
    private List<String> tools;
    
    /**
     * 代码实现（如果是代码型Skill）
     */
    private String code;
    
    /**
     * 代码语言
     */
    private String codeLanguage;
    
    /**
     * 版本
     */
    private String version;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 来源（自动生成/手动创建/社区共享）
     */
    private SkillSource source;
    
    /**
     * 状态
     */
    private SkillStatus status;
    
    /**
     * 执行统计
     */
    private ExecutionStats stats;
    
    /**
     * 标签
     */
    private List<String> tags;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 触发条件
     */
    @Data
    @Builder
    public static class TriggerCondition {
        private TriggerType type;
        private String pattern;
        private List<String> keywords;
        private String intent;
        private double confidenceThreshold;
        
        public enum TriggerType {
            KEYWORD,      // 关键词匹配
            INTENT,       // 意图识别
            REGEX,        // 正则匹配
            SEMANTIC,     // 语义匹配
            SCHEDULED,    // 定时触发
            EVENT         // 事件触发
        }
    }
    
    /**
     * Skill 步骤
     */
    @Data
    @Builder
    public static class SkillStep {
        private int order;
        private String name;
        private String description;
        private StepType type;
        private String toolId;
        private Map<String, Object> parameters;
        private String condition;
        private List<String> dependencies;
        private int retryCount;
        private String fallbackStep;
        
        public enum StepType {
            LLM_CALL,      // 调用LLM
            TOOL_CALL,     // 调用工具
            CODE_EXEC,     // 执行代码
            CONDITION,     // 条件判断
            LOOP,          // 循环
            PARALLEL,      // 并行执行
            WAIT,          // 等待
            OUTPUT         // 输出结果
        }
    }
    
    /**
     * Skill 来源
     */
    public enum SkillSource {
        AUTO_GENERATED,   // 自动生成
        MANUAL_CREATED,   // 手动创建
        COMMUNITY_SHARED, // 社区共享
        SYSTEM_BUILTIN    // 系统内置
    }
    
    /**
     * Skill 状态
     */
    public enum SkillStatus {
        DRAFT,        // 草稿
        TESTING,      // 测试中
        ACTIVE,       // 已激活
        DEPRECATED,   // 已弃用
        ERROR         // 错误状态
    }
    
    /**
     * 执行统计
     */
    @Data
    @Builder
    public static class ExecutionStats {
        private int totalExecutions;
        private int successfulExecutions;
        private int failedExecutions;
        private double averageLatency;
        private double successRate;
        private LocalDateTime lastExecutionTime;
        private String lastError;
    }
    
    /**
     * 计算成功率
     */
    public double calculateSuccessRate() {
        if (stats == null || stats.totalExecutions == 0) {
            return 0.0;
        }
        return (double) stats.successfulExecutions / stats.totalExecutions;
    }
    
    /**
     * 更新执行统计
     */
    public void recordExecution(boolean success, long latency, String error) {
        if (stats == null) {
            stats = ExecutionStats.builder()
                    .totalExecutions(0)
                    .successfulExecutions(0)
                    .failedExecutions(0)
                    .averageLatency(0)
                    .successRate(0)
                    .build();
        }
        
        stats.totalExecutions++;
        if (success) {
            stats.successfulExecutions++;
        } else {
            stats.failedExecutions++;
            stats.lastError = error;
        }
        
        // 更新平均延迟
        stats.averageLatency = (stats.averageLatency * (stats.totalExecutions - 1) + latency) 
                / stats.totalExecutions;
        
        // 更新成功率
        stats.successRate = calculateSuccessRate();
        stats.lastExecutionTime = LocalDateTime.now();
    }
}