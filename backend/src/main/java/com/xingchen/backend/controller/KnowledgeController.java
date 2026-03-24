package com.xingchen.backend.controller;

import com.xingchen.backend.common.Result;
import com.xingchen.backend.vector.HybridSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库控制器
 * 
 * 提供知识库管理接口
 */
@RestController
@RequestMapping("/api/knowledge")
@Slf4j
@RequiredArgsConstructor
public class KnowledgeController {

    private final HybridSearchService hybridSearchService;

    /**
     * 混合检索
     */
    @GetMapping("/search")
    public Result<List<HybridSearchService.HybridSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        
        List<HybridSearchService.HybridSearchResult> results = 
                hybridSearchService.hybridSearch(query, topK);
        
        return Result.success(results);
    }

    /**
     * 带过滤条件的检索
     */
    @GetMapping("/search/filter")
    public Result<List<HybridSearchService.HybridSearchResult>> searchWithFilter(
            @RequestParam String query,
            @RequestParam String category,
            @RequestParam(defaultValue = "5") int topK) {
        
        List<HybridSearchService.HybridSearchResult> results = 
                hybridSearchService.hybridSearch(query, topK, "category", category);
        
        return Result.success(results);
    }

    /**
     * 手动索引文档
     */
    @PostMapping("/index")
    public Result<Void> indexDocument(@RequestBody IndexRequest request) {
        hybridSearchService.indexDocument(
                request.getArticleId(),
                request.getTitle(),
                request.getContent(),
                request.getCategory()
        );
        return Result.success();
    }

    /**
     * 删除文档索引
     */
    @DeleteMapping("/index/{articleId}")
    public Result<Void> deleteDocument(@PathVariable Long articleId) {
        hybridSearchService.deleteDocument(articleId);
        return Result.success();
    }

    // ========== 请求类 ==========

    @lombok.Data
    public static class IndexRequest {
        private Long articleId;
        private String title;
        private String content;
        private String category;
    }
}