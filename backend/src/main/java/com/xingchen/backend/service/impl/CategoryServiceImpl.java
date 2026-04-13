package com.xingchen.backend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.CategoryCreateDTO;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleCategory;
import com.xingchen.backend.entity.Category;
import com.xingchen.backend.mapper.ArticleCategoryMapper;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.CategoryMapper;
import com.xingchen.backend.service.CategoryService;
import com.xingchen.backend.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final ArticleMapper articleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> getCategoryList() {
        List<Category> categories = categoryMapper.selectAll();
        return categories.stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1 && c.getIsDeleted() != null && c.getIsDeleted() == 0)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> getUserCategoryList(Long userId) {
        QueryWrapper articleWrapper = QueryWrapper.create()
                .from("t_article")
                .where("user_id = ?", userId)
                .and("status = 1")
                .and("is_deleted = 0");
        List<Article> userArticles = articleMapper.selectListByQuery(articleWrapper);

        if (userArticles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> articleIds = userArticles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        QueryWrapper acWrapper = QueryWrapper.create()
                .from("t_article_category")
                .in("article_id", articleIds);
        List<ArticleCategory> articleCategories = articleCategoryMapper.selectListByQuery(acWrapper);

        if (articleCategories.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> categoryIds = articleCategories.stream()
                .map(ArticleCategory::getCategoryId)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper categoryWrapper = QueryWrapper.create()
                .from("t_category")
                .in("id", categoryIds)
                .and("status = 1")
                .and("is_deleted = 0");
        List<Category> categories = categoryMapper.selectListByQuery(categoryWrapper);

        Map<Long, Long> categoryArticleCountMap = articleCategories.stream()
                .collect(Collectors.groupingBy(ArticleCategory::getCategoryId, Collectors.counting()));

        return categories.stream()
                .map(category -> {
                    CategoryVO vo = new CategoryVO();
                    BeanUtils.copyProperties(category, vo);
                    vo.setName(category.getCategoryName());
                    long count = categoryArticleCountMap.getOrDefault(category.getId(), 0L);
                    vo.setArticleCount((int) count);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryVO getCategoryById(Long id) {
        Category category = categoryMapper.selectOneById(id);
        return category != null ? convertToVO(category) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public CategoryVO createCategory(CategoryCreateDTO dto) {
        Category category = new Category();
        category.setCategoryName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setParentId(dto.getParentId());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setStatus(1);
        category.setIsDeleted(0);

        categoryMapper.insert(category);
        log.info("创建分类成功: {}", category.getCategoryName());
        return convertToVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public CategoryVO updateCategory(Long id, CategoryCreateDTO dto) {
        Category category = categoryMapper.selectOneById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        if (dto.getName() != null) {
            category.setCategoryName(dto.getName());
        }
        if (dto.getDescription() != null) {
            category.setDescription(dto.getDescription());
        }
        if (dto.getParentId() != null) {
            category.setParentId(dto.getParentId());
        }
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }

        categoryMapper.update(category);
        log.info("更新分类成功: {}", category.getCategoryName());
        return convertToVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public void deleteCategory(Long id) {
        Category category = categoryMapper.selectOneById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        category.setIsDeleted(1);
        categoryMapper.update(category);
        log.info("删除分类成功: {}", category.getCategoryName());
    }

    private CategoryVO convertToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        vo.setName(category.getCategoryName());
        // 从关联表实时计算文章数量
        int count = countArticlesByCategoryId(category.getId());
        vo.setArticleCount(count);
        return vo;
    }

    /**
     * 统计分类下的文章数量
     */
    private int countArticlesByCategoryId(Long categoryId) {
        try {
            QueryWrapper wrapper = QueryWrapper.create()
                .from("t_article_category")
                .where("category_id = ?", categoryId);
            return (int) articleCategoryMapper.selectCountByQuery(wrapper);
        } catch (Exception e) {
            // 如果查询失败，返回数据库中的缓存值
            return 0;
        }
    }
}
