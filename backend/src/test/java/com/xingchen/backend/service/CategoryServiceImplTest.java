package com.xingchen.backend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.CategoryCreateDTO;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleCategory;
import com.xingchen.backend.entity.Category;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.mapper.ArticleCategoryMapper;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.CategoryMapper;
import com.xingchen.backend.service.impl.CategoryServiceImpl;
import com.xingchen.backend.vo.CategoryVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ArticleCategoryMapper articleCategoryMapper;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // ---- Helper methods ----

    private Category buildCategory(Long id, String name, String description, Integer status, Integer isDeleted) {
        Category category = new Category();
        category.setId(id);
        category.setCategoryName(name);
        category.setDescription(description);
        category.setStatus(status);
        category.setIsDeleted(isDeleted);
        return category;
    }

    private ArticleCategory buildArticleCategory(Long articleId, Long categoryId) {
        ArticleCategory ac = new ArticleCategory();
        ac.setArticleId(articleId);
        ac.setCategoryId(categoryId);
        return ac;
    }

    private Article buildArticle(Long id, Long userId) {
        Article article = new Article();
        article.setId(id);
        article.setUserId(userId);
        return article;
    }

    // ---- getCategoryList ----

    @Nested
    @DisplayName("getCategoryList()")
    class GetCategoryListTests {

        @Test
        @DisplayName("Should return active categories with article counts")
        void getCategoryList_Success() {
            Category activeCategory = buildCategory(1L, "Java", "Java articles", 1, 0);
            Category inactiveCategory = buildCategory(2L, "Draft", "Draft category", 0, 0);
            Category deletedCategory = buildCategory(3L, "Deleted", "Deleted category", 1, 1);

            List<Category> allCategories = List.of(activeCategory, inactiveCategory, deletedCategory);
            when(categoryMapper.selectAll()).thenReturn(allCategories);

            ArticleCategory ac1 = buildArticleCategory(100L, 1L);
            ArticleCategory ac2 = buildArticleCategory(101L, 1L);
            when(articleCategoryMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(List.of(ac1, ac2));

            List<CategoryVO> result = categoryService.getCategoryList();

            assertNotNull(result);
            assertEquals(1, result.size());
            CategoryVO vo = result.get(0);
            assertEquals(1L, vo.getId());
            assertEquals("Java", vo.getName());
            assertEquals("Java articles", vo.getDescription());
            assertEquals(2, vo.getArticleCount());

            verify(categoryMapper).selectAll();
            verify(articleCategoryMapper).selectListByQuery(any(QueryWrapper.class));
        }

        @Test
        @DisplayName("Should return empty list when no active categories exist")
        void getCategoryList_Empty() {
            Category inactiveCategory = buildCategory(1L, "Draft", "Draft", 0, 0);
            Category deletedCategory = buildCategory(2L, "Deleted", "Deleted", 1, 1);

            when(categoryMapper.selectAll()).thenReturn(List.of(inactiveCategory, deletedCategory));

            List<CategoryVO> result = categoryService.getCategoryList();

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(categoryMapper).selectAll();
            verifyNoInteractions(articleCategoryMapper);
        }

        @Test
        @DisplayName("Should return empty list when no categories exist at all")
        void getCategoryList_NoCategoriesAtAll() {
            when(categoryMapper.selectAll()).thenReturn(new ArrayList<>());

            List<CategoryVO> result = categoryService.getCategoryList();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should default article count to 0 when no articles linked")
        void getCategoryList_ZeroArticleCount() {
            Category activeCategory = buildCategory(1L, "Java", "Java articles", 1, 0);
            when(categoryMapper.selectAll()).thenReturn(List.of(activeCategory));
            when(articleCategoryMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(new ArrayList<>());

            List<CategoryVO> result = categoryService.getCategoryList();

            assertEquals(1, result.size());
            assertEquals(0, result.get(0).getArticleCount());
        }
    }

    // ---- getUserCategoryList ----

    @Nested
    @DisplayName("getUserCategoryList()")
    class GetUserCategoryListTests {

        @Test
        @DisplayName("Should return categories used by the user's articles")
        void getUserCategoryList_Success() {
            Long userId = 1L;
            Article article1 = buildArticle(100L, userId);
            Article article2 = buildArticle(101L, userId);
            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(List.of(article1, article2));

            ArticleCategory ac1 = buildArticleCategory(100L, 1L);
            ArticleCategory ac2 = buildArticleCategory(101L, 1L);
            ArticleCategory ac3 = buildArticleCategory(101L, 2L);
            when(articleCategoryMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(List.of(ac1, ac2, ac3));

            Category cat1 = buildCategory(1L, "Java", "Java articles", 1, 0);
            Category cat2 = buildCategory(2L, "Spring", "Spring articles", 1, 0);
            when(categoryMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(List.of(cat1, cat2));

            List<CategoryVO> result = categoryService.getUserCategoryList(userId);

            assertNotNull(result);
            assertEquals(2, result.size());

            CategoryVO vo1 = result.get(0);
            assertEquals(1L, vo1.getId());
            assertEquals("Java", vo1.getName());
            assertEquals(2, vo1.getArticleCount());

            CategoryVO vo2 = result.get(1);
            assertEquals(2L, vo2.getId());
            assertEquals("Spring", vo2.getName());
            assertEquals(1, vo2.getArticleCount());
        }

        @Test
        @DisplayName("Should return empty list when user has no articles")
        void getUserCategoryList_NoArticles() {
            Long userId = 1L;
            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(new ArrayList<>());

            List<CategoryVO> result = categoryService.getUserCategoryList(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(articleMapper).selectListByQuery(any(QueryWrapper.class));
            verifyNoInteractions(articleCategoryMapper);
            verifyNoInteractions(categoryMapper);
        }

        @Test
        @DisplayName("Should return empty list when articles have no categories")
        void getUserCategoryList_NoArticleCategories() {
            Long userId = 1L;
            Article article = buildArticle(100L, userId);
            when(articleMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(List.of(article));

            when(articleCategoryMapper.selectListByQuery(any(QueryWrapper.class)))
                    .thenReturn(new ArrayList<>());

            List<CategoryVO> result = categoryService.getUserCategoryList(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ---- createCategory ----

    @Nested
    @DisplayName("createCategory()")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category and return VO")
        void createCategory_Success() {
            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setName("New Category");
            dto.setDescription("A new category");

            when(categoryMapper.insert(any(Category.class))).thenReturn(1);
            when(articleCategoryMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(0L);

            CategoryVO result = categoryService.createCategory(dto);

            assertNotNull(result);
            assertEquals("New Category", result.getName());
            assertEquals("A new category", result.getDescription());
            assertEquals(0, result.getArticleCount());

            verify(categoryMapper).insert(any(Category.class));
        }

        @Test
        @DisplayName("Should create category with parentId and sortOrder")
        void createCategory_WithParentAndSort() {
            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setName("Sub Category");
            dto.setDescription("A sub category");
            dto.setParentId(5L);
            dto.setSortOrder(10);

            when(categoryMapper.insert(any(Category.class))).thenReturn(1);
            when(articleCategoryMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(0L);

            CategoryVO result = categoryService.createCategory(dto);

            assertNotNull(result);
            assertEquals("Sub Category", result.getName());
            assertEquals(5L, result.getParentId());
            assertEquals(10, result.getSortOrder());

            verify(categoryMapper).insert(argThat(category -> {
                Category c = (Category) category;
                return "Sub Category".equals(c.getCategoryName())
                        && c.getStatus() == 1
                        && c.getIsDeleted() == 0
                        && Long.valueOf(5L).equals(c.getParentId())
                        && Integer.valueOf(10).equals(c.getSortOrder());
            }));
        }

        @Test
        @DisplayName("Should default sortOrder to 0 when not provided")
        void createCategory_DefaultSortOrder() {
            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setName("Test");
            dto.setDescription("Test desc");

            when(categoryMapper.insert(any(Category.class))).thenReturn(1);
            when(articleCategoryMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(0L);

            categoryService.createCategory(dto);

            verify(categoryMapper).insert(argThat(category -> {
                Category c = (Category) category;
                return Integer.valueOf(0).equals(c.getSortOrder());
            }));
        }
    }

    // ---- updateCategory ----

    @Nested
    @DisplayName("updateCategory()")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category and return VO")
        void updateCategory_Success() {
            Category existing = buildCategory(1L, "Old Name", "Old desc", 1, 0);
            when(categoryMapper.selectOneById(1L)).thenReturn(existing);
            when(categoryMapper.update(any(Category.class))).thenReturn(1);
            when(articleCategoryMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(3L);

            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setName("New Name");
            dto.setDescription("New desc");

            CategoryVO result = categoryService.updateCategory(1L, dto);

            assertNotNull(result);
            assertEquals("New Name", result.getName());
            assertEquals("New desc", result.getDescription());
            assertEquals(3, result.getArticleCount());

            verify(categoryMapper).update(argThat(category -> {
                Category c = (Category) category;
                return "New Name".equals(c.getCategoryName())
                        && "New desc".equals(c.getDescription());
            }));
        }

        @Test
        @DisplayName("Should update only non-null fields")
        void updateCategory_PartialUpdate() {
            Category existing = buildCategory(1L, "Old Name", "Old desc", 1, 0);
            when(categoryMapper.selectOneById(1L)).thenReturn(existing);
            when(categoryMapper.update(any(Category.class))).thenReturn(1);
            when(articleCategoryMapper.selectCountByQuery(any(QueryWrapper.class))).thenReturn(0L);

            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setName("New Name");
            // description is null, should not be updated

            CategoryVO result = categoryService.updateCategory(1L, dto);

            assertNotNull(result);
            assertEquals("New Name", result.getName());

            verify(categoryMapper).update(argThat(category -> {
                Category c = (Category) category;
                return "New Name".equals(c.getCategoryName())
                        && "Old desc".equals(c.getDescription());
            }));
        }

        @Test
        @DisplayName("Should throw BusinessException when category not found")
        void updateCategory_NotFound() {
            when(categoryMapper.selectOneById(999L)).thenReturn(null);

            CategoryCreateDTO dto = new CategoryCreateDTO();
            dto.setName("New Name");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> categoryService.updateCategory(999L, dto));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getCode(), exception.getCode());
            assertEquals(ErrorCode.CATEGORY_NOT_FOUND.getMessage(), exception.getMessage());

            verify(categoryMapper, never()).update(any());
        }
    }

    // ---- deleteCategory ----

    @Nested
    @DisplayName("deleteCategory()")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should soft delete category")
        void deleteCategory_Success() {
            Category existing = buildCategory(1L, "Java", "Java articles", 1, 0);
            when(categoryMapper.selectOneById(1L)).thenReturn(existing);
            when(categoryMapper.update(any(Category.class))).thenReturn(1);

            assertDoesNotThrow(() -> categoryService.deleteCategory(1L));

            verify(categoryMapper).update(argThat(category -> {
                Category c = (Category) category;
                return Integer.valueOf(1).equals(c.getIsDeleted());
            }));
        }

        @Test
        @DisplayName("Should throw BusinessException when category not found")
        void deleteCategory_NotFound() {
            when(categoryMapper.selectOneById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> categoryService.deleteCategory(999L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
            assertEquals("分类不存在", exception.getMessage());

            verify(categoryMapper, never()).update(any());
        }
    }
}
