package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("t_password_reset_token")
public class PasswordResetToken {
    
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("email")
    private String email;
    
    @Column("token")
    private String token;
    
    @Column("code")
    private String code;
    
    @Column("expire_time")
    private LocalDateTime expireTime;
    
    @Column("used")
    private Integer used;
    
    @Column("create_time")
    private LocalDateTime createTime;
}
