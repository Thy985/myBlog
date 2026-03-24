package com.xingchen.backend.memory;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 语义化记忆提取器
 * 使用 Embedding 模型进行语义相似度匹配
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SemanticMemoryExtractor {

    private final EmbeddingModel embeddingModel;
    private final MemoryRepository memoryRepository;

    // 相似度阈值
    private static final float SIMILARITY_THRESHOLD = 0.85f;
    // 更新阈值（相似度在此范围内更新而非新增）
    private static final float UPDATE_THRESHOLD = 0.92f;

    /**
     * 语义化提取记忆
     */
    @Async("taskExecutor")
    public void extractMemory(Long userId, String conversation) {
        try {
            // 1. 生成对话的 Embedding
            Response<Embedding> embeddingResponse = embeddingModel.embed(conversation);
            float[] newEmbedding = embeddingResponse.content().vector();

            // 2. 获取用户现有记忆
            List<Memory> existingMemories = memoryRepository.findByUserId(userId);

            // 3. 语义匹配
            MemoryMatchResult matchResult = findBestMatch(newEmbedding, existingMemories);

            // 4. 决策：更新还是新增
            if (matchResult.similarity > UPDATE_THRESHOLD) {
                // 更新现有记忆
                updateMemory(matchResult.memory, conversation, newEmbedding);
                log.debug("更新用户 {} 的记忆，相似度: {}", userId, matchResult.similarity);
            } else if (matchResult.similarity < SIMILARITY_THRESHOLD) {
                // 新增记忆
                createNewMemory(userId, conversation, newEmbedding);
                log.debug("新增用户 {} 的记忆", userId);
            } else {
                // 相似度在阈值之间，合并记忆
                mergeMemory(matchResult.memory, conversation, newEmbedding);
                log.debug("合并用户 {} 的记忆", userId);
            }

        } catch (Exception e) {
            log.error("语义记忆提取失败", e);
        }
    }

    /**
     * 语义搜索记忆
     */
    public List<Memory> searchMemory(Long userId, String query, int limit) {
        try {
            Response<Embedding> queryEmbedding = embeddingModel.embed(query);
            float[] queryVector = queryEmbedding.content().vector();

            List<Memory> userMemories = memoryRepository.findByUserId(userId);
            
            return userMemories.stream()
                    .map(mem -> new MemoryScore(mem, cosineSimilarity(queryVector, mem.getEmbedding())))
                    .filter(ms -> ms.score > 0.7f) // 只返回相关度较高的
                    .sorted(Comparator.comparingDouble(ms -> -ms.score))
                    .limit(limit)
                    .map(ms -> ms.memory)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("语义搜索失败", e);
            return Collections.emptyList();
        }
    }

    // ========== 私有方法 ==========

    private MemoryMatchResult findBestMatch(float[] newEmbedding, List<Memory> existingMemories) {
        Memory bestMatch = null;
        float bestScore = 0;

        for (Memory mem : existingMemories) {
            float similarity = cosineSimilarity(newEmbedding, mem.getEmbedding());
            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = mem;
            }
        }

        return new MemoryMatchResult(bestMatch, bestScore);
    }

    private void updateMemory(Memory memory, String newContent, float[] newEmbedding) {
        // 更新内容（保留重要信息，合并新信息）
        String mergedContent = mergeContent(memory.getContent(), newContent);
        memory.setContent(mergedContent);
        memory.setEmbedding(newEmbedding);
        memory.setUpdateTime(LocalDateTime.now());
        memory.setConfidence(memory.getConfidence() + 1); // 置信度增加
        
        memoryRepository.save(memory);
    }

    private void createNewMemory(Long userId, String content, float[] embedding) {
        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setContent(content);
        memory.setEmbedding(embedding);
        memory.setCreateTime(LocalDateTime.now());
        memory.setUpdateTime(LocalDateTime.now());
        memory.setConfidence(1);
        memory.setTtlDays(30); // 默认 30 天
        memory.setCategory(detectCategory(content));
        
        memoryRepository.save(memory);
    }

    private void mergeMemory(Memory memory, String newContent, float[] newEmbedding) {
        // 计算平均 Embedding
        float[] averagedEmbedding = averageEmbeddings(memory.getEmbedding(), newEmbedding);
        
        String mergedContent = memory.getContent() + "\n" + newContent;
        memory.setContent(mergedContent);
        memory.setEmbedding(averagedEmbedding);
        memory.setUpdateTime(LocalDateTime.now());
        
        memoryRepository.save(memory);
    }

    private String mergeContent(String existing, String newContent) {
        // 简单合并，实际可以使用 LLM 进行智能合并
        return existing + "\n[更新] " + newContent;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0;
        float normA = 0;
        float normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] averageEmbeddings(float[] a, float[] b) {
        float[] result = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (a[i] + b[i]) / 2;
        }
        return result;
    }

    private String detectCategory(String content) {
        // 简单的类别检测
        if (content.contains("喜欢") || content.contains("偏好")) {
            return "PREFERENCE";
        } else if (content.contains("项目") || content.contains("技术")) {
            return "PROJECT";
        } else if (content.contains("工作") || content.contains("职业")) {
            return "CAREER";
        }
        return "GENERAL";
    }

    // ========== 内部类 ==========

    private record MemoryMatchResult(Memory memory, float similarity) {}
    private record MemoryScore(Memory memory, float score) {}
}