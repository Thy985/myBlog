package com.xingchen.backend.ai.tool;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 * 管理所有可用工具
 */
@Component
@Slf4j
public class AIToolRegistry {
    
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;
    
    public AIToolRegistry(CircuitBreakerRegistry circuitBreakerRegistry,
                        MeterRegistry meterRegistry,
                        Collection<Tool> toolBeans) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistry = meterRegistry;
        
        // 自动注册所有工具
        toolBeans.forEach(this::register);
        
        log.info("工具注册表初始化完成，共 {} 个工具", tools.size());
    }
    
    /**
     * 注册工具
     */
    public void register(Tool tool) {
        if (tool == null || tool.getName() == null) {
            log.warn("尝试注册无效工具");
            return;
        }

        String name = tool.getName();
        tools.put(name, tool);

        log.info("注册工具: {} (async={}, timeout={}ms)",
                name, tool.isAsync(), tool.getTimeout());
    }
    
    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * 执行工具
     */
    public Tool.ToolResult execute(String toolName, Map<String, Object> parameters) {
        Tool tool = tools.get(toolName);
        if (tool == null) {
            log.error("工具不存在: {}", toolName);
            return Tool.ToolResult.error("工具不存在: " + toolName);
        }
        
        // 参数校验
        if (!validateParameters(tool, parameters)) {
            return Tool.ToolResult.error("参数校验失败");
        }
        
        // 使用熔断器执行
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(toolName);
        Timer timer = Timer.builder("ai.tool.execution")
                .tag("tool", toolName)
                .register(meterRegistry);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Tool.ToolResult result = cb.executeSupplier(() -> tool.execute(parameters));
            long elapsed = System.currentTimeMillis() - startTime;
            
            timer.record(elapsed, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            log.debug("工具执行完成: tool={}, elapsed={}ms, success={}", 
                    toolName, elapsed, result.success());
            
            return result.withExecutionTime(elapsed);
            
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("工具执行失败: tool={}, elapsed={}ms", toolName, elapsed, e);
            return Tool.ToolResult.error("执行失败: " + e.getMessage())
                    .withExecutionTime(elapsed);
        }
    }
    
    /**
     * 获取所有工具描述（用于Prompt）
     */
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用工具:\n");
        
        tools.values().forEach(tool -> {
            sb.append("- ").append(tool.getName()).append(": ")
              .append(tool.getDescription()).append("\n");
            
            Tool.ToolParameter[] params = tool.getParameters();
            if (params != null && params.length > 0) {
                sb.append("  参数:\n");
                for (Tool.ToolParameter param : params) {
                    sb.append("    - ").append(param.name())
                      .append(" (").append(param.type()).append(")")
                      .append(param.required() ? " [必填]" : " [可选]")
                      .append(": ").append(param.description());
                    if (param.defaultValue() != null) {
                        sb.append("，默认: ").append(param.defaultValue());
                    }
                    sb.append("\n");
                }
            }
        });
        
        return sb.toString();
    }
    
    /**
     * 参数校验
     */
    private boolean validateParameters(Tool tool, Map<String, Object> parameters) {
        Tool.ToolParameter[] definedParams = tool.getParameters();
        if (definedParams == null) {
            return true;
        }
        
        for (Tool.ToolParameter param : definedParams) {
            if (param.required() && !parameters.containsKey(param.name())) {
                log.error("缺少必填参数: tool={}, param={}", tool.getName(), param.name());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return tools.size();
    }
    
    /**
     * 获取所有可用工具
     */
    public java.util.List<Tool> getAvailableTools() {
        return new java.util.ArrayList<>(tools.values());
    }
}