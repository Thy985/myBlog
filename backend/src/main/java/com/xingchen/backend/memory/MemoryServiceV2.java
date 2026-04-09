package com.xingchen.backend.memory;

import com.xingchen.backend.service.AIService;
import com.xingchen.backend.vector.QdrantVectorService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 记忆服务 V2（基于向量检索）
 *
 * 核心特性：
 * 1. 语义化记忆提取 - 使用 Embedding 相似度
 * 2. 记忆分层 - 短期/长期/永久
 * 3. 记忆衰减 - 自动降低低置信度记忆权重
 * 4. 记忆合并 - 相似记忆自动合并
 */
@Service
@Slf4j
public class MemoryServiceV2 {

    private final EmbeddingModel embeddingModel;
    private final MemoryRepository memoryRepository;
    private final QdrantVectorService qdrantVectorService;

    @Autowired
    @Lazy
    private AIService aiService;

    public MemoryServiceV2(EmbeddingModel embeddingModel,
                          MemoryRepository memoryRepository,
                          QdrantVectorService qdrantVectorService) {
        this.embeddingModel = embeddingModel;
        this.memoryRepository = memoryRepository;
        this.qdrantVectorService = qdrantVectorService;
    }

    private volatile boolean llmMergeEnabled = false;

    @Value("${memory.collection:myblog_memories}")
    private String collectionName;
    
    @Value("${memory.short-term-days:7}")
    private int shortTermDays;
    
    @Value("${memory.long-term-days:30}")
    private int longTermDays;
    
    @Value("${memory.similarity-threshold:0.96}")
    private float similarityThreshold;

    @Value("${memory.merge-threshold:0.98}")
    private float mergeThreshold;

    // 本地缓存（用户最近记忆）
    private final Map<Long, List<Memory>> userMemoryCache = new ConcurrentHashMap<>();

    private volatile boolean vectorStoreAvailable = true;
    private int embeddingDimensions;

    @PostConstruct
    public void init() {
        // 从实际的 EmbeddingModel 获取维度，而不是从配置文件
        this.embeddingDimensions = embeddingModel.dimension();
        log.info("记忆服务 V2 初始化中，向量维度: {}", embeddingDimensions);

        try {
            qdrantVectorService.createCollectionIfNotExists(collectionName, embeddingDimensions);
            log.info("记忆向量集合初始化完成: {}, 向量维度: {}", collectionName, embeddingDimensions);
            vectorStoreAvailable = true;
        } catch (Exception e) {
            log.error("初始化记忆向量集合失败，将降级到纯数据库模式", e);
            vectorStoreAvailable = false;
        }
    }

    /**
     * 检查向量存储是否可用
     */
    public boolean isVectorStoreAvailable() {
        return vectorStoreAvailable;
    }

    /**
     * 尝试重新连接向量存储
     */
    public void tryReconnectVectorStore() {
        if (!vectorStoreAvailable) {
            try {
                qdrantVectorService.createCollectionIfNotExists(collectionName, embeddingDimensions);
                vectorStoreAvailable = true;
                log.info("向量存储重新连接成功");
            } catch (Exception e) {
                log.warn("向量存储重新连接失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 定时检查向量存储健康状态
     */
    @Scheduled(fixedDelayString = "${memory.health-check-interval:120000}")
    public void checkVectorStoreHealth() {
        if (!vectorStoreAvailable) {
            tryReconnectVectorStore();
        }
    }

    /**
     * 获取服务健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("vectorStoreAvailable", vectorStoreAvailable);
        status.put("llmMergeEnabled", llmMergeEnabled);
        status.put("collectionName", collectionName);
        status.put("embeddingDimensions", embeddingDimensions);

        if (!vectorStoreAvailable) {
            status.put("status", "DEGRADED");
            status.put("message", "向量存储不可用，使用纯数据库模式");
        } else {
            status.put("status", "HEALTHY");
            status.put("message", "所有服务正常");
        }

        return status;
    }

    /**
     * 添加记忆（语义化提取）
     */
    @Async("taskExecutor")
    public void addMemory(Long userId, String content, String type) {
        try {
            // 1. 生成 Embedding
            Embedding embedding = embeddingModel.embed(content).content();
            float[] vector = embedding.vector();
            
            // 2. 语义相似度搜索
            List<QdrantVectorService.SearchResult> similarMemories = 
                    qdrantVectorService.search(collectionName, vector, 5, "user_id", String.valueOf(userId));
            
            // 3. 决策：更新/合并/新增
            MemoryOperation operation = decideOperation(similarMemories, vector);
            
            switch (operation.action()) {
                case UPDATE -> updateMemory(operation.existingMemory(), content, vector);
                case MERGE -> mergeMemory(operation.existingMemory(), content, vector);
                case CREATE -> createMemory(userId, content, vector, type);
            }
            
            // 4. 更新缓存
            refreshUserMemoryCache(userId);
            
        } catch (Exception e) {
            log.error("添加记忆失败: userId={}", userId, e);
        }
    }

    /**
     * 检索记忆（语义搜索）
     */
    public String retrieveMemory(Long userId, String query, int limit) {
        try {
            // 1. 生成查询向量
            float[] queryVector = embeddingModel.embed(query).content().vector();

            // 2. 向量搜索
            List<QdrantVectorService.SearchResult> results =
                    qdrantVectorService.search(collectionName, queryVector, limit, "user_id", String.valueOf(userId));

            log.info("记忆检索: userId={}, query='{}', 原始结果数={}", userId, query, results.size());

            if (results.isEmpty()) {
                log.info("记忆检索: userId={}, 没有找到结果", userId);
                return "";
            }

            // 3. 过滤低相似度结果（使用更精确的阈值）
            List<QdrantVectorService.SearchResult> filtered = results.stream()
                    .filter(r -> r.score() > 0.92f)
                    .collect(Collectors.toList());

            log.info("记忆检索: userId={}, 过滤后结果数={}", userId, filtered.size());

            if (filtered.isEmpty()) {
                return "";
            }

            // 4. 格式化输出
            StringBuilder sb = new StringBuilder();
            sb.append("相关记忆：\n");
            for (int i = 0; i < filtered.size(); i++) {
                QdrantVectorService.SearchResult result = filtered.get(i);
                String content = result.payload().get("content");
                sb.append("[").append(i + 1).append("] ")
                  .append(content)
                  .append(" (相关度: ").append(String.format("%.2f", result.score())).append(")\n");
                log.info("记忆检索结果: userId={}, content='{}', score={}", userId, content, result.score());
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("检索记忆失败: userId={}", userId, e);
            return "";
        }
    }

    /**
     * 获取用户记忆上下文
     */
    public String getUserMemoryContext(Long userId) {
        List<Memory> memories = getUserMemories(userId);
        
        if (memories.isEmpty()) {
            return "";
        }
        
        // 按类型分组
        Map<String, List<Memory>> grouped = memories.stream()
                .collect(Collectors.groupingBy(Memory::getCategory));
        
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户记忆\n\n");
        
        grouped.forEach((category, list) -> {
            sb.append("### ").append(category).append("\n");
            list.forEach(m -> sb.append("- ").append(m.getContent()).append("\n"));
            sb.append("\n");
        });
        
        return sb.toString();
    }

    /**
     * 清理过期记忆
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 点
    public void cleanupExpiredMemories() {
        log.info("开始清理过期记忆");
        try {
            // 1. 从数据库清理
            memoryRepository.deleteExpired();
            
            // 2. 从向量库清理
            // 向量库过期清理待实现

            // 3. 清理本地缓存
            userMemoryCache.clear();
            
            log.info("过期记忆清理完成");
        } catch (Exception e) {
            log.error("清理过期记忆失败", e);
        }
    }

    /**
     * 记忆衰减（降低低置信度记忆权重）
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨 1 点
    public void decayMemories() {
        log.info("开始记忆衰减处理");
        try {
            List<Memory> allMemories = memoryRepository.findAll();
            
            for (Memory memory : allMemories) {
                // 根据时间衰减置信度
                long daysSinceUpdate = java.time.Duration.between(
                        memory.getUpdateTime(), LocalDateTime.now()).toDays();
                
                int decayFactor = (int) (daysSinceUpdate / 7); // 每周衰减
                int newConfidence = Math.max(1, memory.getConfidence() - decayFactor);
                
                if (newConfidence != memory.getConfidence()) {
                    memory.setConfidence(newConfidence);
                    memoryRepository.save(memory);
                }
                
                // 删除极低置信度的记忆
                if (newConfidence <= 0) {
                    deleteMemory(memory);
                }
            }
            
            log.info("记忆衰减处理完成");
        } catch (Exception e) {
            log.error("记忆衰减处理失败", e);
        }
    }

    // ========== 私有方法 ==========

    private MemoryOperation decideOperation(List<QdrantVectorService.SearchResult> similar, float[] newVector) {
        if (similar.isEmpty()) {
            log.info("decideOperation: 没有相似记忆，执行 CREATE");
            return new MemoryOperation(MemoryAction.CREATE, null);
        }

        QdrantVectorService.SearchResult bestMatch = similar.get(0);
        float similarity = bestMatch.score();
        String content = bestMatch.payload().get("content");

        log.info("decideOperation: bestMatch similarity={}, content='{}', thresholds: similarity={}, merge={}",
                similarity, content != null ? content.substring(0, Math.min(50, content.length())) : "null",
                similarityThreshold, mergeThreshold);

        // 从 payload 中获取 memory_id（数据库自增 ID）
        String memoryIdStr = bestMatch.payload().get("memory_id");
        if (memoryIdStr == null || memoryIdStr.isEmpty()) {
            log.warn("记忆 payload 中没有 memory_id，创建新记忆");
            return new MemoryOperation(MemoryAction.CREATE, null);
        }

        // 获取完整记忆对象
        Memory existing = memoryRepository.findById(Long.parseLong(memoryIdStr));

        if (similarity > mergeThreshold) {
            log.info("decideOperation: similarity {} > merge {} -> MERGE", similarity, mergeThreshold);
            return new MemoryOperation(MemoryAction.MERGE, existing);
        } else if (similarity > similarityThreshold) {
            log.info("decideOperation: similarity {} > similarity {} -> UPDATE", similarity, similarityThreshold);
            return new MemoryOperation(MemoryAction.UPDATE, existing);
        } else {
            log.info("decideOperation: similarity {} <= similarity {} -> CREATE", similarity, similarityThreshold);
            return new MemoryOperation(MemoryAction.CREATE, null);
        }
    }

    private void createMemory(Long userId, String content, float[] vector, String type) {
        // 生成 UUID 作为 Qdrant point ID
        String pointId = UUID.randomUUID().toString();

        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setContent(content);
        memory.setCategory(detectCategory(type, content));
        memory.setConfidence(1);
        memory.setTtlDays(calculateTtl(type));
        memory.setCreateTime(LocalDateTime.now());
        memory.setUpdateTime(LocalDateTime.now());
        memory.setVectorId(pointId); // 保存 vector ID

        // 保存到数据库并获取生成的 ID
        Memory savedMemory = memoryRepository.save(memory);

        // 保存到向量库
        Map<String, String> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(userId));
        payload.put("content", content);
        payload.put("category", savedMemory.getCategory());
        payload.put("confidence", String.valueOf(savedMemory.getConfidence()));
        payload.put("memory_id", String.valueOf(savedMemory.getId()));
        payload.put("vector_id", pointId);

        qdrantVectorService.addVector(collectionName, pointId, vector, payload);

        log.debug("创建记忆: userId={}, type={}, vectorId={}", userId, type, pointId);
    }

    private void updateMemory(Memory memory, String newContent, float[] newVector) {
        // 合并内容
        memory.setContent(mergeContent(memory.getContent(), newContent));
        memory.setUpdateTime(LocalDateTime.now());
        memory.setConfidence(memory.getConfidence() + 1);

        memoryRepository.save(memory);

        // 更新向量
        Map<String, String> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(memory.getUserId()));
        payload.put("content", memory.getContent());
        payload.put("category", memory.getCategory());
        payload.put("confidence", String.valueOf(memory.getConfidence()));
        payload.put("memory_id", String.valueOf(memory.getId()));
        payload.put("vector_id", memory.getVectorId());

        qdrantVectorService.addVector(collectionName, memory.getVectorId(), newVector, payload);

        log.debug("更新记忆: id={}, vectorId={}", memory.getId(), memory.getVectorId());
    }

    private void mergeMemory(Memory memory, String newContent, float[] newVector) {
        String mergedContent;

        if (llmMergeEnabled && aiService != null) {
            try {
                mergedContent = llmMergeContent(memory.getContent(), newContent);
                log.info("使用 LLM 智能合并记忆: id={}", memory.getId());
            } catch (Exception e) {
                log.warn("LLM 合并失败，降级到简单追加: {}", e.getMessage());
                mergedContent = memory.getContent() + "\n补充：" + newContent;
            }
        } else {
            mergedContent = memory.getContent() + "\n补充：" + newContent;
        }

        memory.setContent(mergedContent);
        memory.setUpdateTime(LocalDateTime.now());
        memory.setConfidence(memory.getConfidence() + 1);

        memoryRepository.save(memory);

        // 更新向量
        Map<String, String> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(memory.getUserId()));
        payload.put("content", memory.getContent());
        payload.put("category", memory.getCategory());
        payload.put("confidence", String.valueOf(memory.getConfidence()));
        payload.put("memory_id", String.valueOf(memory.getId()));
        payload.put("vector_id", memory.getVectorId());

        // 生成新的 embedding 向量
        float[] mergedVector = embeddingModel.embed(mergedContent).content().vector();
        qdrantVectorService.addVector(collectionName, memory.getVectorId(), mergedVector, payload);

        log.debug("合并记忆: id={}, vectorId={}", memory.getId(), memory.getVectorId());
    }

    /**
     * LLM 智能合并记忆内容
     */
    private String llmMergeContent(String existingContent, String newContent) {
        String prompt = String.format("""
            你是一个记忆整合专家。请将两条相关的记忆智能合并成一条连贯的记忆。

            原记忆：
            %s

            新记忆：
            %s

            要求：
            1. 去除重复信息，保留核心内容
            2. 将相关细节整合到连贯的叙述中
            3. 保持原意的完整性
            4. 输出合并后的单条记忆，不要包含任何解释性文字

            合并后的记忆：
            """, existingContent, newContent);

        String merged = aiService.chat(prompt);

        if (merged == null || merged.trim().isEmpty()) {
            log.warn("LLM 合并返回空结果，使用简单追加");
            return existingContent + "\n补充：" + newContent;
        }

        return merged.trim();
    }

    private void deleteMemory(Memory memory) {
        memoryRepository.delete(memory.getId());
        if (memory.getVectorId() != null) {
            qdrantVectorService.deleteVector(collectionName, memory.getVectorId());
        }
        log.debug("删除记忆: id={}, vectorId={}", memory.getId(), memory.getVectorId());
    }

    private List<Memory> getUserMemories(Long userId) {
        // 先查缓存
        List<Memory> cached = userMemoryCache.get(userId);
        if (cached != null) {
            return cached;
        }
        
        // 查数据库
        List<Memory> memories = memoryRepository.findByUserId(userId);
        userMemoryCache.put(userId, memories);
        return memories;
    }

    private void refreshUserMemoryCache(Long userId) {
        List<Memory> memories = memoryRepository.findByUserId(userId);
        userMemoryCache.put(userId, memories);
    }

    private String detectCategory(String type, String content) {
        if (type != null && !type.isEmpty()) {
            return type.toUpperCase();
        }
        
        // 基于内容检测
        if (content.contains("喜欢") || content.contains("偏好")) {
            return "PREFERENCE";
        } else if (content.contains("项目") || content.contains("技术")) {
            return "PROJECT";
        } else if (content.contains("工作") || content.contains("职业")) {
            return "CAREER";
        } else if (content.contains("问题") || content.contains("bug")) {
            return "ISSUE";
        }
        return "GENERAL";
    }

    private int calculateTtl(String type) {
        return switch (type.toUpperCase()) {
            case "SHORT_TERM" -> shortTermDays;
            case "LONG_TERM" -> longTermDays;
            case "PERMANENT" -> 365 * 10; // 10 年
            default -> longTermDays;
        };
    }

    private String mergeContent(String existing, String newContent) {
        return existing + "\n[更新] " + newContent;
    }

    // ========== 内部类 ==========
    
    private enum MemoryAction {
        CREATE, UPDATE, MERGE
    }
    
    private record MemoryOperation(MemoryAction action, Memory existingMemory) {}
}