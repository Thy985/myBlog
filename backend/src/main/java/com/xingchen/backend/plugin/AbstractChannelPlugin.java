package com.xingchen.backend.plugin;

import com.xingchen.backend.plugin.context.PluginContext;
import com.xingchen.backend.plugin.message.IncomingMessage;
import com.xingchen.backend.plugin.message.OutgoingMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象通道插件
 * 
 * 提供通道插件的基础实现
 */
@Slf4j
public abstract class AbstractChannelPlugin implements ChannelPlugin {

    protected PluginContext context;
    protected PluginMetadata metadata;
    protected PluginStatus status = PluginStatus.LOADED;
    protected PluginConfig config;
    protected MessageHandler messageHandler;

    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void load(PluginContext context) {
        this.context = context;
        this.config = context.getConfig();
        this.metadata = PluginMetadata.builder()
                .id(context.getPluginId())
                .name(getChannelName())
                .version("1.0.0")
                .type(PluginMetadata.PluginType.CHANNEL)
                .build();
        
        log.info("{} 通道插件已加载", getChannelName());
    }

    @Override
    public void initialize() {
        updateStatus(PluginStatus.INITIALIZED);
        log.info("{} 通道插件已初始化", getChannelName());
    }

    @Override
    public void start() {
        updateStatus(PluginStatus.RUNNING);
        log.info("{} 通道插件已启动", getChannelName());
    }

    @Override
    public void stop() {
        updateStatus(PluginStatus.STOPPED);
        log.info("{} 通道插件已停止", getChannelName());
    }

    @Override
    public void unload() {
        updateStatus(PluginStatus.UNLOADED);
        log.info("{} 通道插件已卸载", getChannelName());
    }

    @Override
    public PluginStatus getStatus() {
        return status;
    }

    @Override
    public PluginConfig getConfig() {
        return config;
    }

    @Override
    public void updateConfig(PluginConfig config) {
        this.config = config;
        log.info("{} 通道插件配置已更新", getChannelName());
    }

    @Override
    public void onEvent(PluginEvent event) {
        // 默认空实现，子类可覆盖
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    @Override
    public void onMessageReceived(IncomingMessage message) {
        if (messageHandler != null) {
            messageHandler.handle(message, this);
        }
    }

    protected void updateStatus(PluginStatus newStatus) {
        this.status = newStatus;
        
        // 发送状态变更事件
        if (context != null) {
            PluginEvent event = new PluginEvent();
            event.setType(newStatus == PluginStatus.RUNNING ? 
                    PluginEvent.EventType.PLUGIN_STARTED : 
                    PluginEvent.EventType.PLUGIN_STOPPED);
            event.setSourcePluginId(context.getPluginId());
            context.emitEvent(event);
        }
    }
}