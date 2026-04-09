package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.dto.CategoryCreateDTO;
import com.xingchen.backend.service.CategoryService;
import com.xingchen.backend.vo.CategoryVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@Validated
public class CategoryController {

    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    public Result<List<CategoryVO>> getCategoryList() {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 1L;
        return Result.success(categoryService.getUserCategoryList(userId));
    }

    @GetMapping("/discover")
    public Result<List<CategoryVO>> getDiscoverCategoryList() {
        return Result.success(categoryService.getCategoryList());
    }

    @GetMapping("/user")
    public Result<List<CategoryVO>> getUserCategoryList() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(categoryService.getUserCategoryList(userId));
    }

    @GetMapping("/{id}")
    public Result<CategoryVO> getCategoryById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @PostMapping
    @SaCheckRole("ADMIN")
    public Result<CategoryVO> createCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        return Result.success(categoryService.createCategory(dto));
    }

    @PutMapping("/{id}")
    @SaCheckRole("ADMIN")
    public Result<CategoryVO> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryCreateDTO dto) {
        return Result.success(categoryService.updateCategory(id, dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckRole("ADMIN")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
