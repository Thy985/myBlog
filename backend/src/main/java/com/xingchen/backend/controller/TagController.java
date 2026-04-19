package com.xingchen.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.TagService;
import com.xingchen.backend.vo.TagVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
@Validated
public class TagController {

    private final TagService tagService;

    @GetMapping("/list")
    public Result<List<TagVO>> getTagList() {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 1L;
        return Result.success(tagService.getUserTagList(userId));
    }

    @GetMapping("/discover")
    public Result<List<TagVO>> getDiscoverTagList() {
        return Result.success(tagService.getTagList());
    }

    @GetMapping("/user")
    public Result<List<TagVO>> getUserTagList() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(tagService.getUserTagList(userId));
    }

    @GetMapping("/hot")
    public Result<List<TagVO>> getHotTags(@RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit) {
        return Result.success(tagService.getHotTags(limit));
    }

    @GetMapping("/{id}")
    public Result<TagVO> getTagById(@PathVariable Long id) {
        return Result.success(tagService.getTagById(id));
    }

    @PostMapping
    @SaCheckRole("ADMIN")
    public Result<TagVO> createTag(@RequestParam String name,
                                   @RequestParam(required = false) String color) {
        return Result.success(tagService.createTag(name, color));
    }

    @PutMapping("/{id}")
    @SaCheckRole("ADMIN")
    public Result<TagVO> updateTag(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam(required = false) String color) {
        return Result.success(tagService.updateTag(id, name, color));
    }

    @DeleteMapping("/{id}")
    @SaCheckRole("ADMIN")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }
}
