package com.xingchen.backend.meta;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 元能力 - 智能体自我进化的基本单元
 * 
 * 元能力是比插件更高层的抽象，它描述的是"如何创建能力"的能力
 */
@Data
@Builder
public class MetaCapability {
    
    /**
     * 元能力ID
     */
    private String id;
    
    /**
     * 名称
     */
    private String name;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 元能力类型
     */
    private MetaType type;
    
    /**
     * 输入参数定义
     */
    private List<ParameterDef> inputs;
    
    /**
     * 输出参数定义
     */
    private List<ParameterDef> outputs;
    
    /**
     * 执行步骤模板
     */
    private List<StepTemplate> steps;
    
    /**
     * 可用工具列表
     */
    private List<ToolOption> availableTools;
    
    /**
     * 验证规则
     */
    private List<ValidationRule> validations;
    
    /**
     * 修复策略
     */
    private List<RepairStrategy> repairStrategies;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 使用次数
     */
    private int usageCount;
    
    /**
     * 成功率
     */
    private double successRate;
    
    /**
     * 元能力类型
     */
    public enum MetaType {
        /** 代码生成 */
        CODE_GENERATION,
        /** 工具选择 */
        TOOL_SELECTION,
        /** 工作流编排 */
        WORKFLOW_ORCHESTRATION,
        /** 错误诊断 */
        ERROR_DIAGNOSIS,
        /** 自我修复 */
        SELF_REPAIR,
        /** 知识提取 */
        KNOWLEDGE_EXTRACTION,
        /** 技能合成 */
        SKILL_COMPOSITION
    }
    
    /**
     * 参数定义
     */
    @Data
    @Builder
    public static class ParameterDef {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
        private List<String> enumValues;
    }
    
    /**
     * 步骤模板
     */
    @Data
    @Builder
    public static class StepTemplate {
        private int order;
        private String name;
        private String description;
        private String actionType;
        private Map<String, Object> config;
        private List<String> dependencies;
        private String condition;
    }
    
    /**
     * 工具选项
     */
    @Data
    @Builder
    public static class ToolOption {
        private String toolId;
        private String toolType;
        private String description;
        private Map<String, Object> capabilities;
        private double cost;
        private double latency;
        private double accuracy;
    }
    
    /**
     * 验证规则
     */
    @Data
    @Builder
    public static class ValidationRule {
        private String name;
        private String condition;
        private String errorMessage;
        private ValidationType type;
        
        public enum ValidationType {
            SYNTAX, SEMANTIC, EXECUTION, OUTPUT
        }
    }
    
    /**
     * 修复策略
     */
    @Data
    @Builder
    public static class RepairStrategy {
        private String name;
        private List<String> errorPatterns;
        private String fixAction;
        private int maxRetries;
        private boolean fallbackToAlternative;
    }
}