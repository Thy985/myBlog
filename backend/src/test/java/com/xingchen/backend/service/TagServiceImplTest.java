package com.xingchen.backend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleTag;
import com.xingchen.backend.entity.Tag;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.ArticleTagMapper;
import com.xingchen.backend.mapper.TagMapper;
import com.xingchen.backend.service.impl.TagServiceImpl;
import com.xingchen.backend.vo.TagVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagServiceImpl Tests")
class TagServiceImplTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private ArticleTagMapper articleTagMapper;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private TagServiceImpl tagService;

    // ==================== Helper Methods ====================

    private Tag buildTag(Long id, String name, String color) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setTagName(name);
        tag.setColor(color);
        tag.setIsDeleted(0);
        tag.setArticleCount(0);
        return tag;
    }

    private Tag buildDeletedTag(Long id, String name, String color) {
        Tag tag = buildTag(id, name, color);
        tag.setIsDeleted(1);
        return tag;
    }

    private Article buildArticle(Long id, Long userId) {
        Article article = new Article();
        article.setId(id);
        article.setUserId(userId);
        article.setStatus(1);
        article.setIsDeleted(0);
        return article;
    }

    private ArticleTag buildArticleTag(Long articleId, Long tagId) {
        ArticleTag at = new ArticleTag();
        at.setArticleId(articleId);
        at.setTagId(tagId);
        return at;
    }

    private Map<String, Object> buildTagCountMap(Long id, Long count) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("count", count);
        return map;
    }

    // ==================== getTagList Tests ====================

    @Nested
    @DisplayName("getTagList")
    class GetTagListTests {

        @Test
        @DisplayName("should return all active tags with article counts")
        void getTagList_Success() {
            // Arrange
            Tag tag1 = buildTag(1L, "Java", "#FF0000");
            Tag tag2 = buildTag(2L, "Python", "#00FF00");
            when(tagMapper.selectAll()).thenReturn(Arrays.asList(tag1, tag2));

            Map<String, Object> count1 = buildTagCountMap(1L, 5L);
            Map<String, Object> count2 = buildTagCountMap(2L, 3L);
            when(tagMapper.selectArticleCountGroupByTag())
                    .thenReturn(Arrays.asList(count1, count2));

            // Act
            List<TagVO> result = tagService.getTagList();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());

            TagVO vo1 = result.get(0);
            assertEquals(1L, vo1.getId());
            assertEquals("Java", vo1.getName());
            assertEquals("#FF0000", vo1.getColor());
            assertEquals(5, vo1.getArticleCount());

            TagVO vo2 = result.get(1);
            assertEquals(2L, vo2.getId());
            assertEquals("Python", vo2.getName());
            assertEquals("#00FF00", vo2.getColor());
            assertEquals(3, vo2.getArticleCount());
        }

        @Test
        @DisplayName("should skip deleted tags")
        void getTagList_SkipsDeletedTags() {
            // Arrange
            Tag tag1 = buildTag(1L, "Java", "#FF0000");
            Tag tag2 = buildDeletedTag(2L, "Python", "#00FF00");
            when(tagMapper.selectAll()).thenReturn(Arrays.asList(tag1, tag2));

            Map<String, Object> count1 = buildTagCountMap(1L, 5L);
            when(tagMapper.selectArticleCountGroupByTag())
                    .thenReturn(Collections.singletonList(count1));

            // Act
            List<TagVO> result = tagService.getTagList();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Java", result.get(0).getName());
        }

        @Test
        @DisplayName("should return empty list when no tags exist")
        void getTagList_Empty() {
            // Arrange
            when(tagMapper.selectAll()).thenReturn(Collections.emptyList());
            when(tagMapper.selectArticleCountGroupByTag()).thenReturn(Collections.emptyList());

            // Act
            List<TagVO> result = tagService.getTagList();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should default article count to 0 when tag has no count entry")
        void getTagList_DefaultArticleCount() {
            // Arrange
            Tag tag1 = buildTag(1L, "Rust", "#000000");
            when(tagMapper.selectAll()).thenReturn(Collections.singletonList(tag1));
            // No matching entry in count map
            when(tagMapper.selectArticleCountGroupByTag()).thenReturn(Collections.emptyList());

            // Act
            List<TagVO> result = tagService.getTagList();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(0, result.get(0).getArticleCount());
        }

        @Test
        @DisplayName("should handle tag with isDeleted null (treat as not deleted)")
        void getTagList_NullIsDeleted() {
            // Arrange
            Tag tag = buildTag(1L, "Go", "#0000FF");
            tag.setIsDeleted(null);
            when(tagMapper.selectAll()).thenReturn(Collections.singletonList(tag));
            when(tagMapper.selectArticleCountGroupByTag()).thenReturn(Collections.emptyList());

            // Act
            List<TagVO> result = tagService.getTagList();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Go", result.get(0).getName());
        }
    }

    // ==================== getUserTagList Tests ====================

    @Nested
    @DisplayName("getUserTagList")
    class GetUserTagListTests {

        @Test
        @DisplayName("should return tags for user's published articles")
        void getUserTagList_Success() {
            // Arrange
            Long userId = 1L;
            Article article1 = buildArticle(100L, userId);
            Article article2 = buildArticle(101L, userId);
            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Arrays.asList(article1, article2));

            ArticleTag at1 = buildArticleTag(100L, 1L);
            ArticleTag at2 = buildArticleTag(101L, 1L);
            ArticleTag at3 = buildArticleTag(101L, 2L);
            when(articleTagMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Arrays.asList(at1, at2, at3));

            Tag tag1 = buildTag(1L, "Java", "#FF0000");
            Tag tag2 = buildTag(2L, "Spring", "#6DB33F");
            when(tagMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Arrays.asList(tag1, tag2));

            // Act
            List<TagVO> result = tagService.getUserTagList(userId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());

            TagVO vo1 = result.stream().filter(v -> v.getId().equals(1L)).findFirst().orElse(null);
            assertNotNull(vo1);
            assertEquals("Java", vo1.getName());
            assertEquals(2, vo1.getArticleCount());

            TagVO vo2 = result.stream().filter(v -> v.getId().equals(2L)).findFirst().orElse(null);
            assertNotNull(vo2);
            assertEquals("Spring", vo2.getName());
            assertEquals(1, vo2.getArticleCount());
        }

        @Test
        @DisplayName("should return empty list when user has no articles")
        void getUserTagList_NoArticles() {
            // Arrange
            Long userId = 999L;
            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<TagVO> result = tagService.getUserTagList(userId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            // Should not query further
            verify(articleTagMapper, never()).selectListByQuery(any(QueryWrapper.class));
        }

        @Test
        @DisplayName("should return empty list when articles have no tags")
        void getUserTagList_NoArticleTags() {
            // Arrange
            Long userId = 1L;
            Article article = buildArticle(100L, userId);
            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Collections.singletonList(article));

            when(articleTagMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<TagVO> result = tagService.getUserTagList(userId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            // Should not query tags
            verify(tagMapper, never()).selectListByQuery(any(QueryWrapper.class));
        }
    }

    // ==================== getHotTags Tests ====================

    @Nested
    @DisplayName("getHotTags")
    class GetHotTagsTests {

        @Test
        @DisplayName("should return top N tags sorted by article count descending")
        void getHotTags_WithLimit() {
            // Arrange - set up getTagList chain
            Tag tag1 = buildTag(1L, "Java", "#FF0000");
            Tag tag2 = buildTag(2L, "Python", "#00FF00");
            Tag tag3 = buildTag(3L, "Go", "#0000FF");
            when(tagMapper.selectAll()).thenReturn(Arrays.asList(tag1, tag2, tag3));

            Map<String, Object> count1 = buildTagCountMap(1L, 10L);
            Map<String, Object> count2 = buildTagCountMap(2L, 20L);
            Map<String, Object> count3 = buildTagCountMap(3L, 5L);
            when(tagMapper.selectArticleCountGroupByTag())
                    .thenReturn(Arrays.asList(count1, count2, count3));

            // Act
            List<TagVO> result = tagService.getHotTags(2);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            // Sorted descending by articleCount: Python(20), Java(10)
            assertEquals("Python", result.get(0).getName());
            assertEquals(20, result.get(0).getArticleCount());
            assertEquals("Java", result.get(1).getName());
            assertEquals(10, result.get(1).getArticleCount());
        }

        @Test
        @DisplayName("should default to limit 10 when null is passed")
        void getHotTags_NullLimit() {
            // Arrange
            List<Tag> tags = new ArrayList<>();
            List<Map<String, Object>> counts = new ArrayList<>();
            for (int i = 1; i <= 15; i++) {
                Tag t = buildTag((long) i, "Tag" + i, "#000");
                tags.add(t);
                counts.add(buildTagCountMap((long) i, (long) (15 - i)));
            }
            when(tagMapper.selectAll()).thenReturn(tags);
            when(tagMapper.selectArticleCountGroupByTag()).thenReturn(counts);

            // Act
            List<TagVO> result = tagService.getHotTags(null);

            // Assert
            assertNotNull(result);
            assertEquals(10, result.size());
        }

        @Test
        @DisplayName("should default to limit 10 when zero is passed")
        void getHotTags_ZeroLimit() {
            // Arrange
            List<Tag> tags = new ArrayList<>();
            List<Map<String, Object>> counts = new ArrayList<>();
            for (int i = 1; i <= 15; i++) {
                Tag t = buildTag((long) i, "Tag" + i, "#000");
                tags.add(t);
                counts.add(buildTagCountMap((long) i, (long) (15 - i)));
            }
            when(tagMapper.selectAll()).thenReturn(tags);
            when(tagMapper.selectArticleCountGroupByTag()).thenReturn(counts);

            // Act
            List<TagVO> result = tagService.getHotTags(0);

            // Assert
            assertNotNull(result);
            assertEquals(10, result.size());
        }

        @Test
        @DisplayName("should return empty list when no tags exist")
        void getHotTags_Empty() {
            // Arrange
            when(tagMapper.selectAll()).thenReturn(Collections.emptyList());
            when(tagMapper.selectArticleCountGroupByTag()).thenReturn(Collections.emptyList());

            // Act
            List<TagVO> result = tagService.getHotTags(5);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return all tags when limit exceeds tag count")
        void getHotTags_LimitExceedsSize() {
            // Arrange
            Tag tag1 = buildTag(1L, "Kotlin", "#7F52FF");
            when(tagMapper.selectAll()).thenReturn(Collections.singletonList(tag1));
            Map<String, Object> count1 = buildTagCountMap(1L, 3L);
            when(tagMapper.selectArticleCountGroupByTag())
                    .thenReturn(Collections.singletonList(count1));

            // Act
            List<TagVO> result = tagService.getHotTags(100);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // ==================== getTagById Tests ====================

    @Nested
    @DisplayName("getTagById")
    class GetTagByIdTests {

        @Test
        @DisplayName("should return tag VO when tag exists")
        void getTagById_Success() {
            // Arrange
            Tag tag = buildTag(1L, "Java", "#FF0000");
            when(tagMapper.selectOneById(1L)).thenReturn(tag);

            // Act
            TagVO result = tagService.getTagById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Java", result.getName());
            assertEquals("#FF0000", result.getColor());
        }

        @Test
        @DisplayName("should return null when tag does not exist")
        void getTagById_NotFound() {
            // Arrange
            when(tagMapper.selectOneById(999L)).thenReturn(null);

            // Act
            TagVO result = tagService.getTagById(999L);

            // Assert
            assertNull(result);
        }
    }

    // ==================== createTag Tests ====================

    @Nested
    @DisplayName("createTag")
    class CreateTagTests {

        @Test
        @DisplayName("should create tag and return VO with generated ID")
        void createTag_Success() {
            // Arrange
            String tagName = "Docker";
            String color = "#2496ED";

            doAnswer(invocation -> {
                Tag tag = invocation.getArgument(0);
                tag.setId(10L);
                return 1;
            }).when(tagMapper).insert(any(Tag.class));

            // Act
            TagVO result = tagService.createTag(tagName, color);

            // Assert
            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals(tagName, result.getName());
            assertEquals(color, result.getColor());

            verify(tagMapper, times(1)).insert(any(Tag.class));
        }

        @Test
        @DisplayName("should create tag with null color")
        void createTag_NullColor() {
            // Arrange
            doAnswer(invocation -> {
                Tag tag = invocation.getArgument(0);
                tag.setId(20L);
                return 1;
            }).when(tagMapper).insert(any(Tag.class));

            // Act
            TagVO result = tagService.createTag("K8s", null);

            // Assert
            assertNotNull(result);
            assertEquals(20L, result.getId());
            assertEquals("K8s", result.getName());
            assertNull(result.getColor());
        }
    }

    // ==================== updateTag Tests ====================

    @Nested
    @DisplayName("updateTag")
    class UpdateTagTests {

        @Test
        @DisplayName("should update tag successfully")
        void updateTag_Success() {
            // Arrange
            Long tagId = 1L;
            Tag existingTag = buildTag(tagId, "Old Name", "#000000");
            when(tagMapper.selectOneById(tagId)).thenReturn(existingTag);

            // Mock for countArticlesByTagId (private method called internally)
            when(articleTagMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(5L);

            // Act
            TagVO result = tagService.updateTag(tagId, "New Name", "#FFFFFF");

            // Assert
            assertNotNull(result);
            assertEquals(tagId, result.getId());
            assertEquals("New Name", result.getName());
            assertEquals("#FFFFFF", result.getColor());
            assertEquals(5, result.getArticleCount());

            verify(tagMapper, times(1)).update(any(Tag.class));
        }

        @Test
        @DisplayName("should throw NOT_FOUND when tag does not exist")
        void updateTag_TagNotFound() {
            // Arrange
            when(tagMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> tagService.updateTag(999L, "New Name", "#FFFFFF"));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
            assertEquals("标签不存在", exception.getMessage());

            verify(tagMapper, never()).update(any(Tag.class));
        }

        @Test
        @DisplayName("should update tag with null color")
        void updateTag_NullColor() {
            // Arrange
            Long tagId = 2L;
            Tag existingTag = buildTag(tagId, "Rust", "#000000");
            when(tagMapper.selectOneById(tagId)).thenReturn(existingTag);
            when(articleTagMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(0L);

            // Act
            TagVO result = tagService.updateTag(tagId, "Rust Updated", null);

            // Assert
            assertNotNull(result);
            assertEquals("Rust Updated", result.getName());
            assertNull(result.getColor());
        }

        @Test
        @DisplayName("should return 0 article count when count query throws exception")
        void updateTag_ArticleCountQueryFails() {
            // Arrange
            Long tagId = 1L;
            Tag existingTag = buildTag(tagId, "Java", "#FF0000");
            when(tagMapper.selectOneById(tagId)).thenReturn(existingTag);
            // Simulate exception in countArticlesByTagId
            when(articleTagMapper.selectCountByQuery(any(QueryWrapper.class)))
                    .thenThrow(new RuntimeException("DB error"));

            // Act
            TagVO result = tagService.updateTag(tagId, "Java Updated", "#FF0000");

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getArticleCount());
        }
    }

    // ==================== deleteTag Tests ====================

    @Nested
    @DisplayName("deleteTag")
    class DeleteTagTests {

        @Test
        @DisplayName("should delete tag when it exists")
        void deleteTag_Success() {
            // Arrange
            Long tagId = 1L;
            Tag existingTag = buildTag(tagId, "Java", "#FF0000");
            when(tagMapper.selectOneById(tagId)).thenReturn(existingTag);

            // Act
            assertDoesNotThrow(() -> tagService.deleteTag(tagId));

            // Assert
            verify(tagMapper, times(1)).deleteById(tagId);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when tag does not exist")
        void deleteTag_TagNotFound() {
            // Arrange
            when(tagMapper.selectOneById(999L)).thenReturn(null);

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> tagService.deleteTag(999L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
            assertEquals("标签不存在", exception.getMessage());

            verify(tagMapper, never()).deleteById(anyLong());
        }
    }
}
