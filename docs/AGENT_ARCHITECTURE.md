# 🤖 AI Agent 智能体架构文档

本文档详细介绍 Myblog 系统的 AI 智能体架构设计，包括整体架构、核心模块、数据流和扩展机制。

> **版本**: 2.0.0  
> **更新日期**: 2026-04-29  
> **状态**: 生产就绪

---

## 📋 目录

- [架构概览](#架构概览)
- [核心模块](#核心模块)
- [数据流设计](#数据流设计)
- [工具系统](#工具系统)
- [记忆系统](#记忆系统)
- [前端交互](#前端交互)
- [扩展指南](#扩展指南)

---

## 架构概览

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户交互层                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AgentFloatPanel (悬浮球) → AgentChat (聊天面板)          │   │
│  │  • 可拖拽悬浮球    • 全屏对话界面    • 实时消息流          │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Agent 编排层 (Orchestration)                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AgentOrchestrator (中央编排器)                           │   │
│  │  ├─ SecurityFilterChain (安全过滤)                        │   │
│  │  ├─ IntentClassifier (意图分类)                           │   │
│  │  ├─ MemoryManager (记忆管理)                              │   │
│  │  ├─ AIToolRegistry (工具注册表)                           │   │
│  │  └─ LLMProviderManager (模型路由)                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ AgentWorkflow   │  │ AgentSession    │  │ ExecutionPlan   │  │
│  │ (工作流管理)     │  │ Manager (会话)   │  │ (执行计划)       │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   AI 能力层      │ │   工具执行层     │ │   记忆存储层     │
│  ┌───────────┐  │ │  ┌───────────┐  │ │  ┌───────────┐  │
│  │LLMProvider│  │ │  │ Tool      │  │ │  │ Working   │  │
│  │Manager    │  │ │  │ Interface │  │ │  │ Memory    │  │
│  └─────┬─────┘  │ │  └─────┬─────┘  │ │  └─────┬─────┘  │
│        │        │ │        │        │ │        │        │
│  ┌─────┴─────┐  │ │  ┌─────┴─────┐  │ │  ┌─────┴─────┐  │
│  │ • OpenAI  │  │ │  │• Article  │  │ │  │ • SQLite  │  │
│  │ • GLM     │  │ │  │• Category │  │ │  │ • Redis   │  │
│  │ • Baidu   │  │ │  │• Tag      │  │ │  │ • Vector  │  │
│  │ • Ollama  │  │ │  │• Search   │  │ │  │   DB      │  │
│  │ • ...     │  │ │  │• Code     │  │ │  │           │  │
│  └───────────┘  │ │  └───────────┘  │ │  └───────────┘  │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

### 架构演进

**v1.0 → v2.0 重大改进**

| 维度 | v1.0 (旧架构) | v2.0 (当前架构) |
|------|--------------|----------------|
| 架构模式 | 单层 Provider | 分层编排 (Orchestration) |
| 工具系统 | 硬编码调用 | 注册表 + 动态发现 |
| 记忆管理 | 简单缓存 | 三级记忆 (工作/短期/长期) |
| 会话隔离 | 全局共享 | 用户级隔离 + 过期清理 |
| 流式响应 | 不支持 | SSE 实时推送 |
| 容错机制 | 无 | 断路器 + 熔断保护 |

---

## 核心模块

### 1. AgentOrchestrator (中央编排器)

**职责**: 协调所有 Agent 组件，处理用户请求的完整生命周期

```java
@Service
public class AgentOrchestrator {
    // 核心依赖
    private final SecurityFilterChain securityFilterChain;  // 输入安全过滤
    private final IntentClassifierInterface intentClassifier;  // 意图识别
    private final UnifiedMemoryService unifiedMemoryService;   // 记忆管理
    private final KnowledgeBaseService knowledgeBaseService;     // RAG 知识库
    private final AIToolRegistry toolRegistry;                   // 工具注册表
    private final UserLLMProviderManager userProviderManager;    // LLM 路由
    private final CircuitBreakerRegistry circuitBreakerRegistry; // 熔断保护
    private final MeterRegistry meterRegistry;                   // 监控指标
}
```

**处理流程**:

```
用户输入
    ↓
[SecurityFilterChain] 安全过滤（敏感词、注入检测）
    ↓
[IntentClassifier] 意图分类（写作/查询/编辑/删除/闲聊）
    ↓
[MemoryManager] 加载上下文记忆
    ↓
[KnowledgeBaseService] RAG 检索（如需要）
    ↓
[AIToolRegistry] 工具调用（如需要）
    ↓
[LLMProviderManager] LLM 生成回复
    ↓
[MemoryManager] 更新记忆
    ↓
返回响应
```

### 2. AgentSessionManager (会话管理)

**职责**: 管理用户会话，确保权限隔离和数据安全

**核心特性**:
- ✅ 用户级会话隔离（每个用户独立会话空间）
- ✅ 自动过期清理（默认 60 分钟 TTL）
- ✅ 定时任务清理（每 10 分钟扫描过期会话）
- ✅ 持久化存储（支持 Redis/本地缓存双写）

```java
@Component
public class AgentSessionManager {
    // 本地会话缓存（快速访问）
    private final Map<String, SessionEntry> sessionCache = new ConcurrentHashMap<>();
    
    // 用户会话映射（统计活跃会话）
    private final Map<Long, Set<String>> userSessionMap = new ConcurrentHashMap<>();
    
    // 会话 TTL: 60 分钟
    private static final long SESSION_TTL_MINUTES = 60;
}
```

### 3. AgentWorkflow (工作流管理)

**职责**: 管理复杂任务的工作流执行

```java
@Service
public class AgentWorkflow {
    
    /**
     * 任务步骤
     */
    @Data
    public static class TaskStep {
        private int index;           // 步骤索引
        private String description;  // 步骤描述
        private int total;           // 步骤总数
        private StepStatus status;   // 状态: PENDING/EXECUTING/COMPLETED/FAILED
    }
    
    /**
     * 初始化工作流
     */
    public AgentState initializeWorkflow(Long userId) { ... }
    
    /**
     * 执行工作流
     */
    public AgentState executeWorkflow(String sessionId, Long userId, String task) { ... }
}
```

### 4. AgentState (状态管理)

**职责**: 维护 Agent 执行状态，支持持久化

```java
public class AgentState {
    private String sessionId;           // 会话ID
    private Long userId;                // 用户ID
    private StateStatus status;         // 状态: IDLE/EXECUTING/COMPLETED/ERROR
    private String currentTask;         // 当前任务
    private List<TaskStep> steps;       // 执行步骤
    private Map<String, Object> context; // 上下文数据
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
```

---

## 数据流设计

### 请求处理流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   用户输入   │────→│  WebSocket  │────→│   Agent     │
│  (自然语言)  │     │   / HTTP    │     │  Controller │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          ▼                          │
                    │  ┌─────────────────────────────────────────────┐   │
                    │  │           AgentOrchestrator                  │   │
                    │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐      │   │
                    │  │  │Security │→ │ Intent  │→ │ Memory  │      │   │
                    │  │  │ Filter  │  │Classifier│  │ Load    │      │   │
                    │  │  └─────────┘  └─────────┘  └─────────┘      │   │
                    │  │       │            │            │            │   │
                    │  │       ▼            ▼            ▼            │   │
                    │  │  ┌─────────────────────────────────────────┐ │   │
                    │  │  │         Decision & Routing              │ │   │
                    │  │  │  (工具调用? RAG? 直接回复?)              │ │   │
                    │  │  └─────────────────────────────────────────┘ │   │
                    │  │       │            │            │            │   │
                    │  │       ▼            ▼            ▼            │   │
                    │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐      │   │
                    │  │  │  Tool   │  │   RAG   │  │   LLM   │      │   │
                    │  │  │Execute  │  │ Retrieve│  │ Generate│      │   │
                    │  │  └─────────┘  └─────────┘  └─────────┘      │   │
                    │  └─────────────────────────────────────────────┘   │
                    │                          │                          │
                    │                          ▼                          │
                    │  ┌─────────────────────────────────────────────┐   │
                    │  │           Response Assembly                  │   │
                    │  │  (格式化 + 记忆更新 + 事件发送)               │   │
                    │  └─────────────────────────────────────────────┘   │
                    │                          │                          │
                    └──────────────────────────┼──────────────────────────┘
                                               ▼
                                        ┌─────────────┐
                                        │  流式响应   │
                                        │  (SSE/WS)   │
                                        └─────────────┘
```

### 事件驱动架构

```java
// Agent 事件类型
public enum AgentEventType {
    THOUGHT_START,      // 开始思考
    THOUGHT_STEP,       // 思考步骤
    THOUGHT_COMPLETE,   // 思考完成
    
    INTENT_DETECTED,    // 意图识别完成
    PLAN_CREATED,       // 执行计划创建
    PLAN_PROGRESS,      // 计划进度更新
    
    TOOL_CALL_START,    // 工具调用开始
    TOOL_CALL_COMPLETE, // 工具调用完成
    TOOL_CALL_ERROR,    // 工具调用失败
    
    RAG_RETRIEVE_START, // RAG 检索开始
    RAG_RETRIEVE_RESULT,// RAG 检索结果
    
    LLM_GENERATE_START, // LLM 生成开始
    LLM_GENERATE_CHUNK, // LLM 生成片段
    LLM_GENERATE_COMPLETE,// LLM 生成完成
    
    MESSAGE_COMPLETE,   // 消息完成
    ERROR               // 错误
}
```

---

## 工具系统

### 工具注册表

```java
@Component
public class AIToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    
    // 自动注册所有工具
    public AIToolRegistry(Collection<Tool> toolBeans) {
        toolBeans.forEach(this::register);
    }
    
    public void register(Tool tool) { ... }
    public Tool getTool(String name) { ... }
    public ToolResult execute(String toolName, Map<String, Object> params) { ... }
}
```

### 内置工具列表

| 工具名称 | 功能描述 | 权限要求 |
|---------|---------|---------|
| `article_generator` | SEO 文章生成 | 登录用户 |
| `article_query` | 文章查询检索 | 公开访问 |
| `article_update` | 文章编辑更新 | 文章所有者/管理员 |
| `article_delete` | 文章删除 | 文章所有者/管理员 |
| `article_publish` | 文章发布 | 登录用户 |
| `category_tool` | 分类管理 | 登录用户 |
| `tag_tool` | 标签管理 | 登录用户 |
| `content_audit` | 内容审核 | 管理员 |
| `content_opportunity` | 内容机会分析 | 登录用户 |
| `tavily_search` | 网络搜索 | 登录用户 |
| `code_executor` | 代码执行 | 管理员 |

### 工具接口定义

```java
public interface Tool {
    String getName();                    // 工具唯一标识
    String getDescription();             // 工具描述（用于 LLM 理解）
    ToolParameter[] getParameters();     // 参数定义
    ToolResult execute(Map<String, Object> params);  // 执行逻辑
    boolean isAsync();                   // 是否异步执行
    long getTimeout();                   // 超时时间（毫秒）
}

@Data
public class ToolParameter {
    private String name;                 // 参数名
    private String description;          // 参数描述
    private String type;                 // 参数类型: string/number/array/object
    private boolean required;            // 是否必填
    private Object defaultValue;         // 默认值
}

@Data
public class ToolResult {
    private boolean success;             // 是否成功
    private String message;              // 结果消息
    private Object data;                 // 结果数据
    private List<String> logs;           // 执行日志
}
```

---

## 记忆系统

### 三级记忆架构

```
┌─────────────────────────────────────────────────────────┐
│                    记忆层级                              │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ 工作记忆     │  │  短期记忆    │  │  长期记忆    │     │
│  │ (Working)   │  │ (Short-term)│  │ (Long-term) │     │
│  ├─────────────┤  ├─────────────┤  ├─────────────┤     │
│  │ • 当前会话   │  │ • 最近对话   │  │ • 用户画像   │     │
│  │ • 上下文     │  │ • 对话历史   │  │ • 偏好设置   │     │
│  │ • 临时变量   │  │ • 会话摘要   │  │ • 知识积累   │     │
│  ├─────────────┤  ├─────────────┤  ├─────────────┤     │
│  │ 存储: 内存   │  │ 存储: Redis  │  │ 存储: SQLite │     │
│  │ 寿命: 会话级 │  │ 寿命: 24小时 │  │ 寿命: 永久   │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### 记忆管理接口

```java
public interface MemoryManager {
    // 加载记忆上下文
    MemoryContext load(Long userId, String sessionId, MemoryRequirements requirements);
    
    // 保存记忆
    void save(Long userId, List<MemoryItem> memories);
    
    // 添加工作记忆
    void addWorkingMemory(String sessionId, MemoryItem item);
    
    // 检索记忆（简化版）
    String retrieveMemory(String userInput, String sessionId);
    
    // 更新记忆
    void updateMemory(String userInput, String response, String sessionId);
}
```

---

## 前端交互

### 组件架构

```
AgentFloatPanel (悬浮面板容器)
├── AgentFloatBall (悬浮球)
│   ├── 拖拽交互
│   ├── 边缘吸附
│   └── 未读消息徽章
│
└── AgentChat (聊天面板)
    ├── AgentHeader (头部)
    │   ├── 标题
    │   ├── 模型选择
    │   └── 操作按钮
    │
    ├── AgentMessageList (消息列表)
    │   └── AgentMessage (消息项)
    │       ├── AgentAvatar (头像)
    │       ├── AgentMessageContent (内容)
    │       │   └── AgentMarkdown (Markdown 渲染)
    │       ├── AgentThoughtProcess (思考过程)
    │       │   ├── AgentIntentCard (意图卡片)
    │       │   ├── AgentPlanProgress (计划进度)
    │       │   ├── AgentToolCalls (工具调用)
    │       │   └── AgentRagSources (RAG 来源)
    │       └── AgentMessageActions (操作按钮)
    │           ├── 复制
    │           ├── 重试
    │           └── 点赞/点踩
    │
    ├── AgentInputArea (输入区)
    │   ├── AgentQuickCommands (快捷指令)
    │   ├── AgentInput (输入框)
    │   └── AgentSendButton (发送按钮)
    │
    └── AgentStatusBar (状态栏)
        ├── 连接状态
        └── 模型信息
```

### 状态管理 (Pinia Stores)

```typescript
// 会话状态
export const useAgentSessionStore = defineStore('agentSession', {
  state: () => ({
    sessionId: null,        // 当前会话ID
    messages: [],           // 消息列表
    isConnected: false,     // WebSocket 连接状态
  }),
  actions: {
    async sendMessage(content: string),
    async retryMessage(messageId: string),
    clearSession(),
  }
})

// 执行状态
export const useAgentExecutionStore = defineStore('agentExecution', {
  state: () => ({
    isExecuting: false,     // 是否执行中
    currentIntent: null,    // 当前意图
    currentPlan: null,      // 当前计划
    toolCalls: [],          // 工具调用列表
    ragSources: [],         // RAG 来源
  })
})

// UI 状态
export const useAgentUIStore = defineStore('agentUI', {
  state: () => ({
    isExpanded: false,      // 面板是否展开
    unreadCount: 0,         // 未读消息数
    isDragging: false,      // 是否拖拽中
  })
})
```

### 视觉设计系统

```css
/* 色彩系统 */
--agent-primary: #6366F1;        /* 靛紫色 - 主色调 */
--agent-accent: #60A5FA;         /* 天蓝色 - 强调色 */
--agent-success: #10B981;        /* 翠绿色 - 成功 */
--agent-warning: #F59E0B;        /* 琥珀色 - 警告 */
--agent-error: #EF4444;          /* 红色 - 错误 */

/* 背景色 */
--agent-bg: #0A0A0B;             /* 主背景 */
--agent-bg-card: #16161A;        /* 卡片背景 */
--agent-bg-elevated: #1A1A1D;    /*  elevated 背景 */

/* 文本色 */
--agent-text: #E5E7EB;           /* 主文本 */
--agent-text-secondary: #F3F4F6; /* 次要文本 */
--agent-text-muted: #71717A;     /* 辅助文本 */

/* 玻璃拟态 */
--agent-glass-bg: rgba(22, 22, 26, 0.8);
--agent-glass-border: rgba(255, 255, 255, 0.08);
--agent-glass-blur: blur(12px);

/* 发光效果 */
--agent-glow-primary: 0 0 20px rgba(99, 102, 241, 0.4);
--agent-glow-success: 0 0 20px rgba(16, 185, 129, 0.4);
```

---

## 扩展指南

### 添加新工具

```java
@Component
public class MyCustomTool implements Tool {
    
    @Override
    public String getName() {
        return "my_custom_tool";
    }
    
    @Override
    public String getDescription() {
        return "描述工具功能，供 LLM 理解何时使用";
    }
    
    @Override
    public ToolParameter[] getParameters() {
        return new ToolParameter[] {
            new ToolParameter("param1", "参数描述", "string", true, null),
            new ToolParameter("param2", "参数描述", "number", false, 0)
        };
    }
    
    @Override
    public ToolResult execute(Map<String, Object> params) {
        // 实现工具逻辑
        return ToolResult.success("执行成功", resultData);
    }
    
    @Override
    public boolean isAsync() {
        return false;
    }
    
    @Override
    public long getTimeout() {
        return 30000; // 30秒超时
    }
}
```

### 添加新 LLM Provider

```java
@Component
public class MyLLMProvider implements LLMProvider {
    
    @Override
    public String getProviderName() {
        return "MyProvider";
    }
    
    @Override
    public String getModelName() {
        return "my-model";
    }
    
    @Override
    public AIResponse chat(AIRequest request) {
        // 实现 API 调用
    }
    
    @Override
    public void streamChat(AIRequest request, Consumer<AIResponse> onChunk) {
        // 实现流式响应
    }
}
```

### 自定义意图分类器

```java
@Component
public class MyIntentClassifier implements IntentClassifierInterface {
    
    @Override
    public Intent classify(String userInput) {
        // 实现意图识别逻辑
        // 返回意图类型和置信度
        return new Intent(IntentType.CUSTOM, 0.95);
    }
}
```

---

## 监控与运维

### 关键指标

| 指标 | 说明 | 告警阈值 |
|-----|------|---------|
| `agent.request.latency` | 请求延迟 | > 5s |
| `agent.request.error_rate` | 错误率 | > 5% |
| `agent.session.active` | 活跃会话数 | > 1000 |
| `agent.tool.execution.time` | 工具执行时间 | > 10s |
| `agent.llm.token.usage` | Token 使用量 | 按配额 |

### 日志规范

```java
// 请求日志
log.info("[Agent] Request received: userId={}, sessionId={}, intent={}", userId, sessionId, intent);

// 执行日志
log.debug("[Agent] Tool executing: tool={}, params={}", toolName, params);

// 错误日志
log.error("[Agent] Execution failed: sessionId={}, error={}", sessionId, errorMessage, exception);
```

---

## 版本历史

| 版本 | 日期 | 变更内容 |
|-----|------|---------|
| 2.0.0 | 2026-04-29 | 架构重构：分层编排、工具注册表、三级记忆 |
| 1.5.0 | 2026-04-12 | 新增流式响应、RAG 检索、断路器保护 |
| 1.0.0 | 2026-03-24 | 初始版本：基础对话、简单工具调用 |

---

**文档维护**: 如有问题或建议，请提交 Issue 或联系开发团队。
