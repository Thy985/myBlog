package com.xingchen.backend.messaging;

import com.xingchen.backend.vector.HybridSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Embedding 消息消费者
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmbeddingConsumer {

    private final HybridSearchService hybridSearchService;

    @RabbitListener(
            queues = "${async.embedding.queue:embedding.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleEmbeddingTask(@Payload EmbeddingMessage message) {
        log.info("接收 Embedding 任务: type={}, articleId={}, requestId={}",
                message.getTaskType(), message.getArticleId(), message.getRequestId());

        long startTime = System.currentTimeMillis();

        try {
            switch (message.getTaskType()) {
                case INDEX_DOCUMENT:
                    handleIndex(message);
                    break;
                case DELETE_DOCUMENT:
                    handleDelete(message);
                    break;
                case UPDATE_DOCUMENT:
                    handleUpdate(message);
                    break;
                default:
                    log.warn("未知的任务类型: {}", message.getTaskType());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Embedding 任务完成: articleId={}, duration={}ms", message.getArticleId(), duration);

        } catch (Exception e) {
            log.error("Embedding 任务失败: articleId={}, error={}", message.getArticleId(), e.getMessage(), e);
            // 可以在这里实现重试逻辑或死信队列
            throw e;
        }
    }

    private void handleIndex(EmbeddingMessage message) {
        hybridSearchService.indexDocument(
                message.getArticleId(),
                message.getTitle(),
                message.getContent(),
                message.getCategory()
        );
    }

    private void handleDelete(EmbeddingMessage message) {
        hybridSearchService.deleteDocument(message.getArticleId());
    }

    private void handleUpdate(EmbeddingMessage message) {
        // 先删除旧文档
        hybridSearchService.deleteDocument(message.getArticleId());
        // 再索引新文档
        hybridSearchService.indexDocument(
                message.getArticleId(),
                message.getTitle(),
                message.getContent(),
                message.getCategory()
        );
    }
}