import { useAuthStore } from './auth'
import { useSettingsStore } from './settings'
import { useUIStore } from './ui'
import { defineStore } from 'pinia'
import { computed } from 'vue'

export { useAuthStore, useSettingsStore, useUIStore }

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

  function getAdminInfo() {
    return authStore.getAdminInfo()
  }

  function getBlogSetting() {
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