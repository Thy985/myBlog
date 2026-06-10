package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.dto.ArticleUpdateDTO;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.service.SearchService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.ArticleVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleController Tests")
class ArticleControllerTest {

    private ArticleService articleService;
    private SearchService searchService;
    private ArticleController articleController;

    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        articleService = mock(ArticleService.class);
        searchService = mock(SearchService.class);
        articleController = new ArticleController(articleService, searchService);

        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        if (stpUtilMock != null) {
            stpUtilMock.close();
        }
    }

    // ==================== Helper Methods ====================

    private ArticleVO buildArticleVO(Long id, Long userId, String title) {
        ArticleVO vo = new ArticleVO();
        vo.setId(id);
        vo.setUserId(userId);
        vo.setTitle(title);
        vo.setDescription("Test summary");
        vo.setContent("Test content");
        vo.setStatus("published");
        vo.setIsTop(0);
        vo.setReadNum(100);
        vo.setLikeNum(10);
        vo.setCollectNum(5);
        vo.setCommentNum(3);
        vo.setCreatedTime(LocalDateTime.now());
        return vo;
    }

    private ArticleListVO buildArticleListVO(Long id, String title) {
        ArticleListVO vo = new ArticleListVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setDescription("Test summary");
        vo.setReadNum(100);
        vo.setLikeNum(10);
        vo.setCollectNum(5);
        vo.setCreatedTime(LocalDateTime.now());
        return vo;
    }

    private ArticleCreateDTO buildCreateDTO() {
        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle("Test Title");
        dto.setContent("Test Content");
        dto.setSummary("Test Summary");
        dto.setCategoryId(1L);
        return dto;
    }

    // ==================== POST /api/article - createArticle Tests ====================

    @Nested
    @DisplayName("POST /api/article - createArticle")
    class CreateArticleTests {

        @Test
        @DisplayName("应该创建文章成功并返回文章详情")
        void createArticle_Success() {
            // Arrange
            Long userId = 1L;
            ArticleCreateDTO dto = buildCreateDTO();
            ArticleVO expectedVO = buildArticleVO(100L, userId, "Test Title");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(articleService.createArticle(eq(userId), eq(dto))).thenReturn(expectedVO);

            // Act
            Result<ArticleVO> result = articleController.createArticle(dto);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertEquals(100L, result.getData().getId());
            assertEquals("Test Title", result.getData().getTitle());

            verify(articleService).createArticle(eq(userId), eq(dto));
        }

        @Test
        @DisplayName("应该创建文章成功并包含标签")
        void createArticle_WithTags_Success() {
            // Arrange
            Long userId = 1L;
            ArticleCreateDTO dto = buildCreateDTO();
            dto.setTagIds(Arrays.asList(1L, 2L));
            ArticleVO expectedVO = buildArticleVO(101L, userId, "Test Title");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(articleService.createArticle(eq(userId), eq(dto))).thenReturn(expectedVO);

            // Act
            Result<ArticleVO> result = articleController.createArticle(dto);

            // Assert
            assertNotNull(result);
            assertEquals(101L, result.getData().getId());
            verify(articleService).createArticle(eq(userId), eq(dto));
        }
    }

    // ==================== PUT /api/article/{id} - updateArticle Tests ====================

    @Nested
    @DisplayName("PUT /api/article/{id} - updateArticle")
    class UpdateArticleTests {

        @Test
        @DisplayName("应该更新文章成功并返回更新后的详情")
        void updateArticle_Success() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;
            ArticleUpdateDTO dto = new ArticleUpdateDTO();
            dto.setTitle("Updated Title");
            dto.setContent("Updated Content");
            dto.setSummary("Updated Summary");
            ArticleVO expectedVO = buildArticleVO(articleId, userId, "Updated Title");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(articleService.updateArticle(eq(userId), eq(articleId), eq(dto))).thenReturn(expectedVO);

            // Act
            Result<ArticleVO> result = articleController.updateArticle(articleId, dto);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("Updated Title", result.getData().getTitle());
            verify(articleService).updateArticle(eq(userId), eq(articleId), eq(dto));
        }

        @Test
        @DisplayName("文章不存在时应该抛出BusinessException")
        void updateArticle_ArticleNotFound_ThrowsException() {
            // Arrange
            Long userId = 1L;
            Long articleId = 999L;
            ArticleUpdateDTO dto = new ArticleUpdateDTO();
            dto.setTitle("Updated Title");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(articleService.updateArticle(eq(userId), eq(articleId), eq(dto)))
                    .thenThrow(new BusinessException(40004, "文章不存在"));

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleController.updateArticle(articleId, dto));
            assertEquals(40004, exception.getCode());
            assertTrue(exception.getMessage().contains("文章不存在"));
        }

        @Test
        @DisplayName("非作者更新文章时应该抛出无权限异常")
        void updateArticle_NoPermission_ThrowsException() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;
            ArticleUpdateDTO dto = new ArticleUpdateDTO();
            dto.setTitle("Updated Title");

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(articleService.updateArticle(eq(userId), eq(articleId), eq(dto)))
                    .thenThrow(new BusinessException(40005, "无更新权限"));

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleController.updateArticle(articleId, dto));
            assertEquals(40005, exception.getCode());
        }
    }

    // ==================== DELETE /api/article/{id} - deleteArticle Tests ====================

    @Nested
    @DisplayName("DELETE /api/article/{id} - deleteArticle")
    class DeleteArticleTests {

        @Test
        @DisplayName("应该删除文章成功")
        void deleteArticle_Success() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doNothing().when(articleService).deleteArticle(userId, articleId);

            // Act
            Result<Void> result = articleController.deleteArticle(articleId);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            verify(articleService).deleteArticle(userId, articleId);
        }

        @Test
        @DisplayName("文章不存在时应该抛出异常")
        void deleteArticle_ArticleNotFound_ThrowsException() {
            // Arrange
            Long userId = 1L;
            Long articleId = 999L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doThrow(new BusinessException(404, "资源不存在"))
                    .when(articleService).deleteArticle(userId, articleId);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleController.deleteArticle(articleId));
            assertEquals(404, exception.getCode());
        }

        @Test
        @DisplayName("非作者删除文章时应该抛出无权限异常")
        void deleteArticle_NoPermission_ThrowsException() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doThrow(new BusinessException(40006, "无删除权限"))
                    .when(articleService).deleteArticle(userId, articleId);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleController.deleteArticle(articleId));
            assertEquals(40006, exception.getCode());
        }
    }

    // ==================== GET /api/article/{id} - getArticle Tests ====================

    @Nested
    @DisplayName("GET /api/article/{id} - getArticle")
    class GetArticleTests {

        @Test
        @DisplayName("已登录用户应该获取文章详情成功")
        void getArticle_LoggedIn_Success() {
            // Arrange
            Long articleId = 100L;
            Long userId = 1L;
            ArticleVO expectedVO = buildArticleVO(articleId, userId, "Test Article");

            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            when(articleService.getArticleById(articleId, userId)).thenReturn(expectedVO);

            // Act
            Result<ArticleVO> result = articleController.getArticle(articleId);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(articleId, result.getData().getId());
            verify(articleService).getArticleById(articleId, userId);
        }

        @Test
        @DisplayName("未登录用户应该获取文章详情成功(userId为null)")
        void getArticle_NotLoggedIn_Success() {
            // Arrange
            Long articleId = 100L;
            ArticleVO expectedVO = buildArticleVO(articleId, 2L, "Test Article");

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            when(articleService.getArticleById(articleId, null)).thenReturn(expectedVO);

            // Act
            Result<ArticleVO> result = articleController.getArticle(articleId);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(articleId, result.getData().getId());
            verify(articleService).getArticleById(articleId, null);
        }

        @Test
        @DisplayName("文章不存在时应该返回data为null")
        void getArticle_NotFound_ReturnsNullData() {
            // Arrange
            Long articleId = 999L;

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            when(articleService.getArticleById(articleId, null)).thenReturn(null);

            // Act
            Result<ArticleVO> result = articleController.getArticle(articleId);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertNull(result.getData());
        }
    }

    // ==================== GET /api/article/list - getArticleList Tests ====================

    @Nested
    @DisplayName("GET /api/article/list - getArticleList")
    class GetArticleListTests {

        @Test
        @DisplayName("应该获取分页文章列表成功")
        void getArticleList_Success() {
            // Arrange
            List<ArticleListVO> articles = Arrays.asList(
                    buildArticleListVO(1L, "Article 1"),
                    buildArticleListVO(2L, "Article 2")
            );
            PageResult<ArticleListVO> expectedPage = PageResult.of(articles, 100L, 1, 10);

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            when(articleService.getArticleList(1, 10, null, null, null, null, null))
                    .thenReturn(expectedPage);

            // Act
            Result<PageResult<ArticleListVO>> result = articleController.getArticleList(
                    1, 10, null, null, null, null);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(2, result.getData().getList().size());
            assertEquals(100L, result.getData().getTotal());
            verify(articleService).getArticleList(1, 10, null, null, null, null, null);
        }

        @Test
        @DisplayName("应该使用筛选条件获取分页列表成功")
        void getArticleList_WithFilters_Success() {
            // Arrange
            List<ArticleListVO> articles = Collections.singletonList(
                    buildArticleListVO(1L, "Java Article")
            );
            PageResult<ArticleListVO> expectedPage = PageResult.of(articles, 1L, 1, 10);

            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(articleService.getArticleList(1, 10, "Java", 1L, 2L, 3L, 1L))
                    .thenReturn(expectedPage);

            // Act
            Result<PageResult<ArticleListVO>> result = articleController.getArticleList(
                    1, 10, "Java", 1L, 2L, 3L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getData().getList().size());
            assertEquals("Java Article", result.getData().getList().get(0).getTitle());
            verify(articleService).getArticleList(1, 10, "Java", 1L, 2L, 3L, 1L);
        }

        @Test
        @DisplayName("应该返回空列表当没有文章时")
        void getArticleList_EmptyResult() {
            // Arrange
            PageResult<ArticleListVO> emptyPage = PageResult.of(Collections.emptyList(), 0L, 1, 10);

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            when(articleService.getArticleList(1, 10, "nonexistent", null, null, null, null))
                    .thenReturn(emptyPage);

            // Act
            Result<PageResult<ArticleListVO>> result = articleController.getArticleList(
                    1, 10, "nonexistent", null, null, null);

            // Assert
            assertNotNull(result);
            assertTrue(result.getData().getList().isEmpty());
            assertEquals(0L, result.getData().getTotal());
        }
    }

    // ==================== GET /api/article/search - searchArticles Tests ====================

    @Nested
    @DisplayName("GET /api/article/search - searchArticles")
    class SearchArticlesTests {

        @Test
        @DisplayName("应该搜索文章成功并保存搜索历史")
        void searchArticles_Success() {
            // Arrange
            String keyword = "Spring Boot";
            List<ArticleListVO> articles = Arrays.asList(
                    buildArticleListVO(1L, "Spring Boot Tutorial"),
                    buildArticleListVO(2L, "Advanced Spring Boot")
            );
            PageResult<ArticleListVO> searchResult = PageResult.of(articles, 2L, 1, 10);

            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(articleService.getUserArticles(1L, 1, 10, keyword, 1L)).thenReturn(searchResult);
            doNothing().when(searchService).saveSearchHistoryAsync(eq(1L), eq(keyword), eq(2));

            // Act
            Result<PageResult<ArticleListVO>> result = articleController.searchArticles(
                    keyword, 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(2, result.getData().getList().size());
            verify(articleService).getUserArticles(1L, 1, 10, keyword, 1L);
            verify(searchService).saveSearchHistoryAsync(1L, keyword, 2);
        }

        @Test
        @DisplayName("未登录用户搜索时userId应该为null")
        void searchArticles_NotLoggedIn() {
            // Arrange
            String keyword = "Java";
            PageResult<ArticleListVO> emptyResult = PageResult.of(Collections.emptyList(), 0L, 1, 10);

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            when(articleService.getUserArticles(null, 1, 10, keyword, null)).thenReturn(emptyResult);
            doNothing().when(searchService).saveSearchHistoryAsync(eq(null), eq(keyword), eq(0));

            // Act
            Result<PageResult<ArticleListVO>> result = articleController.searchArticles(
                    keyword, 1, 10);

            // Assert
            assertNotNull(result);
            assertTrue(result.getData().getList().isEmpty());
            verify(articleService).getUserArticles(null, 1, 10, keyword, null);
            verify(searchService).saveSearchHistoryAsync(null, keyword, 0);
        }

        @Test
        @DisplayName("应该使用默认分页参数搜索成功")
        void searchArticles_DefaultPagination() {
            // Arrange
            String keyword = "test";
            PageResult<ArticleListVO> searchResult = PageResult.of(
                    Collections.singletonList(buildArticleListVO(1L, "test article")), 1L, 1, 10);

            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);
            when(articleService.getUserArticles(null, 1, 10, keyword, null)).thenReturn(searchResult);
            doNothing().when(searchService).saveSearchHistoryAsync(eq(null), eq(keyword), eq(1));

            // Act
            Result<PageResult<ArticleListVO>> result = articleController.searchArticles(
                    keyword, 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getData().getList().size());
        }
    }

    // ==================== POST /api/article/{id}/like - likeArticle Tests ====================

    @Nested
    @DisplayName("POST /api/article/{id}/like - likeArticle")
    class LikeArticleTests {

        @Test
        @DisplayName("应该点赞文章成功")
        void likeArticle_Success() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doNothing().when(articleService).likeArticle(userId, articleId);

            // Act
            Result<Void> result = articleController.likeArticle(articleId);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            verify(articleService).likeArticle(userId, articleId);
        }

        @Test
        @DisplayName("重复点赞时应该抛出异常")
        void likeArticle_AlreadyLiked_ThrowsException() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doThrow(new BusinessException(400, "已点赞"))
                    .when(articleService).likeArticle(userId, articleId);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleController.likeArticle(articleId));
            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("已点赞"));
        }
    }

    // ==================== POST /api/article/{id}/collect - collectArticle Tests ====================

    @Nested
    @DisplayName("POST /api/article/{id}/collect - collectArticle")
    class CollectArticleTests {

        @Test
        @DisplayName("应该收藏文章成功")
        void collectArticle_Success() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doNothing().when(articleService).collectArticle(userId, articleId);

            // Act
            Result<Void> result = articleController.collectArticle(articleId);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            verify(articleService).collectArticle(userId, articleId);
        }

        @Test
        @DisplayName("重复收藏时应该抛出异常")
        void collectArticle_AlreadyCollected_ThrowsException() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(userId);
            doThrow(new BusinessException(400, "已收藏"))
                    .when(articleService).collectArticle(userId, articleId);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleController.collectArticle(articleId));
            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("已收藏"));
        }
    }

    // ==================== GET /api/article/hot - getHotArticles Tests ====================

    @Nested
    @DisplayName("GET /api/article/hot - getHotArticles")
    class GetHotArticlesTests {

        @Test
        @DisplayName("应该获取热门文章列表成功")
        void getHotArticles_Success() {
            // Arrange
            List<ArticleVO> hotArticles = Arrays.asList(
                    buildArticleVO(1L, 1L, "Hot Article 1"),
                    buildArticleVO(2L, 2L, "Hot Article 2"),
                    buildArticleVO(3L, 3L, "Hot Article 3")
            );

            when(articleService.getHotArticles(10)).thenReturn(hotArticles);

            // Act
            Result<List<ArticleVO>> result = articleController.getHotArticles(10);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(3, result.getData().size());
            assertEquals("Hot Article 1", result.getData().get(0).getTitle());
            verify(articleService).getHotArticles(10);
        }

        @Test
        @DisplayName("应该使用默认limit=10获取热门文章")
        void getHotArticles_DefaultLimit() {
            // Arrange
            when(articleService.getHotArticles(10)).thenReturn(Collections.emptyList());

            // Act
            Result<List<ArticleVO>> result = articleController.getHotArticles(10);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertTrue(result.getData().isEmpty());
            verify(articleService).getHotArticles(10);
        }

        @Test
        @DisplayName("自定义limit时应该获取指定数量的热门文章")
        void getHotArticles_CustomLimit() {
            // Arrange
            List<ArticleVO> hotArticles = Collections.singletonList(
                    buildArticleVO(1L, 1L, "Top Article")
            );

            when(articleService.getHotArticles(5)).thenReturn(hotArticles);

            // Act
            Result<List<ArticleVO>> result = articleController.getHotArticles(5);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getData().size());
            verify(articleService).getHotArticles(5);
        }
    }
}
