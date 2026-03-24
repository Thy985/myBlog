package com.xingchen.backend.plugin.impl;

import com.xingchen.backend.cache.CacheService;
import com.xingchen.backend.plugin.*;
import com.xingchen.backend.plugin.context.PluginContext;
import com.xingchen.backend.plugin.context.PluginLogger;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.MemoryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 插件管理器实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PluginManagerImpl implements PluginManager {

    private final ApplicationContext applicationContext;
    private final AIService aiService;
    private final MemoryService memoryService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final CacheService cacheService;

    // 已加载的插件
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    // 插件元数据
    private final Map<String, PluginMetadata> metadataMap = new ConcurrentHashMap<>();
    // 插件状态
    private final Map<String, PluginStatus> statusMap = new ConcurrentHashMap<>();
    // 插件上下文
    private final Map<String, PluginContext> contextMap = new ConcurrentHashMap<>();
    // 插件配置
    private final Map<String, PluginConfig> configMap = new ConcurrentHashMap<>();
    // 通道插件
    private final Map<String, ChannelPlugin> channelPlugins = new ConcurrentHashMap<>();
    // 事件订阅
    private final Map<PluginEvent.EventType, List<Consumer<PluginEvent>>> eventSubscribers = new ConcurrentHashMap<>();
    // 类加载器
    private final Map<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();

    // 插件目录
    private static final String PLUGIN_DIR = "plugins";
    // 数据目录
    private static final String DATA_DIR = "data/plugins";

    @PostConstruct
    public void initialize() {
        log.info("初始化插件管理器");
        
        // 创建插件目录
        createPluginDirectories();
        
        // 加载内置插件
        loadBuiltInPlugins();
        
        // 加载外部插件
        loadExternalPlugins();
        
        // 启动已启用的插件
        startEnabledPlugins();
        
        log.info("插件管理器初始化完成，共加载 {} 个插件", plugins.size());
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭插件管理器");
        
        // 停止所有插件
        plugins.keySet().forEach(this::unloadPlugin);
        
        // 关闭类加载器
        classLoaders.values().forEach(loader -> {
            try {
                loader.close();
            } catch (Exception e) {
                log.error("关闭类加载器失败", e);
            }
        });
        
        log.info("插件管理器已关闭");
    }

    @Override
    public String loadPlugin(String pluginPath) {
        try {
            File pluginFile = new File(pluginPath);
            if (!pluginFile.exists()) {
                log.error("插件文件不存在: {}", pluginPath);
                return null;
            }

            // 创建类加载器
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{pluginFile.toURI().toURL()},
                    this.getClass().getClassLoader()
            );

            // 加载插件配置
            PluginMetadata metadata = loadMetadata(classLoader);
            if (metadata == null) {
                log.error("无法加载插件元数据: {}", pluginPath);
                return null;
            }

            String pluginId = metadata.getId();
            
            // 检查是否已存在
            if (plugins.containsKey(pluginId)) {
                log.warn("插件已存在: {}", pluginId);
                return pluginId;
            }

            // 加载插件类
            Class<?> pluginClass = classLoader.loadClass(metadata.getEntryClass());
            Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();

            // 创建上下文
            PluginContext context = createPluginContext(pluginId);
            
            // 保存
            plugins.put(pluginId, plugin);
            metadataMap.put(pluginId, metadata);
            statusMap.put(pluginId, PluginStatus.LOADED);
            contextMap.put(pluginId, context);
            classLoaders.put(pluginId, classLoader);

            // 加载配置
            PluginConfig config = loadPluginConfig(pluginId);
            configMap.put(pluginId, config);

            // 如果是通道插件，注册
            if (plugin instanceof ChannelPlugin) {
                channelPlugins.put(metadata.getProperties().get("channelType").toString(), 
                        (ChannelPlugin) plugin);
            }

            log.info("插件加载成功: {}", pluginId);
            return pluginId;

        } catch (Exception e) {
            log.error("加载插件失败: {}", pluginPath, e);
            return null;
        }
    }

    @Override
    public void unloadPlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return;
        }

        try {
            // 停止插件
            stopPlugin(pluginId);
            
            // 卸载
            plugin.unload();
            statusMap.put(pluginId, PluginStatus.UNLOADED);
            
            // 清理
            plugins.remove(pluginId);
            metadataMap.remove(pluginId);
            contextMap.remove(pluginId);
            configMap.remove(pluginId);
            
            // 关闭类加载器
            URLClassLoader loader = classLoaders.remove(pluginId);
            if (loader != null) {
                loader.close();
            }

            log.info("插件卸载成功: {}", pluginId);
        } catch (Exception e) {
            log.error("卸载插件失败: {}", pluginId, e);
        }
    }

    @Override
    public void enablePlugin(String pluginId) {
        PluginConfig config = configMap.get(pluginId);
        if (config != null) {
            config.setEnabled(true);
            savePluginConfig(pluginId, config);
            startPlugin(pluginId);
        }
    }

    @Override
    public void disablePlugin(String pluginId) {
        PluginConfig config = configMap.get(pluginId);
        if (config != null) {
            config.setEnabled(false);
            savePluginConfig(pluginId, config);
            stopPlugin(pluginId);
        }
    }

    @Override
    public void reloadPlugin(String pluginId) {
        PluginMetadata metadata = metadataMap.get(pluginId);
        if (metadata == null) {
            return;
        }
        
        // 获取插件路径（这里简化处理，实际需要存储路径）
        unloadPlugin(pluginId);
        // loadPlugin(path); // 需要重新加载
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPlugin(String pluginId, Class<T> pluginClass) {
        Plugin plugin = plugins.get(pluginId);
        if (pluginClass.isInstance(plugin)) {
            return (T) plugin;
        }
        return null;
    }

    @Override
    public PluginContext getPluginContext(String pluginId) {
        return contextMap.get(pluginId);
    }

    @Override
    public boolean hasPlugin(String pluginId) {
        return plugins.containsKey(pluginId);
    }

    @Override
    public PluginStatus getPluginStatus(String pluginId) {
        return statusMap.getOrDefault(pluginId, PluginStatus.UNLOADED);
    }

    @Override
    public List<PluginMetadata> listPlugins() {
        return new ArrayList<>(metadataMap.values());
    }

    @Override
    public List<PluginMetadata> listEnabledPlugins() {
        return metadataMap.values().stream()
                .filter(m -> {
                    PluginConfig config = configMap.get(m.getId());
                    return config != null && config.isEnabled();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PluginMetadata> getMetadata(String pluginId) {
        return Optional.ofNullable(metadataMap.get(pluginId));
    }

    @Override
    public void updatePluginConfig(String pluginId, PluginConfig config) {
        configMap.put(pluginId, config);
        savePluginConfig(pluginId, config);
        
        // 通知插件配置更新
        Plugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.updateConfig(config);
        }
    }

    @Override
    public void dispatchEvent(PluginEvent event) {
        List<Consumer<PluginEvent>> subscribers = eventSubscribers.get(event.getType());
        if (subscribers != null) {
            subscribers.forEach(handler -> {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    log.error("事件处理失败", e);
                }
            });
        }
    }

    @Override
    public void subscribeEvent(String pluginId, PluginEvent.EventType eventType, 
                               Consumer<PluginEvent> handler) {
        eventSubscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    public void unsubscribeEvent(String pluginId, PluginEvent.EventType eventType) {
        // 简化实现，实际需要更精确的取消订阅
    }

    @Override
    public void registerChannelPlugin(String pluginId, ChannelPlugin channelPlugin) {
        channelPlugins.put(channelPlugin.getChannelType(), channelPlugin);
    }

    @Override
    public ChannelPlugin getChannelPlugin(String channelType) {
        return channelPlugins.get(channelType);
    }

    // ========== 私有方法 ==========

    private void createPluginDirectories() {
        new File(PLUGIN_DIR).mkdirs();
        new File(DATA_DIR).mkdirs();
    }

    private void loadBuiltInPlugins() {
        // 加载内置的 FeishuChannelPlugin
        try {
            Class<?> feishuPluginClass = Class.forName(
                    "com.xingchen.backend.plugin.feishu.FeishuChannelPlugin");
            Plugin plugin = (Plugin) feishuPluginClass.getDeclaredConstructor().newInstance();
            
            String pluginId = "feishu-channel";
            PluginMetadata metadata = PluginMetadata.builder()
                    .id(pluginId)
                    .name("飞书通道")
                    .version("1.0.0")
                    .description("飞书消息通道插件")
                    .type(PluginMetadata.PluginType.CHANNEL)
                    .entryClass(feishuPluginClass.getName())
                    .build();
            
            PluginContext context = createPluginContext(pluginId);
            PluginConfig config = new PluginConfig();
            config.setPluginId(pluginId);
            config.setEnabled(true);
            
            plugins.put(pluginId, plugin);
            metadataMap.put(pluginId, metadata);
            statusMap.put(pluginId, PluginStatus.LOADED);
            contextMap.put(pluginId, context);
            configMap.put(pluginId, config);
            
            if (plugin instanceof ChannelPlugin) {
                channelPlugins.put("feishu", (ChannelPlugin) plugin);
            }
            
            log.info("内置插件加载成功: {}", pluginId);
        } catch (Exception e) {
            log.warn("加载内置飞书插件失败: {}", e.getMessage());
        }
    }

    private void loadExternalPlugins() {
        File pluginDir = new File(PLUGIN_DIR);
        File[] files = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        
        if (files != null) {
            for (File file : files) {
                loadPlugin(file.getAbsolutePath());
            }
        }
    }

    private void startEnabledPlugins() {
        configMap.forEach((pluginId, config) -> {
            if (config.isEnabled()) {
                startPlugin(pluginId);
            }
        });
    }

    private void startPlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        PluginContext context = contextMap.get(pluginId);
        
        if (plugin == null || context == null) {
            return;
        }
        
        try {
            statusMap.put(pluginId, PluginStatus.INITIALIZING);
            plugin.load(context);
            plugin.initialize();
            plugin.start();
            statusMap.put(pluginId, PluginStatus.RUNNING);
            
            log.info("插件启动成功: {}", pluginId);
        } catch (Exception e) {
            log.error("插件启动失败: {}", pluginId, e);
            statusMap.put(pluginId, PluginStatus.ERROR);
        }
    }

    private void stopPlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return;
        }
        
        try {
            statusMap.put(pluginId, PluginStatus.STOPPING);
            plugin.stop();
            statusMap.put(pluginId, PluginStatus.STOPPED);
            
            log.info("插件停止成功: {}", pluginId);
        } catch (Exception e) {
            log.error("插件停止失败: {}", pluginId, e);
        }
    }

    private PluginContext createPluginContext(String pluginId) {
        PluginContext context = new PluginContext();
        context.setPluginId(pluginId);
        context.setConfig(configMap.get(pluginId));
        context.setPluginManager(this);
        context.setAiService(aiService);
        context.setMemoryService(memoryService);
        context.setKnowledgeBaseService(knowledgeBaseService);
        context.setCacheService(cacheService);
        context.setLogger(new PluginLogger(pluginId));
        context.setDataDirectory(DATA_DIR + "/" + pluginId);
        
        // 创建数据目录
        new File(context.getDataDirectory()).mkdirs();
        
        return context;
    }

    private PluginMetadata loadMetadata(URLClassLoader classLoader) {
        // 简化实现，实际应该从插件中读取配置
        return null;
    }

    private PluginConfig loadPluginConfig(String pluginId) {
        // 从文件或数据库加载配置
        PluginConfig config = new PluginConfig();
        config.setPluginId(pluginId);
        config.setEnabled(true);
        return config;
    }

    private void savePluginConfig(String pluginId, PluginConfig config) {
        // 保存配置到文件或数据库
    }
}