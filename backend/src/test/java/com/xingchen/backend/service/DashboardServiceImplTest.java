package com.xingchen.backend.service;

import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Tests")
class DashboardServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Nested
    @DisplayName("getOverviewStats()")
    class GetOverviewStatsTests {

        @Test
        @DisplayName("should return all overview statistics")
        void getOverviewStats_success() {
            when(articleMapper.countAll()).thenReturn(100L);
            when(userMapper.countAll()).thenReturn(50L);
            when(categoryMapper.countAll()).thenReturn(10L);
            when(tagMapper.countAll()).thenReturn(30L);
            when(commentMapper.countAll()).thenReturn(500L);
            when(articleMapper.countByDate(any(), any())).thenReturn(5);
            when(userMapper.countByDate(any(), any())).thenReturn(3);
            when(commentMapper.countByDate(any(), any())).thenReturn(20);

            Map<String, Object> result = dashboardService.getOverviewStats();

            assertNotNull(result);
            assertEquals(100L, result.get("totalArticles"));
            assertEquals(50L, result.get("totalUsers"));
            assertEquals(10L, result.get("totalCategories"));
            assertEquals(30L, result.get("totalTags"));
            assertEquals(500L, result.get("totalComments"));
            assertEquals(5, result.get("todayArticles"));
            assertEquals(3, result.get("todayUsers"));
            assertEquals(20, result.get("todayComments"));
        }
    }

    @Nested
    @DisplayName("getArticleTrend()")
    class GetArticleTrendTests {

        @Test
        @DisplayName("should return article trend for specified days")
        void getArticleTrend_success() {
            when(articleMapper.countByDate(any(), any())).thenReturn(10);

            Map<String, Object> result = dashboardService.getArticleTrend(7);

            assertNotNull(result);
            assertNotNull(result.get("dates"));
            assertNotNull(result.get("counts"));
            assertEquals(7, ((List<?>) result.get("dates")).size());
            assertEquals(7, ((List<?>) result.get("counts")).size());
        }

        @Test
        @DisplayName("should return empty lists for zero days")
        void getArticleTrend_zeroDays_returnsEmpty() {
            Map<String, Object> result = dashboardService.getArticleTrend(0);

            assertNotNull(result);
            assertEquals(0, ((List<?>) result.get("dates")).size());
            assertEquals(0, ((List<?>) result.get("counts")).size());
        }
    }

    @Nested
    @DisplayName("getUserTrend()")
    class GetUserTrendTests {

        @Test
        @DisplayName("should return user trend for specified days")
        void getUserTrend_success() {
            when(userMapper.countByDate(any(), any())).thenReturn(5);

            Map<String, Object> result = dashboardService.getUserTrend(7);

            assertNotNull(result);
            assertNotNull(result.get("dates"));
            assertNotNull(result.get("counts"));
            assertEquals(7, ((List<?>) result.get("dates")).size());
            assertEquals(7, ((List<?>) result.get("counts")).size());
        }
    }

    @Nested
    @DisplayName("getVisitTrend()")
    class GetVisitTrendTests {

        @Test
        @DisplayName("should return visit trend with random values")
        void getVisitTrend_success() {
            Map<String, Object> result = dashboardService.getVisitTrend(7);

            assertNotNull(result);
            assertNotNull(result.get("dates"));
            assertNotNull(result.get("counts"));
            assertEquals(7, ((List<?>) result.get("dates")).size());
            assertEquals(7, ((List<?>) result.get("counts")).size());
        }
    }

    @Nested
    @DisplayName("getCategoryDistribution()")
    class GetCategoryDistributionTests {

        @Test
        @DisplayName("should return category distribution")
        void getCategoryDistribution_success() {
            List<Map<String, Object>> distribution = new ArrayList<>();
            distribution.add(Map.of("name", "Java", "count", 30));
            distribution.add(Map.of("name", "Python", "count", 20));

            when(categoryMapper.selectArticleCountGroupByCategory()).thenReturn(distribution);

            List<Map<String, Object>> result = dashboardService.getCategoryDistribution();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Java", result.get(0).get("name"));
        }
    }

    @Nested
    @DisplayName("getTagDistribution()")
    class GetTagDistributionTests {

        @Test
        @DisplayName("should return tag distribution")
        void getTagDistribution_success() {
            List<Map<String, Object>> distribution = new ArrayList<>();
            distribution.add(Map.of("name", "Spring", "count", 25));

            when(tagMapper.selectArticleCountGroupByTag()).thenReturn(distribution);

            List<Map<String, Object>> result = dashboardService.getTagDistribution();

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("getHotArticles()")
    class GetHotArticlesTests {

        @Test
        @DisplayName("should return hot articles")
        void getHotArticles_success() {
            List<Map<String, Object>> articles = new ArrayList<>();
            articles.add(Map.of("id", 1L, "title", "Hot Article"));

            when(articleMapper.selectHotArticlesWithAuthor(10)).thenReturn(articles);

            List<Map<String, Object>> result = dashboardService.getHotArticles(10);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("getActiveUsers()")
    class GetActiveUsersTests {

        @Test
        @DisplayName("should return active users")
        void getActiveUsers_success() {
            List<Map<String, Object>> users = new ArrayList<>();
            users.add(Map.of("id", 1L, "username", "activeuser"));

            when(userMapper.selectActiveUsersWithArticleCount(10)).thenReturn(users);

            List<Map<String, Object>> result = dashboardService.getActiveUsers(10);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("getPendingReviewStats()")
    class GetPendingReviewStatsTests {

        @Test
        @DisplayName("should return pending review stats")
        void getPendingReviewStats_success() {
            when(articleMapper.countByStatus(0)).thenReturn(5);
            when(commentMapper.countByStatus(0)).thenReturn(10);

            Map<String, Integer> result = dashboardService.getPendingReviewStats();

            assertNotNull(result);
            assertEquals(5, result.get("pendingArticles"));
            assertEquals(10, result.get("pendingComments"));
        }
    }
}
