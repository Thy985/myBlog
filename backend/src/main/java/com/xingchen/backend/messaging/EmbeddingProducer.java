package com.xingchen.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Embedding 消息生产者
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${async.embedding.exchange:embedding.exchange}")
    private String exchange;

    @Value("${async.embedding.routing-key:embedding.routing.key}")
    private String routingKey;

    /**
     * 发送索引文档任务
     */
    public void sendIndexTask(Long articleId, String title, String content, String category) {
        EmbeddingMessage message = EmbeddingMessage.index(articleId, title, content, category);
        send(message);
        log.info("发送索引任务: articleId={}, requestId={}", articleId, message.getRequestId());
    }

    /**
     * 发送删除文档任务
     */
    public void sendDeleteTask(Long articleId) {
        EmbeddingMessage message = EmbeddingMessage.delete(articleId);
        send(message);
        log.info("发送删除任务: articleId={}, requestId={}", articleId, message.getRequestId());
    }

    private void send(EmbeddingMessage message) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
        } catch (Exception e) {
            log.error("发送 Embedding 消息失败: {}", message.getRequestId(), e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}