package com.xingchen.backend.plugin.feishu;

import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.model.*;
import com.xingchen.backend.plugin.AbstractChannelPlugin;
import com.xingchen.backend.plugin.PluginConfig;
import com.xingchen.backend.plugin.PluginStatus;
import com.xingchen.backend.plugin.message.IncomingMessage;
import com.xingchen.backend.plugin.message.OutgoingMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书通道插件
 *
 * 基于飞书官方 SDK 实现的消息通道
 */
@Slf4j
public class FeishuChannelPlugin extends AbstractChannelPlugin {

    private static final String CHANNEL_TYPE = "feishu";
    private static final String CHANNEL_NAME = "飞书";

    // HTTP 客户端
    private Client httpClient;

    // 消息去重缓存
    private final Set<String> processedMessages = ConcurrentHashMap.newKeySet();
    private static final int MAX_CACHE_SIZE = 1000;

    // 连接配置
    private String appId;
    private String appSecret;
    private boolean encryptEnabled;
    private String encryptKey;

    @Override
    public String getChannelType() {
        return CHANNEL_TYPE;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void load(com.xingchen.backend.plugin.context.PluginContext context) {
        super.load(context);

        // 加载配置
        PluginConfig config = context.getConfig();
        if (config != null) {
            this.appId = config.getString("appId", "");
            this.appSecret = config.getString("appSecret", "");
            this.encryptEnabled = config.getBoolean("encryptEnabled", false);
            this.encryptKey = config.getString("encryptKey", "");
        }

        // 验证配置
        if (appId.isEmpty() || appSecret.isEmpty()) {
            throw new IllegalStateException("飞书配置不完整: appId 或 appSecret 为空");
        }

        // 创建 HTTP 客户端
        this.httpClient = new Client.Builder(appId, appSecret).build();

        log.info("飞书通道插件加载完成");
    }

    @Override
    public void initialize() {
        super.initialize();
        log.info("飞书事件处理器初始化完成");
    }

    @Override
    public void start() {
        super.start();
        log.info("飞书插件已启动");
    }

    @Override
    public void stop() {
        super.stop();
        log.info("飞书插件已停止");
    }

    @Override
    public void connect() {
        log.info("飞书连接");
    }

    @Override
    public void disconnect() {
        stop();
    }

    @Override
    public boolean isConnected() {
        // 飞书 SDK 没有直接提供连接状态，通过状态推断
        return getStatus() == PluginStatus.RUNNING;
    }

    @Override
    public void sendMessage(OutgoingMessage message) {
        try {
            String chatId = message.getSessionId();

            switch (message.getType()) {
                case TEXT -> sendTextMessage(chatId, message.getContent());
                case MARKDOWN -> sendMarkdownMessage(chatId, message.getContent());
                case CARD -> sendCardMessage(chatId, message.getCardData());
                default -> sendTextMessage(chatId, message.getContent());
            }

        } catch (Exception e) {
            log.error("发送飞书消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    @Override
    public void replyMessage(String messageId, OutgoingMessage message) {
        try {
            switch (message.getType()) {
                case TEXT -> replyTextMessage(messageId, message.getContent());
                case MARKDOWN -> replyMarkdownMessage(messageId, message.getContent());
                case CARD -> replyCardMessage(messageId, message.getCardData());
                default -> replyTextMessage(messageId, message.getContent());
            }

        } catch (Exception e) {
            log.error("回复飞书消息失败", e);
            throw new RuntimeException("回复消息失败", e);
        }
    }

    // ========== 发送消息方法 ==========

    private void sendTextMessage(String chatId, String text) {
        try {
            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("text")
                            .content("{\"text\":\"" + escapeJson(text) + "\"}")
                            .build())
                    .build();

            httpClient.im().message().create(req);

        } catch (Exception e) {
            log.error("发送文本消息失败", e);
            throw new RuntimeException("发送失败", e);
        }
    }

    private void sendMarkdownMessage(String chatId, String markdown) {
        try {
            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("interactive")
                            .content(buildMarkdownCard(markdown))
                            .build())
                    .build();

            httpClient.im().message().create(req);

        } catch (Exception e) {
            log.error("发送 Markdown 消息失败", e);
            throw new RuntimeException("发送失败", e);
        }
    }

    private void sendCardMessage(String chatId, Map<String, Object> cardData) {
        try {
            String cardJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(cardData);

            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("interactive")
                            .content(cardJson)
                            .build())
                    .build();

            httpClient.im().message().create(req);

        } catch (Exception e) {
            log.error("发送卡片消息失败", e);
            throw new RuntimeException("发送失败", e);
        }
    }

    private void replyTextMessage(String messageId, String text) {
        try {
            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content("{\"text\":\"" + escapeJson(text) + "\"}")
                            .msgType("text")
                            .build())
                    .build();

            httpClient.im().message().reply(req);

        } catch (Exception e) {
            log.error("回复文本消息失败", e);
            throw new RuntimeException("回复失败", e);
        }
    }

    private void replyMarkdownMessage(String messageId, String markdown) {
        try {
            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content(buildMarkdownCard(markdown))
                            .msgType("interactive")
                            .build())
                    .build();

            httpClient.im().message().reply(req);

        } catch (Exception e) {
            log.error("回复 Markdown 消息失败", e);
            throw new RuntimeException("回复失败", e);
        }
    }

    private void replyCardMessage(String messageId, Map<String, Object> cardData) {
        try {
            String cardJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(cardData);

            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content(cardJson)
                            .msgType("interactive")
                            .build())
                    .build();

            httpClient.im().message().reply(req);

        } catch (Exception e) {
            log.error("回复卡片消息失败", e);
            throw new RuntimeException("回复失败", e);
        }
    }

    private String buildMarkdownCard(String markdown) {
        return "{\"config\":{\"wide_screen_mode\":true},\"elements\":[{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"" +
               escapeJson(markdown) + "\"}}]}";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private void sendWelcomeMessage(String chatId) {
        String welcome = """
            👋 你好！我是智能助手

            我可以帮你：
            • 💬 回答问题
            • 📝 写文章
            • 💻 写代码
            • 🔍 搜索知识库

            直接发送消息开始对话吧！
            """;
        sendMarkdownMessage(chatId, welcome);
    }

    private void sendHelpMessage(String chatId) {
        String help = """
            📖 使用帮助

            **基本命令：**
            • `/help` - 显示帮助
            • `/clear` - 清除对话历史
            • `/memory` - 查看我的记忆

            **功能：**
            • 直接输入问题，我会尽力回答
            • 在群聊中 @我 可以触发对话
            • 支持代码高亮和 Markdown
            """;
        sendMarkdownMessage(chatId, help);
    }

    private void sendSettingsMessage(String chatId) {
        sendTextMessage(chatId, "设置功能开发中...");
    }
}
