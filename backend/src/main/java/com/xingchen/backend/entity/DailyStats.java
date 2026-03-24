package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table(value = "t_daily_stats")
public class DailyStats {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("stats_date")
    private LocalDate statsDate; // 统计日期

    @Column("pv_count")
    private Integer pvCount; // 页面浏览量

    @Column("uv_count")
    private Integer uvCount; // 独立访客数

    @Column("ip_count")
    private Integer ipCount; // IP数

    @Column("new_user_count")
    private Integer newUserCount; // 新增用户数

    @Column("article_publish_count")
    private Integer articlePublishCount; // 文章发布数

    @Column("comment_count")
    private Integer commentCount; // 评论数

    @Column("like_count")
    private Integer likeCount; // 点赞数

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;
}
