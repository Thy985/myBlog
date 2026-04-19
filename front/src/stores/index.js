/**
 * 状态管理入口
 *
 * 已拆分为三个独立store:
 * - useAuthStore: 认证状态（用户信息、登录/登出）
 * - useSettingsStore: 设置状态（博客配置）
 * - useUIStore: UI状态（侧边栏、深色模式）
 *
 * 为保持向后兼容，useMainStore 仍可使用，但建议逐步迁移到独立store
 */

import { useAuthStore } from './auth'
import { useSettingsStore } from './settings'
import { useUIStore } from './ui'

export { useAuthStore, useSettingsStore, useUIStore }

/**
 * 向后兼容的 main store
 * 使用 Pinia storeToRefs 确保响应式，同时直接委托给各个 store
 */
import { defineStore } from 'pinia'
import { computed } from 'vue'

export const useMainStore = defineStore('main', () => {
  const authStore = useAuthStore()
  const settingsStore = useSettingsStore()
  const uiStore = useUIStore()

  const user = computed(() => authStore.user)
  const setting = computed(() => settingsStore.setting)
  const menuWidth = computed(() => uiStore.menuWidth)
  const isDarkMode = computed(() => uiStore.isDarkMode)

  function isLoggedIn() {
    return authStore.isLoggedIn()
  }

  async function getAdminInfo() {
    return authStore.getAdminInfo()
  }

  async function getBlogSetting() {
    return settingsStore.getBlogSetting()
  }

  function logout() {
    authStore.logout()
    settingsStore.clearSettingCache()
  }

  function handleMenuWidth() {
    uiStore.handleMenuWidth()
  }

  function toggleDarkMode() {
    uiStore.toggleDarkMode()
  }

  function applyDarkMode() {
    uiStore.applyDarkMode()
  }

  function initDarkMode() {
    uiStore.initDarkMode()
  }

  function hasValidCache() {
    return authStore.hasValidCache() && settingsStore.hasValidCache()
  }

  return {
    user,
    setting,
    menuWidth,
    isDarkMode,
    isLoggedIn,
    getAdminInfo,
    getBlogSetting,
    logout,
    handleMenuWidth,
    toggleDarkMode,
    applyDarkMode,
    initDarkMode,
    hasValidCache
  }
})
