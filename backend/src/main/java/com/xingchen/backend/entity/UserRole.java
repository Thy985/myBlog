package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_user_role")
public class UserRole {
    @Column("user_id")
    private Long userId;

    @Column("role_id")
    private Long roleId;

    @Column("create_time")
    private LocalDateTime createTime;
}
