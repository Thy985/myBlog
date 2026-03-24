package com.xingchen.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchResultVO {
    private Long id;
    private String title;
    private String summary;
    private String content; // 匹配的内容片段
    private String type; // ARTICLE, COMMENT等
    private String authorName;
    private LocalDateTime createdTime;
    private String highlight; // 高亮显示的关键词
}
