package com.xingchen.backend.meta;

import com.xingchen.backend.plugin.PluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表
 *
 * 管理所有可用工具，包括本地插件和远程 MCP 服务
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ToolRegistry {

    private final PluginManager pluginManager;

    @Value("${mcp.whisper.url:}")
    private String whisperUrl;

    @Value("${mcp.vision.url:}")
    private String visionUrl;

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    // 本地工具
    private final Map<String, ToolOption> localTools = new HashMap<>();
    // 远程 MCP 服务
    private final Map<String, ToolOption> mcpServices = new HashMap<>();
    // MCP 服务连接状态
    private final Map<String, McpConnectionStatus> mcpConnectionStatus = new ConcurrentHashMap<>();
    // 混合工具组合
    private final Map<String, List<String>> toolCombinations = new HashMap<>();

    @PostConstruct
    public void initialize() {
        registerBuiltinTools();
        log.info("工具注册表初始化完成，共 {} 个本地工具，{} 个 MCP 服务",
                localTools.size(), mcpServices.size());
        initializeMcpConnections();
    }

    /**
     * 初始化 MCP 服务连接
     */
    private void initializeMcpConnections() {
        if (!mcpEnabled) {
            log.info("MCP 服务已禁用，使用模拟模式");
            for (String toolId : mcpServices.keySet()) {
                mcpConnectionStatus.put(toolId, new McpConnectionStatus(false, "MCP 已禁用", 0));
            }
            return;
        }

        mcpServices.forEach((toolId, tool) -> {
            mcpConnectionStatus.put(toolId, new McpConnectionStatus(false, "待连接", 0));
        });

        checkMcpHealthAll();
    }

    /**
     * 定期检查 MCP 服务健康状态
     */
    @Scheduled(fixedDelayString = "${mcp.health-check-interval:60000}")
    public void checkMcpHealthAll() {
        if (!mcpEnabled) {
            return;
        }

        mcpServices.forEach((toolId, tool) -> {
            try {
                boolean healthy = checkMcpServiceHealth(toolId);
                mcpConnectionStatus.put(toolId,
                        new McpConnectionStatus(healthy, healthy ? "健康" : "连接失败", System.currentTimeMillis()));
            } catch (Exception e) {
                log.warn("MCP 服务 {} 健康检查失败: {}", toolId, e.getMessage());
                mcpConnectionStatus.put(toolId,
                        new McpConnectionStatus(false, e.getMessage(), System.currentTimeMillis()));
            }
        });
    }

    /**
     * 检查单个 MCP 服务健康状态
     */
    private boolean checkMcpServiceHealth(String toolId) {
        if (!mcpEnabled) {
            return false;
        }

        return switch (toolId) {
            case "mcp-whisper" -> checkWhisperHealth();
            case "mcp-vision" -> checkVisionHealth();
            default -> false;
        };
    }

    /**
     * Whisper 服务健康检查
     */
    private boolean checkWhisperHealth() {
        if (whisperUrl == null || whisperUrl.isEmpty()) {
            return false;
        }
        try {
            // 简化的健康检查 - 实际应该调用服务健康端点
            return true;
        } catch (Exception e) {
            log.warn("Whisper 服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Vision 服务健康检查
     */
    private boolean checkVisionHealth() {
        if (visionUrl == null || visionUrl.isEmpty()) {
            return false;
        }
        try {
            // 简化的健康检查 - 实际应该调用服务健康端点
            return true;
        } catch (Exception e) {
            log.warn("Vision 服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 MCP 服务连接状态
     */
    public McpConnectionStatus getMcpConnectionStatus(String toolId) {
        return mcpConnectionStatus.getOrDefault(toolId,
                new McpConnectionStatus(false, "未知", 0));
    }

    /**
     * 检查 MCP 服务是否已连接
     */
    public boolean isMcpConnected(String toolId) {
        if (!mcpEnabled) {
            return false;
        }
        McpConnectionStatus status = mcpConnectionStatus.get(toolId);
        return status != null && status.connected();
    }

    /**
     * MCP 连接状态
     */
    public record McpConnectionStatus(boolean connected, String message, long lastCheckTime) {}

    /**
     * 注册内置工具
     */
    private void registerBuiltinTools() {
        // LLM 工具
        registerTool(ToolOption.builder()
                .toolId("llm-glm")
                .toolType("llm")
                .description("智谱 GLM 大模型")
                .capabilities(Map.of(
                        "text-generation", 0.9,
                        "code-generation", 0.85,
                        "reasoning", 0.88
                ))
                .cost(0.01)
                .latency(2000)
                .accuracy(0.9)
                .build());
        
        // 代码执行工具
        registerTool(ToolOption.builder()
                .toolId("code-executor")
                .toolType("code")
                .description("本地代码执行器")
                .capabilities(Map.of(
                        "java-execution", 0.95,
                        "python-execution", 0.95,
                        "javascript-execution", 0.9
                ))
                .cost(0.0)
                .latency(500)
                .accuracy(1.0)
                .build());
        
        // 文件操作工具
        registerTool(ToolOption.builder()
                .toolId("file-operator")
                .toolType("file")
                .description("文件读写操作")
                .capabilities(Map.of(
                        "read", 1.0,
                        "write", 1.0,
                        "delete", 1.0,
                        "list", 1.0
                ))
                .cost(0.0)
                .latency(100)
                .accuracy(1.0)
                .build());
        
        // 搜索工具
        registerTool(ToolOption.builder()
                .toolId("hybrid-search")
                .toolType("search")
                .description("混合检索（BM25 + 向量）")
                .capabilities(Map.of(
                        "text-search", 0.92,
                        "semantic-search", 0.88,
                        "hybrid-search", 0.95
                ))
                .cost(0.001)
                .latency(300)
                .accuracy(0.9)
                .build());
        
        // 语音识别工具（模拟 MCP 服务）
        registerMcpService(ToolOption.builder()
                .toolId("mcp-whisper")
                .toolType("audio")
                .description("Whisper 语音识别（远程 MCP）")
                .capabilities(Map.of(
                        "speech-to-text", 0.95,
                        "audio-transcription", 0.93
                ))
                .cost(0.05)
                .latency(5000)
                .accuracy(0.95)
                .build());
        
        // 图像识别工具（模拟 MCP 服务）
        registerMcpService(ToolOption.builder()
                .toolId("mcp-vision")
                .toolType("vision")
                .description("图像识别分析（远程 MCP）")
                .capabilities(Map.of(
                        "image-analysis", 0.9,
                        "object-detection", 0.88,
                        "ocr", 0.92
                ))
                .cost(0.03)
                .latency(3000)
                .accuracy(0.9)
                .build());
        
        // 数据库工具
        registerTool(ToolOption.builder()
                .toolId("database")
                .toolType("database")
                .description("数据库操作")
                .capabilities(Map.of(
                        "query", 0.98,
                        "insert", 0.98,
                        "update", 0.98,
                        "delete", 0.98
                ))
                .cost(0.0)
                .latency(50)
                .accuracy(1.0)
                .build());
        
        // 缓存工具
        registerTool(ToolOption.builder()
                .toolId("cache")
                .toolType("cache")
                .description("缓存操作")
                .capabilities(Map.of(
                        "get", 0.99,
                        "set", 0.99,
                        "delete", 0.99
                ))
                .cost(0.0)
                .latency(5)
                .accuracy(1.0)
                .build());
    }
    
    /**
     * 注册本地工具
     */
    public void registerTool(ToolOption tool) {
        localTools.put(tool.getToolId(), tool);
        log.debug("注册本地工具: {}", tool.getToolId());
    }
    
    /**
     * 注册 MCP 服务
     */
    public void registerMcpService(ToolOption service) {
        mcpServices.put(service.getToolId(), service);
        log.debug("注册 MCP 服务: {}", service.getToolId());
    }
    
    /**
     * 获取工具
     */
    public ToolOption getTool(String toolId) {
        ToolOption tool = localTools.get(toolId);
        if (tool != null) return tool;
        return mcpServices.get(toolId);
    }
    
    /**
     * 获取所有可用工具
     */
    public List<ToolOption> getAvailableTools() {
        List<ToolOption> all = new ArrayList<>();
        all.addAll(localTools.values());
        all.addAll(mcpServices.values());
        return all;
    }
    
    /**
     * 根据能力搜索工具
     */
    public List<ToolOption> findToolsByCapability(String capability) {
        List<ToolOption> result = new ArrayList<>();
        
        for (ToolOption tool : getAvailableTools()) {
            if (tool.getCapabilities().containsKey(capability)) {
                result.add(tool);
            }
        }
        
        return result;
    }
    
    /**
     * 获取默认工具
     */
    public ToolOption getDefaultTool() {
        return localTools.get("llm-glm");
    }
    
    /**
     * 智能选择工具
     * 
     * 根据任务需求、成本、延迟等因素选择最佳工具
     */
    public ToolSelectionResult selectBestTool(String task, List<String> requiredCapabilities,
                                               ToolSelectionConstraints constraints) {
        List<ToolOption> candidates = new ArrayList<>();
        
        // 收集所有满足能力要求的工具
        for (String capability : requiredCapabilities) {
            candidates.addAll(findToolsByCapability(capability));
        }
        
        // 去重
        candidates = candidates.stream().distinct().toList();
        
        // 评分排序
        List<ToolScore> scores = candidates.stream()
                .map(tool -> new ToolScore(tool, calculateScore(tool, task, constraints)))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
        
        if (scores.isEmpty()) {
            return ToolSelectionResult.builder()
                    .success(false)
                    .message("未找到满足要求的工具")
                    .build();
        }
        
        // 返回最佳工具
        ToolScore best = scores.get(0);
        
        // 如果最佳工具分数不够高，考虑组合多个工具
        if (best.score() < 0.7 && scores.size() > 1) {
            List<ToolOption> combination = buildToolCombination(scores, requiredCapabilities);
            if (combination.size() > 1) {
                return ToolSelectionResult.builder()
                        .success(true)
                        .tools(combination)
                        .strategy(ToolSelectionResult.SelectionStrategy.COMBINATION)
                        .message("使用工具组合")
                        .build();
            }
        }
        
        return ToolSelectionResult.builder()
                .success(true)
                .tools(List.of(best.tool()))
                .strategy(ToolSelectionResult.SelectionStrategy.SINGLE)
                .message("选择最佳工具: " + best.tool().getToolId())
                .build();
    }
    
    /**
     * 计算工具评分
     */
    private double calculateScore(ToolOption tool, String task, ToolSelectionConstraints constraints) {
        double score = 0;
        
        // 能力匹配度 (40%)
        score += calculateCapabilityScore(tool) * 0.4;
        
        // 准确率 (20%)
        score += tool.getAccuracy() * 0.2;
        
        // 成本（越低越好）(15%)
        if (constraints.getMaxCost() > 0) {
            double costRatio = tool.getCost() / constraints.getMaxCost();
            score += (1 - Math.min(costRatio, 1)) * 0.15;
        } else {
            score += (1 - tool.getCost()) * 0.15;
        }
        
        // 延迟（越低越好）(15%)
        if (constraints.getMaxLatency() > 0) {
            double latencyRatio = tool.getLatency() / constraints.getMaxLatency();
            score += (1 - Math.min(latencyRatio, 1)) * 0.15;
        } else {
            score += (1 - tool.getLatency() / 10000) * 0.15;
        }
        
        // 可用性 (10%)
        score += (isToolAvailable(tool) ? 1 : 0) * 0.1;
        
        return score;
    }
    
    /**
     * 计算能力匹配分数
     */
    private double calculateCapabilityScore(ToolOption tool) {
        return tool.getCapabilities().values().stream()
                .mapToDouble(v -> v instanceof Number ? ((Number) v).doubleValue() : 0)
                .average()
                .orElse(0);
    }
    
    /**
     * 构建工具组合
     */
    private List<ToolOption> buildToolCombination(List<ToolScore> scores, 
                                                   List<String> requiredCapabilities) {
        // 简化实现：返回前2-3个工具
        int count = Math.min(3, scores.size());
        return scores.subList(0, count).stream()
                .map(ToolScore::tool)
                .toList();
    }
    
    /**
     * 检查工具是否可用
     */
    private boolean isToolAvailable(ToolOption tool) {
        // 本地工具始终可用
        if (localTools.containsKey(tool.getToolId())) {
            return true;
        }
        // MCP 服务需要检查连接状态
        // 简化实现，实际应该检查服务健康状态
        return true;
    }
    
    /**
     * 工具评分
     */
    private record ToolScore(ToolOption tool, double score) {}
    
    /**
     * 工具选择约束
     */
    @lombok.Data
    @lombok.Builder
    public static class ToolSelectionConstraints {
        private double maxCost;
        private double maxLatency;
        private double minAccuracy;
        private boolean preferLocal;
        private boolean allowCombination;
    }
    
    /**
     * 工具选择结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ToolSelectionResult {
        private boolean success;
        private List<ToolOption> tools;
        private SelectionStrategy strategy;
        private String message;
        
        public enum SelectionStrategy {
            SINGLE,      // 单一工具
            COMBINATION, // 工具组合
            FALLBACK     // 降级方案
        }
    }
}