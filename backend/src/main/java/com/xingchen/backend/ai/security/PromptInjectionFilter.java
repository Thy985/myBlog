package com.xingchen.backend.ai.security;

import com.xingchen.backend.ai.model.AIRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt注入过滤器
 * 检测并阻止Prompt注入攻击
 */
@Component
@Slf4j
public class PromptInjectionFilter implements SecurityFilter {
    
    /**
     * 攻击模式列表
     */
    private static final List<AttackPattern> ATTACK_PATTERNS = List.of(
        new AttackPattern(
            Pattern.compile("ignore\\s+(all\\s+)?(previous|above|prior)\\s+instructions?", 
                    Pattern.CASE_INSENSITIVE),
            "忽略指令攻击"
        ),
        new AttackPattern(
            Pattern.compile("system\\s+prompt|system\\s+instruction", 
                    Pattern.CASE_INSENSITIVE),
            "系统提示词探测"
        ),
        new AttackPattern(
            Pattern.compile("you\\s+are\\s+now|from\\s+now\\s+on\\s+you\\s+are", 
                    Pattern.CASE_INSENSITIVE),
            "角色切换攻击"
        ),
        new AttackPattern(
            Pattern.compile("DAN|Do\\s+Anything\\s+Now|jailbreak", 
                    Pattern.CASE_INSENSITIVE),
            "越狱攻击"
        ),
        new AttackPattern(
            Pattern.compile("</?system>|</?instruction>|</?prompt>", 
                    Pattern.CASE_INSENSITIVE),
            "标签注入"
        ),
        new AttackPattern(
            Pattern.compile("(repeat|say|print)\\s+(after\\s+me|the\\s+above|your\\s+instructions?)", 
                    Pattern.CASE_INSENSITIVE),
            "重复指令攻击"
        ),
        new AttackPattern(
            Pattern.compile("new\\s+instruction[s?]:|updated\\s+instruction[s?]:", 
                    Pattern.CASE_INSENSITIVE),
            "新指令注入"
        )
    );
    
    @Override
    public AIRequest filter(AIRequest request) throws SecurityException {
        String message = request.getMessage();
        if (message == null) {
            return request;
        }
        
        String lowerMessage = message.toLowerCase();
        
        for (AttackPattern pattern : ATTACK_PATTERNS) {
            if (pattern.regex.matcher(lowerMessage).find()) {
                log.warn("检测到Prompt注入攻击: userId={}, type={}, message={}", 
                        request.getUserId(), pattern.name, 
                        message.substring(0, Math.min(100, message.length())));
                throw new SecurityException("检测到不安全的输入模式，请重新输入");
            }
        }
        
        return request;
    }
    
    @Override
    public int getOrder() {
        return 20;
    }
    
    private record AttackPattern(Pattern regex, String name) {}
}