package com.xingchen.backend.ai.security;

import com.xingchen.backend.ai.model.AIRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 敏感词过滤器
 * 检测并阻止敏感内容
 */
@Component
@Slf4j
public class SensitiveWordFilter implements SecurityFilter {
    
    @Value("classpath:sensitive-words.txt")
    private Resource sensitiveWordsResource;
    
    private final Set<String> sensitiveWords = new HashSet<>();
    
    @PostConstruct
    public void init() {
        try {
            if (sensitiveWordsResource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(sensitiveWordsResource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            sensitiveWords.add(line.toLowerCase());
                        }
                    }
                }
                log.info("加载敏感词库: {} 个", sensitiveWords.size());
            } else {
                // 使用默认敏感词
                loadDefaultWords();
            }
        } catch (IOException e) {
            log.error("加载敏感词库失败", e);
            loadDefaultWords();
        }
    }
    
    private void loadDefaultWords() {
        // 默认敏感词列表
        sensitiveWords.addAll(Set.of(
            "暴力", "色情", "赌博", "毒品", "枪支", "炸弹",
            "terrorist", "porn", "gambling", "drugs"
        ));
        log.info("使用默认敏感词库: {} 个", sensitiveWords.size());
    }
    
    @Override
    public AIRequest filter(AIRequest request) throws SecurityException {
        String message = request.getMessage();
        if (message == null) {
            return request;
        }
        
        String lowerMessage = message.toLowerCase();
        
        for (String word : sensitiveWords) {
            if (lowerMessage.contains(word)) {
                log.warn("检测到敏感词: userId={}, word={}", request.getUserId(), word);
                throw new SecurityException("输入包含敏感内容，请修改后重试");
            }
        }
        
        return request;
    }
    
    @Override
    public int getOrder() {
        return 30;
    }
}