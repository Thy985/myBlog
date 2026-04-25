package com.xingchen.backend.config;

import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;
import com.xingchen.backend.service.FeishuBotService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 飞书机器人长连接配置（WebSocket 模式）- 全局配置方式
 *
 * 使用飞书官方 Java SDK 的 ws.Client 建立长连接：
 * - 无需公网域名
 * - 无需配置加密策略
 * - 实时接收飞书消息事件
 *
 * @deprecated 已弃用，请使用 FeishuChannelPlugin 进行多用户连接管理。
 *             启用条件：application.yaml 中配置 feishu.bot.enabled=true AND feishu.bot.use-legacy=true
 *             未来版本将移除此配置类。
 */
@Configuration
@ConditionalOnProperty(name = "feishu.bot.use-legacy", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class FeishuBotConfig {

    @Value("${feishu.bot.app-id:}")
    private String appId;

    @Value("${feishu.bot.app-secret:}")
    private String appSecret;

    private final FeishuBotService feishuBotService;
    private Client wsClient;

    @PostConstruct
    public void init() {
        if (appId.isBlank() || appSecret.isBlank()) {
            log.warn("飞书机器人未配置 app-id 或 app-secret，跳过启动");
            return;
        }

        try {
            // 构建事件处理器
            EventDispatcher eventDispatcher = EventDispatcher.newBuilder("", "")
                    // 监听「接收消息」事件
                    .onP2MessageReceiveV1(new com.lark.oapi.service.im.ImService.P2MessageReceiveV1Handler() {
                        @Override
                        public void handle(P2MessageReceiveV1 event) throws Exception {
                            // 使用 0L 作为系统用户ID（全局配置方式）
                            feishuBotService.handleMessageEvent(event, 0L);
                        }
                    })
                    .build();

            // 建立 WebSocket 长连接
            wsClient = new Client.Builder(appId, appSecret)
                    .eventHandler(eventDispatcher)
                    .build();

            // 异步启动（不阻塞主线程）
            new Thread(() -> {
                try {
                    wsClient.start();
                } catch (Exception e) {
                    log.error("飞书长连接启动失败: {}", e.getMessage(), e);
                }
            }, "feishu-ws-client").start();

            log.info("飞书机器人长连接启动中... appId={}", appId);

        } catch (Exception e) {
            log.error("飞书机器人配置失败: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        // SDK 会自动管理连接生命周期
        log.info("飞书机器人长连接关闭");
    }
}
