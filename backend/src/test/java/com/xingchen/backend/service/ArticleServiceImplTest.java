package com.xingchen.backend.service;

import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.dto.ArticleUpdateDTO;
import com.xingchen.backend.entity.*;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.impl.ArticleServiceImpl;
import com.xingchen.backend.util.RedisLockUtil;
import com.xingchen.backend.vo.ArticleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleServiceImpl Tests")
class ArticleServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisLockUtil redisLockUtil;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleContentMapper articleContentMapper;

    @Mock
    private ArticleCategoryMapper articleCategoryMapper;

    @Mock
    private ArticleTagMapper articleTagMapper;

    @Mock
    private ArticleLikeMapper articleLikeMapper;

    @Mock
    private ArticleCollectMapper articleCollectMapper;

    @Mock
    private ArticleReadStatsMapper articleReadStatsMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private SearchHistoryMapper searchHistoryMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private ArticleServiceImpl articleService;

    // ==================== Helper Methods ====================

    private ArticleCreateDTO buildCreateDTO() {
        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle("Test Title");
        dto.setContent("Test Content");
        dto.setSummary("Test Summary");
        dto.setCategoryId(1L);
        return dto;
    }

    private Article buildArticle(Long id, Long userId, String title) {
        Article article = new Article();
        article.setId(id);
        article.setUserId(userId);
        article.setTitle(title);
        article.setTitleImage("image.jpg");
        article.setDescription("Test Summary");
        article.setStatus(1);
        article.setCommentStatus(1);
        article.setTopStatus(0);
        article.setViewStatus(1);
        article.setReadNum(10);
        article.setCommentNum(0);
        article.setLikeNum(0);
        article.setCollectNum(0);
        article.setShareNum(0);
        article.setIsDeleted(0);
        article.setCreatedTime(LocalDateTime.now());
        article.setPublishTime(LocalDateTime.now());
        return article;
    }

    /**
     * Mock redisLockUtil.executeWithLock to directly invoke the supplier (bypassing actual Redis lock).
     */
    @SuppressWarnings("unchecked")
    private void mockExecuteWithLock() {
        when(redisLockUtil.executeWithLock(
                anyString(), anyLong(), anyInt(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(4);
                    return supplier.get();
                });
    }

    /**
     * Mock redisTemplate.opsForHash() chain so that cache lookups return null (cache miss).
     * This allows executeWithLock's supplier to be called.
     */
    private void mockCacheMiss() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(anyString(), anyString())).thenReturn(null);
        when(hashOperations.hasKey(anyString(), anyString())).thenReturn(false);
    }

    /**
     * Set up the full mock chain for getArticleById's loadArticleFromDB flow.
     * Used by createArticle and updateArticle tests that call getArticleById at the end.
     */
    private void mockGetArticleByIdChain(Long articleId, Long userId, String title) {
        // Article from DB (first load - for readNum update)
        Article article = buildArticle(articleId, userId, title);
        when(articleMapper.selectOneById(articleId)).thenReturn(article);

        // Read stats - create new stats record
        when(articleReadStatsMapper.selectByArticleAndDate(eq(articleId), any()))
                .thenReturn(null);

        // Article from DB (second load - after readNum increment)
        Article updatedArticle = buildArticle(articleId, userId, title);
        updatedArticle.setReadNum(11);
        when(articleMapper.selectOneById(articleId)).thenReturn(updatedArticle);

        // Author user
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setNickname("Test User");
        user.setAvatar("avatar.jpg");
        when(userMapper.selectOneById(userId)).thenReturn(user);

        // Article content
        ArticleContent content = new ArticleContent();
        content.setArticleId(articleId);
        content.setContent("Test Content");
        content.setWordCount(12);
        when(articleContentMapper.selectByArticleId(articleId)).thenReturn(content);

        // Article category
        ArticleCategory articleCategory = new ArticleCategory();
        articleCategory.setArticleId(articleId);
        articleCategory.setCategoryId(1L);
        when(articleCategoryMapper.selectByArticleId(articleId)).thenReturn(articleCategory);

        // Category
        Category category = new Category();
        category.setId(1L);
        category.setCategoryName("Test Category");
        category.setDescription("A test category");
        when(categoryMapper.selectOneById(1L)).thenReturn(category);

        // Article tags
        ArticleTag articleTag = new ArticleTag();
        articleTag.setArticleId(articleId);
        articleTag.setTagId(1L);
        when(articleTagMapper.selectByArticleId(articleId))
                .thenReturn(Collections.singletonList(articleTag));

        // Tag
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setTagName("Test Tag");
        tag.setColor("#FF0000");
        when(tagMapper.selectOneById(1L)).thenReturn(tag);

        // Pre/next articles (none found)
        when(articleMapper.selectPreArticle(articleId)).thenReturn(null);
        when(articleMapper.selectNextArticle(articleId)).thenReturn(null);

        // Cache put (no-op verification)
        doNothing().when(hashOperations).put(anyString(), anyString(), any());
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    // ==================== createArticle Tests ====================

    @Nested
    @DisplayName("createArticle")
    class CreateArticleTests {

        @Test
        @DisplayName("should create article successfully with basic fields")
        void createArticle_Success() {
            // Arrange
            Long userId = 1L;
            ArticleCreateDTO dto = buildCreateDTO();

            // Article insert returns ID
            Article insertedArticle = new Article();
            insertedArticle.setId(100L);
            doAnswer(invocation -> {
                Article article = invocation.getArgument(0);
                article.setId(100L);
                return 1;
            }).when(articleMapper).insert(any(Article.class));

            // Mock Redis cache and lock for getArticleById at the end
            mockCacheMiss();
            mockExecuteWithLock();
            mockGetArticleByIdChain(100L, userId, dto.getTitle());

            // Act
            ArticleVO result = articleService.createArticle(userId, dto);

            // Assert
            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("Test Title", result.getTitle());

            // Verify mappers called
            verify(articleMapper, times(1)).insert(any(Article.class));
            verify(articleContentMapper, times(1)).insert(any(ArticleContent.class));
            verify(articleCategoryMapper, times(1)).insert(any(ArticleCategory.class));
            verify(articleTagMapper, never()).insert(any(ArticleTag.class));
        }

        @Test
        @DisplayName("should create article with tags when tagIds provided")
        void createArticle_WithTags() {
            // Arrange
            Long userId = 1L;
            ArticleCreateDTO dto = buildCreateDTO();
            dto.setTagIds(Arrays.asList(1L, 2L, 3L));

            // Article insert returns ID via fallback lookup
            doAnswer(invocation -> {
                Article article = invocation.getArgument(0);
                // Don't set ID - simulates auto-fill not working
                return 1;
            }).when(articleMapper).insert(any(Article.class));

            // Fallback: selectByUserId returns recent articles, one matches title
            Article foundArticle = new Article();
            foundArticle.setId(100L);
            foundArticle.setTitle(dto.getTitle());
            when(articleMapper.selectByUserId(userId, 0, 10))
                    .thenReturn(Collections.singletonList(foundArticle));

            // Mock Redis cache and lock
            mockCacheMiss();
            mockExecuteWithLock();
            mockGetArticleByIdChain(100L, userId, dto.getTitle());

            // Act
            ArticleVO result = articleService.createArticle(userId, dto);

            // Assert
            assertNotNull(result);
            assertEquals(100L, result.getId());

            // Verify tags inserted (3 tags)
            verify(articleMapper, times(1)).insert(any(Article.class));
            verify(articleContentMapper, times(1)).insert(any(ArticleContent.class));
            verify(articleCategoryMapper, times(1)).insert(any(ArticleCategory.class));
            verify(articleTagMapper, times(3)).insert(any(ArticleTag.class));
        }

        @Test
        @DisplayName("should throw RuntimeException when article ID cannot be obtained")
        void createArticle_NullId_ThrowsException() {
            // Arrange
            Long userId = 1L;
            ArticleCreateDTO dto = buildCreateDTO();

            // Article insert doesn't set ID
            doAnswer(invocation -> {
                // No ID set on article
                return 1;
            }).when(articleMapper).insert(any(Article.class));

            // Fallback: no recent articles found
            when(articleMapper.selectByUserId(userId, 0, 10))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> articleService.createArticle(userId, dto));
            assertTrue(exception.getMessage().contains("无法获取文章ID"));
        }
    }

    // ==================== updateArticle Tests ====================

    @Nested
    @DisplayName("updateArticle")
    class UpdateArticleTests {

        @Test
        @DisplayName("should update article successfully with new fields")
        void updateArticle_Success() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;
            ArticleUpdateDTO dto = new ArticleUpdateDTO();
            dto.setTitle("Updated Title");
            dto.setContent("Updated Content");
            dto.setSummary("Updated Summary");

            // Existing article found (for existence & permission check in updateArticle)
            Article existingArticle = buildArticle(articleId, userId, "Old Title");
            when(articleMapper.selectOneById(articleId)).thenReturn(existingArticle);

            // Set up chain first (for getArticleById at the end of updateArticle)
            mockCacheMiss();
            mockExecuteWithLock();
            mockGetArticleByIdChain(articleId, userId, dto.getTitle());

            // Content update mock AFTER chain (chain sets selectByArticleId to return new content;
            // override with old content so updateArticle's update-content branch is taken)
            ArticleContent existingContent = new ArticleContent();
            existingContent.setArticleId(articleId);
            existingContent.setContent("Old Content");
            when(articleContentMapper.selectByArticleId(articleId)).thenReturn(existingContent);

            // Act
            ArticleVO result = articleService.updateArticle(userId, articleId, dto);

            // Assert
            assertNotNull(result);
            assertEquals("Updated Title", result.getTitle());

            // Verify article was updated
            verify(articleMapper, times(1)).update(any(Article.class));
            // Verify content was updated (not inserted)
            verify(articleContentMapper, times(1)).update(any(ArticleContent.class));
            verify(articleContentMapper, never()).insert(any(ArticleContent.class));
        }

        @Test
        @DisplayName("should throw ARTICLE_NOT_FOUND when article does not exist")
        void updateArticle_ArticleNotFound() {
            // Arrange
            Long userId = 1L;
            Long articleId = 999L;
            ArticleUpdateDTO dto = new ArticleUpdateDTO();
            dto.setTitle("Updated Title");

            // Article not found
            when(articleMapper.selectOneById(articleId)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleService.updateArticle(userId, articleId, dto));
            assertEquals(ErrorCode.ARTICLE_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("should throw ARTICLE_UPDATE_NO_PERMISSION when user is not the author")
        void updateArticle_NoPermission() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;
            ArticleUpdateDTO dto = new ArticleUpdateDTO();
            dto.setTitle("Updated Title");

            // Article exists but belongs to another user
            Article article = buildArticle(articleId, 2L, "Other User's Article");
            when(articleMapper.selectOneById(articleId)).thenReturn(article);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleService.updateArticle(userId, articleId, dto));
            assertEquals(ErrorCode.ARTICLE_UPDATE_NO_PERMISSION.getCode(), exception.getCode());
        }
    }

    // ==================== deleteArticle Tests ====================

    @Nested
    @DisplayName("deleteArticle")
    class DeleteArticleTests {

        @Test
        @DisplayName("should soft delete article successfully")
        void deleteArticle_Success() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            // Existing article found
            Article existingArticle = buildArticle(articleId, userId, "Test Article");
            when(articleMapper.selectOneById(articleId)).thenReturn(existingArticle);

            // Act
            assertDoesNotThrow(() -> articleService.deleteArticle(userId, articleId));

            // Assert - verify soft delete (isDeleted = 1) via ArgumentCaptor
            var articleCaptor = ArgumentCaptor.forClass(Article.class);
            verify(articleMapper, times(1)).update(articleCaptor.capture());
            assertEquals(1, articleCaptor.getValue().getIsDeleted());
        }

        @Test
        @DisplayName("should throw NOT_FOUND when article does not exist")
        void deleteArticle_ArticleNotFound() {
            // Arrange
            Long userId = 1L;
            Long articleId = 999L;

            // Article not found
            when(articleMapper.selectOneById(articleId)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleService.deleteArticle(userId, articleId));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("should throw ARTICLE_DELETE_NO_PERMISSION when user is not the author")
        void deleteArticle_NoPermission() {
            // Arrange
            Long userId = 1L;
            Long articleId = 100L;

            // Article exists but belongs to another user
            Article article = buildArticle(articleId, 2L, "Other User's Article");
            when(articleMapper.selectOneById(articleId)).thenReturn(article);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> articleService.deleteArticle(userId, articleId));
            assertEquals(ErrorCode.ARTICLE_DELETE_NO_PERMISSION.getCode(), exception.getCode());
        }
    }

    // ==================== getArticleById Tests ====================

    @Nested
    @DisplayName("getArticleById")
    class GetArticleByIdTests {

        @Test
        @DisplayName("should return article VO with full details on success")
        void getArticleById_Success() {
            // Arrange
            Long articleId = 100L;
            Long userId = 1L;

            // Mock Redis cache miss and lock
            mockCacheMiss();
            mockExecuteWithLock();
            mockGetArticleByIdChain(articleId, userId, "Test Article");

            // Act
            ArticleVO result = articleService.getArticleById(articleId, userId);

            // Assert
            assertNotNull(result);
            assertEquals(articleId, result.getId());
            assertEquals("Test Article", result.getTitle());
            assertEquals("published", result.getStatus());
            assertEquals(0, result.getIsTop());

            // Author
            assertNotNull(result.getAuthor());
            assertEquals(userId, result.getAuthor().getId());
            assertEquals("testuser", result.getAuthor().getUsername());

            // Content
            assertEquals("Test Content", result.getContent());

            // Category
            assertNotNull(result.getCategory());

            // Tags
            assertNotNull(result.getTags());
            assertEquals(1, result.getTags().size());

            // Verify mappers were called
            verify(articleMapper, atLeast(1)).selectOneById(articleId);
            verify(articleContentMapper, times(1)).selectByArticleId(articleId);
            verify(articleCategoryMapper, times(1)).selectByArticleId(articleId);
            verify(articleTagMapper, times(1)).selectByArticleId(articleId);
        }

        @Test
        @DisplayName("should return null when article is not found")
        void getArticleById_NotFound() {
            // Arrange
            Long articleId = 999L;

            // Mock Redis cache miss
            mockCacheMiss();
            mockExecuteWithLock();

            // Article not found in DB
            when(articleMapper.selectOneById(articleId)).thenReturn(null);

            // Act
            ArticleVO result = articleService.getArticleById(articleId, null);

            // Assert - returns null when article doesn't exist
            assertNull(result);
            verify(articleMapper, times(1)).selectOneById(articleId);
            // No read stats update when article not found
            verify(articleReadStatsMapper, never()).selectByArticleAndDate(any(), any());
        }
    }
}
