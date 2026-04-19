package com.xingchen.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.dto.CommandDTO;
import com.xingchen.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AssistantWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AssistantWebSocketHandler.class);

    private final AIService aiService;
    private final CommandParserService commandParserService;
    private final ArticleGenerationService articleGenerationService;
    private final ScheduledTaskService scheduledTaskService;
    private final MemoryService memoryService;
    private final FeishuService feishuService;
    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            sessions.put(userId.toString(), session);
            
            memoryService.initUserMemory(userId);
            
            log.info("WebSocket 连接建立，用户: {}", userId);
            sendMessage(session, "欢迎使用 AI 助手！请告诉我您想要做什么。\n\n📝 支持的命令：\n- 写一篇关于xxx的文章\n- 每天早上8点帮我写一篇xxx\n- 查看我的定时任务\n- 取消任务xxx");
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserId(session);
        String payload = message.getPayload();
        
        log.info("收到用户 {} 的原始消息: {}", userId, payload);
        
        try {
            // 解析前端发送的 JSON 格式消息
            String userMessage;
            try {
                JsonNode jsonNode = objectMapper.readTree(payload);
                if (jsonNode.has("content")) {
                    userMessage = jsonNode.get("content").asText();
                } else if (jsonNode.has("type") && "ping".equals(jsonNode.get("type").asText())) {
                    // 心跳消息，直接返回
                    sendMessage(session, "{\"type\":\"pong\"}");
                    return;
                } else {
                    userMessage = payload;
                }
            } catch (Exception e) {
                // 不是 JSON 格式，直接使用原始消息
                userMessage = payload;
            }
            
            log.info("解析后的用户消息: {}", userMessage);
            
            memoryService.saveConversation(userId, "user", userMessage);
            
            CommandDTO command = commandParserService.parse(userMessage);
            log.info("命令解析结果 - userId: {}, intent: {}, topic: {}, articleId: {}", 
                userId, command.getIntent(), command.getTopic(), command.getArticleId());
            
            String response = processCommand(userId, command);
            
            // 保存对话
            memoryService.saveConversation(userId, "assistant", response);
            
            sendMessage(session, response);
            
        } catch (Exception e) {
            log.error("处理消息失败，用户: {}", userId, e);
            try {
                sendMessage(session, "抱歉，处理消息时出现问题，请重试 😅");
            } catch (Exception sendErr) {
                log.error("发送错误消息也失败: {}", sendErr.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId.toString());
            log.info("WebSocket 连接关闭，用户: {}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误", exception);
        Long userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId.toString());
        }
    }

    private String processCommand(Long userId, CommandDTO command) {
        return switch (command.getIntent()) {
            case "create_article" -> {
                try {
                    var article = articleGenerationService.generateArticle(
                            userId,
                            command.getTopic(),
                            command.getWordCount() != null ? command.getWordCount() : 2000,
                            command.getStyle()
                    );
                    
                    memoryService.saveEntity(userId, article.getTitle(), "article", 
                            "文章标题: " + article.getTitle() + ", ID: " + article.getId());
                    
                    feishuService.sendArticleNotification(userId, article.getTitle(), article.getId());
                    
                    yield "文章已生成！\n\n📝 标题: " + article.getTitle() + "\n🆔 ID: " + article.getId() + 
                          "\n\n我已通过飞书通知您（如果已配置）。您可以登录后台查看和编辑。";
                } catch (Exception e) {
                    yield "文章生成失败: " + e.getMessage();
                }
            }
            case "schedule_task" -> {
                try {
                    var task = new com.xingchen.backend.entity.ScheduledTask();
                    task.setUserId(userId);
                    task.setTaskName("定时文章: " + command.getTopic());
                    task.setTopicTemplate(command.getTopic());
                    task.setCronExpression(command.getCronExpression());
                    task.setWordCount(command.getWordCount() != null ? command.getWordCount() : 2000);
                    task.setStyle(command.getStyle());
                    task.setAutoPublish(0);
                    task.setNotifyFeishu(1);
                    scheduledTaskService.create(task);
                    
                    memoryService.saveEntity(userId, "task_" + command.getTopic(), "scheduled_task",
                            "定时任务: " + command.getTopic() + ", Cron: " + command.getCronExpression());
                    
                    yield "✅ 定时任务已创建！\n\n📅 主题: " + command.getTopic() + 
                          "\n⏰ 执行时间: " + command.getCronExpression() + 
                          "\n✍️ 字数: " + (command.getWordCount() != null ? command.getWordCount() : 2000);
                } catch (Exception e) {
                    yield "任务创建失败: " + e.getMessage();
                }
            }
            case "list_tasks" -> {
                var tasks = scheduledTaskService.getByUserId(userId);
                if (tasks.isEmpty()) {
                    yield "您还没有创建任何定时任务。\n\n试试说：\"每天早上8点帮我写一篇科技新闻\"";
                }
                StringBuilder sb = new StringBuilder("📋 您的定时任务：\n\n");
                tasks.forEach(t -> sb.append("🆔 ID: ").append(t.getId())
                        .append("\n📝 主题: ").append(t.getTaskName())
                        .append("\n⏰ Cron: ").append(t.getCronExpression())
                        .append("\n📊 状态: ").append(t.getStatus())
                        .append("\n🔄 已执行: ").append(t.getRunCount()).append("次\n\n"));
                yield sb.toString();
            }
            case "cancel_task" -> {
                try {
                    scheduledTaskService.delete(command.getTaskId());
                    yield "✅ 任务已取消。";
                } catch (Exception e) {
                    yield "取消任务失败: " + e.getMessage();
                }
            }
            case "publish_article" -> {
                Long articleId = command.getArticleId();
                log.info("发布文章命令 - userId: {}, articleId: {}", userId, articleId);
                if (articleId == null) {
                    yield "❌ 请指定要发布的文章ID，例如：发布文章123";
                }
                try {
                    articleService.publishArticle(userId, articleId);
                    yield "✅ 文章已发布！";
                } catch (BusinessException e) {
                    log.error("发布文章业务异常 - userId: {}, articleId: {}, error: {}", userId, articleId, e.getMessage());
                    yield "发布文章失败: " + e.getMessage();
                } catch (Exception e) {
                    log.error("发布文章系统异常 - userId: {}, articleId: {}", userId, articleId, e);
                    yield "发布文章失败: 系统错误，请稍后重试";
                }
            }
            case "edit_article" -> {
                Long editArticleId = command.getArticleId();
                String editContent = command.getContent();
                try {
                    // 获取原文章
                    var article = articleService.getArticleById(editArticleId, userId);
                    if (article == null) {
                        yield "❌ 文章不存在或无权编辑";
                    }
                    // 创建更新DTO
                    var updateDTO = new com.xingchen.backend.dto.ArticleUpdateDTO();
                    updateDTO.setTitle(article.getTitle());
                    updateDTO.setSummary(article.getDescription());
                    updateDTO.setContent(editContent.isEmpty() ? article.getContent() : editContent);
                    updateDTO.setThumbnail(article.getThumbnail());

                    articleService.updateArticle(userId, editArticleId, updateDTO);
                    yield "✅ 文章已更新！";
                } catch (Exception e) {
                    yield "编辑文章失败: " + e.getMessage();
                }
            }
            case "create_category" -> {
                String categoryName = command.getTopic();
                try {
                    var createDTO = new com.xingchen.backend.dto.CategoryCreateDTO();
                    createDTO.setName(categoryName);
                    createDTO.setDescription("由AI助手创建");
                    createDTO.setSortOrder(0);

                    var category = categoryService.createCategory(createDTO);
                    yield "✅ 分类「" + category.getName() + "」创建成功！";
                } catch (Exception e) {
                    yield "创建分类失败: " + e.getMessage();
                }
            }
            case "create_tag" -> {
                String tagName = command.getTopic();
                try {
                    // 生成随机颜色
                    String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8"};
                    String color = colors[(int)(Math.random() * colors.length)];

                    var tag = tagService.createTag(tagName, color);
                    yield "✅ 标签「" + tag.getName() + "」创建成功！";
                } catch (Exception e) {
                    yield "创建标签失败: " + e.getMessage();
                }
            }
            default -> {
                List<Map<String, Object>> recentConv = memoryService.getRecentConversations(userId, 10);
                Map<String, Object> prefs = memoryService.getPreferences(userId);

                String contextPrompt = buildContextPrompt(recentConv, prefs);
                String userMessage = command.getContent();

                // 使用用户配置的 API Key 进行对话，传入自定义系统提示词
                String reply = aiService.chatWithUserApiKeyAndPrompt(userId, userMessage, contextPrompt);
                yield reply;
            }
        };
    }

    private String buildContextPrompt(List<Map<String, Object>> conversations, Map<String, Object> preferences) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是用户的AI助手。");
        
        if (!preferences.isEmpty()) {
            sb.append("\n\n用户偏好：");
            preferences.forEach((k, v) -> sb.append("\n- ").append(k).append(": ").append(v));
        }
        
        if (!conversations.isEmpty()) {
            sb.append("\n\n近期对话：");
            conversations.forEach(c -> {
                sb.append("\n").append(c.get("role")).append(": ").append(c.get("content"));
            });
        }
        
        return sb.toString();
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    private Long getUserId(WebSocketSession session) {
        String uri = session.getUri() != null ? session.getUri().toString() : "";
        int lastSlash = uri.lastIndexOf("/");
        if (lastSlash > 0) {
            try {
                return Long.parseLong(uri.substring(lastSlash + 1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
