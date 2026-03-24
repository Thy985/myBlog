package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_article_stats")
public class ArticleStats {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("article_id")
    private Long articleId;

    @Column("daily_pv")
    private Integer dailyPv; // 当日PV

    @Column("daily_uv")
    private Integer dailyUv; // 当日UV

    @Column("stats_date")
    private LocalDateTime statsDate;

    @Column("create_time")
    private LocalDateTime createTime;
}
