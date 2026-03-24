package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_role")
public class Role {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("role_name")
    private String roleName;

    @Column("role_code")
    private String roleCode;

    @Column("description")
    private String description;

    @Column("sort_order")
    private Integer sortOrder;

    @Column("status")
    private Integer status;

    @Column("create_time")
    private LocalDateTime createTime;

    @Column("update_time")
    private LocalDateTime updateTime;

    @Column("is_deleted")
    private Integer isDeleted;
}
