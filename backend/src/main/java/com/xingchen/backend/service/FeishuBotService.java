package com.xingchen.backend.service;

import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;

/**
 * 飞书机器人服务接口
 */
public interface FeishuBotService {

    /**
     * 处理飞书消息事件（长连接回调）
     * @param event 飞书消息事件
     * @param userId 系统用户ID
     */
    void handleMessageEvent(P2MessageReceiveV1 event, Long userId);

    /**
     * 主动发送文本消息到飞书
     * @param chatId 飞书聊天ID
     * @param text 消息内容
     * @param userId 系统用户ID（用于获取对应的飞书配置）
     */
    void sendTextMessage(String chatId, String text, Long userId);

    /**
     * 回复飞书消息
     * @param messageId 飞书消息ID
     * @param text 回复内容
     * @param userId 系统用户ID（用于获取对应的飞书配置）
     */
    void replyTextMessage(String messageId, String text, Long userId);
}
