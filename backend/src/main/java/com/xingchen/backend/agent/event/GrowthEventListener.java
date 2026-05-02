package com.xingchen.backend.agent.event;

import com.xingchen.backend.config.GrowthRabbitMQConfig;
import com.xingchen.backend.service.GrowthOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthEventListener {

    private final GrowthOrchestrator growthOrchestrator;

    @RabbitListener(queues = GrowthRabbitMQConfig.GROWTH_TASK_QUEUE, containerFactory = "growthListenerContainerFactory")
    public void handleGrowthTask(GrowthEvent event) {
        log.info("收到Growth任务事件: eventType={}, userId={}, eventId={}",
                event.getEventType(), event.getUserId(), event.getEventId());

        try {
            switch (event.getEventType()) {
                case GrowthEvent.TYPE_GROWTH_TASK_STARTED -> handleTaskStarted(event);
                case GrowthEvent.TYPE_CONTENT_GENERATED -> handleContentGenerated(event);
                default -> log.warn("未处理的事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理Growth任务事件失败: eventId={}", event.getEventId(), e);
        }
    }

    @RabbitListener(queues = GrowthRabbitMQConfig.GROWTH_NOTIFY_QUEUE, containerFactory = "growthListenerContainerFactory")
    public void handleGrowthNotification(GrowthEvent event) {
        log.info("收到Growth通知事件: eventType={}, userId={}, eventId={}",
                event.getEventType(), event.getUserId(), event.getEventId());

        try {
            switch (event.getEventType()) {
                case GrowthEvent.TYPE_OPPORTUNITY_DISCOVERED -> handleOpportunityDiscovered(event);
                case GrowthEvent.TYPE_PERFORMANCE_ALERT -> handlePerformanceAlert(event);
                default -> log.warn("未处理的通知类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理Growth通知事件失败: eventId={}", event.getEventId(), e);
        }
    }

    @RabbitListener(queues = GrowthRabbitMQConfig.GROWTH_CONTENT_QUEUE, containerFactory = "growthListenerContainerFactory")
    public void handleContentEvent(GrowthEvent event) {
        log.info("收到内容事件: eventType={}, userId={}, eventId={}",
                event.getEventType(), event.getUserId(), event.getEventId());

        try {
            switch (event.getEventType()) {
                case GrowthEvent.TYPE_CONTENT_GENERATED -> handleContentGenerated(event);
                case GrowthEvent.TYPE_CONTENT_OPTIMIZED -> handleContentOptimized(event);
                default -> log.warn("未处理的内容事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理内容事件失败: eventId={}", event.getEventId(), e);
        }
    }

    private void handleTaskStarted(GrowthEvent event) {
        String taskName = event.getData() != null ?
                (String) event.getData().get("taskName") : "unknown";

        log.info("开始执行Growth任务: taskName={}, userId={}",
                taskName, event.getUserId());

        GrowthOrchestrator.GrowthTaskResult result =
                growthOrchestrator.executeDailyGrowthTask(event.getUserId());

        if (result.isSuccess()) {
            log.info("Growth任务执行成功: userId={}", event.getUserId());
        } else {
            log.warn("Growth任务执行失败: userId={}, message={}",
                    event.getUserId(), result.getMessage());
        }
    }

    private void handleContentGenerated(GrowthEvent event) {
        Map<String, Object> data = event.getData();
        if (data != null) {
            log.info("内容已生成: articleId={}, title={}",
                    event.getArticleId(), data.get("title"));
        }
    }

    private void handleContentOptimized(GrowthEvent event) {
        Map<String, Object> data = event.getData();
        if (data != null) {
            log.info("内容已优化: articleId={}, improvements={}",
                    event.getArticleId(), data.get("improvements"));
        }
    }

    private void handleOpportunityDiscovered(GrowthEvent event) {
        Map<String, Object> data = event.getData();
        if (data != null) {
            log.info("发现内容机会: userId={}, opportunities={}",
                    event.getUserId(), data.keySet());
        }
    }

    private void handlePerformanceAlert(GrowthEvent event) {
        Map<String, Object> data = event.getData();
        if (data != null) {
            String alertType = (String) data.get("alertType");
            String message = (String) data.get("message");

            log.warn("性能告警: userId={}, articleId={}, type={}, message={}",
                    event.getUserId(), event.getArticleId(), alertType, message);
        }
    }
}