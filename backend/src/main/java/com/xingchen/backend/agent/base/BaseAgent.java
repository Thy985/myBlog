package com.xingchen.backend.agent.base;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Slf4j
public abstract class BaseAgent {

    protected final String agentId;// 智能体 ID
    protected final String agentType;// 任务类型
    protected final Map<String, Object> config;// 任务配置

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
        long startTime = System.currentTimeMillis();// 任务开始时间
        String taskId = context.getTaskId() != null ? context.getTaskId() : UUID.randomUUID().toString();// 任务ID
        context.setTaskId(taskId);

        log.info("[{}] Agent开始执行任务: taskId={}, input={}",
                agentType, taskId, truncate(context.getInput(), 100));

        try {
            preCheck(context);// 预检查

            AgentResult result = executeImpl(context);// 执行任务

            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);// 任务耗时
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

    /**
     * 预检查，检测上下文是否合法存在
     *
     * @param context 任务上下文
     * @throws AgentException 预检查失败
     */
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

    /**
     * 转坏为int 值
     *
     * @param key         配置项 key
     * @param defaultValue 默认值
     * @return 配置项的值
     */
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
    private String truncate(String text, int maxLength) {//防止日志过长
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    @Data
    @lombok.Builder
    public static class AgentContext {
        private String taskId;// 任务ID
        private String input;// 任务输入
        private Map<String, Object> params;// 任务参数
        private Map<String, Object> memory;// 任务记忆
        private Long userId;// 用户ID
        private String sessionId;// 会话ID

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
        private String output;// 任务输出
        private Object data;// 给程序的对象
        private String error;
        private String errorCode;
        private long durationMs;// 任务执行时长
        private Instant timestamp;// 何时完成

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
