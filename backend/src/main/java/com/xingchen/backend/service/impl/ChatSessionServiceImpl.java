package com.xingchen.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    private static final String KEY_PREFIX = "ai:chat:session:";
    private static final Duration SESSION_EXPIRE = Duration.ofDays(7);
    private static final int MAX_HISTORY = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, String>> getHistory(String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.error("获取会话历史失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void addMessage(String sessionId, Map<String, String> message) {
        try {
            List<Map<String, String>> history = getHistory(sessionId);
            history.add(message);
            trimHistory(sessionId, MAX_HISTORY);
            saveHistory(sessionId, history);
        } catch (Exception e) {
            log.error("保存会话消息失败: {}", e.getMessage());
        }
    }

    @Override
    public void saveConversation(String sessionId, String userMessage, String aiMessage) {
        try {
            List<Map<String, String>> history = getHistory(sessionId);
            history.add(createMessage("user", userMessage));
            history.add(createMessage("assistant", aiMessage));
            trimHistory(sessionId, MAX_HISTORY);
            saveHistory(sessionId, history);
        } catch (Exception e) {
            log.error("保存对话失败: {}", e.getMessage());
        }
    }

    @Async
    @Override
    public void asyncSaveConversation(String sessionId, String userMessage, String aiMessage) {
        saveConversation(sessionId, userMessage, aiMessage);
    }

    @Override
    public void trimHistory(String sessionId, int maxSize) {
        try {
            List<Map<String, String>> history = getHistory(sessionId);
            if (history.size() > maxSize) {
                List<Map<String, String>> trimmed = new ArrayList<>();
                int start = history.size() - maxSize;
                for (int i = start; i < history.size(); i++) {
                    trimmed.add(history.get(i));
                }
                saveHistory(sessionId, trimmed);
            }
        } catch (Exception e) {
            log.error("修剪会话历史失败: {}", e.getMessage());
        }
    }

    @Override
    public void clearHistory(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

    private void saveHistory(String sessionId, List<Map<String, String>> history) {
        try {
            String json = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, json, SESSION_EXPIRE);
        } catch (JsonProcessingException e) {
            log.error("保存会话历史失败: {}", e.getMessage());
        }
    }

    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
