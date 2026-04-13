package com.xingchen.backend.ai.tool;

import java.util.Map;

/**
 * 工具接口
 * 所有AI工具必须实现此接口
 */
public interface Tool {
    
    /**
     * 获取工具名称
     */
    String getName();
    
    /**
     * 获取工具描述
     */
    String getDescription();
    
    /**
     * 获取参数定义
     */
    ToolParameter[] getParameters();
    
    /**
     * 执行工具
     * @param parameters 参数
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> parameters);
    
    /**
     * 是否异步执行
     */
    default boolean isAsync() {
        return false;
    }
    
    /**
     * 执行超时时间（毫秒）
     */
    default long getTimeout() {
        return 30000; // 默认30秒
    }
    
    /**
     * 判断工具是否与用户输入相关
     * @param userInput 用户输入
     * @param intent 用户意图
     * @return 是否相关
     */
    default boolean isRelevant(String userInput, String intent) {
        // 默认实现：检查用户输入是否包含工具名称或描述中的关键词
        String input = userInput.toLowerCase();
        return input.contains(getName().toLowerCase()) || 
               input.contains(getDescription().toLowerCase());
    }

    /**
     * 从用户输入中提取参数
     * @param userInput 用户输入
     * @return 提取的参数
     */
    default java.util.Map<String, Object> extractParameters(String userInput) {
        // 默认实现：返回空参数
        return java.util.Map.of();
    }
    
    /**
     * 工具参数定义
     */
    record ToolParameter(
            String name,
            String description,
            String type,
            boolean required,
            Object defaultValue
    ) {}
    
    /**
     * 工具执行结果
     */
    record ToolResult(
            boolean success,
            Object data,
            String message,
            long executionTime
    ) {
        public static ToolResult success(Object data) {
            return new ToolResult(true, data, "执行成功", 0);
        }
        
        public static ToolResult success(Object data, String message) {
            return new ToolResult(true, data, message, 0);
        }
        
        public static ToolResult error(String message) {
            return new ToolResult(false, null, message, 0);
        }
        
        public ToolResult withExecutionTime(long time) {
            return new ToolResult(success, data, message, time);
        }
    }
}