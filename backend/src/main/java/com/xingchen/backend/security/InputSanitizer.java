package com.xingchen.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入安全过滤器
 * 防止 Prompt 注入攻击和 XSS
 */
@Component
@Slf4j
public class InputSanitizer {

    // Prompt 注入攻击模式
    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = List.of(
        Pattern.compile("忽略(之前|以上|所有|前面)的?(指令|指示|提示|prompt)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ignore (previous|above|all|prior) (instructions?|prompts?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\s*prompt", Pattern.CASE_INSENSITIVE),
        Pattern.compile("DAN\s*mode", Pattern.CASE_INSENSITIVE),
        Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
        Pattern.compile("开发者模式", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(现在|请)你(是|扮演|作为)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("you are (now|a|an)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(新的|新的)角色", Pattern.CASE_INSENSITIVE),
        Pattern.compile("character\s*reset", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(忘记|清除|重置)之前的?(限制|约束|规则)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(no|without|bypass) (restrictions?|constraints?|rules?)", Pattern.CASE_INSENSITIVE)
    );

    // 敏感内容模式
    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
        Pattern.compile("(密码|password|密钥|secret|token)\\s*[:=]\\s*\\S+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b\\d{16,19}\\b"), // 银行卡号
        Pattern.compile("\\b\\d{18}\\b"), // 身份证号
        Pattern.compile("1[3-9]\\d{9}"), // 手机号
        Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") // 邮箱
    );

    // 最大输入长度
    private static final int MAX_INPUT_LENGTH = 10000;

    /**
     * 清理用户输入
     * @param input 原始输入
     * @return 清理后的输入
     * @throws SecurityException 检测到安全风险时抛出
     */
    public String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // 长度检查
        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("输入超过最大长度: {} > {}", input.length(), MAX_INPUT_LENGTH);
            throw new SecurityException("输入内容过长");
        }

        // 检测 Prompt 注入
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("检测到 Prompt 注入攻击: {}", pattern.pattern());
                throw new SecurityException("输入包含不安全内容");
            }
        }

        // 检测敏感信息泄露（仅记录，不阻断）
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("输入可能包含敏感信息");
            }
        }

        // HTML 转义防止 XSS
        String sanitized = HtmlUtils.htmlEscape(input);

        // 移除控制字符
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        return sanitized;
    }

    /**
     * 轻量级清理（仅 XSS 防护，不检测注入）
     */
    public String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(input);
    }

    /**
     * 验证输入是否安全（不抛出异常）
     */
    public boolean isSafe(String input) {
        try {
            sanitize(input);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}