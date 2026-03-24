package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_tag")
public class Tag {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("tag_name")
    private String tagName;

    @Column("color")
    private String color; // 标签颜色

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
