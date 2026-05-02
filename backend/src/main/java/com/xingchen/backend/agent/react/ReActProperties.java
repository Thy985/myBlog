package com.xingchen.backend.agent.react;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.react")
public class ReActProperties {

    private boolean enabled = true;

    private int maxIterations = 10;

    private int maxTokens = 4096;

    private double temperature = 0.7;

    private boolean useMemory = true;

    private boolean useRag = true;

    private boolean useFunctionCalling = true;

    private boolean streamEnabled = true;

    private boolean enableSelfReflection = true;

    private int reflectionThreshold = 3;

    private boolean enableErrorRecovery = true;

    private int maxRetryAttempts = 3;

    private long stepTimeoutMs = 60000L;

    private boolean logExecutionTrace = true;

    private boolean enableThoughtVisualization = true;
}
