package com.xingchen.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.common.Result;
import com.xingchen.backend.service.SearchService;
import com.xingchen.backend.vo.ArticleListVO;
import com.xingchen.backend.vo.SearchResultVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/articles")
    public Result<PageResult<ArticleListVO>> searchArticles(
            @RequestParam @NotBlank(message = "关键词不能为空")
            @Size(max = 50, message = "关键词长度不能超过50") String keyword,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码至少为1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页至少1条")
            @Max(value = 50, message = "每页最多50条") Integer size) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        PageResult<ArticleListVO> result = searchService.searchArticles(keyword, page, size);

        searchService.saveSearchHistoryAsync(userId, keyword, result.getList() != null ? result.getList().size() : 0);

        return Result.success(result);
    }
    /**
     * 全局搜索
     * @param keyword 搜索关键词
     * @param type 搜索类型（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 搜索结果分页数据
     */
    @GetMapping("/global")
    public Result<PageResult<SearchResultVO>> globalSearch(
            @RequestParam @NotBlank(message = "关键词不能为空")
            @Size(max = 50, message = "关键词长度不能超过50") String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码至少为1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页至少1条")
            @Max(value = 50, message = "每页最多50条") Integer size) {
        return Result.success(searchService.globalSearch(keyword, type, page, size));
    }

    @GetMapping("/suggestions")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam @NotBlank(message = "关键词不能为空")
            @Size(max = 50, message = "关键词长度不能超过50") String keyword,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "返回数量至少为1")
            @Max(value = 20, message = "返回数量最多20条") Integer limit) {
        return Result.success(searchService.getSearchSuggestions(keyword, limit));
    }

    @GetMapping("/hot-keywords")
    public Result<List<String>> getHotSearchKeywords(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "返回数量至少为1")
            @Max(value = 50, message = "返回数量最多50条") Integer limit) {
        return Result.success(searchService.getHotSearchKeywords(limit));
    }
}
