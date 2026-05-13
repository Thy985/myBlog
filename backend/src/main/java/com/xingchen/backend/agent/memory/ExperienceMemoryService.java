package com.xingchen.backend.agent.memory;

import com.xingchen.backend.repository.MemoryVectorRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceMemoryService {

    private final MemoryVectorRepository memoryVectorRepository;
    private final EmbeddingModel embeddingModel;

    private static final String CATEGORY_REFLECTION = "reflection_insight";
    private static final String CATEGORY_GROWTH = "growth_experience";
    private static final String CATEGORY_CONTENT = "content_feedback";
    private static final int DEFAULT_TOP_K = 5;
    private static final int TTL_DAYS = 90;

    public void storeReflectionInsight(
            Long userId,
            String insight,
            String taskType,
            Map<String, Object> metadata) {

        try {
            Embedding embedding = embeddingModel.embed(insight).content();

            Map<String, Object> fullContent = new LinkedHashMap<>();
            fullContent.put("insight", insight);
            fullContent.put("taskType", taskType);
            fullContent.put("metadata", metadata != null ? metadata : Map.of());
            fullContent.put("timestamp", LocalDateTime.now().toString());

            String content = formatContent(fullContent);

            MemoryVectorRepository.MemoryRecord record = new MemoryVectorRepository.MemoryRecord(
                    null,
                    userId,
                    content,
                    toFloatArray(embedding),
                    CATEGORY_REFLECTION,
                    85,
                    TTL_DAYS,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            memoryVectorRepository.insert(record);
            log.info("反思洞察已存储: userId={}, taskType={}, insight={}",
                    userId, taskType, truncate(insight, 50));

        } catch (Exception e) {
            log.error("存储反思洞察失败: userId={}, taskType={}", userId, taskType, e);
        }
    }

    public void storeGrowthExperience(
            Long userId,
            String experience,
            String phase,
            boolean success,
            Map<String, Object> details) {

        try {
            Embedding embedding = embeddingModel.embed(experience).content();

            Map<String, Object> fullContent = new LinkedHashMap<>();
            fullContent.put("experience", experience);
            fullContent.put("phase", phase);
            fullContent.put("success", success);
            fullContent.put("details", details != null ? details : Map.of());
            fullContent.put("timestamp", LocalDateTime.now().toString());

            String content = formatContent(fullContent);

            MemoryVectorRepository.MemoryRecord record = new MemoryVectorRepository.MemoryRecord(
                    null,
                    userId,
                    content,
                    toFloatArray(embedding),
                    CATEGORY_GROWTH,
                    success ? 90 : 70,
                    TTL_DAYS,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            memoryVectorRepository.insert(record);
            log.info("增长经验已存储: userId={}, phase={}, success={}",
                    userId, phase, success);

        } catch (Exception e) {
            log.error("存储增长经验失败: userId={}, phase={}", userId, phase, e);
        }
    }

    public void storeContentFeedback(
            Long userId,
            String contentType,
            String feedback,
            String quality,
            Map<String, Object> details) {

        try {
            Embedding embedding = embeddingModel.embed(feedback).content();

            Map<String, Object> fullContent = new LinkedHashMap<>();
            fullContent.put("feedback", feedback);
            fullContent.put("contentType", contentType);
            fullContent.put("quality", quality);
            fullContent.put("details", details != null ? details : Map.of());
            fullContent.put("timestamp", LocalDateTime.now().toString());

            String formattedContent = formatContent(fullContent);

            int confidence = switch (quality.toUpperCase()) {
                case "HIGH", "APPROVED" -> 95;
                case "MEDIUM", "REVISION" -> 75;
                default -> 60;
            };

            MemoryVectorRepository.MemoryRecord record = new MemoryVectorRepository.MemoryRecord(
                    null,
                    userId,
                    formattedContent,
                    toFloatArray(embedding),
                    CATEGORY_CONTENT,
                    confidence,
                    TTL_DAYS,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            memoryVectorRepository.insert(record);
            log.info("内容反馈已存储: userId={}, contentType={}, quality={}",
                    userId, contentType, quality);

        } catch (Exception e) {
            log.error("存储内容反馈失败: userId={}, contentType={}", userId, contentType, e);
        }
    }

    public List<ExperienceSearchResult> retrieveRelevantInsights(
            Long userId,
            String currentTask,
            String category,
            int topK) {

        try {
            if (currentTask == null || currentTask.isBlank()) {
                return List.of();
            }

            Embedding embedding = embeddingModel.embed(currentTask).content();
            float[] queryVector = toFloatArray(embedding);

            List<MemoryVectorRepository.MemorySearchResult> results =
                    memoryVectorRepository.searchByVector(userId, queryVector, topK * 2);

            if (category != null && !category.isEmpty()) {
                results = results.stream()
                        .filter(r -> category.equals(r.category()))
                        .toList();
            }

            return results.stream()
                    .limit(topK)
                    .map(this::convertToExperienceSearchResult)
                    .toList();

        } catch (Exception e) {
            log.error("检索相关洞察失败: userId={}, task={}", userId, currentTask, e);
            return List.of();
        }
    }

    public List<ExperienceSearchResult> retrieveReflectionInsights(Long userId, int topK) {
        return retrieveRelevantInsights(userId, "reflection insight learning", CATEGORY_REFLECTION, topK);
    }

    public List<ExperienceSearchResult> retrieveGrowthExperiences(Long userId, String phase, int topK) {
        String query = phase != null ? phase : "growth experience success pattern";
        return retrieveRelevantInsights(userId, query, CATEGORY_GROWTH, topK);
    }

    public List<ExperienceSearchResult> retrieveContentFeedback(Long userId, String contentType, int topK) {
        String query = contentType != null ? contentType : "content quality feedback";
        return retrieveRelevantInsights(userId, query, CATEGORY_CONTENT, topK);
    }

    public String buildContextForReflection(Long userId, String currentTask) {
        List<ExperienceSearchResult> insights = retrieveRelevantInsights(
                userId,
                currentTask,
                null,
                DEFAULT_TOP_K
        );

        if (insights.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n=== 历史相关经验 ===\n");

        for (int i = 0; i < insights.size(); i++) {
            ExperienceSearchResult result = insights.get(i);
            context.append(String.format("\n[%d] 相关度: %.2f (%s)\n%s\n",
                    i + 1,
                    result.similarity(),
                    result.category(),
                    result.experience()
            ));
        }

        context.append("=====================\n");
        return context.toString();
    }

    public void cleanupExpiredMemories() {
        try {
            int deleted = memoryVectorRepository.cleanupExpired();
            if (deleted > 0) {
                log.info("清理过期记忆: deleted={}", deleted);
            }
        } catch (Exception e) {
            log.error("清理过期记忆失败", e);
        }
    }

    public long getMemoryCount(Long userId) {
        return memoryVectorRepository.countByUserId(userId);
    }

    public Map<String, Long> getMemoryStats(Long userId) {
        List<MemoryVectorRepository.MemoryRecord> memories = memoryVectorRepository.findByUserId(userId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) memories.size());
        stats.put(CATEGORY_REFLECTION, memories.stream()
                .filter(m -> CATEGORY_REFLECTION.equals(m.category())).count());
        stats.put(CATEGORY_GROWTH, memories.stream()
                .filter(m -> CATEGORY_GROWTH.equals(m.category())).count());
        stats.put(CATEGORY_CONTENT, memories.stream()
                .filter(m -> CATEGORY_CONTENT.equals(m.category())).count());

        return stats;
    }

    private float[] toFloatArray(Embedding embedding) {
        List<Float> values = embedding.vectorAsList();
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private String formatContent(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        data.forEach((key, value) -> {
            sb.append(key).append(": ").append(value).append("\n");
        });
        return sb.toString().trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private ExperienceSearchResult convertToExperienceSearchResult(
            MemoryVectorRepository.MemorySearchResult result) {

        String experience = extractMainContent(result.content());
        String phase = extractField(result.content(), "phase");
        String taskType = extractField(result.content(), "taskType");
        String quality = extractField(result.content(), "quality");

        return new ExperienceSearchResult(
                result.id(),
                result.userId(),
                experience,
                result.category(),
                result.confidence(),
                result.similarity(),
                phase,
                taskType,
                quality
        );
    }

    private String extractMainContent(String content) {
        if (content == null) return "";
        if (content.contains("insight:")) {
            return extractField(content, "insight");
        }
        if (content.contains("experience:")) {
            return extractField(content, "experience");
        }
        if (content.contains("feedback:")) {
            return extractField(content, "feedback");
        }
        return content;
    }

    private String extractField(String content, String field) {
        if (content == null || field == null) return "";
        String searchKey = field + ": ";
        int startIndex = content.indexOf(searchKey);
        if (startIndex == -1) return "";
        startIndex += searchKey.length();
        int endIndex = content.indexOf("\n", startIndex);
        if (endIndex == -1) {
            endIndex = content.length();
        }
        String value = content.substring(startIndex, endIndex).trim();
        if (value.startsWith("{")) {
            int closeBrace = value.lastIndexOf("}");
            if (closeBrace > 0) {
                value = value.substring(0, closeBrace + 1);
            }
        }
        return value;
    }

    public record ExperienceSearchResult(
            Long id,
            Long userId,
            String experience,
            String category,
            int confidence,
            float similarity,
            String phase,
            String taskType,
            String quality
    ) {}
}