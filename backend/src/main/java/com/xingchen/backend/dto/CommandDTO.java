package com.xingchen.backend.dto;

import lombok.Data;

@Data
public class CommandDTO {
    private String intent;
    private String content;
    private String topic;
    private String cronExpression;
    private Long articleId;
    private Long taskId;
    private Integer wordCount;
    private String style;
    private String strategy;
    
    public static CommandDTO chat(String content) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("chat");
        cmd.setContent(content);
        return cmd;
    }
    
    public static CommandDTO createArticle(String topic, Integer wordCount, String style) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("create_article");
        cmd.setTopic(topic);
        cmd.setWordCount(wordCount);
        cmd.setStyle(style);
        return cmd;
    }
    
    public static CommandDTO scheduleTask(String topic, String cronExpression) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("schedule_task");
        cmd.setTopic(topic);
        cmd.setCronExpression(cronExpression);
        return cmd;
    }
    
    public static CommandDTO cancelTask(Long taskId) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("cancel_task");
        cmd.setTaskId(taskId);
        return cmd;
    }
    
    public static CommandDTO listTasks() {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("list_tasks");
        return cmd;
    }
    
    public static CommandDTO publishArticle(Long articleId) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("publish_article");
        cmd.setArticleId(articleId);
        return cmd;
    }

    public static CommandDTO editArticle(Long articleId, String content) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("edit_article");
        cmd.setArticleId(articleId);
        cmd.setContent(content);
        return cmd;
    }

    public static CommandDTO createCategory(String categoryName) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("create_category");
        cmd.setTopic(categoryName);
        return cmd;
    }

    public static CommandDTO createTag(String tagName) {
        CommandDTO cmd = new CommandDTO();
        cmd.setIntent("create_tag");
        cmd.setTopic(tagName);
        return cmd;
    }
}
