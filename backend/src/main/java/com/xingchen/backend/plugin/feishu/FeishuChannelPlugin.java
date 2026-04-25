package com.xingchen.backend.plugin.feishu;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lark.oapi.Client;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.*;
import com.xingchen.backend.plugin.AbstractChannelPlugin;
import com.xingchen.backend.plugin.ChannelPlugin;
import com.xingchen.backend.plugin.PluginConfig;
import com.xingchen.backend.plugin.PluginStatus;
import com.xingchen.backend.plugin.message.IncomingMessage;
import com.xingchen.backend.plugin.message.OutgoingMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class FeishuChannelPlugin extends AbstractChannelPlugin {

    private static final String CHANNEL_TYPE = "feishu";
    private static final String CHANNEL_NAME = "飞书";

    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(5);
    private static final Duration MAX_RECONNECT_DELAY = Duration.ofMinutes(2);

    private FeishuMessageHandler messageHandler;
    private com.lark.oapi.ws.Client wsClient;
    private Client httpClient;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduler;

    private Long userId = 1L;
    private String appId;
    private String appSecret;

    private volatile boolean connected = false;
    private ScheduledFuture<?> reconnectFuture;
    private int consecutiveReconnectFailures = 0;

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

        this.messageHandler = new FeishuMessageHandler(this::onMessageReceived);

        PluginConfig config = context.getConfig();
        log.info("飞书插件加载配置");

        if (config != null) {
            log.debug("飞书插件配置属性: properties={}", config.getProperties());
            this.appId = config.getString("appId", "");
            this.appSecret = config.getString("appSecret", "");
            this.userId = (long) config.getInt("userId", 1);
            log.info("飞书插件配置已读取, appId={}, userId={}", maskString(appId), userId);
        } else {
            log.warn("飞书插件配置为空");
        }

        if (appId == null || appId.isEmpty()) {
            throw new IllegalStateException("飞书配置不完整: appId 不能为空");
        }

        log.info("飞书通道插件加载完成");
    }

    public void onMessageReceived(IncomingMessage message) {
        if (messageHandler_ != null) {
            messageHandler_.handle(message, this);
        }
    }

    private ChannelPlugin.MessageHandler messageHandler_;

    @Override
    public void setMessageHandler(ChannelPlugin.MessageHandler handler) {
        this.messageHandler_ = handler;
    }

    @Override
    public void initialize() {
        super.initialize();

        if ((appSecret == null || appSecret.isEmpty()) && context != null && context.getConfig() != null) {
            if (Boolean.TRUE.equals(context.getConfig().getProperties().get("_needLazyLoadSecret"))) {
                log.info("飞书插件 appSecret 将延迟加载，等待 ApplicationReadyEvent");
                return;
            }
        }

        if (appSecret == null || appSecret.isEmpty()) {
            throw new IllegalStateException("飞书配置不完整: appSecret 不能为空");
        }

        this.executorService = Executors.newCachedThreadPool(r -> {
            java.lang.Thread t = new java.lang.Thread(r, "feishu-ws-channel");
            t.setDaemon(false);
            return t;
        });

        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            java.lang.Thread t = new java.lang.Thread(r, "feishu-scheduler");
            t.setDaemon(false);
            return t;
        });

        this.httpClient = new Client.Builder(appId, appSecret).build();

        log.info("飞书插件初始化完成");
    }

    @Override
    public void start() {
        super.start();
        connect();
    }

    @Override
    public void stop() {
        disconnect();
        shutdownExecutors();
        super.stop();
    }

    private void shutdownExecutors() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("飞书调度线程池未能在5秒内终止");
                }
            } catch (InterruptedException e) {
                java.lang.Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("飞书执行器线程池未能在5秒内终止");
                }
            } catch (InterruptedException e) {
                java.lang.Thread.currentThread().interrupt();
            }
            executorService = null;
        }
    }

    public void updateAppSecret(String newAppSecret) {
        this.appSecret = newAppSecret;
        if (httpClient != null) {
            this.httpClient = new Client.Builder(appId, appSecret).build();
        }
        log.info("飞书插件 appSecret 已更新");
    }

    @Override
    public void connect() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.warn("飞书配置不完整，无法建立连接");
            updateStatus(PluginStatus.ERROR);
            return;
        }

        updateStatus(PluginStatus.CONNECTING);

        try {
            String decryptedSecret = appSecret;
            if (appSecret.startsWith("ENC:")) {
                decryptedSecret = com.xingchen.backend.util.AesUtil.getInstance().decrypt(appSecret);
            }

            final Long currentUserId = this.userId;
            final String finalSecret = decryptedSecret;

            EventDispatcher eventDispatcher = EventDispatcher.newBuilder("", "")
                    .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                        @Override
                        public void handle(P2MessageReceiveV1 event) throws Exception {
                            messageHandler.handleMessageEvent(event);
                        }
                    })
                    .build();

            wsClient = new com.lark.oapi.ws.Client.Builder(appId, finalSecret)
                    .eventHandler(eventDispatcher)
                    .build();

            executorService.submit(() -> {
                try {
                    wsClient.start();
                    connected = true;
                    consecutiveReconnectFailures = 0;
                    updateStatus(PluginStatus.RUNNING);
                    log.info("飞书 WebSocket 长连接已启动 (userId={})", userId);
                } catch (Exception e) {
                    log.error("飞书 WebSocket 连接异常: {}", e.getMessage(), e);
                    connected = false;
                    updateStatus(PluginStatus.ERROR);
                    scheduleReconnect();
                }
            });

        } catch (Exception e) {
            log.error("飞书连接失败: {}", e.getMessage(), e);
            updateStatus(PluginStatus.ERROR);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (reconnectFuture != null && !reconnectFuture.isCancelled()) {
            return;
        }

        consecutiveReconnectFailures++;
        long delayMs = Math.min(
                RECONNECT_DELAY.toMillis() * (long) Math.pow(2, consecutiveReconnectFailures - 1),
                MAX_RECONNECT_DELAY.toMillis()
        );

        log.info("飞书连接断开，{} ms 后尝试重连 (第 {} 次)", delayMs, consecutiveReconnectFailures);

        reconnectFuture = scheduler.schedule(() -> {
            if (getStatus() != PluginStatus.STOPPED && getStatus() != PluginStatus.UNLOADED) {
                log.info("尝试重新连接飞书...");
                connect();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void disconnect() {
        if (reconnectFuture != null && !reconnectFuture.isCancelled()) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }

        connected = false;

        if (wsClient != null) {
            log.info("飞书 WebSocket 连接已断开");
            wsClient = null;
        }
        updateStatus(PluginStatus.STOPPED);
    }

    @Override
    public boolean isConnected() {
        return connected && wsClient != null && getStatus() == PluginStatus.RUNNING;
    }

    @Override
    public void sendMessage(OutgoingMessage message) {
        if (!isConnected()) {
            throw new IllegalStateException("飞书客户端未连接");
        }

        String chatId = message.getSessionId();
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("发送飞书消息失败: chatId 为空");
        }

        try {
            switch (message.getType()) {
                case TEXT -> sendTextViaSdk(chatId, message.getContent());
                case MARKDOWN -> sendMarkdownMessage(chatId, message.getContent());
                case CARD -> sendCardMessage(chatId, message.getCardData());
                default -> sendTextViaSdk(chatId, message.getContent());
            }
        } catch (Exception e) {
            log.error("发送飞书消息失败: chatId={}, error={}", chatId, e.getMessage(), e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    @Override
    public void replyMessage(String messageId, OutgoingMessage message) {
        if (!isConnected()) {
            throw new IllegalStateException("飞书客户端未连接");
        }

        try {
            switch (message.getType()) {
                case TEXT -> replyTextViaSdk(messageId, message.getContent());
                case MARKDOWN -> replyMarkdownMessage(messageId, message.getContent());
                case CARD -> replyCardMessage(messageId, message.getCardData());
                default -> replyTextViaSdk(messageId, message.getContent());
            }
        } catch (Exception e) {
            log.error("回复飞书消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("回复消息失败", e);
        }
    }

    private void sendTextViaSdk(String chatId, String text) {
        if (httpClient == null) {
            throw new IllegalStateException("飞书 HTTP 客户端未初始化");
        }
        try {
            String contentJson = Jsons.DEFAULT.toJson(Map.of("text", text != null ? text : ""));

            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("text")
                            .content(contentJson)
                            .build())
                    .build();

            CreateMessageResp resp = httpClient.im().message().create(req);
            if (!resp.success()) {
                log.error("发送文本消息失败: code={}, msg={}", resp.getCode(), resp.getMsg());
                throw new RuntimeException("发送文本消息失败: " + resp.getMsg());
            }
        } catch (Exception e) {
            log.error("发送文本消息失败: chatId={}", chatId, e);
            throw new RuntimeException("发送文本消息失败", e);
        }
    }

    private void replyTextViaSdk(String messageId, String text) {
        if (httpClient == null) {
            throw new IllegalStateException("飞书 HTTP 客户端未初始化");
        }
        try {
            if (text != null && text.length() > 4000) {
                text = text.substring(0, 4000) + "\n\n...(内容过长已截断)";
            }
            String contentJson = Jsons.DEFAULT.toJson(Map.of("text", text != null ? text : ""));

            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content(contentJson)
                            .msgType("text")
                            .build())
                    .build();

            ReplyMessageResp resp = httpClient.im().message().reply(req);
            if (!resp.success()) {
                log.error("回复文本消息失败: code={}, msg={}", resp.getCode(), resp.getMsg());
                throw new RuntimeException("回复文本消息失败: " + resp.getMsg());
            }
        } catch (Exception e) {
            log.error("回复文本消息失败: messageId={}", messageId, e);
            throw new RuntimeException("回复文本消息失败", e);
        }
    }

    private void sendMarkdownMessage(String chatId, String markdown) {
        if (httpClient == null) {
            throw new IllegalStateException("飞书 HTTP 客户端未初始化");
        }
        try {
            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("interactive")
                            .content(buildMarkdownCard(markdown))
                            .build())
                    .build();

            CreateMessageResp resp = httpClient.im().message().create(req);
            if (!resp.success()) {
                log.error("发送 Markdown 消息失败: code={}, msg={}", resp.getCode(), resp.getMsg());
                throw new RuntimeException("发送 Markdown 消息失败: " + resp.getMsg());
            }
        } catch (Exception e) {
            log.error("发送 Markdown 消息失败: chatId={}", chatId, e);
            throw new RuntimeException("发送 Markdown 消息失败", e);
        }
    }

    private void replyMarkdownMessage(String messageId, String markdown) {
        if (httpClient == null) {
            throw new IllegalStateException("飞书 HTTP 客户端未初始化");
        }
        try {
            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content(buildMarkdownCard(markdown))
                            .msgType("interactive")
                            .build())
                    .build();

            ReplyMessageResp resp = httpClient.im().message().reply(req);
            if (!resp.success()) {
                log.error("回复 Markdown 消息失败: code={}, msg={}", resp.getCode(), resp.getMsg());
                throw new RuntimeException("回复 Markdown 消息失败: " + resp.getMsg());
            }
        } catch (Exception e) {
            log.error("回复 Markdown 消息失败: messageId={}", messageId, e);
            throw new RuntimeException("回复 Markdown 消息失败", e);
        }
    }

    private void sendCardMessage(String chatId, Map<String, Object> cardData) {
        if (httpClient == null) {
            throw new IllegalStateException("飞书 HTTP 客户端未初始化");
        }
        if (cardData == null) {
            throw new IllegalArgumentException("卡片数据不能为空");
        }
        try {
            String cardJson = Jsons.DEFAULT.toJson(cardData);
            CreateMessageReq req = CreateMessageReq.newBuilder()
                    .receiveIdType("chat_id")
                    .createMessageReqBody(CreateMessageReqBody.newBuilder()
                            .receiveId(chatId)
                            .msgType("interactive")
                            .content(cardJson)
                            .build())
                    .build();

            CreateMessageResp resp = httpClient.im().message().create(req);
            if (!resp.success()) {
                log.error("发送卡片消息失败: code={}, msg={}", resp.getCode(), resp.getMsg());
                throw new RuntimeException("发送卡片消息失败: " + resp.getMsg());
            }
        } catch (Exception e) {
            log.error("发送卡片消息失败: chatId={}", chatId, e);
            throw new RuntimeException("发送卡片消息失败", e);
        }
    }

    private void replyCardMessage(String messageId, Map<String, Object> cardData) {
        if (httpClient == null) {
            throw new IllegalStateException("飞书 HTTP 客户端未初始化");
        }
        if (cardData == null) {
            throw new IllegalArgumentException("卡片数据不能为空");
        }
        try {
            String cardJson = Jsons.DEFAULT.toJson(cardData);
            ReplyMessageReq req = ReplyMessageReq.newBuilder()
                    .messageId(messageId)
                    .replyMessageReqBody(ReplyMessageReqBody.newBuilder()
                            .content(cardJson)
                            .msgType("interactive")
                            .build())
                    .build();

            ReplyMessageResp resp = httpClient.im().message().reply(req);
            if (!resp.success()) {
                log.error("回复卡片消息失败: code={}, msg={}", resp.getCode(), resp.getMsg());
                throw new RuntimeException("回复卡片消息失败: " + resp.getMsg());
            }
        } catch (Exception e) {
            log.error("回复卡片消息失败: messageId={}", messageId, e);
            throw new RuntimeException("回复卡片消息失败", e);
        }
    }

    private String buildMarkdownCard(String markdown) {
        try {
            Map<String, Object> card = Map.of(
                    "config", Map.of("wide_screen_mode", true),
                    "elements", new Object[]{
                            Map.of(
                                    "tag", "div",
                                    "text", Map.of(
                                            "tag", "lark_md",
                                            "content", markdown != null ? markdown : ""
                                    )
                            )
                    }
            );
            return Jsons.DEFAULT.toJson(card);
        } catch (Exception e) {
            log.error("构建 Markdown 卡片失败", e);
            return "{\"config\":{\"wide_screen_mode\":true},\"elements\":[{\"tag\":\"div\",\"text\":{\"tag\":\"lark_md\",\"content\":\"(消息解析失败)\"}}]}";
        }
    }

    private String maskString(String str) {
        if (str == null || str.length() <= 4) {
            return "***";
        }
        return str.substring(0, 4) + "***" + str.substring(str.length() - 4);
    }
}