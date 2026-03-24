package com.xingchen.backend.plugin.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 插件日志记录器
 * 
 * 为每个插件提供独立的日志记录，带插件ID前缀
 */
public class PluginLogger {
    
    private final Logger logger;
    private final String pluginId;
    
    public PluginLogger(String pluginId) {
        this.pluginId = pluginId;
        this.logger = LoggerFactory.getLogger("Plugin." + pluginId);
    }
    
    private String format(String message) {
        return "[" + pluginId + "] " + message;
    }
    
    public void debug(String message) {
        logger.debug(format(message));
    }
    
    public void debug(String message, Object... args) {
        logger.debug(format(message), args);
    }
    
    public void info(String message) {
        logger.info(format(message));
    }
    
    public void info(String message, Object... args) {
        logger.info(format(message), args);
    }
    
    public void warn(String message) {
        logger.warn(format(message));
    }
    
    public void warn(String message, Object... args) {
        logger.warn(format(message), args);
    }
    
    public void warn(String message, Throwable throwable) {
        logger.warn(format(message), throwable);
    }
    
    public void error(String message) {
        logger.error(format(message));
    }
    
    public void error(String message, Object... args) {
        logger.error(format(message), args);
    }
    
    public void error(String message, Throwable throwable) {
        logger.error(format(message), throwable);
    }
}