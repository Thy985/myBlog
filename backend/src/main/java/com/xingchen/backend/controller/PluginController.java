package com.xingchen.backend.controller;

import com.xingchen.backend.common.Result;
import com.xingchen.backend.plugin.PluginConfig;
import com.xingchen.backend.plugin.PluginManager;
import com.xingchen.backend.plugin.PluginMetadata;
import com.xingchen.backend.plugin.PluginStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件管理控制器
 */
@RestController
@RequestMapping("/api/plugins")
@Slf4j
@RequiredArgsConstructor
public class PluginController {

    private final PluginManager pluginManager;

    /**
     * 列出所有插件
     */
    @GetMapping
    public Result<List<PluginMetadata>> listPlugins() {
        return Result.success(pluginManager.listPlugins());
    }

    /**
     * 列出已启用的插件
     */
    @GetMapping("/enabled")
    public Result<List<PluginMetadata>> listEnabledPlugins() {
        return Result.success(pluginManager.listEnabledPlugins());
    }

    /**
     * 获取插件详情
     */
    @GetMapping("/{pluginId}")
    public Result<PluginMetadata> getPlugin(@PathVariable String pluginId) {
        return pluginManager.getMetadata(pluginId)
                .map(Result::success)
                .orElse(Result.fail(404, "插件不存在"));
    }

    /**
     * 获取插件状态
     */
    @GetMapping("/{pluginId}/status")
    public Result<PluginStatus> getPluginStatus(@PathVariable String pluginId) {
        return Result.success(pluginManager.getPluginStatus(pluginId));
    }

    /**
     * 启用插件
     */
    @PostMapping("/{pluginId}/enable")
    public Result<Void> enablePlugin(@PathVariable String pluginId) {
        pluginManager.enablePlugin(pluginId);
        return Result.success();
    }

    /**
     * 禁用插件
     */
    @PostMapping("/{pluginId}/disable")
    public Result<Void> disablePlugin(@PathVariable String pluginId) {
        pluginManager.disablePlugin(pluginId);
        return Result.success();
    }

    /**
     * 重新加载插件
     */
    @PostMapping("/{pluginId}/reload")
    public Result<Void> reloadPlugin(@PathVariable String pluginId) {
        pluginManager.reloadPlugin(pluginId);
        return Result.success();
    }

    /**
     * 获取插件配置
     */
    @GetMapping("/{pluginId}/config")
    public Result<PluginConfig> getPluginConfig(@PathVariable String pluginId) {
        PluginConfig config = pluginManager.getPluginContext(pluginId).getConfig();
        return Result.success(config);
    }

    /**
     * 更新插件配置
     */
    @PutMapping("/{pluginId}/config")
    public Result<Void> updatePluginConfig(@PathVariable String pluginId, 
                                           @RequestBody Map<String, Object> config) {
        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.setPluginId(pluginId);
        pluginConfig.setProperties(config);
        pluginManager.updatePluginConfig(pluginId, pluginConfig);
        return Result.success();
    }

    /**
     * 加载外部插件
     */
    @PostMapping("/load")
    public Result<String> loadPlugin(@RequestParam String path) {
        String pluginId = pluginManager.loadPlugin(path);
        if (pluginId != null) {
            return Result.success(pluginId);
        }
        return Result.fail(500, "加载插件失败");
    }

    /**
     * 卸载插件
     */
    @DeleteMapping("/{pluginId}")
    public Result<Void> unloadPlugin(@PathVariable String pluginId) {
        pluginManager.unloadPlugin(pluginId);
        return Result.success();
    }

    /**
     * 获取通道插件
     */
    @GetMapping("/channel/{channelType}")
    public Result<Map<String, Object>> getChannelPlugin(@PathVariable String channelType) {
        var channel = pluginManager.getChannelPlugin(channelType);
        if (channel == null) {
            return Result.fail(404, "通道插件不存在");
        }
        
        return Result.success(Map.of(
                "type", channel.getChannelType(),
                "name", channel.getChannelName(),
                "connected", channel.isConnected()
        ));
    }
}