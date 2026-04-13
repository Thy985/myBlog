package com.xingchen.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI智能体配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.agent")
public class AIAgentProperties {
    
    private Security security = new Security();
    private Memory memory = new Memory();
    private Intent intent = new Intent();
    private LLM llm = new LLM();
    
    @Data
    public static class Security {
        private int maxInputLength = 4000;
        private boolean enablePromptInjectionCheck = true;
        private boolean enableSensitiveWordCheck = true;
    }
    
    @Data
    public static class Memory {
        private int maxWorkingMemory = 50;
        private int maxShortTermRounds = 100;
        private int workingTtlMinutes = 30;
        private int shortTermTtlDays = 7;
    }
    
    @Data
    public static class Intent {
        private double regexThreshold = 0.8;
        private double embeddingThreshold = 0.75;
    }
    
    @Data
    public static class LLM {
        private int timeoutSeconds = 60;
        private int maxRetries = 3;
        private List<String> modelChain = List.of(
                "openai/gpt-4",
                "anthropic/claude-3-opus",
                "openai/gpt-4-turbo",
                "anthropic/claude-3-sonnet",
                "openai/gpt-3.5-turbo"
        );
    }
}