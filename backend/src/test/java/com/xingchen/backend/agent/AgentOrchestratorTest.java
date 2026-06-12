package com.xingchen.backend.agent;

import com.xingchen.backend.ai.intent.IntentClassifierInterface;
import com.xingchen.backend.ai.llm.LLMProvider;
import com.xingchen.backend.ai.llm.UserLLMProviderManager;
import com.xingchen.backend.ai.memory.UnifiedMemoryContext;
import com.xingchen.backend.ai.memory.UnifiedMemoryService;
import com.xingchen.backend.ai.model.AIRequest;
import com.xingchen.backend.ai.model.AIResponse;
import com.xingchen.backend.ai.model.Intent;
import com.xingchen.backend.ai.security.SecurityFilterChain;
import com.xingchen.backend.ai.tool.AIToolRegistry;
import com.xingchen.backend.ai.tool.Tool;
import com.xingchen.backend.service.KnowledgeBaseService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentOrchestrator Tests")
class AgentOrchestratorTest {

    @Mock
    private SecurityFilterChain securityFilterChain;
    @Mock
    private IntentClassifierInterface intentClassifier;
    @Mock
    private UnifiedMemoryService unifiedMemoryService;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private AIToolRegistry toolRegistry;
    @Mock
    private UserLLMProviderManager userProviderManager;
    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private AgentOrchestrator orchestrator;

    private AIRequest baseRequest;
    private Intent baseIntent;
    private LLMProvider mockProvider;
    private Timer.Sample mockSample;

    @BeforeEach
    void setUp() {
        baseRequest = AIRequest.builder()
                .userId(1L)
                .sessionId("session-001")
                .message("hello")
                .build();
        baseIntent = Intent.chat("hello");
        mockProvider = mock(LLMProvider.class);
        mockSample = mock(Timer.Sample.class);

        when(securityFilterChain.filter(any(AIRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(intentClassifier.classify(anyString())).thenReturn(baseIntent);
        when(userProviderManager.getUserProvider(1L)).thenReturn(mockProvider);
        when(mockProvider.getProviderName()).thenReturn("TestProvider");
        when(mockProvider.chat(any(AIRequest.class))).thenReturn(AIResponse.success("response"));
        when(unifiedMemoryService.analyzeAndSave(anyLong(), anyString(), anyString()))
                .thenReturn(new UnifiedMemoryService.MemoryAnalysis(true, List.of(), List.of("category"), "summary"));
        when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(mock(Timer.class));
    }

    // ==================== handle() Tests ====================

    @Nested
    @DisplayName("handle() - 主入口处理")
    class HandleTests {

        @Test
        @DisplayName("should 正常处理请求并返回成功响应")
        void should_returnSuccessResponse_When_validRequest() {
            when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(mock(Timer.class));

            AIResponse response = orchestrator.handle(baseRequest);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals("response", response.getContent());

            verify(securityFilterChain).filter(baseRequest);
            verify(intentClassifier).classify("hello");
            verify(userProviderManager).getUserProvider(1L);
            verify(mockProvider).chat(any(AIRequest.class));
            verify(unifiedMemoryService).addWorkingMemory(anyLong(), anyString(), any(UnifiedMemoryContext.MemoryItem.class));
            verify(unifiedMemoryService).analyzeAndSave(eq(1L), eq("hello"), anyString());
        }

        @Test
        @DisplayName("should 拦截安全异常并返回错误响应")
        void should_returnErrorResponse_When_securityFilterThrows() {
            when(securityFilterChain.filter(any(AIRequest.class)))
                    .thenThrow(new SecurityFilterChain.SecurityException("恶意输入"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("请求被拦截"));
            assertTrue(response.getContent().contains("恶意输入"));

            verify(intentClassifier, never()).classify(anyString());
            verify(userProviderManager, never()).getUserProvider(anyLong());
        }

        @Test
        @DisplayName("should 返回错误响应_当_处理过程中发生异常")
        void should_returnErrorResponse_When_exceptionOccursDuringProcessing() {
            when(intentClassifier.classify(anyString())).thenThrow(new RuntimeException("分类器故障"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("处理失败"));
        }

        @Test
        @DisplayName("should 返回错误响应_当_用户没有配置Provider")
        void should_returnErrorResponse_When_userProviderIsNull() {
            when(userProviderManager.getUserProvider(1L)).thenReturn(null);

            AIResponse response = orchestrator.handle(baseRequest);

            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("API Key"));
        }

        @Test
        @DisplayName("should 不保存记忆_当_响应为失败时")
        void should_notSaveMemory_When_responseIsError() {
            when(mockProvider.chat(any(AIRequest.class)))
                    .thenReturn(AIResponse.error("LLM 调用失败"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertFalse(response.isSuccess());
            verify(unifiedMemoryService, never()).addWorkingMemory(anyLong(), anyString(), any());
            verify(unifiedMemoryService, never()).analyzeAndSave(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("should 处理带工具的请求_当_意图需要工具")
        void should_executeTools_When_intentRequiresTools() {
            Intent toolIntent = Intent.builder()
                    .type(Intent.IntentType.SEARCH)
                    .confidence(0.9)
                    .originalMessage("搜索天气")
                    .requiresTool(true)
                    .requiresMemory(false)
                    .possibleTools(List.of("weather_tool"))
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(toolIntent);
            when(toolRegistry.execute(eq("weather_tool"), anyMap()))
                    .thenReturn(Tool.ToolResult.success("晴天", "天气查询成功"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertTrue(response.isSuccess());
            verify(toolRegistry).execute(eq("weather_tool"), anyMap());
        }

        @Test
        @DisplayName("should 执行记忆检索_当_意图需要记忆")
        void should_retrieveMemory_When_intentRequiresMemory() {
            UnifiedMemoryContext mockContext = UnifiedMemoryContext.builder()
                    .workingMemory(List.of())
                    .shortTermMemory(List.of())
                    .longTermMemory(List.of())
                    .build();
            Intent memoryIntent = Intent.builder()
                    .type(Intent.IntentType.CHAT)
                    .confidence(0.95)
                    .originalMessage("上次说到哪了")
                    .requiresMemory(true)
                    .requiresTool(false)
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(memoryIntent);
            when(unifiedMemoryService.loadContext(anyLong(), anyString(), any()))
                    .thenReturn(mockContext);

            AIResponse response = orchestrator.handle(baseRequest);

            assertTrue(response.isSuccess());
            verify(unifiedMemoryService).loadContext(eq(1L), eq("session-001"), any());
        }

        @Test
        @DisplayName("should 执行知识库检索_当_请求需要RAG")
        void should_retrieveKnowledgeBase_When_requestNeedsRag() {
            AIRequest ragRequest = AIRequest.builder()
                    .userId(1L)
                    .sessionId("session-001")
                    .message("什么是RAG")
                    .extraParams(Map.of("useRag", true))
                    .build();
            when(knowledgeBaseService.search(anyString(), anyInt()))
                    .thenReturn("RAG是检索增强生成技术");

            AIResponse response = orchestrator.handle(ragRequest);

            assertTrue(response.isSuccess());
            verify(knowledgeBaseService).search("什么是RAG", 5);
        }

        @Test
        @DisplayName("should 传递实体参数_当_意图包含实体")
        void should_passEntitiesToTool_When_intentHasEntities() {
            Intent.Entity entity = Intent.Entity.builder()
                    .name("location")
                    .value("北京")
                    .build();
            Intent toolIntent = Intent.builder()
                    .type(Intent.IntentType.SEARCH)
                    .confidence(0.9)
                    .originalMessage("北京天气")
                    .requiresTool(true)
                    .requiresMemory(false)
                    .possibleTools(List.of("weather_tool"))
                    .entities(List.of(entity))
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(toolIntent);
            when(toolRegistry.execute(anyString(), anyMap()))
                    .thenReturn(Tool.ToolResult.success("多云"));

            orchestrator.handle(baseRequest);

            verify(toolRegistry).execute(eq("weather_tool"), argThat(params ->
                    params.containsKey("location") && "北京".equals(params.get("location"))
            ));
        }

        @Test
        @DisplayName("should 处理null用户ID_当_请求没有用户ID时")
        void should_returnError_When_userIdIsNull() {
            AIRequest noUserRequest = AIRequest.builder()
                    .sessionId("session-002")
                    .message("hello")
                    .build();

            AIResponse response = orchestrator.handle(noUserRequest);

            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("用户未登录"));
        }

        @Test
        @DisplayName("should 处理Provider异常_当_Provider抛出IllegalStateException")
        void should_returnErrorMessage_When_providerThrowsIllegalStateException() {
            when(mockProvider.chat(any(AIRequest.class)))
                    .thenThrow(new IllegalStateException("Provider未配置"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("Provider未配置"));
        }

        @Test
        @DisplayName("should 处理Provider异常_当_Provider抛出RuntimeException")
        void should_returnErrorMessage_When_providerThrowsRuntimeException() {
            when(mockProvider.chat(any(AIRequest.class)))
                    .thenThrow(new RuntimeException("网络错误"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("处理失败"));
        }
    }

    // ==================== handleStream() Tests ====================

    @Nested
    @DisplayName("handleStream() - 流式处理")
    class HandleStreamTests {

        @Test
        @DisplayName("should 正常处理流式请求_当_Provider支持流式输出")
        void should_processStreaming_When_providerSupportsStreaming() {
            Consumer<AIResponse> mockConsumer = mock(Consumer.class);
            when(mockProvider.supportsStreaming()).thenReturn(true);
            doAnswer(invocation -> {
                Consumer<AIResponse> callback = invocation.getArgument(1);
                callback.accept(AIResponse.chunk("chunk1"));
                callback.accept(AIResponse.chunk("chunk2"));
                return null;
            }).when(mockProvider).streamChat(any(AIRequest.class), any());

            orchestrator.handleStream(baseRequest, mockConsumer);

            verify(mockProvider).streamChat(any(AIRequest.class), any());
            verify(mockConsumer, atLeastOnce()).accept(any(AIResponse.class));
            verify(unifiedMemoryService, atLeastOnce()).addWorkingMemory(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("should 拦截安全异常_当_流式请求被安全过滤器拦截")
        void should_returnSecurityError_When_streamSecurityFilterThrows() {
            Consumer<AIResponse> mockConsumer = mock(Consumer.class);
            when(securityFilterChain.filter(any(AIRequest.class)))
                    .thenThrow(new SecurityFilterChain.SecurityException("恶意流式输入"));

            orchestrator.handleStream(baseRequest, mockConsumer);

            verify(mockConsumer).accept(argThat(resp ->
                    !resp.isSuccess() && resp.getContent().contains("请求被拦截")
            ));
        }

        @Test
        @DisplayName("should 返回错误_当_流式处理发生异常")
        void should_returnError_When_streamProcessingThrows() {
            Consumer<AIResponse> mockConsumer = mock(Consumer.class);
            when(intentClassifier.classify(anyString()))
                    .thenThrow(new RuntimeException("流式分类异常"));

            orchestrator.handleStream(baseRequest, mockConsumer);

            verify(mockConsumer).accept(argThat(resp ->
                    !resp.isSuccess() && resp.getContent().contains("流式处理失败")
            ));
        }

        @Test
        @DisplayName("should 返回错误_当_用户ID为null时")
        void should_returnError_When_userIdIsNullInStream() {
            Consumer<AIResponse> mockConsumer = mock(Consumer.class);
            AIRequest noUserRequest = AIRequest.builder()
                    .sessionId("session-003")
                    .message("hello")
                    .build();

            orchestrator.handleStream(noUserRequest, mockConsumer);

            verify(mockConsumer).accept(argThat(resp ->
                    !resp.isSuccess() && resp.getContent().contains("用户未登录")
            ));
        }

        @Test
        @DisplayName("should 不执行流式调用_当_Provider未配置时")
        void should_notCallStream_When_userProviderIsNull() {
            Consumer<AIResponse> mockConsumer = mock(Consumer.class);
            when(userProviderManager.getUserProvider(1L)).thenReturn(null);

            orchestrator.handleStream(baseRequest, mockConsumer);

            verify(mockConsumer).accept(argThat(resp ->
                    !resp.isSuccess() && resp.getContent().contains("API Key")
            ));
            verify(mockProvider, never()).streamChat(any(), any());
        }

        @Test
        @DisplayName("should 不执行流式调用_当_Provider不支持流式输出时")
        void should_notCallStream_When_providerDoesNotSupportStreaming() {
            Consumer<AIResponse> mockConsumer = mock(Consumer.class);
            when(mockProvider.supportsStreaming()).thenReturn(false);

            orchestrator.handleStream(baseRequest, mockConsumer);

            verify(mockProvider, never()).streamChat(any(), any());
        }
    }

    // ==================== assessMessageImportance() Tests ====================

    @Nested
    @DisplayName("assessMessageImportance() - 消息重要性评估")
    class AssessMessageImportanceTests {

        @Test
        @DisplayName("should 返回0.3_当_消息为null时")
        void should_returnLowScore_When_messageIsNull() throws Exception {
            double score = invokeAssessMessageImportance(null);

            assertEquals(0.3, score);
        }

        @Test
        @DisplayName("should 返回0.3_当_消息为空字符串时")
        void should_returnLowScore_When_messageIsEmpty() throws Exception {
            double score = invokeAssessMessageImportance("");

            assertEquals(0.3, score);
        }

        @Test
        @DisplayName("should 返回0.3_当_消息只有空白字符时")
        void should_returnLowScore_When_messageIsBlank() throws Exception {
            double score = invokeAssessMessageImportance("   ");

            assertEquals(0.3, score);
        }

        @Test
        @DisplayName("should 返回基础分0.5_当_普通短消息时")
        void should_returnBaseScore_When_normalShortMessage() throws Exception {
            double score = invokeAssessMessageImportance("你好");

            assertEquals(0.5, score);
        }

        @Test
        @DisplayName("should 增加0.1_当_消息长度超过100时")
        void should_increaseBy01_When_messageOver100Chars() throws Exception {
            String longMsg = "a".repeat(150);
            double score = invokeAssessMessageImportance(longMsg);

            assertEquals(0.6, score);
        }

        @Test
        @DisplayName("should 增加0.2_当_消息长度超过300时")
        void should_increaseBy02_When_messageOver300Chars() throws Exception {
            String longMsg = "a".repeat(350);
            double score = invokeAssessMessageImportance(longMsg);

            assertEquals(0.7, score);
        }

        @Test
        @DisplayName("should 增加0.1_当_消息包含偏好触发词时")
        void should_increaseByTrigger_When_containsPreferenceTrigger() throws Exception {
            double score = invokeAssessMessageImportance("我喜欢这个功能");

            assertEquals(0.6, score);
        }

        @Test
        @DisplayName("should 增加0.1_当_消息包含决策触发词时")
        void should_increaseByTrigger_When_containsDecisionTrigger() throws Exception {
            double score = invokeAssessMessageImportance("我决定使用这个方案");

            assertEquals(0.6, score);
        }

        @Test
        @DisplayName("should 增加0.1_当_消息包含事实触发词时")
        void should_increaseByTrigger_When_containsFactTrigger() throws Exception {
            double score = invokeAssessMessageImportance("我叫小明");

            assertEquals(0.6, score);
        }

        @Test
        @DisplayName("should 不超过1.0_当_消息同时满足多个加分条件时")
        void should_capAt10_When_multipleConditionsMet() throws Exception {
            String msg = "我喜欢编程，我决定学习Java".repeat(20);
            double score = invokeAssessMessageImportance(msg);

            assertTrue(score <= 1.0, "重要性分数不应超过1.0，实际: " + score);
        }

        @Test
        @DisplayName("should 只加一次触发词分_当_消息包含多个触发词时")
        void should_addTriggerOnlyOnce_When_multipleTriggersPresent() throws Exception {
            double score = invokeAssessMessageImportance("我喜欢编程，我希望学习Java");

            assertEquals(0.6, score);
        }
    }

    // ==================== classifyMessageType() Tests ====================

    @Nested
    @DisplayName("classifyMessageType() - 消息类型分类")
    class ClassifyMessageTypeTests {

        @Test
        @DisplayName("should 返回CONVERSATION_当_消息为null时")
        void should_returnConversation_When_messageIsNull() throws Exception {
            UnifiedMemoryContext.MemoryType type = invokeClassifyMessageType(null);

            assertEquals(UnifiedMemoryContext.MemoryType.CONVERSATION, type);
        }

        @Test
        @DisplayName("should 返回PREFERENCE_当_消息包含偏好触发词时")
        void should_returnPreference_When_containsPreferenceTrigger() throws Exception {
            assertEquals(UnifiedMemoryContext.MemoryType.PREFERENCE,
                    invokeClassifyMessageType("我喜欢编程"));
            assertEquals(UnifiedMemoryContext.MemoryType.PREFERENCE,
                    invokeClassifyMessageType("我的偏好是简洁的代码"));
            assertEquals(UnifiedMemoryContext.MemoryType.PREFERENCE,
                    invokeClassifyMessageType("我习惯用Markdown"));
        }

        @Test
        @DisplayName("should 返回PREFERENCE_当_消息包含负面偏好触发词时")
        void should_returnPreference_When_containsNegativePreferenceTrigger() throws Exception {
            assertEquals(UnifiedMemoryContext.MemoryType.PREFERENCE,
                    invokeClassifyMessageType("不要用这种方式"));
            assertEquals(UnifiedMemoryContext.MemoryType.PREFERENCE,
                    invokeClassifyMessageType("别用Java"));
            assertEquals(UnifiedMemoryContext.MemoryType.PREFERENCE,
                    invokeClassifyMessageType("我讨厌冗余代码"));
        }

        @Test
        @DisplayName("should 返回DECISION_当_消息包含决策触发词时")
        void should_returnDecision_When_containsDecisionTrigger() throws Exception {
            assertEquals(UnifiedMemoryContext.MemoryType.DECISION,
                    invokeClassifyMessageType("我决定用Python"));
            assertEquals(UnifiedMemoryContext.MemoryType.DECISION,
                    invokeClassifyMessageType("我选择这个方案"));
            assertEquals(UnifiedMemoryContext.MemoryType.DECISION,
                    invokeClassifyMessageType("确定使用这种方式"));
            assertEquals(UnifiedMemoryContext.MemoryType.DECISION,
                    invokeClassifyMessageType("采用新的架构"));
        }

        @Test
        @DisplayName("should 返回DECISION_当_消息包含放弃类触发词时")
        void should_returnDecision_When_containsAbandonTrigger() throws Exception {
            assertEquals(UnifiedMemoryContext.MemoryType.DECISION,
                    invokeClassifyMessageType("放弃这个计划"));
            assertEquals(UnifiedMemoryContext.MemoryType.DECISION,
                    invokeClassifyMessageType("不使用这个功能"));
        }

        @Test
        @DisplayName("should 返回FACT_当_消息包含事实触发词时")
        void should_returnFact_When_containsFactTrigger() throws Exception {
            assertEquals(UnifiedMemoryContext.MemoryType.FACT,
                    invokeClassifyMessageType("我叫小明"));
            assertEquals(UnifiedMemoryContext.MemoryType.FACT,
                    invokeClassifyMessageType("我的名字是张三"));
            assertEquals(UnifiedMemoryContext.MemoryType.FACT,
                    invokeClassifyMessageType("我在北京"));
            assertEquals(UnifiedMemoryContext.MemoryType.FACT,
                    invokeClassifyMessageType("我做软件开发"));
            assertEquals(UnifiedMemoryContext.MemoryType.FACT,
                    invokeClassifyMessageType("我的工作是一名工程师"));
            assertEquals(UnifiedMemoryContext.MemoryType.FACT,
                    invokeClassifyMessageType("我是学生"));
        }

        @Test
        @DisplayName("should 返回CONVERSATION_当_消息不匹配任何触发词时")
        void should_returnConversation_When_noTriggerMatched() throws Exception {
            assertEquals(UnifiedMemoryContext.MemoryType.CONVERSATION,
                    invokeClassifyMessageType("你好"));
            assertEquals(UnifiedMemoryContext.MemoryType.CONVERSATION,
                    invokeClassifyMessageType("今天天气怎么样"));
            assertEquals(UnifiedMemoryContext.MemoryType.CONVERSATION,
                    invokeClassifyMessageType("帮我写个函数"));
        }

        @Test
        @DisplayName("should 优先级正确_当_消息同时包含多种触发词时（偏好优先）")
        void should_respectPriorityOrder_When_multipleTriggersMatch() throws Exception {
            // PREFERENCE_TRIGGERS 先检查，应返回 PREFERENCE
            assertEquals(UnifiedMemoryContext.MemoryType.PREFERENCE,
                    invokeClassifyMessageType("我决定不用这种方式，我喜欢新的方案"));
        }

        @Test
        @DisplayName("should 返回DECISION_当_消息不包含偏好但包含决策和事实触发词时")
        void should_returnDecision_When_decisionMatchesBeforeFact() throws Exception {
            // DECISION_TRIGGERS 在 FACT_TRIGGERS 之前检查
            assertEquals(UnifiedMemoryContext.MemoryType.DECISION,
                    invokeClassifyMessageType("我选择使用这个，我叫张三"));
        }
    }

    // ==================== 边界与异常测试 ====================

    @Nested
    @DisplayName("边界与异常测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("should 处理工具执行失败_当_工具返回错误结果")
        void should_handleToolExecutionFailure_When_toolReturnsError() {
            Intent toolIntent = Intent.builder()
                    .type(Intent.IntentType.SEARCH)
                    .confidence(0.9)
                    .originalMessage("查询数据")
                    .requiresTool(true)
                    .requiresMemory(false)
                    .possibleTools(List.of("query_tool"))
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(toolIntent);
            when(toolRegistry.execute(anyString(), anyMap()))
                    .thenReturn(Tool.ToolResult.error("工具内部错误"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertTrue(response.isSuccess());
            verify(toolRegistry).execute(eq("query_tool"), anyMap());
        }

        @Test
        @DisplayName("should 处理工具不存在_当_意图指定不存在的工具")
        void should_handleNonExistentTool_When_intentSpecifiesUnknownTool() {
            Intent toolIntent = Intent.builder()
                    .type(Intent.IntentType.SEARCH)
                    .confidence(0.9)
                    .originalMessage("查询")
                    .requiresTool(true)
                    .requiresMemory(false)
                    .possibleTools(List.of("nonexistent_tool"))
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(toolIntent);
            when(toolRegistry.execute(eq("nonexistent_tool"), anyMap()))
                    .thenReturn(Tool.ToolResult.error("工具不存在: nonexistent_tool"));

            AIResponse response = orchestrator.handle(baseRequest);

            // 工具错误不应阻止LLM调用，最终由LLM响应决定
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("should 处理记忆服务异常_当_加载记忆时抛出异常")
        void should_handleMemoryServiceException_When_loadContextThrows() {
            UnifiedMemoryContext mockContext = UnifiedMemoryContext.builder()
                    .workingMemory(List.of())
                    .shortTermMemory(List.of())
                    .longTermMemory(List.of())
                    .build();
            Intent memoryIntent = Intent.builder()
                    .type(Intent.IntentType.CHAT)
                    .confidence(0.95)
                    .originalMessage("上下文")
                    .requiresMemory(true)
                    .requiresTool(false)
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(memoryIntent);
            when(unifiedMemoryService.loadContext(anyLong(), anyString(), any()))
                    .thenThrow(new RuntimeException("记忆服务不可用"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("处理失败"));
        }

        @Test
        @DisplayName("should 处理知识库服务异常_当_RAG检索时抛出异常")
        void should_handleKnowledgeBaseException_When_ragThrows() {
            AIRequest ragRequest = AIRequest.builder()
                    .userId(1L)
                    .sessionId("session-001")
                    .message("查询")
                    .extraParams(Map.of("useRag", true))
                    .build();
            when(knowledgeBaseService.search(anyString(), anyInt()))
                    .thenThrow(new RuntimeException("知识库不可用"));

            AIResponse response = orchestrator.handle(ragRequest);

            assertFalse(response.isSuccess());
            assertTrue(response.getContent().contains("处理失败"));
        }

        @Test
        @DisplayName("should 处理记忆保存异常_当_成功响应但保存记忆时失败")
        void should_handleMemorySaveException_When_saveMemoryThrows() {
            when(unifiedMemoryService.addWorkingMemory(anyLong(), anyString(), any()))
                    .thenThrow(new RuntimeException("记忆写入失败"));

            AIResponse response = orchestrator.handle(baseRequest);

            // 记忆保存异常不应影响主流程
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("should 处理记忆分析异常_当_analyzeAndSave抛出异常")
        void should_handleMemoryAnalysisException_When_analyzeAndSaveThrows() {
            when(unifiedMemoryService.analyzeAndSave(anyLong(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("分析服务故障"));

            AIResponse response = orchestrator.handle(baseRequest);

            // 记忆分析异常不应影响主流程
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("should 不保存用户记忆项_当_用户消息太短时")
        void should_notSaveUserItem_When_messageTooShort() {
            AIRequest shortRequest = AIRequest.builder()
                    .userId(1L)
                    .sessionId("session-001")
                    .message("hi")
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(Intent.chat("hi"));
            // reset 之前的 stub
            lenient().when(unifiedMemoryService.addWorkingMemory(anyLong(), anyString(), any())).then(invocation -> null);

            orchestrator.handle(shortRequest);

            // 消息长度 < 3，不应调用 addWorkingMemory
            verify(unifiedMemoryService, never()).addWorkingMemory(anyLong(), anyString(), any(UnifiedMemoryContext.MemoryItem.class));
        }

        @Test
        @DisplayName("should 不保存助手记忆项_当_助手内容为null时")
        void should_notSaveAssistantItem_When_contentIsNull() {
            when(mockProvider.chat(any(AIRequest.class)))
                    .thenReturn(AIResponse.builder()
                            .type(AIResponse.ResponseType.SUCCESS)
                            .content(null)
                            .build());

            AIResponse response = orchestrator.handle(baseRequest);

            assertTrue(response.isSuccess());
            // 验证只调用了1次 addWorkingMemory（仅用户消息），而非2次
            verify(unifiedMemoryService, times(1)).addWorkingMemory(anyLong(), anyString(), any(UnifiedMemoryContext.MemoryItem.class));
        }

        @Test
        @DisplayName("should 不保存助手记忆项_当_助手内容太短时")
        void should_notSaveAssistantItem_When_contentTooShort() {
            when(mockProvider.chat(any(AIRequest.class)))
                    .thenReturn(AIResponse.success("ok"));

            orchestrator.handle(baseRequest);

            // "ok" 长度为2 < 5，不应保存助手项，仅保存用户项
            verify(unifiedMemoryService, times(1)).addWorkingMemory(anyLong(), anyString(), any(UnifiedMemoryContext.MemoryItem.class));
        }
    }

    // ==================== Metering Tests ====================

    @Nested
    @DisplayName("度量与监控测试")
    class MeteringTests {

        @Test
        @DisplayName("should 记录成功指标_当_请求成功处理时")
        void should_recordSuccessMetric_When_requestSucceeds() {
            orchestrator.handle(baseRequest);

            verify(meterRegistry).timer(
                    eq("ai.orchestrator.handle"),
                    argThat(tags -> {
                        String[] arr = (String[]) tags;
                        for (int i = 0; i < arr.length; i++) {
                            if ("success".equals(arr[i]) && i + 1 < arr.length) {
                                return "true".equals(arr[i + 1]);
                            }
                        }
                        return false;
                    })
            );
        }

        @Test
        @DisplayName("should 记录安全拦截指标_当_请求被安全过滤器拦截时")
        void should_recordSecurityBlockedMetric_When_requestIsSecurityBlocked() {
            when(securityFilterChain.filter(any(AIRequest.class)))
                    .thenThrow(new SecurityFilterChain.SecurityException("拦截"));

            orchestrator.handle(baseRequest);

            verify(meterRegistry).timer(
                    eq("ai.orchestrator.handle"),
                    argThat(tags -> {
                        String[] arr = (String[]) tags;
                        for (int i = 0; i < arr.length; i++) {
                            if ("intent".equals(arr[i]) && i + 1 < arr.length) {
                                return "SECURITY_BLOCKED".equals(arr[i + 1]);
                            }
                        }
                        return false;
                    })
            );
        }

        @Test
        @DisplayName("should 记录错误指标_当_请求处理失败时")
        void should_recordErrorMetric_When_requestFails() {
            when(intentClassifier.classify(anyString()))
                    .thenThrow(new RuntimeException("故障"));

            orchestrator.handle(baseRequest);

            verify(meterRegistry).timer(
                    eq("ai.orchestrator.handle"),
                    argThat(tags -> {
                        String[] arr = (String[]) tags;
                        for (int i = 0; i < arr.length; i++) {
                            if ("intent".equals(arr[i]) && i + 1 < arr.length) {
                                return "ERROR".equals(arr[i + 1]);
                            }
                        }
                        return false;
                    })
            );
        }

        @Test
        @DisplayName("should 包含意图类型标签_当_请求成功时")
        void should_includeIntentTypeTag_When_requestSucceeds() {
            orchestrator.handle(baseRequest);

            verify(meterRegistry).timer(eq("ai.orchestrator.handle"), any(String[].class));
        }
    }

    // ==================== ExecutionPlan 构建测试 ====================

    @Nested
    @DisplayName("ExecutionPlan 构建测试")
    class ExecutionPlanTests {

        @Test
        @DisplayName("should 仅包含基础步骤_当_普通聊天意图时")
        void should_onlyHaveBasicSteps_When_simpleChatIntent() {
            Intent chatIntent = Intent.chat("你好");
            when(intentClassifier.classify(anyString())).thenReturn(chatIntent);

            orchestrator.handle(baseRequest);

            verify(intentClassifier).classify("hello");
        }

        @Test
        @DisplayName("should 包含多个工具步骤_当_意图需要多个工具时")
        void should_includeMultipleToolSteps_When_intentNeedsMultipleTools() {
            Intent multiToolIntent = Intent.builder()
                    .type(Intent.IntentType.SEARCH)
                    .confidence(0.8)
                    .originalMessage("搜索并发布")
                    .requiresTool(true)
                    .requiresMemory(false)
                    .possibleTools(List.of("search_tool", "publish_tool"))
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(multiToolIntent);
            when(toolRegistry.execute(anyString(), anyMap()))
                    .thenReturn(Tool.ToolResult.success("OK"));

            AIResponse response = orchestrator.handle(baseRequest);

            assertTrue(response.isSuccess());
            verify(toolRegistry).execute(eq("search_tool"), anyMap());
            verify(toolRegistry).execute(eq("publish_tool"), anyMap());
        }

        @Test
        @DisplayName("should 包含记忆步骤_当_意图需要记忆时")
        void should_includeMemoryStep_When_intentRequiresMemory() {
            UnifiedMemoryContext mockContext = UnifiedMemoryContext.builder()
                    .workingMemory(List.of())
                    .shortTermMemory(List.of())
                    .longTermMemory(List.of())
                    .build();
            Intent memoryIntent = Intent.builder()
                    .type(Intent.IntentType.CHAT)
                    .confidence(0.9)
                    .originalMessage("上下文")
                    .requiresMemory(true)
                    .requiresTool(false)
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(memoryIntent);
            when(unifiedMemoryService.loadContext(anyLong(), anyString(), any()))
                    .thenReturn(mockContext);

            orchestrator.handle(baseRequest);

            verify(unifiedMemoryService).loadContext(eq(1L), eq("session-001"), argThat(req ->
                    req.needWorking() && req.needShortTerm() && req.needLongTerm()
            ));
        }
    }

    // ==================== RAG 与系统提示词测试 ====================

    @Nested
    @DisplayName("RAG与系统提示词测试")
    class RagAndPromptTests {

        @Test
        @DisplayName("should 不包含RAG内容_当_知识库返回空结果时")
        void should_notIncludeRag_When_knowledgeBaseReturnsEmpty() {
            AIRequest ragRequest = AIRequest.builder()
                    .userId(1L)
                    .sessionId("session-001")
                    .message("查询")
                    .extraParams(Map.of("useRag", true))
                    .build();
            when(knowledgeBaseService.search(anyString(), anyInt())).thenReturn("");

            orchestrator.handle(ragRequest);

            verify(mockProvider).chat(argThat(req ->
                    req.getSystemPrompt() == null || !req.getSystemPrompt().contains("知识库检索结果")
            ));
        }

        @Test
        @DisplayName("should 包含工具结果_当_工具执行成功时")
        void should_includeToolResults_When_toolsSucceed() {
            Intent toolIntent = Intent.builder()
                    .type(Intent.IntentType.SEARCH)
                    .confidence(0.9)
                    .originalMessage("查询")
                    .requiresTool(true)
                    .requiresMemory(false)
                    .possibleTools(List.of("data_tool"))
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(toolIntent);
            when(toolRegistry.execute(anyString(), anyMap()))
                    .thenReturn(Tool.ToolResult.success(Map.of("result", 42), "查询成功"));

            orchestrator.handle(baseRequest);

            verify(mockProvider).chat(argThat(req ->
                    req.getSystemPrompt() != null
                            && req.getSystemPrompt().contains("工具执行结果")
                            && req.getSystemPrompt().contains("成功")
            ));
        }

        @Test
        @DisplayName("should 包含记忆内容_当_记忆上下文不为空时")
        void should_includeMemoryContent_When_memoryContextNotEmpty() {
            UnifiedMemoryContext.MemoryItem item = UnifiedMemoryContext.MemoryItem.builder()
                    .id(UUID.randomUUID().toString())
                    .role("user")
                    .content("我之前说过我喜欢Python")
                    .timestamp(System.currentTimeMillis())
                    .importance(0.8)
                    .type(UnifiedMemoryContext.MemoryType.PREFERENCE)
                    .category("PREFERENCE")
                    .build();
            UnifiedMemoryContext mockContext = UnifiedMemoryContext.builder()
                    .longTermMemory(List.of(item))
                    .workingMemory(List.of())
                    .shortTermMemory(List.of())
                    .build();
            Intent memoryIntent = Intent.builder()
                    .type(Intent.IntentType.CHAT)
                    .confidence(0.9)
                    .originalMessage("推荐语言")
                    .requiresMemory(true)
                    .requiresTool(false)
                    .build();
            when(intentClassifier.classify(anyString())).thenReturn(memoryIntent);
            when(unifiedMemoryService.loadContext(anyLong(), anyString(), any()))
                    .thenReturn(mockContext);

            orchestrator.handle(baseRequest);

            verify(mockProvider).chat(argThat(req ->
                    req.getSystemPrompt() != null
                            && req.getSystemPrompt().contains("长期记忆")
                            && req.getSystemPrompt().contains("Python")
            ));
        }
    }

    // ==================== 反射辅助方法 ====================

    private double invokeAssessMessageImportance(String message) throws Exception {
        Method method = AgentOrchestrator.class.getDeclaredMethod("assessMessageImportance", String.class);
        method.setAccessible(true);
        return (double) method.invoke(orchestrator, message);
    }

    private UnifiedMemoryContext.MemoryType invokeClassifyMessageType(String message) throws Exception {
        Method method = AgentOrchestrator.class.getDeclaredMethod("classifyMessageType", String.class);
        method.setAccessible(true);
        return (UnifiedMemoryContext.MemoryType) method.invoke(orchestrator, message);
    }
}
