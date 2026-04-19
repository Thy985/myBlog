import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Agent UI Store - UI 层状态管理
 *
 * 职责：
 * 1. 管理 UI 状态（抽屉开关、未读计数等）
 * 2. 管理用户交互状态（选中、展开等）
 * 3. 管理配置选项
 */
export const useAgentUIStore = defineStore('agentUI', () => {
  // ============ State ============
  const isDrawerOpen = ref(false)
  const unreadCount = ref(0)
  const expandedPanels = ref(new Set())
  const selectedMessageId = ref(null)
  const inputText = ref('')

  // ============ Getters ============
  const hasUnread = computed(() => unreadCount.value > 0)

  const isPanelExpanded = (panelId) => expandedPanels.value.has(panelId)

  // ============ Drawer Operations ============
  function openDrawer() {
    isDrawerOpen.value = true
    unreadCount.value = 0
  }

  function closeDrawer() {
    isDrawerOpen.value = false
  }

  function toggleDrawer() {
    isDrawerOpen.value = !isDrawerOpen.value
    if (isDrawerOpen.value) {
      unreadCount.value = 0
    }
  }

  // ============ Unread Management ============
  function incrementUnread() {
    if (!isDrawerOpen.value) {
      unreadCount.value++
    }
  }

  function clearUnread() {
    unreadCount.value = 0
  }

  // ============ Panel Management ============
  function expandPanel(panelId) {
    expandedPanels.value.add(panelId)
  }

  function collapsePanel(panelId) {
    expandedPanels.value.delete(panelId)
  }

  function togglePanel(panelId) {
    if (expandedPanels.value.has(panelId)) {
      expandedPanels.value.delete(panelId)
    } else {
      expandedPanels.value.add(panelId)
    }
  }

  function clearExpandedPanels() {
    expandedPanels.value.clear()
  }

  // ============ Message Selection ============
  function selectMessage(messageId) {
    selectedMessageId.value = messageId
  }

  function clearSelection() {
    selectedMessageId.value = null
  }

  // ============ Input Management ============
  function setInputText(text) {
    inputText.value = text
  }

  function clearInput() {
    inputText.value = ''
  }

  // ============ Return ============
  return {
    // State
    isDrawerOpen,
    unreadCount,
    expandedPanels,
    selectedMessageId,
    inputText,
    // Getters
    hasUnread,
    isPanelExpanded,
    // Methods
    openDrawer,
    closeDrawer,
    toggleDrawer,
    incrementUnread,
    clearUnread,
    expandPanel,
    collapsePanel,
    togglePanel,
    clearExpandedPanels,
    selectMessage,
    clearSelection,
    setInputText,
    clearInput
  }
})
