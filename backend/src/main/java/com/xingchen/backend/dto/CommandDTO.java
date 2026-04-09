package com.xingchen.backend.dto;

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

    // Getters and Setters
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    // Factory methods
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
