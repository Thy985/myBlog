package com.xingchen.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.model.*;
import com.xingchen.backend.entity.FeishuAppConfig;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.FeishuAppConfigService;
import com.xingchen.backend.service.FeishuBotService;
import com.xingchen.backend.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书机器人服务实现
 *
 * 核心链路：飞书消息 → 提取文本 → 调 AI（带记忆+知识库）→ 回复飞书
 *
 * 特性：
 * - 消息去重（飞书会重试推送）
 * - @机器人 触发（群聊中只响应 @bot 的消息）
 * - 异步处理（3秒内响应飞书，AI 回复异步发送）
 * - 共享 AIService（和网页端共用同一套 AI + 记忆 + 知识库）
 */
@Service
@Slf4j
public class FeishuBotServiceImpl implements FeishuBotService {

    private final AIService aiService;
    private final MemoryService memoryService;
    private final FeishuAppConfigService feishuAppConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 为每个用户缓存一个 Client（避免重复创建）
    private final Map<Long, Client> clientCache = new ConcurrentHashMap<>();

    // 消息去重（缓存最近 500 条 messageId）
    private final Set<String> processedMessages = ConcurrentHashMap.newKeySet();
    private static final int MAX_CACHE_SIZE = 500;

    public FeishuBotServiceImpl(AIService aiService, MemoryService memoryService,
                                 @org.springframework.context.annotation.Lazy FeishuAppConfigService feishuAppConfigService) {
        this.aiService = aiService;
        this.memoryService = memoryService;
        this.feishuAppConfigService = feishuAppConfigService;
        log.info("飞书 Bot Service 初始化完成");
    }

    /**
     * 获取或创建指定用户的飞书 Client
     */
    private Client getClient(Long userId) {
        return clientCache.computeIfAbsent(userId, id -> {
            FeishuAppConfig config = feishuAppConfigService.getByUserId(id);
            if (config == null || config.getAppId() == null || config.getAppSecret() == null) {
                log.warn("用户 {} 没有配置飞书应用", id);
                return null;
            }
            // 解密 App Secret
            String appSecret = config.getDecryptedAppSecret();
            if (appSecret == null) {
                log.error("用户 {} 的飞书 App Secret 解密失败", id);
                return null;
            }
            return Client.newBuilder(config.getAppId(), appSecret)
                    .logReqAtDebug(true)
                    .build();
        });
    }

    @Override
    @Async
    public void handleMessageEvent(P2MessageReceiveV1 event, Long userId) {
        try {
            P2MessageReceiveV1Data data = event.getEvent();
            if (data == null || data.getMessage() == null) return;

            String messageId = data.getMessage().getMessageId();
            String chatId = data.getMessage().getChatId();
            String chatType = data.getMessage().getChatType();
            String msgType = data.getMessage().getMessageType();
            String senderId = data.getSender() != null && data.getSender().getSenderId() != null
                    ? data.getSender().getSenderId().getOpenId() : "unknown";

            // 1. 消息去重
            if (!processedMessages.add(messageId)) {
                log.debug("重复消息，跳过: {}", messageId);
                return;
            }
            if (processedMessages.size() > MAX_CACHE_SIZE) {
                processedMessages.clear(); // 简单清理
            }

            // 2. 只处理文本消息
            if (!"text".equals(msgType)) {
                replyTextMessage(messageId, "目前只支持文本消息哦 😅", userId);
                return;
            }

            // 3. 提取文本内容
            String content = data.getMessage().getContent();
            String userText = extractTextContent(content);
            if (userText == null || userText.isBlank()) return;

            // 4. 群聊中需要 @机器人 才响应（去掉 @bot 的部分）
            if ("group".equals(chatType)) {
                if (!userText.contains("@_all") && data.getMessage().getMentions() == null) {
                    return; // 群聊中没有 @bot，不响应
                }
                // 去掉 @xxx 的部分
                userText = userText.replaceAll("@\\S+\\s*", "").trim();
                if (userText.isBlank()) {
                    replyTextMessage(messageId, "你好！有什么我可以帮你的吗？ ✨", userId);
                    return;
                }
            }

            log.info("飞书消息 [{}] from {} (userId={}): {}", chatType, senderId, userId, 
                    userText.substring(0, Math.min(50, userText.length())));

            // 5. 调用 AI（使用用户配置的 API Key）
            String aiResponse;
            try {
                // 优先使用用户配置的 API Key
                aiResponse = aiService.chatWithUserApiKey(userId, userText);
            } catch (Exception e) {
                log.error("AI 回复失败 (userId={}): {}", userId, e.getMessage());
                // 如果用户没有配置 API Key，使用默认的
                try {
                    aiResponse = aiService.chatWithRag(userText);
                } catch (Exception e2) {
                    aiResponse = "抱歉，我暂时无法回答，请稍后再试 😅";
                }
            }

            // 6. 保存对话到记忆系统
            try {
                memoryService.saveConversation(userId, "user", userText);
                memoryService.saveConversation(userId, "assistant", aiResponse);
            } catch (Exception e) {
                log.debug("保存飞书对话记忆失败: {}", e.getMessage());
            }

            // 7. 回复飞书
            replyTextMessage(messageId, aiResponse, userId);

        } catch (Exception e) {
            log.error("处理飞书消息失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendTextMessage(String chatId, String text, Long userId) {
        Client client = getClient(userId);
        if (client == null) {
            log.warn("用户 {} 的飞书 Client 未初始化", userId);
            return;
        }

        try {
            String content = "{\"text\":\"" + escapeJson(text) + "\"}";

            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("text")
                            .content(content)
                            .build())
                    .build();

            CreateMessageResp resp = client.im().message().create(req);
            if (!resp.success()) {
                log.error("飞书发送消息失败 (userId={}): code={}, msg={}", userId, resp.getCode(), resp.getMsg());
            }
        } catch (Exception e) {
            log.error("飞书发送消息异常 (userId={}): {}", userId, e.getMessage(), e);
        }
    }

    @Override
    public void replyTextMessage(String messageId, String text, Long userId) {
        Client client = getClient(userId);
        if (client == null) {
            log.warn("用户 {} 的飞书 Client 未初始化", userId);
            return;
        }

        try {
            // 飞书文本消息限制 4096 字符
            if (text.length() > 4000) {
                text = text.substring(0, 4000) + "\n\n...(内容过长已截断)";
            }

            String content = "{\"text\":\"" + escapeJson(text) + "\"}";

            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .msgType("text")
                            .content(content)
                            .build())
                    .build();

            ReplyMessageResp resp = client.im().message().reply(req);
            if (!resp.success()) {
                log.error("飞书回复消息失败 (userId={}): code={}, msg={}", userId, resp.getCode(), resp.getMsg());
            } else {
                log.debug("飞书回复成功 (userId={}): messageId={}", userId, messageId);
            }
        } catch (Exception e) {
            log.error("飞书回复消息异常 (userId={}): {}", userId, e.getMessage(), e);
        }
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 从飞书消息 content JSON 中提取纯文本
     * 飞书文本消息格式: {"text":"消息内容"}
     */
    private String extractTextContent(String contentJson) {
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            if (node.has("text")) {
                return node.get("text").asText("");
            }
            return contentJson;
        } catch (Exception e) {
            return contentJson;
        }
    }

    /**
     * JSON 字符串转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
