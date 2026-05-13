package com.xingchen.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.service.impl.ChatSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSessionServiceImpl Tests")
class ChatSessionServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private ChatSessionServiceImpl chatSessionService;

    private static final String KEY_PREFIX = "ai:chat:session:";
    private static final int MAX_HISTORY = 20;

    @BeforeEach
    void setUp() {
        chatSessionService = new ChatSessionServiceImpl(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("getHistory()")
    class GetHistoryTests {

        @Test
        @DisplayName("should return empty list when no history")
        void getHistory_noHistory_returnsEmptyList() {
            when(valueOperations.get(KEY_PREFIX + "session123")).thenReturn(null);

            List<Map<String, String>> result = chatSessionService.getHistory("session123");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when history is empty string")
        void getHistory_emptyString_returnsEmptyList() {
            when(valueOperations.get(KEY_PREFIX + "session123")).thenReturn("");

            List<Map<String, String>> result = chatSessionService.getHistory("session123");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return parsed history when exists")
        void getHistory_withHistory_returnsParsedList() throws Exception {
            List<Map<String, String>> history = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "Hello");
            history.add(message);

            String json = objectMapper.writeValueAsString(history);
            when(valueOperations.get(KEY_PREFIX + "session123")).thenReturn(json);

            List<Map<String, String>> result = chatSessionService.getHistory("session123");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("user", result.get(0).get("role"));
            assertEquals("Hello", result.get(0).get("content"));
        }

        @Test
        @DisplayName("should return empty list on deserialization error")
        void getHistory_deserializationError_returnsEmptyList() {
            when(valueOperations.get(KEY_PREFIX + "session123")).thenReturn("invalid json");

            List<Map<String, String>> result = chatSessionService.getHistory("session123");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("addMessage()")
    class AddMessageTests {

        @Test
        @DisplayName("should handle exception gracefully")
        void addMessage_exception_handledGracefully() {
            when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");

            assertDoesNotThrow(() -> chatSessionService.addMessage("session123", message));
        }
    }

    @Nested
    @DisplayName("saveConversation()")
    class SaveConversationTests {

        @Test
        @DisplayName("should handle exception gracefully")
        void saveConversation_exception_handledGracefully() {
            when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

            assertDoesNotThrow(() ->
                    chatSessionService.saveConversation("session123", "User message", "AI response"));
        }
    }

    @Nested
    @DisplayName("trimHistory()")
    class TrimHistoryTests {

        @Test
        @DisplayName("should not trim when under max size")
        void trimHistory_underMaxSize_noTrim() {
            when(valueOperations.get(KEY_PREFIX + "session123")).thenReturn(null);

            chatSessionService.trimHistory("session123", MAX_HISTORY);

            verify(valueOperations, never()).set(eq(KEY_PREFIX + "session123"), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("clearHistory()")
    class ClearHistoryTests {

        @Test
        @DisplayName("should delete session from Redis")
        void clearHistory_success() {
            when(redisTemplate.delete(KEY_PREFIX + "session123")).thenReturn(true);

            assertDoesNotThrow(() -> chatSessionService.clearHistory("session123"));

            verify(redisTemplate).delete(KEY_PREFIX + "session123");
        }

        @Test
        @DisplayName("should handle delete failure gracefully")
        void clearHistory_deleteFailure_noException() {
            when(redisTemplate.delete(KEY_PREFIX + "session123")).thenReturn(false);

            assertDoesNotThrow(() -> chatSessionService.clearHistory("session123"));
        }
    }
}
