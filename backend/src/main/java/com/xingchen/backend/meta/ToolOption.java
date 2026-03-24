package com.xingchen.backend.meta;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 工具选项
 * 
 * 描述一个可用工具的元数据和能力
 */
@Data
@Builder
public class ToolOption {
    
    /**
     * 工具唯一标识
     */
    private String toolId;
    
    /**
     * 工具类型 (llm, code, file, search, audio, vision, database, cache, etc.)
     */
    private String toolType;
    
    /**
     * 工具描述
     */
    private String description;
    
    /**
     * 工具能力映射 (能力名称 -> 能力评分 0-1)
     */
    private Map<String, Double> capabilities;
    
    /**
     * 成本（每次调用）
     */
    private double cost;
    
    /**
     * 延迟（毫秒）
     */
    private double latency;
    
    /**
     * 准确率（0-1）
     */
    private double accuracy;
    
    /**
     * 是否本地工具
     */
    @Builder.Default
    private boolean local = true;
    
    /**
     * 是否 MCP 服务
     */
    @Builder.Default
    private boolean mcpService = false;
    
    /**
     * 获取特定能力的评分
     */
    public double getCapabilityScore(String capability) {
        return capabilities.getOrDefault(capability, 0.0);
    }
    
    /**
     * 检查是否支持特定能力
     */
    public boolean hasCapability(String capability) {
        return capabilities.containsKey(capability);
    }
    
    /**
     * 计算综合评分
     */
    public double calculateOverallScore() {
        if (capabilities == null || capabilities.isEmpty()) {
            return accuracy;
        }
        double avgCapability = capabilities.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        return (avgCapability + accuracy) / 2.0;
    }
}
