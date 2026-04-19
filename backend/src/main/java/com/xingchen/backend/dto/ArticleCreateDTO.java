package com.xingchen.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ArticleCreateDTO {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 150, message = "标题长度不能超过150字符")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    @Size(max = 300, message = "文章摘要不能超过300字符")
    private String summary;

    private String titleImage;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    private List<Long> tagIds;
    
    private Integer commentStatus = 1; // 0-关闭，1-开启

    private Integer topStatus = 0; // 0-不置顶，1-置顶

    private Integer viewStatus = 1; // 0-私密，1-公开
}
