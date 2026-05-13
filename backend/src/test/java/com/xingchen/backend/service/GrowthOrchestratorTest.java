package com.xingchen.backend.service;

import com.xingchen.backend.agent.base.BaseAgent;
import com.xingchen.backend.agent.content.ContentAuditAgent;
import com.xingchen.backend.agent.content.ContentGenerationAgent;
import com.xingchen.backend.agent.content.OpportunityDiscoveryAgent;
import com.xingchen.backend.agent.content.SEOOptimizationAgent;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.service.GrowthOrchestrator;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrowthOrchestrator Tests")
class GrowthOrchestratorTest {

    @Mock
    private ContentAuditAgent contentAuditAgent;

    @Mock
    private OpportunityDiscoveryAgent opportunityDiscoveryAgent;

    @Mock
    private ContentGenerationAgent contentGenerationAgent;

    @Mock
    private SEOOptimizationAgent seoOptimizationAgent;

    @Mock
    private ContentOptimizer contentOptimizer;

    @Mock
    private SEOService seoService;

    @Mock
    private ArticleService articleService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private GrowthOrchestrator growthOrchestrator;

    @Nested
    @DisplayName("GrowthTaskResult Tests")
    class GrowthTaskResultTests {

        @Test
        @DisplayName("should create successful result")
        void successfulResult_containsCorrectData() {
            GrowthOrchestrator.GrowthTaskResult result =
                    new GrowthOrchestrator.GrowthTaskResult(true, "Success message",
                            Map.of("key", "value"));

            assertTrue(result.isSuccess());
            assertEquals("Success message", result.getMessage());
            assertNotNull(result.getData());
            assertEquals("value", result.getData().get("key"));
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("should create failed result")
        void failedResult_containsCorrectData() {
            GrowthOrchestrator.GrowthTaskResult result =
                    new GrowthOrchestrator.GrowthTaskResult(false, "Error message",
                            Map.of("error", "details"));

            assertFalse(result.isSuccess());
            assertEquals("Error message", result.getMessage());
            assertNotNull(result.getData());
        }
    }

    @Nested
    @DisplayName("GrowthReport Tests")
    class GrowthReportTests {

        @Test
        @DisplayName("should initialize with current time and empty lists")
        void growthReport_initializesCorrectly() {
            GrowthOrchestrator.GrowthReport report = new GrowthOrchestrator.GrowthReport();

            assertNotNull(report.getGeneratedAt());
            assertNotNull(report.getTasks());
            assertTrue(report.getTasks().isEmpty());
            assertNotNull(report.getRecommendations());
            assertTrue(report.getRecommendations().isEmpty());
        }

        @Test
        @DisplayName("should allow setting summary")
        void growthReport_allowsSettingSummary() {
            GrowthOrchestrator.GrowthReport report = new GrowthOrchestrator.GrowthReport();
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalArticles", 10);

            report.setSummary(summary);

            assertEquals(summary, report.getSummary());
        }

        @Test
        @DisplayName("should allow setting tasks")
        void growthReport_allowsSettingTasks() {
            GrowthOrchestrator.GrowthReport report = new GrowthOrchestrator.GrowthReport();
            List<Map<String, Object>> tasks = new ArrayList<>();
            tasks.add(Map.of("name", "Task 1"));

            report.setTasks(tasks);

            assertEquals(1, report.getTasks().size());
        }

        @Test
        @DisplayName("should allow setting recommendations")
        void growthReport_allowsSettingRecommendations() {
            GrowthOrchestrator.GrowthReport report = new GrowthOrchestrator.GrowthReport();
            List<Map<String, Object>> recommendations = new ArrayList<>();
            recommendations.add(Map.of("suggestion", "Add more tags"));

            report.setRecommendations(recommendations);

            assertEquals(1, report.getRecommendations().size());
        }
    }

    @Nested
    @DisplayName("executeDailyGrowthTask()")
    class ExecuteDailyGrowthTaskTests {

        @Test
        @DisplayName("should skip task when lock cannot be acquired")
        void executeDailyGrowthTask_lockNotAcquired_skipsTask() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.executeDailyGrowthTask(1L);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("正在执行中"));
        }

        @Test
        @DisplayName("should release lock when exception occurs")
        void executeDailyGrowthTask_exception_releasesLock() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
            when(contentAuditAgent.execute(any())).thenThrow(new RuntimeException("Unexpected error"));

            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.executeDailyGrowthTask(1L);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("执行失败"));
            verify(redisTemplate).delete(anyString());
        }
    }

    @Nested
    @DisplayName("generateContent()")
    class GenerateContentTests {

        @Test
        @DisplayName("should generate content successfully")
        void generateContent_success() {
            Map<String, Object> agentData = new HashMap<>();
            agentData.put("title", "Generated Title");
            agentData.put("content", "Generated Content");
            agentData.put("description", "Generated Summary");

            when(contentGenerationAgent.execute(any())).thenReturn(
                    BaseAgent.AgentResult.success(agentData));

            ArticleVO articleVO = new ArticleVO();
            articleVO.setId(1L);
            articleVO.setTitle("Generated Title");
            when(articleService.createArticle(anyLong(), any())).thenReturn(articleVO);

            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.generateContent(1L, "Topic", "tag1,tag2", "tech");

            assertTrue(result.isSuccess());
            assertEquals(1L, result.getData().get("articleId"));
        }

        @Test
        @DisplayName("should handle content generation failure")
        void generateContent_agentFailure_returnsFailure() {
            when(contentGenerationAgent.execute(any())).thenReturn(
                    BaseAgent.AgentResult.failure("GEN_ERROR", "Generation failed"));

            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.generateContent(1L, "Topic", null, null);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Generation failed"));
        }
    }

    @Nested
    @DisplayName("optimizeArticle()")
    class OptimizeArticleTests {

        @Test
        @DisplayName("should optimize article successfully")
        void optimizeArticle_success() {
            when(seoOptimizationAgent.execute(any())).thenReturn(
                    BaseAgent.AgentResult.success(Map.of("optimized", true)));

            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.optimizeArticle(1L, 100L);

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("should handle optimization failure")
        void optimizeArticle_failure() {
            when(seoOptimizationAgent.execute(any())).thenReturn(
                    BaseAgent.AgentResult.failure("OPT_ERROR", "Optimization failed"));

            GrowthOrchestrator.GrowthTaskResult result =
                    growthOrchestrator.optimizeArticle(1L, 100L);

            assertFalse(result.isSuccess());
        }
    }
}
