package com.xingchen.backend.service;

/**
 * 飞书 WebSocket 连接管理器
 * 支持动态启动、停止和重启连接
 */
public interface FeishuWebSocketManager {

    /**
     * 启动指定用户的飞书 WebSocket 连接
     */
    void startConnection(Long userId, String appId, String appSecret);

    /**
     * 停止指定用户的飞书 WebSocket 连接
     */
    void stopConnection(Long userId);

    /**
     * 重启指定用户的飞书 WebSocket 连接
     */
    void restartConnection(Long userId, String appId, String appSecret);

    /**
     * 获取连接状态
     */
    String getConnectionStatus(Long userId);

    /**
     * 测试连接（不保存配置）
     */
    boolean testConnection(String appId, String appSecret);

    /**
     * 停止所有连接（应用关闭时调用）
     */
    void stopAllConnections();
}
