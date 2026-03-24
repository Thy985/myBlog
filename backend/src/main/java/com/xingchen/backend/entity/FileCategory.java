package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_file_category")
public class FileCategory {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("file_count")
    private Integer fileCount;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column("is_deleted")
    private Integer isDeleted;
}
