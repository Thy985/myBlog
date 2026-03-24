package com.xingchen.backend.plugin.message;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 发送的消息
 */
@Data
@Builder
public class OutgoingMessage {
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型
     */
    private MessageType type;
    
    /**
     * 目标会话ID
     */
    private String sessionId;
    
    /**
     * 回复的消息ID
     */
    private String replyToMessageId;
    
    /**
     * 是否使用流式发送
     */
    private boolean streaming;
    
    /**
     * 卡片数据（当type为CARD时使用）
     */
    private Map<String, Object> cardData;
    
    /**
     * 图片URL（当type为IMAGE时使用）
     */
    private String imageUrl;
    
    /**
     * 文件URL（当type为FILE时使用）
     */
    private String fileUrl;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> properties;
    
    /**
     * 消息类型
     */
    public enum MessageType {
        TEXT,
        MARKDOWN,
        HTML,
        CARD,
        IMAGE,
        FILE
    }
    
    /**
     * 创建文本消息
     */
    public static OutgoingMessage text(String content) {
        return OutgoingMessage.builder()
                .type(MessageType.TEXT)
                .content(content)
                .build();
    }
    
    /**
     * 创建Markdown消息
     */
    public static OutgoingMessage markdown(String content) {
        return OutgoingMessage.builder()
                .type(MessageType.MARKDOWN)
                .content(content)
                .build();
    }
    
    /**
     * 创建卡片消息
     */
    public static OutgoingMessage card(Map<String, Object> cardData) {
        return OutgoingMessage.builder()
                .type(MessageType.CARD)
                .cardData(cardData)
                .build();
    }
    
    /**
     * 设置回复
     */
    public OutgoingMessage replyTo(String messageId) {
        this.replyToMessageId = messageId;
        return this;
    }
    
    /**
     * 设置目标会话
     */
    public OutgoingMessage toSession(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}