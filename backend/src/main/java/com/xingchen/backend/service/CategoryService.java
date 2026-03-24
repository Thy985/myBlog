package com.xingchen.backend.service;

import com.xingchen.backend.dto.CategoryCreateDTO;
import com.xingchen.backend.vo.CategoryVO;

import java.util.List;

public interface CategoryService {
    List<CategoryVO> getCategoryList();

    List<CategoryVO> getUserCategoryList(Long userId);

    CategoryVO getCategoryById(Long id);

    CategoryVO createCategory(CategoryCreateDTO dto);

    CategoryVO updateCategory(Long id, CategoryCreateDTO dto);

    void deleteCategory(Long id);
}
