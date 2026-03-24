package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

@Data
@Table(value = "t_article_tag")
public class ArticleTag {
    @Column("article_id")
    private Long articleId;

    @Column("tag_id")
    private Long tagId;
}
