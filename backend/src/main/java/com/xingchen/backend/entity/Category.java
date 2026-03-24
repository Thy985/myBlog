package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_category")
public class Category {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("category_name")
    private String categoryName;

    @Column("description")
    private String description;

    @Column("parent_id")
    private Long parentId;

    @Column("sort_order")
    private Integer sortOrder;

    @Column("article_count")
    private Integer articleCount;

    @Column("status")
    private Integer status;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column("is_deleted")
    private Integer isDeleted;
}
