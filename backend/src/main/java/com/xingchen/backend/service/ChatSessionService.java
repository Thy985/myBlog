package com.xingchen.backend.service;

import java.util.List;
import java.util.Map;

public interface ChatSessionService {

    List<Map<String, String>> getHistory(String sessionId);

    void addMessage(String sessionId, Map<String, String> message);

    void saveConversation(String sessionId, String userMessage, String aiMessage);

    void asyncSaveConversation(String sessionId, String userMessage, String aiMessage);

    void trimHistory(String sessionId, int maxSize);

    void clearHistory(String sessionId);
}
