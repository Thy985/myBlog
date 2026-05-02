package com.xingchen.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagVO {
    private Long id;
    private String name;
    private String color;
    private Integer articleCount;
    private LocalDateTime createdTime;
}
