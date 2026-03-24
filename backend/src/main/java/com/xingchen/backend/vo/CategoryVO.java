package com.xingchen.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private String categoryName;
    private String description;
    private Long parentId;
    private Integer sortOrder;
    private Integer articleCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    public String getCategoryName() {
        return this.name;
    }
    
    public void setCategoryName(String categoryName) {
        this.name = categoryName;
    }
}
