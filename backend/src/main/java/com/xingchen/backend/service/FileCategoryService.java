package com.xingchen.backend.service;

import com.xingchen.backend.entity.FileCategory;

import java.util.List;

public interface FileCategoryService {
    FileCategory createCategory(String name, String description);

    void deleteCategory(Long id);

    List<FileCategory> getCategoryList();
}
