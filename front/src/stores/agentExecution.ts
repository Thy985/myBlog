import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  AgentEventType,
  ThoughtSubType,
  ActionSubType,
  ObservationSubType
} from '@/types/agent'
import type { AgentEvent } from '@/types/agent'
import logger from '@/utils/logger'

export interface ExecutionSession {
  id: string
  userMessage: string
  messageId: string
  startTime: number
  endTime?: number
  status: 'running' | 'completed' | 'failed' | 'cancelled'
  summary?: string
  events: AgentEvent[]
  currentStep: number
  totalSteps: number
}

export const useAgentExecutionStore = defineStore('agentExecution', () => {
  const currentExecution = ref<ExecutionSession | null>(null)
  const executionHistory = ref<ExecutionSession[]>([])
  const isExecuting = ref(false)

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

  function startExecution(userMessage: string, messageId: string): string {
    const execution: ExecutionSession = {
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

  function endExecution(status: 'completed' | 'failed' | 'cancelled' = 'completed', summary = ''): void {
    if (!currentExecution.value) {return}

    currentExecution.value.status = status
    currentExecution.value.endTime = Date.now()
    currentExecution.value.summary = summary

    executionHistory.value.unshift({ ...currentExecution.value })

    if (executionHistory.value.length > 50) {
      executionHistory.value = executionHistory.value.slice(0, 50)
    }

    currentExecution.value = null
    isExecuting.value = false
  }

  function cancelExecution(): void {
    endExecution('cancelled', '用户取消')
  }

  function failExecution(error: string): void {
    endExecution('failed', error)
  }

  function handleEvent(event: AgentEvent): void {
    if (!currentExecution.value) {
      logger.warn('[AgentExecution] No active execution to handle event')
      return
    }

    currentExecution.value.events.push(event)

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

  function handleThoughtEvent(event: AgentEvent): void {
    if (event.subType === ThoughtSubType.PLAN && event.payload.data?.steps) {
      currentExecution.value.totalSteps = event.payload.data.steps.length
    }
  }

  function handleActionEvent(event: AgentEvent): void {
    if (event.payload.status === 'completed') {
      currentExecution.value.currentStep++
    }
  }

  function handleObservationEvent(): void {
    // 观察事件不需要特殊处理
  }

  function handleDoneEvent(event: AgentEvent): void {
    endExecution('completed', event.payload.summary)
  }

  function handleErrorEvent(event: AgentEvent): void {
    if (!event.payload.recoverable) {
      failExecution(event.payload.message)
    }
  }

  function clearHistory(): void {
    executionHistory.value = []
  }

  function getExecutionById(id: string): ExecutionSession | undefined {
    if (currentExecution.value?.id === id) {
      return currentExecution.value
    }
    return executionHistory.value.find(e => e.id === id)
  }

  return {
    currentExecution,
    executionHistory,
    isExecuting,
    hasActiveExecution,
    currentThoughts,
    currentActions,
    currentObservations,
    currentIntent,
    currentPlan,
    ragSources,
    toolCalls,
    executionProgress,
    startExecution,
    endExecution,
    cancelExecution,
    failExecution,
    handleEvent,
    clearHistory,
    getExecutionById
  }
})

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}