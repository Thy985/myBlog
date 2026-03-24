package com.xingchen.backend.plugin;

import com.xingchen.backend.plugin.message.IncomingMessage;
import com.xingchen.backend.plugin.message.OutgoingMessage;

/**
 * 通道插件接口
 * 
 * 用于接入各种消息渠道（飞书、钉钉、企业微信等）
 */
public interface ChannelPlugin extends Plugin {
    
    /**
     * 获取通道类型
     */
    String getChannelType();
    
    /**
     * 获取通道名称
     */
    String getChannelName();
    
    /**
     * 连接通道
     */
    void connect();
    
    /**
     * 断开连接
     */
    void disconnect();
    
    /**
     * 检查连接状态
     */
    boolean isConnected();
    
    /**
     * 发送消息
     */
    void sendMessage(OutgoingMessage message);
    
    /**
     * 回复消息
     */
    void replyMessage(String messageId, OutgoingMessage message);
    
    /**
     * 处理接收到的消息
     */
    void onMessageReceived(IncomingMessage message);
    
    /**
     * 设置消息处理器
     */
    void setMessageHandler(MessageHandler handler);
    
    /**
     * 消息处理器接口
     */
    @FunctionalInterface
    interface MessageHandler {
        void handle(IncomingMessage message, ChannelPlugin channel);
    }
}