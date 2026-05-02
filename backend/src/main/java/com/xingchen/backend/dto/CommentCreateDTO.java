package com.xingchen.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateDTO {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Long parentId; // 父评论ID，为空表示顶级评论

    private Long rootId; // 根评论ID，用于前端传递

    private Long replyToId; // 回复用户ID

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字符")
    private String content;
}
