package com.xingchen.backend.ai.util;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息构建工具类
 * 
 * 统一处理 AI 对话消息的构建逻辑，避免代码重复
 */
public class MessageBuilder {

    /**
     * 从 AIRequest 构建聊天消息列表
     * 
     * @param systemPrompt 系统提示词（可选）
     * @param history 历史对话记录（可选）
     * @param currentMessage 当前用户消息（必填）
     * @return ChatMessage 列表
     */
    public static List<ChatMessage> buildMessages(String systemPrompt, 
                                                   List<Map<String, String>> history, 
                                                   String currentMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 添加系统提示词
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 添加历史消息
        if (history != null) {
            for (Map<String, String> entry : history) {
                String role = entry.get("role");
                String content = entry.get("content");
                
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AiMessage(content));
                }
                // 忽略其他角色类型
            }
        }

        // 3. 添加当前用户消息
        if (currentMessage != null && !currentMessage.isEmpty()) {
            messages.add(new UserMessage(currentMessage));
        }

        return messages;
    }

    /**
     * 快速构建单条用户消息
     * 
     * @param message 用户消息内容
     * @return 包含单条消息的列表
     */
    public static List<ChatMessage> buildUserMessage(String message) {
        return List.of(new UserMessage(message));
    }

    /**
     * 快速构建带系统提示的消息列表
     * 
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return ChatMessage 列表
     */
    public static List<ChatMessage> buildWithSystemPrompt(String systemPrompt, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        
        messages.add(new UserMessage(userMessage));
        return messages;
    }

    /**
     * 计算消息列表的总 Token 数（估算）
     * 
     * @param messages 消息列表
     * @return 估算的 Token 数量
     */
    public static int estimateTotalTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        
        return messages.stream()
                .mapToInt(msg -> estimateTokens(msg.toString()))
                .sum();
    }

    /**
     * 估算文本的 Token 数量
     * 简单估算：中文按字符数，英文按单词数
     * 
     * @param text 文本内容
     * @return 估算的 Token 数
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 简单估算：平均每 4 个字符约 1 个 token
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * 从历史消息中截取最近的 N 轮对话
     * 用于控制上下文长度，避免超出 Token 限制
     * 
     * @param history 完整历史消息
     * @param maxRounds 最大保留轮数（一轮 = user + assistant）
     * @return 截取后的历史消息
     */
    public static List<Map<String, String>> truncateHistory(List<Map<String, String>> history, int maxRounds) {
        if (history == null || history.isEmpty()) {
            return history;
        }

        int totalMessages = history.size();
        int maxMessages = maxRounds * 2; // 每轮包含 user 和 assistant 两条消息

        if (totalMessages <= maxMessages) {
            return history;
        }

        // 保留最后 maxMessages 条消息
        return history.subList(totalMessages - maxMessages, totalMessages);
    }

    /**
     * 计算历史消息的总字符数
     * 
     * @param history 历史消息列表
     * @return 总字符数
     */
    public static int calculateHistoryLength(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }

        return history.stream()
                .map(msg -> msg.getOrDefault("content", ""))
                .mapToInt(String::length)
                .sum();
    }

    /**
     * 检查消息列表是否超过 Token 预算
     * 
     * @param messages 消息列表
     * @param tokenBudget Token 预算
     * @return 是否超限
     */
    public static boolean exceedsTokenBudget(List<ChatMessage> messages, int tokenBudget) {
        return estimateTotalTokens(messages) > tokenBudget;
    }
}
