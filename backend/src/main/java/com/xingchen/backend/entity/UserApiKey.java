package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "user_api_key")
public class UserApiKey {
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("provider")
    private String provider;
    
    @Column("api_key")
    private String apiKey;
    
    @Column("base_url")
    private String baseUrl;
    
    @Column("default_model")
    private String defaultModel;
    
    @Column("enabled")
    private Integer enabled;
    
    @Column("quota")
    private Integer quota;
    
    @Column("used")
    private Integer used;
    
    @Column("expire_at")
    private LocalDateTime expireAt;
    
    @Column("create_time")
    private LocalDateTime createTime;
    
    @Column("update_time")
    private LocalDateTime updateTime;
}
