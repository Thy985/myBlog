package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_comment_like")
public class CommentLike {
    @Column("comment_id")
    private Long commentId;

    @Column("user_id")
    private Long userId;

    @Column("create_time")
    private LocalDateTime createdTime;
}
