package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "t_login_history")
public class LoginHistory {
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("login_time")
    private LocalDateTime loginTime;
    
    @Column("login_ip")
    private String loginIp;
    
    @Column("device")
    private String device;
    
    @Column("browser")
    private String browser;//浏览器
    
    @Column("os")
    private String os;// 操作系统
    
    @Column("status")
    private Integer status;
    
    @Column("message")
    private String message;
}