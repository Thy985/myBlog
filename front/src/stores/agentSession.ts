import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { startAgentSession } from '@/api/frontend/agent'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const STORAGE_KEY = 'blog:agent:session:v2'

export interface AgentMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
  isStreaming?: boolean
  isError?: boolean
}

export const useAgentSessionStore = defineStore('agentSession', () => {
  const sessionId = ref<string | null>(null)
  const messages = ref<AgentMessage[]>([])

  const hasMessages = computed(() => messages.value.length > 0)
  const lastMessage = computed(() => messages.value[messages.value.length - 1] || null)

  async function initSession(): Promise<boolean> {
    try {
      const res = await startAgentSession()
      if (res.code === API_STATUS.SUCCESS && res.data?.sessionId) {
        sessionId.value = res.data.sessionId
        return true
      }
      throw new Error(res.message || '初始化会话失败')
    } catch (err: any) {
      logger.error('[AgentSession] 初始化会话失败:', err)
      return false
    }
  }

  function clearSession(): void {
    sessionId.value = null
    messages.value = []
    clearStorage()
  }

  function addMessage(message: Omit<AgentMessage, 'timestamp'> & { timestamp?: number }): void {
    messages.value.push({
      ...message,
      timestamp: message.timestamp || Date.now()
    })
  }

  function updateMessage(id: string, updates: Partial<AgentMessage>): void {
    const index = messages.value.findIndex(m => m.id === id)
    if (index >= 0) {
      messages.value[index] = { ...messages.value[index], ...updates }
    }
  }

  function removeMessage(id: string): void {
    const index = messages.value.findIndex(m => m.id === id)
    if (index >= 0) {
      messages.value.splice(index, 1)
    }
  }

  function getMessageById(id: string): AgentMessage | undefined {
    return messages.value.find(m => m.id === id)
  }

  function loadFromStorage(): boolean {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (!saved) {return false}

      const state = JSON.parse(saved)
      if (state.messages && Array.isArray(state.messages)) {
        messages.value = state.messages.map((msg: AgentMessage) => ({
          ...msg,
          isStreaming: false,
          isError: msg.isError || false
        }))
      }
      return true
    } catch (e: any) {
      logger.warn('[AgentSession] Failed to save state:', e)
      localStorage.removeItem(STORAGE_KEY)
      return false
    }
  }

  function saveToStorage(): void {
    try {
      const state = {
        messages: messages.value,
        savedAt: Date.now(),
        version: '2.0'
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    } catch (e: any) {
      logger.warn('[AgentSession] Failed to load state:', e)
    }
  }

  function clearStorage(): void {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch (e: any) {
      logger.warn('[AgentSession] Failed to clear storage:', e)
    }
  }

  let saveTimer: ReturnType<typeof setTimeout> | null = null
  const stopWatch = watch(messages, () => {
    if (saveTimer) {clearTimeout(saveTimer)}
    saveTimer = setTimeout(() => {
      if (messages.value.length > 0) {
        saveToStorage()
      }
    }, 300)
  }, { deep: true })

  function $dispose(): void {
    if (saveTimer) {clearTimeout(saveTimer); saveTimer = null}
    stopWatch()
  }

  return {
    sessionId,
    messages,
    hasMessages,
    lastMessage,
    initSession,
    clearSession,
    addMessage,
    updateMessage,
    removeMessage,
    getMessageById,
    loadFromStorage,
    saveToStorage
  }
})