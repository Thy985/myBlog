package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_search_history")
public class SearchHistory {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("keyword")
    private String keyword;

    @Column("search_count")
    private Integer searchCount;

    @Column("last_search_time")
    private LocalDateTime lastSearchTime;

    @Column("ip_address")
    private String ipAddress;

    @Column("result_count")
    private Integer resultCount;

    @Column("create_time")
    private LocalDateTime createTime;
}
