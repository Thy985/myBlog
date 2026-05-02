package com.xingchen.backend.agent.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthEvent implements Serializable {

    private String eventId;
    private String eventType;
    private Long userId;
    private Long articleId;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    private String source;

    public static final String TYPE_CONTENT_GENERATED = "CONTENT_GENERATED";
    public static final String TYPE_CONTENT_PUBLISHED = "CONTENT_PUBLISHED";
    public static final String TYPE_CONTENT_OPTIMIZED = "CONTENT_OPTIMIZED";
    public static final String TYPE_OPPORTUNITY_DISCOVERED = "OPPORTUNITY_DISCOVERED";
    public static final String TYPE_GROWTH_TASK_STARTED = "GROWTH_TASK_STARTED";
    public static final String TYPE_GROWTH_TASK_COMPLETED = "GROWTH_TASK_COMPLETED";
    public static final String TYPE_GROWTH_TASK_FAILED = "GROWTH_TASK_FAILED";
    public static final String TYPE_PERFORMANCE_ALERT = "PERFORMANCE_ALERT";

    public static GrowthEvent create(String eventType, Long userId) {
        return GrowthEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static GrowthEvent contentGenerated(Long userId, Long articleId, Map<String, Object> metadata) {
        return GrowthEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(TYPE_CONTENT_GENERATED)
                .userId(userId)
                .articleId(articleId)
                .data(metadata)
                .timestamp(LocalDateTime.now())
                .source("ContentGenerationAgent")
                .build();
    }

    public static GrowthEvent growthTaskCompleted(Long userId, String taskName, Map<String, Object> result) {
        return GrowthEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(TYPE_GROWTH_TASK_COMPLETED)
                .userId(userId)
                .data(Map.of("taskName", taskName, "result", result))
                .timestamp(LocalDateTime.now())
                .source("GrowthOrchestrator")
                .build();
    }

    public static GrowthEvent performanceAlert(Long userId, Long articleId, String alertType, String message) {
        return GrowthEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(TYPE_PERFORMANCE_ALERT)
                .userId(userId)
                .articleId(articleId)
                .data(Map.of("alertType", alertType, "message", message))
                .timestamp(LocalDateTime.now())
                .source("BehaviorAnalyzerAgent")
                .build();
    }
}