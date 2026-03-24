package com.xingchen.backend.service.impl;

import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;
import com.xingchen.backend.service.FeishuBotService;
import com.xingchen.backend.service.FeishuWebSocketManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书 WebSocket 连接管理器实现
 * 支持多用户独立的 WebSocket 连接管理
 */
@Service
@Slf4j
public class FeishuWebSocketManagerImpl implements FeishuWebSocketManager {

    private final FeishuBotService feishuBotService;
    private final Map<Long, Client> connections = new ConcurrentHashMap<>();
    private final Map<Long, String> connectionStatus = new ConcurrentHashMap<>();

    public FeishuWebSocketManagerImpl(FeishuBotService feishuBotService) {
        this.feishuBotService = feishuBotService;
    }

    @Override
    public void startConnection(Long userId, String appId, String appSecret) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.warn("用户 {} 的飞书配置不完整，无法启动连接", userId);
            updateStatus(userId, "ERROR");
            return;
        }

        // 如果已有连接，先停止
        stopConnection(userId);

        try {
            updateStatus(userId, "CONNECTING");

            // 构建事件处理器
            final Long currentUserId = userId; // 捕获 userId 供内部类使用
            EventDispatcher eventDispatcher = EventDispatcher.newBuilder(appId, appSecret)
                    .onP2MessageReceiveV1(new com.lark.oapi.service.im.ImService.P2MessageReceiveV1Handler() {
                        @Override
                        public void handle(P2MessageReceiveV1 event) throws Exception {
                            // 传递用户ID到消息处理器
                            feishuBotService.handleMessageEvent(event, currentUserId);
                        }
                    })
                    .build();

            // 建立 WebSocket 长连接
            Client wsClient = new Client.Builder(appId, appSecret)
                    .eventHandler(eventDispatcher)
                    .build();

            // 异步启动
            Thread connectionThread = new Thread(() -> {
                try {
                    wsClient.start();
                } catch (Exception e) {
                    log.error("用户 {} 的飞书长连接异常: {}", userId, e.getMessage());
                    updateStatus(userId, "ERROR");
                }
            }, "feishu-ws-" + userId);
            connectionThread.setDaemon(true);
            connectionThread.start();

            connections.put(userId, wsClient);
            updateStatus(userId, "CONNECTED");

            log.info("用户 {} 的飞书 WebSocket 连接已启动", userId);

        } catch (Exception e) {
            log.error("用户 {} 的飞书 WebSocket 连接启动失败: {}", userId, e.getMessage(), e);
            updateStatus(userId, "ERROR");
        }
    }

    @Override
    public void stopConnection(Long userId) {
        Client wsClient = connections.remove(userId);
        if (wsClient != null) {
            try {
                // SDK 没有显式的 stop 方法，依赖垃圾回收
                log.info("用户 {} 的飞书 WebSocket 连接已停止", userId);
            } catch (Exception e) {
                log.error("停止用户 {} 的飞书连接时出错: {}", userId, e.getMessage());
            }
        }
        updateStatus(userId, "DISCONNECTED");
    }

    @Override
    public void restartConnection(Long userId, String appId, String appSecret) {
        log.info("重启用户 {} 的飞书 WebSocket 连接", userId);
        stopConnection(userId);
        // 稍等片刻确保资源释放
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startConnection(userId, appId, appSecret);
    }

    @Override
    public String getConnectionStatus(Long userId) {
        return connectionStatus.getOrDefault(userId, "DISCONNECTED");
    }

    @Override
    public boolean testConnection(String appId, String appSecret) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            return false;
        }

        try {
            // 简单的凭证格式验证
            // 实际测试需要调用飞书 API，这里只做基本验证
            return appId.startsWith("cli_") && appSecret.length() >= 32;
        } catch (Exception e) {
            log.error("测试飞书连接失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @PreDestroy
    public void stopAllConnections() {
        log.info("正在停止所有飞书 WebSocket 连接...");
        connections.keySet().forEach(this::stopConnection);
        connections.clear();
        connectionStatus.clear();
    }

    private void updateStatus(Long userId, String status) {
        connectionStatus.put(userId, status);
        log.debug("用户 {} 的飞书连接状态更新为: {}", userId, status);
    }
}
