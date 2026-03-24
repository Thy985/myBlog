package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_article")
public class Article {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("title")
    private String title;

    @Column("title_image")
    private String titleImage;

    @Column("description")
    private String description;

    @Column("status")
    private Integer status; // 0-草稿，1-已发布

    @Column("comment_status")
    private Integer commentStatus; // 0-关闭，1-开启

    @Column("top_status")
    private Integer topStatus; // 0-不置顶，1-置顶

    @Column("view_status")
    private Integer viewStatus; // 0-私密，1-公开

    @Column("read_num")
    private Integer readNum;

    @Column("comment_num")
    private Integer commentNum;

    @Column("like_num")
    private Integer likeNum;

    @Column("collect_num")
    private Integer collectNum;

    @Column("share_num")
    private Integer shareNum;

    @Column("publish_time")
    private LocalDateTime publishTime;

    @Column("create_time")
    private LocalDateTime createdTime;

    @Column("update_time")
    private LocalDateTime updatedTime;

    @Column("is_deleted")
    private Integer isDeleted;
}
