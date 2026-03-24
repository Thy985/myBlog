package com.xingchen.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.entity.FeishuConfig;
import com.xingchen.backend.mapper.FeishuConfigMapper;
import com.xingchen.backend.service.FeishuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeishuServiceImpl implements FeishuService {

    private final FeishuConfigMapper feishuConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public FeishuConfig getByUserId(Long userId) {
        return feishuConfigMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(FeishuConfig::getUserId).eq(userId)
        );
    }

    @Override
    public FeishuConfig saveOrUpdate(FeishuConfig config) {
        FeishuConfig existing = getByUserId(config.getUserId());

        if (existing != null) {
            config.setId(existing.getId());
            config.setUpdateTime(LocalDateTime.now());
            feishuConfigMapper.update(config);
        } else {
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            feishuConfigMapper.insert(config);
        }

        return config;
    }

    @Override
    public void sendMessage(Long userId, String message) {
        FeishuConfig config = getByUserId(userId);
        
        if (config == null || config.getEnabled() == 0 || config.getWebhookUrl() == null) {
            log.warn("用户 {} 未配置飞书", userId);
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("msg_type", "text");
            
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("text", message);
            payload.put("content", textContent);

            String json = objectMapper.writeValueAsString(payload);
            sendWebhook(config.getWebhookUrl(), json);
            
            log.info("飞书消息发送成功: {}", userId);
        } catch (Exception e) {
            log.error("飞书消息发送失败: {}", userId, e);
        }
    }

    @Override
    public void sendArticleNotification(Long userId, String articleTitle, Long articleId) {
        String message = String.format("""
            📝 AI 文章已生成！
            
            标题: %s
            文章ID: %d
            
            请登录博客后台查看和编辑。
            """, articleTitle, articleId);
        
        sendMessage(userId, message);
    }

    @Override
    public void sendInteractiveCard(Long userId, String title, String content, String articleId) {
        FeishuConfig config = getByUserId(userId);
        
        if (config == null || config.getEnabled() == 0 || config.getWebhookUrl() == null) {
            return;
        }

        try {
            String cardJson = buildArticleReviewCard(title, content, articleId);
            sendWebhook(config.getWebhookUrl(), cardJson);
            
            log.info("飞书卡片消息发送成功: {}", userId);
        } catch (Exception e) {
            log.error("飞书卡片消息发送失败: {}", userId, e);
        }
    }

    private void sendWebhook(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }
        }
    }

    private String buildArticleReviewCard(String title, String content, String articleId) {
        return """
        {
            "msg_type": "interactive",
            "card": {
                "header": {
                    "title": {
                        "tag": "plain_text",
                        "content": "📝 AI 文章已生成"
                    },
                    "template": "blue"
                },
                "elements": [
                    {
                        "tag": "div",
                        "text": {
                            "tag": "lark_md",
                            "content": "**标题**: %s\\n**内容**: %s"
                        }
                    },
                    {
                        "tag": "action",
                        "actions": [
                            {
                                "tag": "button",
                                "text": {"tag": "plain_text", "content": "📖 预览"},
                                "type": "primary",
                                "url": "https://xingchen.icu/article/%s"
                            },
                            {
                                "tag": "button",
                                "text": {"tag": "plain_text", "content": "✅ 发布"},
                                "type": "primary",
                                "value": {"action": "publish", "articleId": "%s"}
                            }
                        ]
                    }
                ]
            }
        }
        """.formatted(title, content.substring(0, Math.min(content.length(), 100)), articleId, articleId);
    }
}
