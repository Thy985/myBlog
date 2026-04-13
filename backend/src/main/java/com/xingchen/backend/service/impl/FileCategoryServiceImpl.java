package com.xingchen.backend.service.impl;

import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.FileCategory;
import com.xingchen.backend.mapper.FileCategoryMapper;
import com.xingchen.backend.service.FileCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileCategoryServiceImpl implements FileCategoryService {

    private final FileCategoryMapper fileCategoryMapper;

    @Override
    public FileCategory createCategory(String name, String description) {
        FileCategory category = new FileCategory();
        category.setName(name);
        category.setDescription(description);
        fileCategoryMapper.insert(category);
        return category;
    }

    @Override
    public void deleteCategory(Long id) {
        FileCategory category = fileCategoryMapper.selectOneById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        fileCategoryMapper.deleteById(id);
    }

    @Override
    public List<FileCategory> getCategoryList() {
        return fileCategoryMapper.selectAll();
    }
}
