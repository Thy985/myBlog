package com.xingchen.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitMQConfig {

    @Value("${async.embedding.queue:embedding.queue}")
    private String queueName;

    @Value("${async.embedding.exchange:embedding.exchange}")
    private String exchangeName;

    @Value("${async.embedding.routing-key:embedding.routing.key}")
    private String routingKey;

    /**
     * 队列
     */
    @Bean
    public Queue embeddingQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", queueName + ".dlq")
                .withArgument("x-message-ttl", 300000)  // 5 分钟超时
                .withArgument("x-max-retries", 3)
                .build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue embeddingDeadLetterQueue() {
        return QueueBuilder.durable(queueName + ".dlq").build();
    }

    /**
     * 交换机
     */
    @Bean
    public DirectExchange embeddingExchange() {
        return new DirectExchange(exchangeName);
    }

    /**
     * 绑定
     */
    @Bean
    public Binding embeddingBinding() {
        return BindingBuilder
                .bind(embeddingQueue())
                .to(embeddingExchange())
                .with(routingKey);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * 监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        return factory;
    }
}