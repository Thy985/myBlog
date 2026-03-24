package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_article_collect")
public class ArticleCollect {
    @Column("article_id")
    private Long articleId;

    @Column("user_id")
    private Long userId;

    @Column("create_time")
    private LocalDateTime createdTime;
}
