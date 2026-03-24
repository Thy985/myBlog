package com.xingchen.backend.plugin;

import com.xingchen.backend.plugin.context.PluginContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 插件管理器接口
 */
public interface PluginManager {
    
    /**
     * 加载插件
     * @param pluginPath 插件路径（JAR文件或目录）
     * @return 插件ID
     */
    String loadPlugin(String pluginPath);
    
    /**
     * 卸载插件
     */
    void unloadPlugin(String pluginId);
    
    /**
     * 启用插件
     */
    void enablePlugin(String pluginId);
    
    /**
     * 禁用插件
     */
    void disablePlugin(String pluginId);
    
    /**
     * 重新加载插件
     */
    void reloadPlugin(String pluginId);
    
    /**
     * 获取插件
     */
    <T> T getPlugin(String pluginId, Class<T> pluginClass);
    
    /**
     * 获取插件上下文
     */
    PluginContext getPluginContext(String pluginId);
    
    /**
     * 检查插件是否存在
     */
    boolean hasPlugin(String pluginId);
    
    /**
     * 获取插件状态
     */
    PluginStatus getPluginStatus(String pluginId);
    
    /**
     * 列出所有插件
     */
    List<PluginMetadata> listPlugins();
    
    /**
     * 列出已启用的插件
     */
    List<PluginMetadata> listEnabledPlugins();
    
    /**
     * 获取插件元数据
     */
    Optional<PluginMetadata> getMetadata(String pluginId);
    
    /**
     * 更新插件配置
     */
    void updatePluginConfig(String pluginId, PluginConfig config);
    
    /**
     * 分发事件
     */
    void dispatchEvent(PluginEvent event);
    
    /**
     * 订阅事件
     */
    void subscribeEvent(String pluginId, PluginEvent.EventType eventType, Consumer<PluginEvent> handler);
    
    /**
     * 取消订阅
     */
    void unsubscribeEvent(String pluginId, PluginEvent.EventType eventType);
    
    /**
     * 注册通道插件（用于消息收发）
     */
    void registerChannelPlugin(String pluginId, ChannelPlugin channelPlugin);
    
    /**
     * 获取通道插件
     */
    ChannelPlugin getChannelPlugin(String channelType);
    
    /**
     * 初始化（启动时调用）
     */
    void initialize();
    
    /**
     * 关闭（系统关闭时调用）
     */
    void shutdown();
}