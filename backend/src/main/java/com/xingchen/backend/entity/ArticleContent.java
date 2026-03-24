package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_article_content")
public class ArticleContent {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("article_id")
    private Long articleId;

    @Column("content")
    private String content; // 正文内容（HTML格式）

    @Column("md_content")
    private String mdContent; // Markdown原文

    @Column("word_count")
    private Integer wordCount; // 字数统计

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
