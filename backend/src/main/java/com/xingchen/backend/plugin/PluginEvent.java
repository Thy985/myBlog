package com.xingchen.backend.plugin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 插件事件
 */
@Data
public class PluginEvent {
    
    /**
     * 事件类型
     */
    private EventType type;
    
    /**
     * 事件源插件ID
     */
    private String sourcePluginId;
    
    /**
     * 事件目标插件ID（可为空，表示广播）
     */
    private String targetPluginId;
    
    /**
     * 事件数据
     */
    private Object data;
    
    /**
     * 事件时间
     */
    private LocalDateTime timestamp;
    
    public PluginEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public PluginEvent(EventType type, String sourcePluginId, Object data) {
        this();
        this.type = type;
        this.sourcePluginId = sourcePluginId;
        this.data = data;
    }
    
    /**
     * 事件类型
     */
    public enum EventType {
        /** 插件启动 */
        PLUGIN_STARTED,
        /** 插件停止 */
        PLUGIN_STOPPED,
        /** 配置更新 */
        CONFIG_UPDATED,
        /** 消息接收 */
        MESSAGE_RECEIVED,
        /** 消息发送 */
        MESSAGE_SENT,
        /** 用户操作 */
        USER_ACTION,
        /** 系统事件 */
        SYSTEM_EVENT,
        /** 自定义事件 */
        CUSTOM
    }
}