package com.xingchen.backend.plugin.impl;

import com.xingchen.backend.cache.CacheService;
import com.xingchen.backend.entity.FeishuAppConfig;
import com.xingchen.backend.plugin.*;
import com.xingchen.backend.plugin.context.PluginContext;
import com.xingchen.backend.plugin.context.PluginLogger;
import com.xingchen.backend.plugin.feishu.FeishuChannelPlugin;
import com.xingchen.backend.service.AIService;
import com.xingchen.backend.service.FeishuAppConfigService;
import com.xingchen.backend.service.KnowledgeBaseService;
import com.xingchen.backend.service.MemoryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
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
    private final FeishuAppConfigService feishuAppConfigService;

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
        
        // 加载内置插件（只加载，不启动）
        loadBuiltInPlugins();
        
        // 加载外部插件（只加载，不启动）
        loadExternalPlugins();
        
        log.info("插件管理器初始化完成，共加载 {} 个插件，等待 ApplicationReadyEvent 后启动", plugins.size());
    }
    
    /**
     * 在 Spring 完全启动后启动插件
     * 确保 AesUtil 等依赖已初始化
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Spring 应用已就绪，开始启动插件...");
        
        // 重新加载飞书配置（此时 AesUtil 已初始化）
        reloadFeishuConfigFromDatabase();
        
        // 启动已启用的插件
        startEnabledPlugins();
        
        log.info("插件启动完成");
    }
    
    /**
     * 重新加载飞书配置（在 AesUtil 初始化后）
     */
    private void reloadFeishuConfigFromDatabase() {
        try {
            PluginConfig config = configMap.get("feishu-channel");
            if (config == null) {
                return;
            }
            
            // 检查是否需要延迟加载 appSecret
            if (Boolean.TRUE.equals(config.getProperties().get("_needLazyLoadSecret"))) {
                log.info("重新加载飞书配置（AesUtil 已初始化）...");
                
                if (feishuAppConfigService != null) {
                    FeishuAppConfig dbConfig = feishuAppConfigService.getByUserId(1L);
                    if (dbConfig != null) {
                        String decryptedSecret = dbConfig.getDecryptedAppSecret();
                        if (decryptedSecret != null && !decryptedSecret.isEmpty()) {
                            config.getProperties().put("appSecret", decryptedSecret);
                            config.getProperties().remove("_needLazyLoadSecret");
                            log.info("飞书 appSecret 已从数据库重新加载并解密");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("重新加载飞书配置失败: {}", e.getMessage());
        }
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

            // 先加载配置（必须在创建 context 之前）
            PluginConfig config = loadPluginConfig(pluginId);
            configMap.put(pluginId, config);
            
            // 创建上下文（此时 config 已可用）
            PluginContext context = createPluginContext(pluginId);
            
            // 保存
            plugins.put(pluginId, plugin);
            metadataMap.put(pluginId, metadata);
            statusMap.put(pluginId, PluginStatus.LOADED);
            contextMap.put(pluginId, context);
            classLoaders.put(pluginId, classLoader);

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
            
            // 先加载配置（必须在创建 context 之前）
            PluginConfig config = loadPluginConfig(pluginId);
            configMap.put(pluginId, config);
            
            // 创建上下文（此时 config 已可用）
            PluginContext context = createPluginContext(pluginId);
            
            plugins.put(pluginId, plugin);
            metadataMap.put(pluginId, metadata);
            statusMap.put(pluginId, PluginStatus.LOADED);
            contextMap.put(pluginId, context);
            
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
        PluginConfig config = new PluginConfig();
        config.setPluginId(pluginId);
        
        // 从 Spring Environment 读取配置
        org.springframework.core.env.Environment env = applicationContext.getEnvironment();
        
        // 构建配置前缀 (plugin.{pluginId})
        String configPrefix = "plugin." + pluginId;
        
        // 读取是否启用 (默认启用)
        String enabledKey = configPrefix + ".enabled";
        boolean enabled = env.getProperty(enabledKey, Boolean.class, true);
        config.setEnabled(enabled);
        
        // 读取所有配置属性
        Map<String, Object> properties = new HashMap<>();
        
        // 对于飞书插件，优先从数据库读取配置，如果没有则回退到 application.yaml
        if ("feishu-channel".equals(pluginId)) {
            loadFeishuConfigFromDatabase(properties, config);
            
            // 如果数据库没有配置，回退到 application.yaml
            if (!properties.containsKey("appId")) {
                loadFeishuConfigFromYaml(env, properties, config);
            }
        }
        
        config.setProperties(properties);
        
        log.debug("插件配置加载: pluginId={}, enabled={}, properties={}", 
                pluginId, config.isEnabled(), properties.keySet());
        
        return config;
    }
    
    /**
     * 从数据库加载飞书配置
     */
    private void loadFeishuConfigFromDatabase(Map<String, Object> properties, PluginConfig config) {
        try {
            if (feishuAppConfigService == null) {
                log.debug("FeishuAppConfigService 未注入，跳过数据库配置读取");
                return;
            }
            
            // 获取默认配置（userId = 1 作为系统默认配置）
            FeishuAppConfig dbConfig = feishuAppConfigService.getByUserId(1L);
            
            if (dbConfig != null && dbConfig.getAppId() != null && !dbConfig.getAppId().isEmpty()) {
                properties.put("appId", dbConfig.getAppId());
                properties.put("_dbConfigId", dbConfig.getId()); // 保存数据库配置ID用于后续刷新
                
                // 尝试获取解密后的 appSecret
                String decryptedSecret = getDecryptedAppSecretSafely(dbConfig);
                if (decryptedSecret != null && !decryptedSecret.isEmpty()) {
                    properties.put("appSecret", decryptedSecret);
                    
                    // 根据数据库配置设置启用状态
                    boolean enabled = dbConfig.getEnabled() != null && dbConfig.getEnabled() == 1;
                    config.setEnabled(enabled);
                    
                    log.info("飞书配置已从数据库加载: appId={}, enabled={}, hasSecret=true", 
                            dbConfig.getAppId(), enabled);
                } else {
                    // 解密失败，可能是密钥不匹配，删除数据库配置并使用 YAML 配置
                    log.warn("数据库中的 appSecret 解密失败，可能是密钥不匹配。删除数据库配置并使用 application.yaml 配置");
                    try {
                        feishuAppConfigService.deleteByUserId(1L);
                        log.info("已删除数据库中的飞书配置，请重新通过前端配置");
                    } catch (Exception deleteEx) {
                        log.warn("删除数据库配置失败: {}", deleteEx.getMessage());
                    }
                    // 清空属性，让系统回退到 YAML 配置
                    properties.clear();
                }
            } else {
                log.debug("数据库中没有找到飞书配置，将使用 application.yaml 配置");
            }
        } catch (Exception e) {
            log.warn("从数据库加载飞书配置失败，将使用 application.yaml 配置: {}", e.getMessage());
        }
    }
    
    /**
     * 安全地获取解密后的 App Secret
     * 处理 AesUtil 可能未初始化的情况
     */
    private String getDecryptedAppSecretSafely(FeishuAppConfig dbConfig) {
        try {
            // 先检查是否有明文缓存
            if (dbConfig.getAppSecretPlain() != null && !dbConfig.getAppSecretPlain().isBlank()) {
                return dbConfig.getAppSecretPlain();
            }
            
            // 检查 AesUtil 是否已初始化
            if (com.xingchen.backend.util.AesUtil.getInstance() == null) {
                log.debug("AesUtil 尚未初始化，将延迟解密 appSecret");
                return null;
            }
            
            return dbConfig.getDecryptedAppSecret();
        } catch (Exception e) {
            log.warn("解密 App Secret 失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 刷新飞书插件的 appSecret（在 AesUtil 初始化后调用）
     */
    public void refreshFeishuAppSecret() {
        try {
            PluginConfig config = configMap.get("feishu-channel");
            if (config == null) {
                return;
            }
            
            Map<String, Object> properties = config.getProperties();
            if (properties == null || !Boolean.TRUE.equals(properties.get("_needLazyLoadSecret"))) {
                return;
            }
            
            Long dbConfigId = (Long) properties.get("_dbConfigId");
            if (dbConfigId == null) {
                return;
            }
            
            // 重新从数据库获取并解密
            FeishuAppConfig dbConfig = feishuAppConfigService.getByUserId(1L);
            if (dbConfig != null) {
                String decryptedSecret = dbConfig.getDecryptedAppSecret();
                if (decryptedSecret != null && !decryptedSecret.isEmpty()) {
                    properties.put("appSecret", decryptedSecret);
                    properties.remove("_needLazyLoadSecret");
                    log.info("飞书插件 appSecret 已刷新");
                    
                    // 如果插件已加载，更新其配置
                    Plugin plugin = plugins.get("feishu-channel");
                    if (plugin instanceof FeishuChannelPlugin) {
                        ((FeishuChannelPlugin) plugin).updateAppSecret(decryptedSecret);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("刷新飞书 appSecret 失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从 application.yaml 加载飞书配置（回退方案）
     */
    private void loadFeishuConfigFromYaml(org.springframework.core.env.Environment env, 
                                          Map<String, Object> properties, 
                                          PluginConfig config) {
        String appId = env.getProperty("feishu.bot.app-id", "");
        String appSecret = env.getProperty("feishu.bot.app-secret", "");
        
        if (!appId.isEmpty()) {
            properties.put("appId", appId);
        }
        if (!appSecret.isEmpty()) {
            properties.put("appSecret", appSecret);
        }
        
        // 如果 feishu.bot.enabled=false，禁用插件
        Boolean feishuEnabled = env.getProperty("feishu.bot.enabled", Boolean.class, true);
        if (!feishuEnabled) {
            config.setEnabled(false);
        }
        
        if (!appId.isEmpty()) {
            log.info("飞书配置已从 application.yaml 加载");
        }
    }

    private void savePluginConfig(String pluginId, PluginConfig config) {
        // 保存配置到文件或数据库
    }
}