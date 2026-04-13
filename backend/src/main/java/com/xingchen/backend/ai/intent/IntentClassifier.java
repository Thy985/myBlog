package com.xingchen.backend.ai.intent;

import com.xingchen.backend.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 意图分类器
 * 识别用户请求的意图
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IntentClassifier {

    private final AIService aiService;

    /**
     * 分类用户意图
     */
    public String classify(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return "unknown";
        }

        String prompt = buildClassificationPrompt(userInput);
        String result = aiService.chat(prompt);

        // 解析分类结果
        return parseIntent(result);
    }

    /**
     * 构建分类提示词
     */
    private String buildClassificationPrompt(String userInput) {
        return String.format("""
            你是一个意图分类器，请将用户输入分类到以下意图之一：
            - 问答：用户提出问题，需要回答
            - 任务：用户要求执行某个任务
            - 闲聊：用户进行日常聊天
            - 命令：用户发出指令
            - 其他：不属于以上类别

            用户输入: %s

            请只输出意图类别，不要输出其他内容。
            """, userInput);
    }

    /**
     * 解析意图结果
     */
    private String parseIntent(String result) {
        if (result == null) {
            return "unknown";
        }

        String trimmedResult = result.trim().toLowerCase();
        if (trimmedResult.contains("问答")) {
            return "question";
        } else if (trimmedResult.contains("任务")) {
            return "task";
        } else if (trimmedResult.contains("闲聊")) {
            return "chat";
        } else if (trimmedResult.contains("命令")) {
            return "command";
        } else {
            return "other";
        }
    }
}
