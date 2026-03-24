package com.xingchen.backend.feishu.event;

import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReadV1;
import com.lark.oapi.service.im.v1.model.P2ChatAccessEventBotP2pChatEnteredV1;

/**
 * 飞书事件处理器接口
 */
public interface FeishuEventHandler {

    /**
     * 处理接收到的消息
     */
    void onMessageReceive(P2MessageReceiveV1 event, Long userId);

    /**
     * 处理卡片操作
     */
    void onCardAction(Object event, Long userId);

    /**
     * 处理用户进入单聊
     */
    void onUserEnterChat(P2ChatAccessEventBotP2pChatEnteredV1 event, Long userId);

    /**
     * 处理菜单点击
     */
    void onMenuClick(Object event, Long userId);

    /**
     * 处理消息已读
     */
    void onMessageRead(P2MessageReadV1 event, Long userId);
}
