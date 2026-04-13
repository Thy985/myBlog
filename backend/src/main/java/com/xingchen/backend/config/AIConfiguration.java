package com.xingchen.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * AI智能体配置
 */
@Configuration
public class AIConfiguration {

    @Value("${ai.agent.security.max-input-length:4000}")
    private int maxInputLength;

    @Value("${ai.agent.memory.max-working-memory:50}")
    private int maxWorkingMemory;

    @Value("${ai.agent.memory.max-short-term-rounds:100}")
    private int maxShortTermRounds;

    @Value("${ai.agent.llm.timeout-seconds:60}")
    private int llmTimeoutSeconds;
}
