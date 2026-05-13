import request from '@/axios'
import { getToken } from '@/composables/auth'
import logger from '@/utils/logger'
import {
  AgentEventType,
  ThoughtSubType,
  ActionSubType,
  ObservationSubType,
  DEFAULT_AGENT_CONFIG
} from '@/types/agent'
import type { AgentEvent, AgentConfig } from '@/types/agent'
import type { ApiResponse } from '@/types/api'

/**
 * Agent API 服务
 *
 * 设计原则：
 * 1. 单一职责：只负责网络通信和事件解析
 * 2. 依赖倒置：通过回调函数与上层解耦
 * 3. 统一协议：所有事件都符合 AgentEvent 协议
 */

// ============================================================================
// 网络错误处理
// ============================================================================

export const NetworkErrorType = {
  TIMEOUT: 'timeout',
  ABORT: 'abort',
  NETWORK: 'network',
  SERVER: 'server',
  UNKNOWN: 'unknown'
} as const

export type NetworkErrorTypeValue = typeof NetworkErrorType[keyof typeof NetworkErrorType]

function classifyNetworkError(error: Error): NetworkErrorTypeValue {
  if (error.name === 'AbortError') return NetworkErrorType.ABORT
  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    return NetworkErrorType.NETWORK
  }
  if (error.message?.includes('timeout')) return NetworkErrorType.TIMEOUT
  if (error.message?.startsWith('HTTP 5')) return NetworkErrorType.SERVER
  return NetworkErrorType.UNKNOWN
}

// ============================================================================
// 事件解析器 - 统一的事件转换
// ============================================================================

/** 解析 SSE 数据为统一的 AgentEvent */
function parseAgentEvent(line: string): AgentEvent | null {
  const trimmed = line.trim()
  if (!trimmed || !trimmed.startsWith('data:')) return null

  const dataStr = trimmed.slice(5).trim()
  if (!dataStr) return null

  try {
    const rawData = JSON.parse(dataStr)
    return normalizeEvent(rawData)
  } catch (e) {
    logger.warn('[AgentAPI] Failed to parse event:', dataStr, e)
    return null
  }
}

/** 将后端事件标准化为 AgentEvent 协议 */
function normalizeEvent(raw: Record<string, any>): AgentEvent | null {
  if (raw.error) return createErrorEvent(raw)
  if (raw.done) return createDoneEvent(raw)
  if (raw.content !== undefined && !raw.subType) return createMessageEvent(raw)
  if (raw.subType) return createStructuredEvent(raw)
  return null
}

function createErrorEvent(raw: Record<string, any>): AgentEvent {
  return {
    id: generateId(),
    type: AgentEventType.ERROR,
    executionId: raw.executionId || '',
    messageId: raw.messageId || '',
    timestamp: Date.now(),
    meta: {},
    payload: {
      code: raw.errorCode || 'UNKNOWN_ERROR',
      message: raw.error,
      details: raw.details,
      recoverable: raw.recoverable ?? false
    }
  } as AgentEvent
}

function createDoneEvent(raw: Record<string, any>): AgentEvent {
  return {
    id: generateId(),
    type: AgentEventType.DONE,
    executionId: raw.executionId || '',
    messageId: raw.messageId || '',
    timestamp: Date.now(),
    meta: {
      executionTime: raw.totalExecutionTime
    },
    payload: {
      totalExecutionTime: raw.totalExecutionTime,
      summary: raw.summary
    }
  } as AgentEvent
}

function createMessageEvent(raw: Record<string, any>): AgentEvent {
  return {
    id: generateId(),
    type: AgentEventType.MESSAGE,
    executionId: raw.executionId || '',
    messageId: raw.messageId || '',
    timestamp: Date.now(),
    meta: {},
    payload: {
      content: raw.content,
      isStreaming: raw.isStreaming,
      format: raw.format || 'markdown'
    }
  } as AgentEvent
}

function createStructuredEvent(raw: Record<string, any>): AgentEvent | null {
  const subType = raw.subType as string

  if (Object.values(ThoughtSubType).includes(subType as ThoughtSubType)) {
    return {
      id: generateId(),
      type: AgentEventType.THOUGHT,
      subType,
      executionId: raw.executionId || '',
      messageId: raw.messageId || '',
      timestamp: Date.now(),
      meta: {
        confidence: raw.confidence,
        executionTime: raw.executionTime
      },
      payload: {
        content: raw.content || raw.description || '',
        data: raw.data || extractThoughtData(raw, subType)
      }
    } as AgentEvent
  }

  if (Object.values(ActionSubType).includes(subType as ActionSubType)) {
    return {
      id: generateId(),
      type: AgentEventType.ACTION,
      subType,
      executionId: raw.executionId || '',
      messageId: raw.messageId || '',
      timestamp: Date.now(),
      meta: {
        executionTime: raw.executionTime
      },
      payload: {
        name: raw.name || raw.toolName || '',
        input: raw.input || raw.parameters || {},
        status: raw.status || 'pending'
      }
    } as AgentEvent
  }

  if (Object.values(ObservationSubType).includes(subType as ObservationSubType)) {
    return {
      id: generateId(),
      type: AgentEventType.OBSERVATION,
      subType,
      executionId: raw.executionId || '',
      messageId: raw.messageId || '',
      timestamp: Date.now(),
      meta: {
        executionTime: raw.executionTime
      },
      payload: {
        content: raw.content || raw.message || '',
        data: raw.data || extractObservationData(raw, subType)
      }
    } as AgentEvent
  }

  return null
}

function extractThoughtData(raw: Record<string, any>, subType: string): Record<string, any> | undefined {
  switch (subType) {
    case ThoughtSubType.INTENT:
      return {
        type: raw.intent?.type || raw.intentType,
        confidence: raw.intent?.confidence || raw.confidence,
        entities: raw.intent?.entities || raw.entities || [],
        requiresTool: raw.intent?.requiresTool ?? raw.requiresTool,
        possibleTools: raw.intent?.possibleTools || raw.possibleTools
      }
    case ThoughtSubType.PLAN:
      return {
        steps: raw.steps || [],
        estimatedTime: raw.estimatedTime
      }
    case ThoughtSubType.MEMORY:
      return {
        memoryType: raw.memoryType || 'short_term',
        memories: raw.memories || []
      }
    default:
      return undefined
  }
}

function extractObservationData(raw: Record<string, any>, subType: string): Record<string, any> | undefined {
  switch (subType) {
    case ObservationSubType.TOOL_RESULT:
      return {
        toolName: raw.toolName || raw.name,
        success: raw.success ?? true,
        output: raw.result || raw.output,
        error: raw.error
      }
    case ObservationSubType.RAG:
      return {
        query: raw.query || '',
        sources: raw.sources || [],
        searchTime: raw.searchTime
      }
    default:
      return undefined
  }
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

// ============================================================================
// SSE 流处理
// ============================================================================

interface StreamCallbacks {
  onEvent?: (event: AgentEvent) => void
  onComplete?: () => void
  onError?: (error: AgentEvent) => void
}

async function processSSEStream(
  body: ReadableStream<Uint8Array>,
  callbacks: StreamCallbacks,
  signal?: AbortSignal
): Promise<void> {
  const { onEvent, onComplete, onError } = callbacks
  const reader = body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (!signal?.aborted) {
      const { done, value } = await reader.read()

      if (done) {
        if (buffer.trim()) {
          processBuffer(buffer, onEvent)
        }
        onComplete?.()
        return
      }

      buffer += decoder.decode(value, { stream: true })
      buffer = processBuffer(buffer, onEvent)
    }
  } catch (err: any) {
    try {
      reader.releaseLock()
    } catch {
      // 忽略释放锁时的错误
    }

    if (err.message === 'AbortError' || err.name === 'AbortError') {
      return
    }
    onError?.(createNetworkError(err.message || '流处理错误'))
    throw err
  } finally {
    try {
      reader.releaseLock()
    } catch {
      // 锁可能已经被释放，忽略错误
    }
  }
}

function processBuffer(buffer: string, onEvent?: (event: AgentEvent) => void): string {
  const lines = buffer.split('\n')
  const remaining = lines.pop() || ''

  for (const line of lines) {
    const event = parseAgentEvent(line)
    if (event) {
      onEvent?.(event)
    }
  }

  return remaining
}

// ============================================================================
// 公共 API
// ============================================================================

interface StartSessionResponse {
  sessionId: string
}

export function startAgentSession(): Promise<ApiResponse<StartSessionResponse>> {
  return request.post('/agent/session/start')
}

export function chatWithAgent(data: { message: string; sessionId: string }): Promise<ApiResponse<any>> {
  return request.post('/agent/chat', data)
}

interface StreamChatParams {
  message: string
  sessionId: string
  messageId?: string
}

interface StreamChatCallbacks {
  onEvent: (event: AgentEvent) => void
  onError?: (error: AgentEvent) => void
  onComplete?: () => void
  onReconnect?: (info: { retryCount: number }) => void
}

export interface StreamController {
  abort: () => void
  readonly retryCount: number
}

/** SSE 流式聊天（支持自动重连） */
export function streamAgentChat(
  { message, sessionId, messageId }: StreamChatParams,
  callbacks: StreamChatCallbacks,
  config: Partial<AgentConfig['retry']> = {}
): StreamController {
  const { onEvent, onError, onComplete, onReconnect } = callbacks
  const mergedConfig = { ...DEFAULT_AGENT_CONFIG.retry, ...config }

  if (!message?.trim()) {
    onError?.(createValidationError('消息内容不能为空'))
    return { abort: () => {}, retryCount: 0 }
  }

  if (!sessionId) {
    onError?.(createValidationError('会话ID不能为空'))
    return { abort: () => {}, retryCount: 0 }
  }

  let abortController: AbortController | null = null
  let retryCount = 0
  let isAborted = false
  let retryTimeout: ReturnType<typeof setTimeout> | null = null

  function getRetryDelay(): number {
    const delay = mergedConfig.retryDelay * Math.pow(mergedConfig.retryDelayMultiplier, retryCount)
    return Math.min(delay, mergedConfig.maxRetryDelay)
  }

  async function connect(): Promise<void> {
    if (isAborted) return

    abortController = new AbortController()
    const token = getToken()
    const baseApi = import.meta.env.VITE_APP_BASE_API || ''
    const url = `${baseApi}/agent/chat/stream`

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : '',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({
          message: message.trim(),
          sessionId,
          messageId
        }),
        signal: abortController.signal
      })

      if (!response.ok) {
        const errorText = await response.text().catch(() => 'Unknown error')
        throw new Error(`HTTP ${response.status}: ${errorText}`)
      }

      if (!response.body) {
        throw new Error('Response body is null')
      }

      if (retryCount > 0) {
        onReconnect?.({ retryCount })
      }
      retryCount = 0

      await processSSEStream(
        response.body,
        { onEvent, onError, onComplete },
        abortController.signal
      )
    } catch (err: any) {
      if (isAborted || err.name === 'AbortError') return

      const errorType = classifyNetworkError(err)

      if (errorType !== NetworkErrorType.ABORT &&
          errorType !== NetworkErrorType.SERVER &&
          retryCount < mergedConfig.maxRetries &&
          !isAborted) {
        retryCount++
        const delay = getRetryDelay()
        logger.info(`[AgentAPI] 连接失败，${delay}ms 后进行第 ${retryCount} 次重连...`)
        retryTimeout = setTimeout(() => connect(), delay)
        return
      }

      logger.error('[AgentAPI] Connection error:', err)
      onError?.(createNetworkError(err.message || '连接失败，请检查网络'))
    }
  }

  connect()

  return {
    abort: () => {
      isAborted = true
      if (retryTimeout) clearTimeout(retryTimeout)
      if (abortController) {
        abortController.abort()
      }
    },
    get retryCount() {
      return retryCount
    }
  }
}

function createValidationError(message: string): AgentEvent {
  return {
    id: generateId(),
    type: AgentEventType.ERROR,
    executionId: '',
    messageId: '',
    timestamp: Date.now(),
    meta: {},
    payload: {
      code: 'VALIDATION_ERROR',
      message,
      recoverable: true
    }
  } as AgentEvent
}

function createNetworkError(message: string): AgentEvent {
  return {
    id: generateId(),
    type: AgentEventType.ERROR,
    executionId: '',
    messageId: '',
    timestamp: Date.now(),
    meta: {},
    payload: {
      code: 'NETWORK_ERROR',
      message,
      recoverable: true
    }
  } as AgentEvent
}

// ============================================================================
// A/B 实验 API
// ============================================================================

export function getAgentExperiments(): Promise<ApiResponse<any[]>> {
  return request.get('/agent/ab/experiments')
}

export function getAgentABAssign(experimentId: string): Promise<ApiResponse<any>> {
  return request.get('/agent/ab/assign', { params: { experimentId } })
}

// ============================================================================
// 工具注册表 API
// ============================================================================

import type { ToolDefinition } from '@/types/agent'

export function getAvailableTools(): Promise<ApiResponse<ToolDefinition[]>> {
  return request.get('/ai/react/tools')
}

export function executeTool(toolId: string, parameters: Record<string, any>): Promise<ApiResponse<any>> {
  return request.post('/ai/react/tools/execute', { toolId, parameters })
}
