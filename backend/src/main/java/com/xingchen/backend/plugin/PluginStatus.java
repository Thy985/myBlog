package com.xingchen.backend.plugin;

/**
 * 插件状态
 */
public enum PluginStatus {
    /** 已加载 */
    LOADED,
    /** 初始化中 */
    INITIALIZING,
    /** 已初始化 */
    INITIALIZED,
    /** 运行中 */
    RUNNING,
    /** 停止中 */
    STOPPING,
    /** 已停止 */
    STOPPED,
    /** 错误状态 */
    ERROR,
    /** 已卸载 */
    UNLOADED
}