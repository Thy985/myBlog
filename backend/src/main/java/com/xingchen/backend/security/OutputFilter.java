package com.xingchen.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 输出内容过滤器
 * 防止敏感信息泄露、内容安全合规
 */
@Component
@Slf4j
public class OutputFilter {

    // 身份证号
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\b\\d{6}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]\\b");
    
    // 手机号
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
    
    // 银行卡号
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\b\\d{16,19}\\b");
    
    // 邮箱
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    
    // API Key / Token
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[_-]?key|token|secret|password)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_-]{16,})['\"]?");
    
    // 不当内容（可根据需要扩展）
    private static final Pattern[] INAPPROPRIATE_PATTERNS = {
        Pattern.compile("(?i)(暴力|色情|赌博|毒品|诈骗)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
    };

    /**
     * 过滤敏感信息
     * @param output 原始输出
     * @return 过滤后的输出
     */
    public String filterSensitiveInfo(String output) {
        if (output == null || output.isEmpty()) {
            return output;
        }

        String filtered = output;

        // 身份证号脱敏
        filtered = maskPattern(filtered, ID_CARD_PATTERN, match -> {
            String id = match.group();
            return id.substring(0, 6) + "********" + id.substring(14);
        });

        // 手机号脱敏
        filtered = maskPattern(filtered, PHONE_PATTERN, match -> {
            String phone = match.group();
            return phone.substring(0, 3) + "****" + phone.substring(7);
        });

        // 银行卡号脱敏
        filtered = maskPattern(filtered, BANK_CARD_PATTERN, match -> {
            String card = match.group();
            return "****" + card.substring(card.length() - 4);
        });

        // 邮箱脱敏
        filtered = maskPattern(filtered, EMAIL_PATTERN, match -> {
            String email = match.group();
            int atIndex = email.indexOf('@');
            String local = email.substring(0, atIndex);
            String domain = email.substring(atIndex);
            if (local.length() <= 2) {
                return "*" + domain;
            }
            return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
        });

        // API Key 脱敏
        filtered = maskPattern(filtered, API_KEY_PATTERN, match -> {
            String keyType = match.group(1);
            return keyType + ": ****";
        });

        return filtered;
    }

    /**
     * 内容安全检测
     * @param output 输出内容
     * @return 是否包含不当内容
     */
    public ContentSafetyCheckResult checkContentSafety(String output) {
        if (output == null || output.isEmpty()) {
            return ContentSafetyCheckResult.safe();
        }

        for (Pattern pattern : INAPPROPRIATE_PATTERNS) {
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                log.warn("检测到不当内容: {}", matcher.group());
                return ContentSafetyCheckResult.unsafe("内容包含不当信息", matcher.group());
            }
        }

        return ContentSafetyCheckResult.safe();
    }

    /**
     * 完整过滤流程
     */
    public FilterResult filter(String output) {
        // 1. 敏感信息脱敏
        String filtered = filterSensitiveInfo(output);
        
        // 2. 内容安全检测
        ContentSafetyCheckResult safetyResult = checkContentSafety(filtered);
        
        return new FilterResult(filtered, safetyResult.isSafe(), safetyResult.reason());
    }

    private String maskPattern(String input, Pattern pattern, MaskFunction maskFunction) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            matcher.appendReplacement(sb, maskFunction.mask(matcher));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @FunctionalInterface
    private interface MaskFunction {
        String mask(Matcher match);
    }

    /**
     * 过滤结果
     */
    public record FilterResult(String content, boolean isSafe, String reason) {}

    /**
     * 内容安全检测结果
     */
    public record ContentSafetyCheckResult(boolean isSafe, String reason) {
        public static ContentSafetyCheckResult safe() {
            return new ContentSafetyCheckResult(true, null);
        }
        public static ContentSafetyCheckResult unsafe(String reason, String matchedContent) {
            return new ContentSafetyCheckResult(false, reason + ": " + matchedContent);
        }
    }
}