package com.xingchen.backend.workflow;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageRouter {

    private final StringRedisTemplate redisTemplate;

    private final Map<String, RouteRule> routeRules = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<RoutedMessage>>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, MessagePipeline> pipelines = new ConcurrentHashMap<>();

    private static final String MESSAGE_QUEUE_PREFIX = "workflow:message:queue:";
    private static final String ROUTE_TABLE_KEY = "workflow:route:rules";

    public void registerRoute(RouteRule rule) {
        if (rule.getRuleId() == null) {
            rule.setRuleId(UUID.randomUUID().toString());
        }
        routeRules.put(rule.getRuleId(), rule);
        log.info("路由规则已注册: ruleId={}, name={}, pattern={}",
                rule.getRuleId(), rule.getName(), rule.getPattern());
    }

    public void registerSubscriber(String topic, Consumer<RoutedMessage> handler) {
        subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(handler);
        log.info("消息订阅者已注册: topic={}", topic);
    }

    public void unregisterSubscriber(String topic, Consumer<RoutedMessage> handler) {
        List<Consumer<RoutedMessage>> handlers = subscribers.get(topic);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                subscribers.remove(topic);
            }
        }
    }

    public String routeMessage(RoutableMessage message) {
        String routingKey = determineRoutingKey(message);
        log.debug("消息路由: messageId={}, routingKey={}", message.getMessageId(), routingKey);

        List<RouteRule> matchingRules = findMatchingRules(routingKey);

        if (matchingRules.isEmpty()) {
            log.warn("没有匹配的路由规则: routingKey={}", routingKey);
            return null;
        }

        RouteRule bestRule = matchingRules.get(0);
        String targetNode = bestRule.getTargetNode();

        for (RouteRule rule : matchingRules) {
            if (rule.getPriority() > bestRule.getPriority()) {
                bestRule = rule;
                targetNode = rule.getTargetNode();
            }
        }

        RoutedMessage routedMessage = RoutedMessage.builder()
                .messageId(message.getMessageId())
                .source(message.getSource())
                .target(targetNode)
                .payload(message.getPayload())
                .routingKey(routingKey)
                .matchedRule(bestRule.getRuleId())
                .routeTime(Instant.now())
                .metadata(message.getMetadata())
                .build();

        deliverMessage(routedMessage, bestRule);

        return targetNode;
    }

    public void publishMessage(String topic, Object payload, Map<String, Object> metadata) {
        String messageId = UUID.randomUUID().toString();
        publishMessage(topic, payload, metadata, messageId);
    }

    public void publishMessage(String topic, Object payload, Map<String, Object> metadata, String messageId) {
        PubSubMessage message = PubSubMessage.builder()
                .messageId(messageId)
                .topic(topic)
                .payload(payload)
                .metadata(metadata)
                .timestamp(Instant.now())
                .build();

        log.info("发布消息到主题: topic={}, messageId={}", topic, messageId);

        List<Consumer<RoutedMessage>> handlers = subscribers.get(topic);
        if (handlers != null && !handlers.isEmpty()) {
            RoutedMessage routedMessage = RoutedMessage.builder()
                    .messageId(messageId)
                    .source("pubsub:" + topic)
                    .target(topic)
                    .payload(payload)
                    .routingKey(topic)
                    .routeTime(Instant.now())
                    .metadata(metadata)
                    .build();

            for (Consumer<RoutedMessage> handler : handlers) {
                try {
                    handler.accept(routedMessage);
                } catch (Exception e) {
                    log.error("消息处理器执行失败: topic={}, handler={}", topic, e.getMessage());
                }
            }
        } else {
            String queueKey = MESSAGE_QUEUE_PREFIX + topic;
            try {
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
                redisTemplate.opsForList().rightPush(queueKey, json);
                log.debug("消息已存入队列: topic={}, queueKey={}", topic, queueKey);
            } catch (Exception e) {
                log.error("消息入队失败: topic={}", topic, e);
            }
        }
    }

    public List<PubSubMessage> getQueuedMessages(String topic, int limit) {
        String queueKey = MESSAGE_QUEUE_PREFIX + topic;
        List<PubSubMessage> messages = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            String json = redisTemplate.opsForList().leftPop(queueKey);
            if (json == null) break;

            try {
                PubSubMessage message = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(json, PubSubMessage.class);
                messages.add(message);
            } catch (Exception e) {
                log.error("解析队列消息失败: json={}", json, e);
            }
        }

        return messages;
    }

    public String determineRoutingKey(RoutableMessage message) {
        if (message.getRoutingHint() != null) {
            return message.getRoutingHint();
        }

        StringBuilder key = new StringBuilder();

        if (message.getSource() != null) {
            key.append(message.getSource());
        }

        if (message.getMetadata() != null) {
            Object intent = message.getMetadata().get("intent");
            if (intent != null) {
                key.append("/intent/").append(intent);
            }

            Object priority = message.getMetadata().get("priority");
            if (priority != null) {
                key.append("/priority/").append(priority);
            }

            Object userId = message.getMetadata().get("userId");
            if (userId != null) {
                key.append("/user/").append(userId);
            }
        }

        if (message.getPayload() != null) {
            String payloadStr = message.getPayload().toString().toLowerCase();
            if (payloadStr.contains("error") || payloadStr.contains("fail")) {
                key.append("/type/error");
            } else if (payloadStr.contains("notification")) {
                key.append("/type/notification");
            }
        }

        return key.length() > 0 ? key.toString() : "default";
    }

    private List<RouteRule> findMatchingRules(String routingKey) {
        List<RouteRule> matching = new ArrayList<>();

        for (RouteRule rule : routeRules.values()) {
            if (matchesPattern(routingKey, rule.getPattern())) {
                matching.add(rule);
            }
        }

        matching.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return matching;
    }

    private boolean matchesPattern(String routingKey, String pattern) {
        if (pattern == null || pattern.isEmpty() || "*".equals(pattern)) {
            return true;
        }

        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.")
                                   .replace("*", ".*");
            return routingKey.matches(regex);
        }

        return routingKey.equals(pattern) || routingKey.startsWith(pattern + "/");
    }

    private void deliverMessage(RoutedMessage message, RouteRule rule) {
        if (rule.getPipeline() != null) {
            MessagePipeline pipeline = pipelines.get(rule.getPipeline());
            if (pipeline != null) {
                log.debug("应用消息管道: pipeline={}, messageId={}", rule.getPipeline(), message.getMessageId());
                pipeline.process(message);
                return;
            }
        }

        log.info("消息投递: messageId={}, target={}", message.getMessageId(), message.getTarget());
    }

    public void registerPipeline(String pipelineId, MessagePipeline pipeline) {
        pipelines.put(pipelineId, pipeline);
        log.info("消息管道已注册: pipelineId={}", pipelineId);
    }

    public boolean removeRoute(String ruleId) {
        RouteRule removed = routeRules.remove(ruleId);
        if (removed != null) {
            log.info("路由规则已移除: ruleId={}", ruleId);
            return true;
        }
        return false;
    }

    public List<RouteRule> getAllRules() {
        return new ArrayList<>(routeRules.values());
    }

    public RouteRule getRule(String ruleId) {
        return routeRules.get(ruleId);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RouteRule {
        private String ruleId;
        private String name;
        private String pattern;
        private String targetNode;
        private int priority;
        private String pipeline;
        private Map<String, String> metadata;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoutedMessage {
        private String messageId;
        private String source;
        private String target;
        private Object payload;
        private String routingKey;
        private String matchedRule;
        private Instant routeTime;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PubSubMessage {
        private String messageId;
        private String topic;
        private Object payload;
        private Map<String, Object> metadata;
        private Instant timestamp;
    }

    public interface RoutableMessage {
        String getMessageId();
        String getSource();
        Object getPayload();
        Map<String, Object> getMetadata();
        String getRoutingHint();
    }

    public static class SimpleRoutableMessage implements RoutableMessage {
        private final String messageId;
        private final String source;
        private final Object payload;
        private final Map<String, Object> metadata;
        private final String routingHint;

        public SimpleRoutableMessage(String messageId, String source, Object payload,
                                     Map<String, Object> metadata, String routingHint) {
            this.messageId = messageId;
            this.source = source;
            this.payload = payload;
            this.metadata = metadata;
            this.routingHint = routingHint;
        }

        @Override
        public String getMessageId() { return messageId; }
        @Override
        public String getSource() { return source; }
        @Override
        public Object getPayload() { return payload; }
        @Override
        public Map<String, Object> getMetadata() { return metadata; }
        @Override
        public String getRoutingHint() { return routingHint; }
    }

    @lombok.Data
    @lombok.Builder
    public static class MessagePipeline {
        private String pipelineId;
        private String name;
        private List<MessageProcessor> processors;

        public void process(RoutedMessage message) {
            if (processors == null) return;

            for (MessageProcessor processor : processors) {
                try {
                    processor.process(message);
                } catch (Exception e) {
                    log.error("消息处理器执行失败: processor={}, messageId={}",
                            processor.getClass().getSimpleName(), message.getMessageId());
                }
            }
        }
    }

    public interface MessageProcessor {
        void process(RoutedMessage message);
    }

    public static class LoggingProcessor implements MessageProcessor {
        @Override
        public void process(RoutedMessage message) {
            log.info("消息日志: messageId={}, from={}, to={}",
                    message.getMessageId(), message.getSource(), message.getTarget());
        }
    }

    public static class MetadataEnricherProcessor implements MessageProcessor {
        private final Map<String, Object> additionalMetadata;

        public MetadataEnricherProcessor(Map<String, Object> additionalMetadata) {
            this.additionalMetadata = additionalMetadata;
        }

        @Override
        public void process(RoutedMessage message) {
            if (additionalMetadata != null && message.getMetadata() != null) {
                message.getMetadata().putAll(additionalMetadata);
            }
        }
    }
}