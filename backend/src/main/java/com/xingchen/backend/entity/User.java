package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table(value = "t_user")
public class User {
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("username")
    private String username;
    
    @Column("password")
    private String password;
    
    @Column("email")
    private String email;
    
    @Column("phone")
    private String phone;
    
    @Column("avatar")
    private String avatar;
    
    @Column("nickname")
    private String nickname;
    
    @Column("intro")
    private String intro;
    
    @Column("status")
    private Integer status;
    
    @Column("mfa_enabled")
    private Integer mfaEnabled;
    
    @Column("mfa_type")
    private String mfaType;
    
    @Column("mfa_secret")
    private String mfaSecret;
    
    @Column("backup_codes")
    private String backupCodes;
    
    @Column("last_login_time")
    private LocalDateTime lastLoginTime;
    
    @Column("last_login_ip")
    private String lastLoginIp;
    
    @Column("created_time")
    private LocalDateTime createdTime;
    
    @Column("updated_time")
    private LocalDateTime updatedTime;
    
    @Column("is_deleted")
    private Integer isDeleted;
    
    @Column("gender")
    private Integer gender;
    
    @Column("birthday")
    private LocalDate birthday;
    
    @Column("bio")
    private String bio;
    
    @Column("website")
    private String website;
    
    @Column("location")
    private String location;
    
    @Column("github")
    private String github;
    
    @Column("gitee")
    private String gitee;
    
    @Column("csdn")
    private String csdn;
    
    @Column("zhihu")
    private String zhihu;
    
    @Column("login_count")
    private Integer loginCount;
    
    @Column("register_time")
    private LocalDateTime registerTime;
    
    @Column("register_ip")
    private String registerIp;
    
    @Column("password_modify_time")
    private LocalDateTime passwordModifyTime;
}