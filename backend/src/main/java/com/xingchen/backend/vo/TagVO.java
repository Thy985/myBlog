package com.xingchen.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagVO {
    private Long id;
    private String name;
    private String tagName;
    private String color;
    private Integer articleCount;
    private LocalDateTime createdTime;
    
    public String getTagName() {
        return this.name;
    }
    
    public void setTagName(String tagName) {
        this.name = tagName;
    }
}
