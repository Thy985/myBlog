package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "ai_assistant")
public class AIAssistant {
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("name")
    private String name;
    
    @Column("avatar")
    private String avatar;
    
    @Column("personality")
    private String personality;
    
    @Column("system_prompt")
    private String systemPrompt;
    
    @Column("default_greeting")
    private String defaultGreeting;
    
    @Column("default_style")
    private String defaultStyle;
    
    @Column("max_memory_rounds")
    private Integer maxMemoryRounds;
    
    @Column("evolution_interval")
    private Integer evolutionInterval;
    
    @Column("create_time")
    private LocalDateTime createTime;
    
    @Column("update_time")
    private LocalDateTime updateTime;
}
