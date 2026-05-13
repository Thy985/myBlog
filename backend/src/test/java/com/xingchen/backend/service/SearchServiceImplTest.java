package com.xingchen.backend.service;

import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.SearchHistory;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.SearchHistoryMapper;
import com.xingchen.backend.service.impl.SearchServiceImpl;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.SearchResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchServiceImpl Tests")
class SearchServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private SearchHistoryMapper searchHistoryMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SearchServiceImpl searchService;

    // ==================== searchArticles Tests ====================

    @Nested
    @DisplayName("searchArticles")
    class SearchArticlesTests {

        @Test
        @DisplayName("should return paginated search results with escaped HTML")
        void searchArticles_Success() {
            // Arrange
            Article article = new Article();
            article.setId(1L);
            article.setTitle("Test <script>alert('xss')</script> Title");
            article.setDescription("Description with <b>HTML</b>");
            when(articleMapper.searchByKeyword(eq("test"), eq(0), eq(10)))
                    .thenReturn(Collections.singletonList(article));
            when(articleMapper.countByKeyword("test")).thenReturn(1L);

            // Act
            PageResult<ArticleListVO> result = searchService.searchArticles("test", 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getList().size());
            assertEquals(1L, result.getTotal());
            // XSS escaped
            assertFalse(result.getList().get(0).getTitle().contains("<script>"));
            assertFalse(result.getList().get(0).getDescription().contains("<b>"));
        }

        @Test
        @DisplayName("should return empty results when no matches found")
        void searchArticles_NoResults() {
            // Arrange
            when(articleMapper.searchByKeyword(eq("nonexistent"), eq(0), eq(10)))
                    .thenReturn(Collections.emptyList());
            when(articleMapper.countByKeyword("nonexistent")).thenReturn(0L);

            // Act
            PageResult<ArticleListVO> result = searchService.searchArticles("nonexistent", 1, 10);

            // Assert
            assertNotNull(result);
            assertTrue(result.getList().isEmpty());
            assertEquals(0L, result.getTotal());
        }

        @Test
        @DisplayName("should handle null title and description gracefully")
        void searchArticles_NullFields() {
            // Arrange
            Article article = new Article();
            article.setId(1L);
            article.setTitle(null);
            article.setDescription(null);
            when(articleMapper.searchByKeyword(eq("test"), eq(0), eq(10)))
                    .thenReturn(Collections.singletonList(article));
            when(articleMapper.countByKeyword("test")).thenReturn(1L);

            // Act
            PageResult<ArticleListVO> result = searchService.searchArticles("test", 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getList().size());
        }

        @Test
        @DisplayName("should calculate correct pagination for page 2")
        void searchArticles_Page2() {
            // Arrange
            when(articleMapper.searchByKeyword(eq("test"), eq(10), eq(10)))
                    .thenReturn(Collections.emptyList());
            when(articleMapper.countByKeyword("test")).thenReturn(15L);

            // Act
            PageResult<ArticleListVO> result = searchService.searchArticles("test", 2, 10);

            // Assert
            assertEquals(2, result.getPage());
            assertEquals(15L, result.getTotal());
            assertEquals(2, result.getPages());
        }
    }

    // ==================== globalSearch Tests ====================

    @Nested
    @DisplayName("globalSearch")
    class GlobalSearchTests {

        @Test
        @DisplayName("should return article results when type is null")
        void globalSearch_NullType_SearchesArticles() {
            // Arrange
            Article article = new Article();
            article.setId(1L);
            article.setTitle("Test Title");
            article.setDescription("Test Description");
            when(articleMapper.searchByKeyword(eq("test"), eq(0), eq(10)))
                    .thenReturn(Collections.singletonList(article));
            when(articleMapper.countByKeyword("test")).thenReturn(1L);

            // Act
            PageResult<SearchResultVO> result = searchService.globalSearch("test", null, 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getList().size());
            assertEquals("article", result.getList().get(0).getType());
            assertEquals(1L, result.getList().get(0).getId());
        }

        @Test
        @DisplayName("should return article results when type is 'article'")
        void globalSearch_ArticleType() {
            // Arrange
            Article article = new Article();
            article.setId(1L);
            article.setTitle("Test Title");
            article.setDescription("Test Description");
            when(articleMapper.searchByKeyword(eq("test"), eq(0), eq(10)))
                    .thenReturn(Collections.singletonList(article));
            when(articleMapper.countByKeyword("test")).thenReturn(1L);

            // Act
            PageResult<SearchResultVO> result = searchService.globalSearch("test", "article", 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getList().size());
            assertEquals("article", result.getList().get(0).getType());
        }

        @Test
        @DisplayName("should return empty results for unsupported type")
        void globalSearch_UnsupportedType() {
            // Arrange
            when(articleMapper.countByKeyword("test")).thenReturn(0L);

            // Act
            PageResult<SearchResultVO> result = searchService.globalSearch("test", "comment", 1, 10);

            // Assert
            assertNotNull(result);
            assertTrue(result.getList().isEmpty());
        }

        @Test
        @DisplayName("should escape HTML in search results")
        void globalSearch_XssEscape() {
            // Arrange
            Article article = new Article();
            article.setId(1L);
            article.setTitle("<script>alert('xss')</script>");
            article.setDescription("<img onerror='hack()'>");
            when(articleMapper.searchByKeyword(eq("test"), eq(0), eq(10)))
                    .thenReturn(Collections.singletonList(article));
            when(articleMapper.countByKeyword("test")).thenReturn(1L);

            // Act
            PageResult<SearchResultVO> result = searchService.globalSearch("test", null, 1, 10);

            // Assert
            assertFalse(result.getList().get(0).getTitle().contains("<script>"));
            assertFalse(result.getList().get(0).getContent().contains("<img"));
        }

        @Test
        @DisplayName("should handle null title and description in article")
        void globalSearch_NullTitleAndDescription() {
            // Arrange
            Article article = new Article();
            article.setId(1L);
            article.setTitle(null);
            article.setDescription(null);
            when(articleMapper.searchByKeyword(eq("test"), eq(0), eq(10)))
                    .thenReturn(Collections.singletonList(article));
            when(articleMapper.countByKeyword("test")).thenReturn(1L);

            // Act
            PageResult<SearchResultVO> result = searchService.globalSearch("test", null, 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getList().size());
            assertEquals("", result.getList().get(0).getTitle());
            assertEquals("", result.getList().get(0).getContent());
        }
    }

    // ==================== getSearchSuggestions Tests ====================

    @Nested
    @DisplayName("getSearchSuggestions")
    class GetSearchSuggestionsTests {

        @Test
        @DisplayName("should return suggestion titles for valid keyword")
        void getSearchSuggestions_Success() {
            // Arrange
            when(articleMapper.selectSuggestionTitles("test", 5))
                    .thenReturn(Arrays.asList("Test Article 1", "Test Article 2"));

            // Act
            List<String> result = searchService.getSearchSuggestions("test", 5);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Test Article 1", result.get(0));
        }

        @Test
        @DisplayName("should return empty list when keyword is null")
        void getSearchSuggestions_NullKeyword() {
            // Act
            List<String> result = searchService.getSearchSuggestions(null, 5);

            // Assert
            assertTrue(result.isEmpty());
            verify(articleMapper, never()).selectSuggestionTitles(any(), anyInt());
        }

        @Test
        @DisplayName("should return empty list when keyword is empty")
        void getSearchSuggestions_EmptyKeyword() {
            // Act
            List<String> result = searchService.getSearchSuggestions("", 5);

            // Assert
            assertTrue(result.isEmpty());
            verify(articleMapper, never()).selectSuggestionTitles(any(), anyInt());
        }
    }

    // ==================== getHotSearchKeywords Tests ====================

    @Nested
    @DisplayName("getHotSearchKeywords")
    class GetHotSearchKeywordsTests {

        @Test
        @DisplayName("should return cached hot keywords when cache hit")
        void getHotSearchKeywords_CacheHit() {
            // Arrange
            List<String> cached = Arrays.asList("Java", "Spring", "Redis");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("search:hot:keywords:10")).thenReturn(cached);

            // Act
            List<String> result = searchService.getHotSearchKeywords(10);

            // Assert
            assertEquals(3, result.size());
            assertEquals("Java", result.get(0));
            verify(searchHistoryMapper, never()).selectHotKeywords(any(), anyInt());
        }

        @Test
        @DisplayName("should query DB and cache results when cache miss")
        void getHotSearchKeywords_CacheMiss() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("search:hot:keywords:10")).thenReturn(null);

            Map<String, Object> row1 = new HashMap<>();
            row1.put("keyword", "Java");
            Map<String, Object> row2 = new HashMap<>();
            row2.put("keyword", "Spring");
            when(searchHistoryMapper.selectHotKeywords(any(LocalDateTime.class), eq(10)))
                    .thenReturn(Arrays.asList(row1, row2));

            // Act
            List<String> result = searchService.getHotSearchKeywords(10);

            // Assert
            assertEquals(2, result.size());
            assertEquals("Java", result.get(0));
            assertEquals("Spring", result.get(1));
            verify(valueOperations).set(eq("search:hot:keywords:10"), eq(result), eq(10L), eq(java.util.concurrent.TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("should return empty list when no hot keywords found")
        void getHotSearchKeywords_NoResults() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("search:hot:keywords:5")).thenReturn(null);
            when(searchHistoryMapper.selectHotKeywords(any(LocalDateTime.class), eq(5)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<String> result = searchService.getHotSearchKeywords(5);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    // ==================== saveSearchHistoryAsync Tests ====================

    @Nested
    @DisplayName("saveSearchHistoryAsync")
    class SaveSearchHistoryAsyncTests {

        @Test
        @DisplayName("should save search history for valid keyword")
        void saveSearchHistoryAsync_Success() {
            // Act
            searchService.saveSearchHistoryAsync(1L, "Java", 10);

            // Assert
            verify(searchHistoryMapper, times(1)).insert(any(SearchHistory.class));
        }

        @Test
        @DisplayName("should not save when keyword is null")
        void saveSearchHistoryAsync_NullKeyword() {
            // Act
            searchService.saveSearchHistoryAsync(1L, null, 10);

            // Assert
            verify(searchHistoryMapper, never()).insert(any(SearchHistory.class));
        }

        @Test
        @DisplayName("should not save when keyword is empty")
        void saveSearchHistoryAsync_EmptyKeyword() {
            // Act
            searchService.saveSearchHistoryAsync(1L, "", 10);

            // Assert
            verify(searchHistoryMapper, never()).insert(any(SearchHistory.class));
        }

        @Test
        @DisplayName("should not throw exception when insert fails")
        void saveSearchHistoryAsync_InsertFails() {
            // Arrange
            doThrow(new RuntimeException("DB error")).when(searchHistoryMapper).insert(any(SearchHistory.class));

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> searchService.saveSearchHistoryAsync(1L, "Java", 10));
        }
    }
}
