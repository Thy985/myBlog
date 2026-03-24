package com.xingchen.backend.plugin.context;

import com.xingchen.backend.cache.CacheService;
import com.xingchen.backend.plugin.PluginConfig;
import com.xingchen.backend.plugin.PluginEvent;
import com.xingchen.backend.plugin.PluginManager;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.MemoryService;
import lombok.Data;

import java.util.function.Consumer;

/**
 * 插件上下文
 * 
 * 插件通过上下文访问系统资源和能力
 */
@Data
public class PluginContext {
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 插件配置
     */
    private PluginConfig config;
    
    /**
     * 插件管理器
     */
    private PluginManager pluginManager;
    
    /**
     * AI 服务
     */
    private AIService aiService;
    
    /**
     * 记忆服务
     */
    private MemoryService memoryService;
    
    /**
     * 知识库服务
     */
    private KnowledgeBaseService knowledgeBaseService;
    
    /**
     * 缓存服务
     */
    private CacheService cacheService;
    
    /**
     * 日志记录器
     */
    private PluginLogger logger;
    
    /**
     * 数据目录
     */
    private String dataDirectory;
    
    /**
     * 发送事件
     */
    public void emitEvent(PluginEvent event) {
        if (pluginManager != null) {
            pluginManager.dispatchEvent(event);
        }
    }
    
    /**
     * 订阅事件
     */
    public void onEvent(PluginEvent.EventType eventType, Consumer<PluginEvent> handler) {
        if (pluginManager != null) {
            pluginManager.subscribeEvent(pluginId, eventType, handler);
        }
    }
    
    /**
     * 获取其他插件
     */
    public <T> T getPlugin(String pluginId, Class<T> pluginClass) {
        if (pluginManager != null) {
            return pluginManager.getPlugin(pluginId, pluginClass);
        }
        return null;
    }
    
    /**
     * 检查插件是否存在
     */
    public boolean hasPlugin(String pluginId) {
        if (pluginManager != null) {
            return pluginManager.hasPlugin(pluginId);
        }
        return false;
    }
}