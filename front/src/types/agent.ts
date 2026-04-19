/**
 * Agent 事件协议 - 统一的事件抽象
 * 
 * 核心设计原则：
 * 1. 所有 Agent 行为都抽象为三种事件类型：THOUGHT / ACTION / OBSERVATION
 * 2. 每种事件都有统一的元数据结构和 payload
 * 3. 事件之间通过 correlationId 关联
 * 4. 支持扩展：新增事件子类型不需要修改核心协议
 */

// ============================================================================
// 核心事件类型
// ============================================================================

export enum AgentEventType {
    /** Agent 的思考过程：意图识别、规划、反思 */
    THOUGHT = 'thought',
    /** Agent 的执行动作：工具调用、API 请求 */
    ACTION = 'action',
    /** Agent 观察到的结果：工具返回、检索结果 */
    OBSERVATION = 'observation',
    /** 消息内容（对用户的输出） */
    MESSAGE = 'message',
    /** 执行完成 */
    DONE = 'done',
    /** 错误 */
    ERROR = 'error'
}

export enum ThoughtSubType {
    /** 意图识别 */
    INTENT = 'intent',
    /** 执行规划 */
    PLAN = 'plan',
    /** 记忆检索 */
    MEMORY = 'memory',
    /** 反思 */
    REFLECTION = 'reflection'
}

export enum ActionSubType {
    /** 工具调用 */
    TOOL_CALL = 'tool_call',
    /** 外部 API 调用 */
    API_CALL = 'api_call',
    /** 数据库操作 */
    DB_OPERATION = 'db_operation'
}

export enum ObservationSubType {
    /** 工具执行结果 */
    TOOL_RESULT = 'tool_result',
    /** RAG 检索结果 */
    RAG = 'rag',
    /** 记忆上下文 */
    MEMORY_CONTEXT = 'memory_context',
    /** 外部数据 */
    EXTERNAL_DATA = 'external_data'
}

// ============================================================================
// 基础事件接口
// ============================================================================

export interface AgentEventBase {
    /** 事件唯一 ID */
    id: string
    /** 事件类型 */
    type: AgentEventType
    /** 事件子类型 */
    subType?: string
    /** 关联的执行会话 ID */
    executionId: string
    /** 关联的消息 ID */
    messageId: string
    /** 事件发生时间 */
    timestamp: number
    /** 事件元数据 */
    meta: EventMetadata
}

export interface EventMetadata {
    /** 执行耗时（毫秒） */
    executionTime?: number
    /** 置信度（0-1） */
    confidence?: number
    /** 来源 */
    source?: string
    /** 额外属性 */
    [key: string]: any
}

// ============================================================================
// 具体事件类型
// ============================================================================

/** 思考事件 */
export interface ThoughtEvent extends AgentEventBase {
    type: AgentEventType.THOUGHT
    subType: ThoughtSubType
    payload: {
        /** 思考内容 */
        content: string
        /** 结构化数据 */
        data?: IntentData | PlanData | MemoryData | ReflectionData
    }
}

/** 动作事件 */
export interface ActionEvent extends AgentEventBase {
    type: AgentEventType.ACTION
    subType: ActionSubType
    payload: {
        /** 动作名称 */
        name: string
        /** 输入参数 */
        input: Record<string, any>
        /** 动作状态 */
        status: 'pending' | 'executing' | 'completed' | 'failed'
    }
}

/** 观察事件 */
export interface ObservationEvent extends AgentEventBase {
    type: AgentEventType.OBSERVATION
    subType: ObservationSubType
    payload: {
        /** 观察内容 */
        content: string
        /** 结构化数据 */
        data?: ToolResultData | RAGData | MemoryContextData
    }
}

/** 消息事件 */
export interface MessageEvent extends AgentEventBase {
    type: AgentEventType.MESSAGE
    payload: {
        /** 消息内容 */
        content: string
        /** 是否流式 */
        isStreaming?: boolean
        /** 消息格式 */
        format?: 'text' | 'markdown' | 'html'
    }
}

/** 完成事件 */
export interface DoneEvent extends AgentEventBase {
    type: AgentEventType.DONE
    payload: {
        /** 总执行耗时 */
        totalExecutionTime?: number
        /** 执行结果摘要 */
        summary?: string
    }
}

/** 错误事件 */
export interface ErrorEvent extends AgentEventBase {
    type: AgentEventType.ERROR
    payload: {
        /** 错误码 */
        code: string
        /** 错误消息 */
        message: string
        /** 错误详情 */
        details?: any
        /** 是否可恢复 */
        recoverable?: boolean
    }
}

/** 联合类型：所有 Agent 事件 */
export type AgentEvent = 
    | ThoughtEvent 
    | ActionEvent 
    | ObservationEvent 
    | MessageEvent 
    | DoneEvent 
    | ErrorEvent

// ============================================================================
// Payload 数据结构
// ============================================================================

export interface IntentData {
    /** 意图类型 */
    type: string
    /** 置信度 */
    confidence: number
    /** 提取的实体 */
    entities: Entity[]
    /** 是否需要工具 */
    requiresTool: boolean
    /** 可能需要的工具 */
    possibleTools?: string[]
}

export interface Entity {
    name: string
    value: string
    type?: string
}

export interface PlanData {
    /** 执行步骤 */
    steps: PlanStep[]
    /** 预计总耗时 */
    estimatedTime?: number
}

export interface PlanStep {
    id: string
    name: string
    description?: string
    dependencies?: string[]
}

export interface MemoryData {
    /** 记忆类型 */
    memoryType: 'working' | 'short_term' | 'long_term'
    /** 记忆内容 */
    memories: MemoryItem[]
}

export interface MemoryItem {
    id: string
    content: string
    relevance: number
    timestamp: number
}

export interface ReflectionData {
    /** 反思类型 */
    reflectionType: 'self_critique' | 'plan_adjustment' | 'error_analysis'
    /** 反思内容 */
    content: string
    /** 建议 */
    suggestions?: string[]
}

export interface ToolResultData {
    /** 工具名称 */
    toolName: string
    /** 是否成功 */
    success: boolean
    /** 返回数据 */
    output: any
    /** 错误信息 */
    error?: string
}

export interface RAGData {
    /** 检索查询 */
    query: string
    /** 检索来源 */
    sources: RAGSource[]
    /** 检索耗时 */
    searchTime?: number
}

export interface RAGSource {
    id: string
    title: string
    url?: string
    snippet: string
    relevance: number
    metadata?: Record<string, any>
}

export interface MemoryContextData {
    /** 上下文类型 */
    contextType: string
    /** 上下文内容 */
    context: string
    /** 相关记忆 */
    relatedMemories?: MemoryItem[]
}

// ============================================================================
// 执行会话
// ============================================================================

export interface ExecutionSession {
    /** 会话 ID */
    id: string
    /** 关联的用户消息 */
    userMessage: string
    /** 开始时间 */
    startTime: number
    /** 结束时间 */
    endTime?: number
    /** 执行状态 */
    status: 'running' | 'completed' | 'failed' | 'cancelled'
    /** 事件列表 */
    events: AgentEvent[]
    /** 当前步骤 */
    currentStep?: number
    /** 总步骤 */
    totalSteps?: number
}

// ============================================================================
// 消息模型
// ============================================================================

export interface AgentMessage {
    id: string
    role: 'user' | 'assistant' | 'system'
    content: string
    timestamp: number
    /** 关联的执行会话 */
    executionId?: string
    /** 是否流式中 */
    isStreaming?: boolean
    /** 是否错误 */
    isError?: boolean
    /** 消息元数据 */
    meta?: MessageMetadata
}

export interface MessageMetadata {
    /** 关联的执行事件 */
    executionEvents?: string[]
    /** 使用的工具 */
    toolsUsed?: string[]
    /** 检索来源 */
    sources?: RAGSource[]
    /** 执行耗时 */
    executionTime?: number
}

// ============================================================================
// 工具定义
// ============================================================================

export interface ToolDefinition {
    /** 工具唯一标识 */
    id: string
    /** 工具名称 */
    name: string
    /** 工具显示名称 */
    displayName: string
    /** 工具描述 */
    description: string
    /** 工具图标 */
    icon?: string
    /** 输入参数定义 */
    parameters: ToolParameter[]
    /** 输出定义 */
    output?: ToolOutput
    /** 工具分类 */
    category?: string
    /** 是否启用 */
    enabled: boolean
}

export interface ToolParameter {
    name: string
    type: 'string' | 'number' | 'boolean' | 'array' | 'object'
    description: string
    required: boolean
    default?: any
    enum?: any[]
}

export interface ToolOutput {
    type: string
    description: string
}

// ============================================================================
// Store 状态定义
// ============================================================================

export interface AgentSessionState {
    sessionId: string | null
    messages: AgentMessage[]
}

export interface AgentExecutionState {
    /** 当前执行会话 */
    currentExecution: ExecutionSession | null
    /** 执行历史 */
    executionHistory: ExecutionSession[]
    /** 是否正在执行 */
    isExecuting: boolean
}

export interface AgentUIState {
    isDrawerOpen: boolean
    unreadCount: number
    /** 当前展开的详情面板 */
    expandedPanels: Set<string>
    /** 选中的消息 */
    selectedMessageId: string | null
}

// ============================================================================
// 配置选项
// ============================================================================

export interface AgentConfig {
    /** 重连配置 */
    retry: {
        maxRetries: number
        retryDelay: number
        retryDelayMultiplier: number
        maxRetryDelay: number
    }
    /** 流式配置 */
    streaming: {
        enabled: boolean
        bufferSize: number
    }
    /** UI 配置 */
    ui: {
        maxVisibleMessages: number
        messagePageSize: number
        enableVirtualScroll: boolean
    }
}

export const DEFAULT_AGENT_CONFIG: AgentConfig = {
    retry: {
        maxRetries: 3,
        retryDelay: 1000,
        retryDelayMultiplier: 2,
        maxRetryDelay: 10000
    },
    streaming: {
        enabled: true,
        bufferSize: 1024
    },
    ui: {
        maxVisibleMessages: 50,
        messagePageSize: 20,
        enableVirtualScroll: true
    }
}
