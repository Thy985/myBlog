import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAgentUIStore = defineStore('agentUI', () => {
  const isDrawerOpen = ref(false)
  const unreadCount = ref(0)
  const expandedPanels = ref(new Set<string>())
  const selectedMessageId = ref<string | null>(null)
  const inputText = ref('')

  const hasUnread = computed(() => unreadCount.value > 0)

  function isPanelExpanded(panelId: string): boolean {
    return expandedPanels.value.has(panelId)
  }

  function openDrawer(): void {
    isDrawerOpen.value = true
    unreadCount.value = 0
  }

  function closeDrawer(): void {
    isDrawerOpen.value = false
  }

  function toggleDrawer(): void {
    isDrawerOpen.value = !isDrawerOpen.value
    if (isDrawerOpen.value) {
      unreadCount.value = 0
    }
  }

  function incrementUnread(): void {
    if (!isDrawerOpen.value) {
      unreadCount.value++
    }
  }

  function clearUnread(): void {
    unreadCount.value = 0
  }

  function expandPanel(panelId: string): void {
    expandedPanels.value.add(panelId)
  }

  function collapsePanel(panelId: string): void {
    expandedPanels.value.delete(panelId)
  }

  function togglePanel(panelId: string): void {
    if (expandedPanels.value.has(panelId)) {
      expandedPanels.value.delete(panelId)
    } else {
      expandedPanels.value.add(panelId)
    }
  }

  function clearExpandedPanels(): void {
    expandedPanels.value.clear()
  }

  function selectMessage(messageId: string): void {
    selectedMessageId.value = messageId
  }

  function clearSelection(): void {
    selectedMessageId.value = null
  }

  function setInputText(text: string): void {
    inputText.value = text
  }

  function clearInput(): void {
    inputText.value = ''
  }

  return {
    isDrawerOpen,
    unreadCount,
    expandedPanels,
    selectedMessageId,
    inputText,
    hasUnread,
    isPanelExpanded,
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