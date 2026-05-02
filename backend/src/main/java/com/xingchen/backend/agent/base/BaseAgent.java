package com.xingchen.backend.agent.base;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Slf4j
public abstract class BaseAgent {

    protected final String agentId;
    protected final String agentType;
    protected final Map<String, Object> config;

    protected BaseAgent(String agentType) {
        this.agentId = UUID.randomUUID().toString();
        this.agentType = agentType;
        this.config = Map.of();
    }

    protected BaseAgent(String agentType, Map<String, Object> config) {
        this.agentId = UUID.randomUUID().toString();
        this.agentType = agentType;
        this.config = config != null ? config : Map.of();
    }

    public final AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        String taskId = context.getTaskId() != null ? context.getTaskId() : UUID.randomUUID().toString();
        context.setTaskId(taskId);

        log.info("[{}] Agent开始执行任务: taskId={}, input={}",
                agentType, taskId, truncate(context.getInput(), 100));

        try {
            preCheck(context);

            AgentResult result = executeImpl(context);

            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            result.setTaskId(taskId);
            result.setAgentId(agentId);
            result.setAgentType(agentType);
            result.setSuccess(true);
            result.setTimestamp(Instant.now());

            postProcess(result);

            log.info("[{}] Agent任务完成: taskId={}, duration={}ms, success={}",
                    agentType, taskId, duration, result.isSuccess());

            return result;

        } catch (AgentException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] Agent任务失败: taskId={}, error={}", agentType, taskId, e.getMessage());

            return AgentResult.builder()
                    .taskId(taskId)
                    .agentId(agentId)
                    .agentType(agentType)
                    .success(false)
                    .error(e.getMessage())
                    .errorCode(e.getErrorCode())
                    .durationMs(duration)
                    .timestamp(Instant.now())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] Agent任务异常: taskId={}", agentType, taskId, e);

            return AgentResult.builder()
                    .taskId(taskId)
                    .agentId(agentId)
                    .agentType(agentType)
                    .success(false)
                    .error("Unexpected error: " + e.getMessage())
                    .errorCode("UNEXPECTED_ERROR")
                    .durationMs(duration)
                    .timestamp(Instant.now())
                    .build();
        }
    }

    protected void preCheck(AgentContext context) throws AgentException {
        if (context == null) {
            throw new AgentException("Context cannot be null", "INVALID_CONTEXT");
        }
        boolean hasInput = context.getInput() != null && !context.getInput().isBlank();
        boolean hasParams = context.getParams() != null && !context.getParams().isEmpty();
        if (!hasInput && !hasParams) {
            throw new AgentException("Input or params cannot be empty", "INVALID_INPUT");
        }
    }

    protected void postProcess(AgentResult result) {
        log.debug("[{}] 后置处理完成: taskId={}", agentType, result.getTaskId());
    }

    protected abstract AgentResult executeImpl(AgentContext context) throws AgentException;

    public String getAgentId() {
        return agentId;
    }

    public String getAgentType() {
        return agentType;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    protected String getConfigString(String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    protected int getConfigInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    protected boolean getConfigBoolean(String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    @Data
    @lombok.Builder
    public static class AgentContext {
        private String taskId;
        private String input;
        private Map<String, Object> params;
        private Map<String, Object> memory;
        private Long userId;
        private String sessionId;

        public Object getParam(String key) {
            return params != null ? params.get(key) : null;
        }

        public Object getParam(String key, Object defaultValue) {
            Object value = getParam(key);
            return value != null ? value : defaultValue;
        }
    }

    @Data
    @lombok.Builder
    public static class AgentResult {
        private String taskId;
        private String agentId;
        private String agentType;
        private boolean success;
        private String output;
        private Object data;
        private String error;
        private String errorCode;
        private long durationMs;
        private Instant timestamp;

        public static AgentResult success(String output) {
            return AgentResult.builder()
                    .success(true)
                    .output(output)
                    .build();
        }

        public static AgentResult success(Object data) {
            return AgentResult.builder()
                    .success(true)
                    .data(data)
                    .build();
        }

        public static AgentResult failure(String error) {
            return AgentResult.builder()
                    .success(false)
                    .error(error)
                    .build();
        }

        public static AgentResult failure(String errorCode, String error) {
            return AgentResult.builder()
                    .success(false)
                    .errorCode(errorCode)
                    .error(error)
                    .build();
        }
    }

    public static class AgentException extends Exception {
        private final String errorCode;

        public AgentException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public AgentException(String message, String errorCode, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
