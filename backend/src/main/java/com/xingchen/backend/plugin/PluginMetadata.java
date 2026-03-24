package com.xingchen.backend.plugin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 插件元数据
 */
@Data
@Builder
public class PluginMetadata {
    
    /**
     * 插件唯一ID
     */
    private String id;
    
    /**
     * 插件名称
     */
    private String name;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件描述
     */
    private String description;
    
    /**
     * 插件作者
     */
    private String author;
    
    /**
     * 插件类型
     */
    private PluginType type;
    
    /**
     * 依赖的其他插件
     */
    private List<String> dependencies;
    
    /**
     * 支持的AI能力版本
     */
    private String apiVersion;
    
    /**
     * 入口类
     */
    private String entryClass;
    
    /**
     * 插件图标
     */
    private String icon;
    
    /**
     * 插件标签
     */
    private List<String> tags;
    
    /**
     * 额外属性
     */
    private Map<String, Object> properties;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 插件类型
     */
    public enum PluginType {
        /** 消息通道插件 */
        CHANNEL,
        /** 工具插件 */
        TOOL,
        /** 数据处理插件 */
        PROCESSOR,
        /** 存储插件 */
        STORAGE,
        /** 分析插件 */
        ANALYTICS,
        /** 自定义插件 */
        CUSTOM
    }
}