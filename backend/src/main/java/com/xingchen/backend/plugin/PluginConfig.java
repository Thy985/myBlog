package com.xingchen.backend.plugin;

import lombok.Data;

import java.util.Map;

/**
 * 插件配置
 */
@Data
public class PluginConfig {
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 是否启用
     */
    private boolean enabled;
    
    /**
     * 配置属性
     */
    private Map<String, Object> properties;
    
    /**
     * 获取字符串配置
     */
    public String getString(String key, String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        Object value = properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 获取整数配置
     */
    public int getInt(String key, int defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    /**
     * 设置配置
     */
    public void setProperty(String key, Object value) {
        if (properties != null) {
            properties.put(key, value);
        }
    }
}