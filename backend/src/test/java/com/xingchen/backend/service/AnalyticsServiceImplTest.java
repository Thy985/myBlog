package com.xingchen.backend.service;

import com.xingchen.backend.component.PageViewBatchWriter;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.DailyStats;
import com.xingchen.backend.entity.PageView;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.DailyStatsMapper;
import com.xingchen.backend.mapper.PageViewMapper;
import com.xingchen.backend.mapper.UserMapper;
import com.xingchen.backend.service.impl.AnalyticsServiceImpl;
import com.xingchen.backend.util.IpUtils;
import com.xingchen.backend.vo.AnalyticsVO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsServiceImpl Tests")
class AnalyticsServiceImplTest {

    @Mock
    private PageViewMapper pageViewMapper;

    @Mock
    private DailyStatsMapper dailyStatsMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisAnalyticsService redisAnalyticsService;

    @Mock
    private PageViewBatchWriter batchWriter;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Mock
    private HttpServletRequest request;

    @Nested
    @DisplayName("recordPageView()")
    class RecordPageViewTests {

        @Test
        @DisplayName("should skip duplicate visit")
        void recordPageView_duplicate_skips() {
            when(IpUtils.getClientIp(request)).thenReturn("192.168.1.1");
            when(redisAnalyticsService.isDuplicateVisit(anyString(), anyString(), anyInt())).thenReturn(true);

            analyticsService.recordPageView("/article/1", 1L, request);

            verify(batchWriter, never()).addPageView(any());
        }

        @Test
        @DisplayName("should record page view for authenticated user")
        void recordPageView_authenticatedUser_recordsWithUserId() {
            when(IpUtils.getClientIp(request)).thenReturn("192.168.1.1");
            when(redisAnalyticsService.isDuplicateVisit(anyString(), anyString(), anyInt())).thenReturn(false);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
            when(request.getHeader("Referer")).thenReturn("https://google.com");
            when(redisAnalyticsService.getPvCount(anyString())).thenReturn(100L);
            when(redisAnalyticsService.getUvCount(anyString())).thenReturn(50L);

            analyticsService.recordPageView("/article/1", 1L, request);

            verify(batchWriter).addPageView(any(PageView.class));
            verify(redisAnalyticsService).addUv(anyString(), eq("user:1"));
        }

        @Test
        @DisplayName("should record page view for anonymous user")
        void recordPageView_anonymousUser_recordsWithIp() {
            when(IpUtils.getClientIp(request)).thenReturn("192.168.1.1");
            when(redisAnalyticsService.isDuplicateVisit(anyString(), anyString(), anyInt())).thenReturn(false);
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
            when(request.getHeader("Referer")).thenReturn(null);
            when(redisAnalyticsService.getPvCount(anyString())).thenReturn(100L);
            when(redisAnalyticsService.getUvCount(anyString())).thenReturn(50L);

            analyticsService.recordPageView("/article/1", null, request);

            verify(batchWriter).addPageView(any(PageView.class));
            verify(redisAnalyticsService).addUv(anyString(), startsWith("visitor:"));
        }
    }

    @Nested
    @DisplayName("getOverview()")
    class GetOverviewTests {

        @Test
        @DisplayName("should return overview with all stats")
        void getOverview_success() {
            when(redisAnalyticsService.getPvCount(anyString())).thenReturn(1000L);
            when(redisAnalyticsService.getUvCount(anyString())).thenReturn(200L);
            when(redisAnalyticsService.getIpCount(anyString())).thenReturn(150L);
            when(pageViewMapper.countAll()).thenReturn(50000L);
            when(pageViewMapper.countDistinctUsers()).thenReturn(10000L);
            when(pageViewMapper.countDistinctIps()).thenReturn(8000L);
            when(userMapper.countNewUsersToday(any(LocalDate.class))).thenReturn(50L);

            AnalyticsVO result = analyticsService.getOverview();

            assertNotNull(result);
            assertEquals(1000L, result.getTodayPv());
            assertEquals(200L, result.getTodayUv());
            assertEquals(150L, result.getTodayIp());
            assertEquals(50000L, result.getTotalPv());
            assertEquals(10000L, result.getTotalUv());
            assertEquals(50L, result.getNewUsersToday());
        }
    }

    @Nested
    @DisplayName("getTrendData()")
    class GetTrendDataTests {

        @Test
        @DisplayName("should return trend data for date range")
        void getTrendData_success() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 3);

            DailyStats stat1 = new DailyStats();
            stat1.setStatsDate(LocalDate.of(2024, 1, 1));
            stat1.setPvCount(100);
            stat1.setUvCount(50);
            stat1.setNewUserCount(10);

            DailyStats stat2 = new DailyStats();
            stat2.setStatsDate(LocalDate.of(2024, 1, 2));
            stat2.setPvCount(150);
            stat2.setUvCount(75);
            stat2.setNewUserCount(15);

            when(dailyStatsMapper.selectByDateRange(startDate, endDate))
                    .thenReturn(Arrays.asList(stat1, stat2));

            List<Map<String, Object>> result = analyticsService.getTrendData(startDate, endDate);

            assertNotNull(result);
            assertEquals(3, result.size());

            assertEquals("2024-01-01", result.get(0).get("date"));
            assertEquals(100, result.get(0).get("pv"));
            assertEquals(50, result.get(0).get("uv"));

            assertEquals("2024-01-02", result.get(1).get("date"));
            assertEquals(150, result.get(1).get("pv"));
        }

        @Test
        @DisplayName("should fill zeros for missing dates")
        void getTrendData_missingDates_fillsZeros() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 2);

            when(dailyStatsMapper.selectByDateRange(startDate, endDate))
                    .thenReturn(Collections.emptyList());

            List<Map<String, Object>> result = analyticsService.getTrendData(startDate, endDate);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(0, result.get(0).get("pv"));
            assertEquals(0, result.get(1).get("pv"));
        }
    }

    @Nested
    @DisplayName("getDeviceStats()")
    class GetDeviceStatsTests {

        @Test
        @DisplayName("should return device statistics")
        void getDeviceStats_success() {
            Map<String, Long> deviceCounts = new LinkedHashMap<>();
            deviceCounts.put("Desktop", 600L);
            deviceCounts.put("Mobile", 300L);
            deviceCounts.put("Tablet", 100L);

            when(pageViewMapper.countByDeviceType()).thenReturn(deviceCounts);

            List<Map<String, Object>> result = analyticsService.getDeviceStats();

            assertNotNull(result);
            assertEquals(3, result.size());

            assertEquals("Desktop", result.get(0).get("device"));
            assertEquals(600L, result.get(0).get("count"));
            assertEquals(60L, result.get(0).get("percentage"));
        }

        @Test
        @DisplayName("should handle empty device counts")
        void getDeviceStats_empty_returnsEmptyList() {
            when(pageViewMapper.countByDeviceType()).thenReturn(Collections.emptyMap());

            List<Map<String, Object>> result = analyticsService.getDeviceStats();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getHotArticles()")
    class GetHotArticlesTests {

        @Test
        @DisplayName("should return hot articles")
        void getHotArticles_success() {
            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Hot Article 1");
            article1.setReadNum(1000);

            Article article2 = new Article();
            article2.setId(2L);
            article2.setTitle("Hot Article 2");
            article2.setReadNum(500);

            when(articleMapper.selectHotArticles(10)).thenReturn(Arrays.asList(article1, article2));

            List<Map<String, Object>> result = analyticsService.getHotArticles(10);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).get("id"));
            assertEquals("Hot Article 1", result.get(0).get("title"));
            assertEquals(1000, result.get(0).get("readNum"));
        }

        @Test
        @DisplayName("should return empty list when no articles")
        void getHotArticles_empty_returnsEmpty() {
            when(articleMapper.selectHotArticles(10)).thenReturn(Collections.emptyList());

            List<Map<String, Object>> result = analyticsService.getHotArticles(10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getRealtimeStats()")
    class GetRealtimeStatsTests {

        @Test
        @DisplayName("should return realtime statistics")
        void getRealtimeStats_success() {
            when(pageViewMapper.countSince(any(LocalDateTime.class))).thenReturn(50L);
            when(pageViewMapper.countActiveUsersSince(any(LocalDateTime.class))).thenReturn(20L);

            Map<String, Object> result = analyticsService.getRealtimeStats();

            assertNotNull(result);
            assertEquals(50L, result.get("recentPv"));
            assertEquals(20L, result.get("activeUsers"));
        }
    }

    @Nested
    @DisplayName("getArticleStats()")
    class GetArticleStatsTests {

        @Test
        @DisplayName("should return article stats when article exists")
        void getArticleStats_exists_returnsStats() {
            Article article = new Article();
            article.setId(1L);
            article.setTitle("Test Article");
            article.setReadNum(1000);
            article.setLikeNum(100);

            when(articleMapper.selectOneById(1L)).thenReturn(article);
            when(pageViewMapper.countByArticleIdAndDate(anyLong(), any(LocalDate.class))).thenReturn(50L);
            when(pageViewMapper.countByArticleIdAndReferer(anyLong(), any(), any())).thenReturn(new HashMap<>());

            Map<String, Object> result = analyticsService.getArticleStats(1L, 7);

            assertNotNull(result);
            assertEquals(1L, result.get("articleId"));
            assertEquals(true, result.get("exists"));
            assertEquals("Test Article", result.get("title"));
            assertEquals(1000, result.get("totalReadNum"));
            assertEquals(100, result.get("totalLikeNum"));
        }

        @Test
        @DisplayName("should return not exists when article not found")
        void getArticleStats_notFound_returnsNotExists() {
            when(articleMapper.selectOneById(999L)).thenReturn(null);

            Map<String, Object> result = analyticsService.getArticleStats(999L, 7);

            assertNotNull(result);
            assertEquals(999L, result.get("articleId"));
            assertEquals(false, result.get("exists"));
        }
    }
}
