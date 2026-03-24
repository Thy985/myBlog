package com.xingchen.backend.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(value = "scheduled_task")
public class ScheduledTask {
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("task_name")
    private String taskName;
    
    @Column("cron_expression")
    private String cronExpression;
    
    @Column("topic_template")
    private String topicTemplate;
    
    @Column("strategy")
    private String strategy;
    
    @Column("word_count")
    private Integer wordCount;
    
    @Column("style")
    private String style;
    
    @Column("category_id")
    private Long categoryId;
    
    @Column("keywords")
    private String keywords;
    
    @Column("status")
    private String status;
    
    @Column("last_run_time")
    private LocalDateTime lastRunTime;
    
    @Column("next_run_time")
    private LocalDateTime nextRunTime;
    
    @Column("run_count")
    private Integer runCount;
    
    @Column("auto_publish")
    private Integer autoPublish;
    
    @Column("notify_feishu")
    private Integer notifyFeishu;
    
    @Column("create_time")
    private LocalDateTime createTime;
    
    @Column("update_time")
    private LocalDateTime updateTime;
}
