package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

@Data
@Table(value = "t_article_category")
public class ArticleCategory {
    @Column("article_id")
    private Long articleId;

    @Column("category_id")
    private Long categoryId;
}
