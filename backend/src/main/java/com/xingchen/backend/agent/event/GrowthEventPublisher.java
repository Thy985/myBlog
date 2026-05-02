package com.xingchen.backend.agent.event;

import com.xingchen.backend.config.GrowthRabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishContentGenerated(Long userId, Long articleId, Map<String, Object> metadata) {
        GrowthEvent event = GrowthEvent.contentGenerated(userId, articleId, metadata);
        publish(GrowthRabbitMQConfig.GROWTH_CONTENT_ROUTING_KEY, event);
    }

    public void publishGrowthTaskStarted(Long userId, String taskName) {
        GrowthEvent event = GrowthEvent.create(GrowthEvent.TYPE_GROWTH_TASK_STARTED, userId);
        event.setData(Map.of("taskName", taskName));
        publish(GrowthRabbitMQConfig.GROWTH_TASK_ROUTING_KEY, event);
    }

    public void publishGrowthTaskCompleted(Long userId, String taskName, Map<String, Object> result) {
        GrowthEvent event = GrowthEvent.growthTaskCompleted(userId, taskName, result);
        publish(GrowthRabbitMQConfig.GROWTH_TASK_ROUTING_KEY, event);
    }

    public void publishGrowthTaskFailed(Long userId, String taskName, String errorMessage) {
        GrowthEvent event = GrowthEvent.create(GrowthEvent.TYPE_GROWTH_TASK_FAILED, userId);
        event.setData(Map.of("taskName", taskName, "error", errorMessage));
        publish(GrowthRabbitMQConfig.GROWTH_TASK_ROUTING_KEY, event);
    }

    public void publishOpportunityDiscovered(Long userId, Map<String, Object> opportunities) {
        GrowthEvent event = GrowthEvent.create(GrowthEvent.TYPE_OPPORTUNITY_DISCOVERED, userId);
        event.setData(opportunities);
        publish(GrowthRabbitMQConfig.GROWTH_NOTIFY_ROUTING_KEY, event);
    }

    public void publishPerformanceAlert(Long userId, Long articleId, String alertType, String message) {
        GrowthEvent event = GrowthEvent.performanceAlert(userId, articleId, alertType, message);
        publish(GrowthRabbitMQConfig.GROWTH_NOTIFY_ROUTING_KEY, event);
    }

    public void publish(String routingKey, GrowthEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    GrowthRabbitMQConfig.GROWTH_TASK_EXCHANGE,
                    routingKey,
                    event
            );
            log.info("Growth事件已发布: eventType={}, userId={}, eventId={}",
                    event.getEventType(), event.getUserId(), event.getEventId());
        } catch (Exception e) {
            log.error("发布Growth事件失败: eventType={}, userId={}",
                    event.getEventType(), event.getUserId(), e);
        }
    }
}