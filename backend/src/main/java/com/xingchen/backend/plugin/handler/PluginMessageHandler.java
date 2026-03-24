package com.xingchen.backend.plugin.handler;

import com.xingchen.backend.plugin.ChannelPlugin;
import com.xingchen.backend.plugin.message.IncomingMessage;
import com.xingchen.backend.plugin.message.OutgoingMessage;
import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件消息处理器
 * 
 * 统一处理所有通道插件的消息
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PluginMessageHandler implements ChannelPlugin.MessageHandler {

    private final AIService aiService;
    
    // 会话历史
    private final Map<String, java.util.List<Map<String, String>>> sessionHistory = 
            new ConcurrentHashMap<>();
    
    // 最大历史轮数
    private static final int MAX_HISTORY = 10;

    @Override
    @Async("taskExecutor")
    public void handle(IncomingMessage message, ChannelPlugin channel) {
        log.info("处理 {} 消息: from={}, content={}", 
                channel.getChannelType(), 
                message.getSenderId(),
                message.getPlainText().substring(0, Math.min(50, message.getPlainText().length())));
        
        try {
            // 1. 提取纯文本
            String userInput = message.getPlainText();
            
            // 2. 获取会话历史
            String sessionId = message.getSessionId();
            java.util.List<Map<String, String>> history = sessionHistory.computeIfAbsent(
                    sessionId, k -> new java.util.ArrayList<>());
            
            // 3. 调用 AI
            String response = aiService.chatWithContext(userInput, history);
            
            // 4. 更新历史
            addToHistory(history, userInput, response);
            
            // 5. 发送回复
            OutgoingMessage reply = OutgoingMessage.markdown(response)
                    .toSession(sessionId);
            
            // 如果有消息ID，使用回复模式
            if (message.getMessageId() != null) {
                channel.replyMessage(message.getMessageId(), reply);
            } else {
                channel.sendMessage(reply);
            }
            
        } catch (Exception e) {
            log.error("处理消息失败", e);
            
            // 发送错误提示
            OutgoingMessage errorReply = OutgoingMessage.text("抱歉，处理消息时出现错误，请稍后重试。")
                    .toSession(message.getSessionId());
            channel.sendMessage(errorReply);
        }
    }
    
    private void addToHistory(java.util.List<Map<String, String>> history, 
                              String userInput, String response) {
        // 添加用户消息
        Map<String, String> userMsg = new java.util.HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userInput);
        history.add(userMsg);
        
        // 添加助手消息
        Map<String, String> assistantMsg = new java.util.HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", response);
        history.add(assistantMsg);
        
        // 限制历史长度
        while (history.size() > MAX_HISTORY * 2) {
            history.remove(0);
            history.remove(0);
        }
    }
    
    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        sessionHistory.remove(sessionId);
        log.info("清除会话历史: {}", sessionId);
    }
}