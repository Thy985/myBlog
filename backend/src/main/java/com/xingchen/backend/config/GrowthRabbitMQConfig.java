package com.xingchen.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrowthRabbitMQConfig {

    public static final String GROWTH_TASK_QUEUE = "growth.task.queue";
    public static final String GROWTH_TASK_EXCHANGE = "growth.task.exchange";
    public static final String GROWTH_TASK_ROUTING_KEY = "growth.task";
    public static final String GROWTH_DLQ = "growth.task.dlq";

    public static final String GROWTH_NOTIFY_QUEUE = "growth.notify.queue";
    public static final String GROWTH_NOTIFY_EXCHANGE = "growth.notify.exchange";
    public static final String GROWTH_NOTIFY_ROUTING_KEY = "growth.notify";

    public static final String GROWTH_CONTENT_QUEUE = "growth.content.queue";
    public static final String GROWTH_CONTENT_EXCHANGE = "growth.content.exchange";
    public static final String GROWTH_CONTENT_ROUTING_KEY = "growth.content";

    @Bean
    public Queue growthTaskQueue() {
        return QueueBuilder.durable(GROWTH_TASK_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", GROWTH_DLQ)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    @Bean
    public Queue growthDeadLetterQueue() {
        return QueueBuilder.durable(GROWTH_DLQ).build();
    }

    @Bean
    public Queue growthNotifyQueue() {
        return QueueBuilder.durable(GROWTH_NOTIFY_QUEUE).build();
    }

    @Bean
    public Queue growthContentQueue() {
        return QueueBuilder.durable(GROWTH_CONTENT_QUEUE)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public DirectExchange growthTaskExchange() {
        return new DirectExchange(GROWTH_TASK_EXCHANGE);
    }

    @Bean
    public DirectExchange growthNotifyExchange() {
        return new DirectExchange(GROWTH_NOTIFY_EXCHANGE);
    }

    @Bean
    public DirectExchange growthContentExchange() {
        return new DirectExchange(GROWTH_CONTENT_EXCHANGE);
    }

    @Bean
    public Binding growthTaskBinding() {
        return BindingBuilder
                .bind(growthTaskQueue())
                .to(growthTaskExchange())
                .with(GROWTH_TASK_ROUTING_KEY);
    }

    @Bean
    public Binding growthNotifyBinding() {
        return BindingBuilder
                .bind(growthNotifyQueue())
                .to(growthNotifyExchange())
                .with(GROWTH_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Binding growthContentBinding() {
        return BindingBuilder
                .bind(growthContentQueue())
                .to(growthContentExchange())
                .with(GROWTH_CONTENT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter growthJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate growthRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(growthJsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory growthListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(growthJsonMessageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(5);
        return factory;
    }
}