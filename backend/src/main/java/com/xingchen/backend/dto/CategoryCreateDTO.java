package com.xingchen.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateDTO {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50字符")
    private String name;

    @Size(max = 200, message = "描述不能超过200字符")
    private String description;

    private Long parentId;

    private Integer sortOrder;//排序
}
