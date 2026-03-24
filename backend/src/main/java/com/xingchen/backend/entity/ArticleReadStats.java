package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("t_article_read_stats")
public class ArticleReadStats {
    
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("article_id")
    private Long articleId;
    
    @Column("stats_date")
    private LocalDate statsDate;
    
    @Column("read_count")
    private Integer readCount;

    @Column("unique_visitors")
    private Integer uniqueVisitors;//唯一访客数
    
    @Column("create_time")
    private LocalDateTime createTime;
    
    @Column("update_time")
    private LocalDateTime updateTime;
}
