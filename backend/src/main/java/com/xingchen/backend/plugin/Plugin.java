package com.xingchen.backend.plugin;

import com.xingchen.backend.plugin.context.PluginContext;

/**
 * 插件接口 - 所有插件必须实现
 * 
 * 插件生命周期：
 * 1. 加载 (load) - 插件被加载到内存
 * 2. 初始化 (initialize) - 插件初始化配置
 * 3. 启动 (start) - 插件开始工作
 * 4. 停止 (stop) - 插件停止工作
 * 5. 卸载 (unload) - 插件被卸载
 */
public interface Plugin {
    
    /**
     * 获取插件元数据
     */
    PluginMetadata getMetadata();
    
    /**
     * 加载插件
     * @param context 插件上下文
     */
    void load(PluginContext context);
    
    /**
     * 初始化插件
     */
    void initialize();
    
    /**
     * 启动插件
     */
    void start();
    
    /**
     * 停止插件
     */
    void stop();
    
    /**
     * 卸载插件
     */
    void unload();
    
    /**
     * 检查插件状态
     */
    PluginStatus getStatus();
    
    /**
     * 获取插件配置
     */
    default PluginConfig getConfig() {
        return null;
    }
    
    /**
     * 更新插件配置
     */
    default void updateConfig(PluginConfig config) {
        // 默认空实现
    }
    
    /**
     * 处理插件事件
     */
    default void onEvent(PluginEvent event) {
        // 默认空实现
    }
}