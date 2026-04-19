import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { startAgentSession } from '@/api/frontend/agent'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const STORAGE_KEY = 'blog:agent:session:v2'

/**
 * Agent Session Store - 会话层状态管理
 * 
 * 职责：
 * 1. 管理会话生命周期
 * 2. 管理消息列表
 * 3. 本地存储持久化
 */
export const useAgentSessionStore = defineStore('agentSession', () => {
  // ============ State ============
  const sessionId = ref(null)
  const messages = ref([])

  // ============ Getters ============
  const hasMessages = computed(() => messages.value.length > 0)
  const lastMessage = computed(() => messages.value[messages.value.length - 1] || null)

  // ============ Session Management ============
  async function initSession() {
    try {
      const res = await startAgentSession()
      if (res.code === API_STATUS.SUCCESS && res.data?.sessionId) {
        sessionId.value = res.data.sessionId
        return true
      }
      throw new Error(res.message || '初始化会话失败')
    } catch (err) {
      logger.error('[AgentSession] 初始化会话失败:', err)
      return false
    }
  }

  function clearSession() {
    sessionId.value = null
    messages.value = []
    clearStorage()
  }

  // ============ Message Operations ============
  function addMessage(message) {
    messages.value.push({
      ...message,
      timestamp: message.timestamp || Date.now()
    })
  }

  function updateMessage(id, updates) {
    const index = messages.value.findIndex(m => m.id === id)
    if (index >= 0) {
      messages.value[index] = { ...messages.value[index], ...updates }
    }
  }

  function removeMessage(id) {
    const index = messages.value.findIndex(m => m.id === id)
    if (index >= 0) {
      messages.value.splice(index, 1)
    }
  }

  function getMessageById(id) {
    return messages.value.find(m => m.id === id)
  }

  // ============ Storage Operations ============
  function loadFromStorage() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (!saved) {return false}

      const state = JSON.parse(saved)
      if (state.messages && Array.isArray(state.messages)) {
        messages.value = state.messages.map(msg => ({
          ...msg,
          isStreaming: false,
          isError: msg.isError || false
        }))
      }
      return true
    } catch (e) {
      logger.warn('[AgentSession] Failed to save state:', e)
      localStorage.removeItem(STORAGE_KEY)
      return false
    }
  }

  function saveToStorage() {
    try {
      const state = {
        messages: messages.value,
        savedAt: Date.now(),
        version: '2.0'
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    } catch (e) {
      logger.warn('[AgentSession] Failed to load state:', e)
    }
  }

  function clearStorage() {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch (e) {
      logger.warn('[AgentSession] Failed to clear storage:', e)
    }
  }

  // 自动保存 - 添加防抖避免频繁序列化
  let saveTimer = null
  const stopWatch = watch(messages, () => {
    if (saveTimer) {clearTimeout(saveTimer)}
    saveTimer = setTimeout(() => {
      if (messages.value.length > 0) {
        saveToStorage()
      }
    }, 300)
  }, { deep: true })

  function $dispose() {
    if (saveTimer) {clearTimeout(saveTimer); saveTimer = null}
    stopWatch()
  }

  // ============ Return ============
  return {
    // State
    sessionId,
    messages,
    // Getters
    hasMessages,
    lastMessage,
    // Methods
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
