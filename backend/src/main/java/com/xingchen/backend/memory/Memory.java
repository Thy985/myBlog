package com.xingchen.backend.memory;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 记忆实体
 */
@Data
public class Memory {
    private Long id;
    private Long userId;
    private String content;
    private float[] embedding;
    private String category;
    private Integer confidence;
    private Integer ttlDays;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime expireTime;
}