package com.xingchen.backend.plugin.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 接收到的消息
 */
@Data
@Builder
public class IncomingMessage {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 发送者ID
     */
    private String senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 通道类型
     */
    private String channelType;
    
    /**
     * 是否为群聊
     */
    private boolean groupChat;
    
    /**
     * 群聊ID
     */
    private String groupId;
    
    /**
     * 是否@了机器人
     */
    private boolean mentioned;
    
    /**
     * 原始数据
     */
    private Object rawData;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> properties;
    
    /**
     * 接收时间
     */
    private LocalDateTime receiveTime;
    
    /**
     * 消息类型
     */
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        CARD,
        VOICE,
        VIDEO,
        LOCATION,
        SYSTEM
    }
    
    /**
     * 获取纯文本内容
     */
    public String getPlainText() {
        if (content == null) {
            return "";
        }
        // 移除 @提及
        return content.replaceAll("@\\S+\\s*", "").trim();
    }
}