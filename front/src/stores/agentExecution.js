import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  AgentEventType,
  ThoughtSubType,
  ActionSubType,
  ObservationSubType
} from '@/types/agent'
import logger from '@/utils/logger'

/**
 * Agent Execution Store - 执行层状态管理
 *
 * 职责：
 * 1. 管理执行会话（ExecutionSession）
 * 2. 处理 AgentEvent 事件流
 * 3. 维护执行状态机
 */
export const useAgentExecutionStore = defineStore('agentExecution', () => {
  // ============ State ============
  const currentExecution = ref(null)
  const executionHistory = ref([])
  const isExecuting = ref(false)

  // ============ Getters ============
  const hasActiveExecution = computed(() => currentExecution.value !== null)

  const currentThoughts = computed(() => {
    if (!currentExecution.value) {return []}
    return currentExecution.value.events.filter(e => e.type === AgentEventType.THOUGHT)
  })

  const currentActions = computed(() => {
    if (!currentExecution.value) {return []}
    return currentExecution.value.events.filter(e => e.type === AgentEventType.ACTION)
  })

  const currentObservations = computed(() => {
    if (!currentExecution.value) {return []}
    return currentExecution.value.events.filter(e => e.type === AgentEventType.OBSERVATION)
  })

  const currentIntent = computed(() => {
    const intentEvent = currentThoughts.value.find(e => e.subType === ThoughtSubType.INTENT)
    return intentEvent?.payload?.data || null
  })

  const currentPlan = computed(() => {
    const planEvent = currentThoughts.value.find(e => e.subType === ThoughtSubType.PLAN)
    return planEvent?.payload?.data || null
  })

  const ragSources = computed(() => {
    const ragEvent = currentObservations.value.find(e => e.subType === ObservationSubType.RAG)
    return ragEvent?.payload?.data?.sources || []
  })

  const toolCalls = computed(() => {
    return currentActions.value
      .filter(e => e.subType === ActionSubType.TOOL_CALL)
      .map(action => {
        // 查找对应的结果
        const result = currentObservations.value.find(
          o => o.subType === ObservationSubType.TOOL_RESULT &&
                        o.payload?.data?.toolName === action.payload.name
        )
        return {
          ...action,
          result: result?.payload?.data
        }
      })
  })

  const executionProgress = computed(() => {
    if (!currentExecution.value || !currentPlan.value) {return null}

    const totalSteps = currentPlan.value.steps?.length || 0
    const completedSteps = currentExecution.value.events.filter(
      e => e.type === AgentEventType.ACTION && e.payload.status === 'completed'
    ).length

    return {
      current: Math.min(completedSteps, totalSteps),
      total: totalSteps,
      percentage: totalSteps > 0 ? (completedSteps / totalSteps) * 100 : 0
    }
  })

  // ============ Execution Management ============
  function startExecution(userMessage, messageId) {
    const execution = {
      id: generateId(),
      userMessage,
      messageId,
      startTime: Date.now(),
      status: 'running',
      events: [],
      currentStep: 0,
      totalSteps: 0
    }

    currentExecution.value = execution
    isExecuting.value = true

    return execution.id
  }

  function endExecution(status = 'completed', summary = '') {
    if (!currentExecution.value) {return}

    currentExecution.value.status = status
    currentExecution.value.endTime = Date.now()
    currentExecution.value.summary = summary

    // 保存到历史
    executionHistory.value.unshift({ ...currentExecution.value })

    // 限制历史记录数量
    if (executionHistory.value.length > 50) {
      executionHistory.value = executionHistory.value.slice(0, 50)
    }

    currentExecution.value = null
    isExecuting.value = false
  }

  function cancelExecution() {
    endExecution('cancelled', '用户取消')
  }

  function failExecution(error) {
    endExecution('failed', error)
  }

  // ============ Event Handling ============
  function handleEvent(event) {
    if (!currentExecution.value) {
      logger.warn('[AgentExecution] No active execution to handle event')
      return
    }

    // 添加事件到当前执行
    currentExecution.value.events.push(event)

    // 更新执行状态
    switch (event.type) {
      case AgentEventType.THOUGHT:
        handleThoughtEvent(event)
        break
      case AgentEventType.ACTION:
        handleActionEvent(event)
        break
      case AgentEventType.OBSERVATION:
        handleObservationEvent(event)
        break
      case AgentEventType.DONE:
        handleDoneEvent(event)
        break
      case AgentEventType.ERROR:
        handleErrorEvent(event)
        break
    }
  }

  function handleThoughtEvent(event) {
    if (event.subType === ThoughtSubType.PLAN && event.payload.data?.steps) {
      currentExecution.value.totalSteps = event.payload.data.steps.length
    }
  }

  function handleActionEvent(event) {
    if (event.payload.status === 'completed') {
      currentExecution.value.currentStep++
    }
  }

  function handleObservationEvent() {
    // 观察事件不需要特殊处理
  }

  function handleDoneEvent(event) {
    endExecution('completed', event.payload.summary)
  }

  function handleErrorEvent(event) {
    if (!event.payload.recoverable) {
      failExecution(event.payload.message)
    }
  }

  // ============ History Management ============
  function clearHistory() {
    executionHistory.value = []
  }

  function getExecutionById(id) {
    if (currentExecution.value?.id === id) {
      return currentExecution.value
    }
    return executionHistory.value.find(e => e.id === id)
  }

  // ============ Return ============
  return {
    // State
    currentExecution,
    executionHistory,
    isExecuting,
    // Getters
    hasActiveExecution,
    currentThoughts,
    currentActions,
    currentObservations,
    currentIntent,
    currentPlan,
    ragSources,
    toolCalls,
    executionProgress,
    // Methods
    startExecution,
    endExecution,
    cancelExecution,
    failExecution,
    handleEvent,
    clearHistory,
    getExecutionById
  }
})

function generateId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}
