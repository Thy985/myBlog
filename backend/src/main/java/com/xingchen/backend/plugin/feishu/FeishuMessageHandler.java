package com.xingchen.backend.plugin.feishu;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lark.oapi.service.im.v1.model.*;
import com.xingchen.backend.plugin.message.IncomingMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class FeishuMessageHandler {

    private static final int MAX_CACHE_SIZE = 1000;
    private static final Duration MESSAGE_EXPIRE_TIME = Duration.ofMinutes(5);

    private final Cache<String, Boolean> processedMessages;
    private final MessageHandlerDelegate delegate;

    public interface MessageHandlerDelegate {
        void handleMessage(IncomingMessage message);
    }

    public FeishuMessageHandler(MessageHandlerDelegate delegate) {
        this.delegate = delegate;
        this.processedMessages = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(MESSAGE_EXPIRE_TIME)
                .build();
    }

    public void handleMessageEvent(P2MessageReceiveV1 event) {
        P2MessageReceiveV1Data data = event.getEvent();
        if (data == null || data.getMessage() == null) {
            log.warn("飞书消息事件数据为空");
            return;
        }

        EventMessage eventMessage = data.getMessage();
        String messageId = eventMessage.getMessageId();
        if (messageId == null) {
            log.warn("消息 ID 为空");
            return;
        }

        Boolean existing = processedMessages.getIfPresent(messageId);
        if (existing != null) {
            log.debug("重复消息，跳过: {}", messageId);
            return;
        }
        processedMessages.put(messageId, Boolean.TRUE);

        String content = eventMessage.getContent();
        if (content == null) {
            log.warn("消息内容为空, messageId: {}", messageId);
            return;
        }

        String chatId = eventMessage.getChatId();
        String chatType = eventMessage.getChatType();
        String msgType = eventMessage.getMessageType();
        String senderId = extractSenderId(data);

        IncomingMessage incoming = IncomingMessage.builder()
                .messageId(messageId)
                .sessionId(chatId)
                .senderId(senderId)
                .content(content)
                .type(parseMessageType(msgType))
                .channelType("feishu")
                .groupChat("group".equals(chatType))
                .groupId("group".equals(chatType) ? chatId : null)
                .mentioned(isMentionedBot(eventMessage))
                .receiveTime(java.time.LocalDateTime.now())
                .rawData(data)
                .build();

        delegate.handleMessage(incoming);
    }

    private String extractSenderId(P2MessageReceiveV1Data data) {
        try {
            if (data.getSender() != null && data.getSender().getSenderId() != null) {
                Object senderIdObj = data.getSender().getSenderId();
                if (senderIdObj != null) {
                    java.lang.reflect.Method getOpenId = senderIdObj.getClass().getMethod("getOpenId");
                    Object openId = getOpenId.invoke(senderIdObj);
                    if (openId != null) {
                        return openId.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("提取 senderId 失败: {}", e.getMessage());
        }
        return "unknown";
    }

    private boolean isMentionedBot(EventMessage message) {
        if (message.getMentions() != null && message.getMentions().length > 0) {
            return true;
        }
        return false;
    }

    private IncomingMessage.MessageType parseMessageType(String msgType) {
        if (msgType == null) {
            return IncomingMessage.MessageType.TEXT;
        }
        return switch (msgType) {
            case "text" -> IncomingMessage.MessageType.TEXT;
            case "image" -> IncomingMessage.MessageType.IMAGE;
            case "file", "audio", "video", "sticker" -> IncomingMessage.MessageType.FILE;
            case "post" -> IncomingMessage.MessageType.CARD;
            case "media" -> IncomingMessage.MessageType.VIDEO;
            default -> IncomingMessage.MessageType.TEXT;
        };
    }

    public void clearCache() {
        processedMessages.invalidateAll();
    }
}