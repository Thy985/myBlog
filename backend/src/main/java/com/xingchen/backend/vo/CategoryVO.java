package com.xingchen.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private Integer sortOrder;
    private Integer articleCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
