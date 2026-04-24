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
}

function classifyNetworkError(error) {
  if (error.name === 'AbortError') {return NetworkErrorType.ABORT}
  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    return NetworkErrorType.NETWORK
  }
  if (error.message?.includes('timeout')) {return NetworkErrorType.TIMEOUT}
  if (error.message?.startsWith('HTTP 5')) {return NetworkErrorType.SERVER}
  return NetworkErrorType.UNKNOWN
}

// ============================================================================
// 事件解析器 - 统一的事件转换
// ============================================================================

/**
 * 解析 SSE 数据为统一的 AgentEvent
 * @param {string} line - SSE 数据行
 * @returns {AgentEvent|null}
 */
function parseAgentEvent(line) {
  const trimmed = line.trim()
  if (!trimmed || !trimmed.startsWith('data:')) {return null}

  const dataStr = trimmed.slice(5).trim()
  if (!dataStr) {return null}

  try {
    const rawData = JSON.parse(dataStr)
    return normalizeEvent(rawData)
  } catch (e) {
    logger.warn('[AgentAPI] Failed to parse event:', dataStr, e)
    return null
  }
}

/**
 * 将后端事件标准化为 AgentEvent 协议
 * @param {Object} raw - 原始事件数据
 * @returns {AgentEvent|null}
 */
function normalizeEvent(raw) {
  // 根据事件类型路由到对应的解析器
  if (raw.error) {
    return createErrorEvent(raw)
  }
  if (raw.done) {
    return createDoneEvent(raw)
  }
  if (raw.content !== undefined && !raw.subType) {
    return createMessageEvent(raw)
  }
  if (raw.subType) {
    return createStructuredEvent(raw)
  }

  return null
}

function createErrorEvent(raw) {
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
  }
}

function createDoneEvent(raw) {
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
  }
}

function createMessageEvent(raw) {
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
  }
}

function createStructuredEvent(raw) {
  const subType = raw.subType

  // Thought 类型事件
  if (Object.values(ThoughtSubType).includes(subType)) {
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
    }
  }

  // Action 类型事件
  if (Object.values(ActionSubType).includes(subType)) {
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
    }
  }

  // Observation 类型事件
  if (Object.values(ObservationSubType).includes(subType)) {
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
    }
  }

  return null
}

function extractThoughtData(raw, subType) {
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

function extractObservationData(raw, subType) {
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

function generateId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

// ============================================================================
// SSE 流处理
// ============================================================================

async function processSSEStream(body, callbacks, signal) {
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
  } catch (err) {
    // 确保在所有情况下都释放 reader
    try {
      reader.releaseLock()
    } catch (releaseErr) {
      // 忽略释放锁时的错误
    }

    if (err.message === 'AbortError' || err.name === 'AbortError') {
      return
    }
    // 调用错误回调而不是直接抛出
    onError?.(createNetworkError(err.message || '流处理错误'))
    throw err
  } finally {
    // 双重保险：确保 reader 被释放
    try {
      reader.releaseLock()
    } catch (e) {
      // 锁可能已经被释放，忽略错误
    }
  }
}

function processBuffer(buffer, onEvent) {
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

/**
 * 开始新会话
 * @returns {Promise<{code: number, data: {sessionId: string}, message: string}>}
 */
export function startAgentSession() {
  return request.post('/agent/session/start')
}

/**
 * 普通聊天（非流式）
 * @param {{message: string, sessionId: string}} data
 * @returns {Promise<{code: number, data: any, message: string}>}
 */
export function chatWithAgent(data) {
  return request.post('/agent/chat', data)
}

/**
 * SSE 流式聊天（支持自动重连）
 * 
 * @param {Object} params - 请求参数
 * @param {string} params.message - 用户消息
 * @param {string} params.sessionId - 会话ID
 * @param {string} params.messageId - 消息ID（用于关联事件）
 * @param {Object} callbacks - 回调函数
 * @param {Function} callbacks.onEvent - 收到 AgentEvent
 * @param {Function} callbacks.onError - 收到错误
 * @param {Function} callbacks.onComplete - 流式完成
 * @param {Function} callbacks.onReconnect - 重连回调
 * @param {Object} config - 配置选项
 * @returns {{abort: Function, retryCount: number}} 控制器
 */
export function streamAgentChat(
  { message, sessionId, messageId },
  callbacks,
  config = {}
) {
  const { onEvent, onError, onComplete, onReconnect } = callbacks
  const mergedConfig = { ...DEFAULT_AGENT_CONFIG.retry, ...config }

  // 参数验证
  if (!message?.trim()) {
    onError?.(createValidationError('消息内容不能为空'))
    return { abort: () => { }, retryCount: 0 }
  }

  if (!sessionId) {
    onError?.(createValidationError('会话ID不能为空'))
    return { abort: () => { }, retryCount: 0 }
  }

  let abortController = null
  let retryCount = 0
  let isAborted = false
  let retryTimeout = null

  function getRetryDelay() {
    const delay = mergedConfig.retryDelay * Math.pow(mergedConfig.retryDelayMultiplier, retryCount)
    return Math.min(delay, mergedConfig.maxRetryDelay)
  }

  async function connect() {
    if (isAborted) {return}

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

    } catch (err) {
      if (isAborted || err.name === 'AbortError') {return}

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
      clearTimeout(retryTimeout)
      if (abortController) {
        abortController.abort()
      }
    },
    get retryCount() {
      return retryCount
    }
  }
}

function createValidationError(message) {
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
  }
}

function createNetworkError(message) {
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
  }
}

/**
 * 获取 A/B 实验列表
 * @returns {Promise<{code: number, data: any[], message: string}>}
 */
export function getAgentExperiments() {
  return request.get('/agent/ab/experiments')
}

/**
 * 获取用户实验分配
 * @param {string} experimentId - 实验ID
 * @returns {Promise<{code: number, data: any, message: string}>}
 */
export function getAgentABAssign(experimentId) {
  return request.get('/agent/ab/assign', { params: { experimentId } })
}

// ============================================================================
// 工具注册表 API
// ============================================================================

/**
 * 获取可用工具列表
 * @returns {Promise<{code: number, data: ToolDefinition[], message: string}>}
 */
export function getAvailableTools() {
  return request.get('/agent/tools')
}

/**
 * 执行工具调用（用于调试）
 * @param {string} toolId - 工具ID
 * @param {Object} parameters - 工具参数
 * @returns {Promise<{code: number, data: any, message: string}>}
 */
export function executeTool(toolId, parameters) {
  return request.post('/agent/tools/execute', { toolId, parameters })
}
