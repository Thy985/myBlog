package com.xingchen.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ArticleUpdateDTO {
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    private String content;

    private String summary;

    private String thumbnail;//缩略图

    private Long categoryId;

    private List<Long> tagIds;
}
