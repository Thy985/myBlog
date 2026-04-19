/**
 * UI状态管理
 * 管理侧边栏宽度、深色模式等UI相关状态
 */
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

import logger from '@/utils/logger'

const STORAGE_KEYS = {
  MENU_WIDTH: 'menu_width',
  DARK_MODE: 'darkMode',
  THEME_MODE: 'themeMode'
}

export const useUIStore = defineStore('ui', () => {
  const getStoredValue = (key, defaultValue) => {
    try {
      const stored = localStorage.getItem(key)
      return stored !== null ? stored : defaultValue
    } catch (e) {
      return defaultValue
    }
  }

  const menuWidth = ref(getStoredValue(STORAGE_KEYS.MENU_WIDTH, '250px'))
  const isDarkMode = ref(getStoredValue(STORAGE_KEYS.DARK_MODE, 'false') === 'true')
  const themeMode = ref(getStoredValue(STORAGE_KEYS.THEME_MODE, 'light'))

  let mediaQuery = null
  let systemThemeListener = null

  watch(menuWidth, (newValue) => {
    try {
      localStorage.setItem(STORAGE_KEYS.MENU_WIDTH, newValue)
    } catch (e) {
      logger.error('Failed to save menu width:', e)
    }
  })

  watch(isDarkMode, (newValue) => {
    try {
      localStorage.setItem(STORAGE_KEYS.DARK_MODE, String(newValue))
    } catch (e) {
      logger.error('Failed to save dark mode:', e)
    }
  })

  watch(themeMode, (newValue) => {
    try {
      localStorage.setItem(STORAGE_KEYS.THEME_MODE, newValue)
    } catch (e) {
      logger.error('Failed to save theme mode:', e)
    }
  })

  function handleMenuWidth() {
    menuWidth.value = menuWidth.value === '250px' ? '64px' : '250px'
  }

  function setMenuWidth(width) {
    menuWidth.value = width
  }

  function broadcastThemeChange(theme) {
    window.dispatchEvent(new CustomEvent('themeChange', {
      detail: { theme, isDark: theme === 'dark' }
    }))
  }

  function applyDarkMode() {
    if (isDarkMode.value) {
      document.documentElement.classList.add('dark')
      document.documentElement.classList.remove('light')
      broadcastThemeChange('dark')
    } else {
      document.documentElement.classList.remove('dark')
      document.documentElement.classList.add('light')
      broadcastThemeChange('light')
    }
  }

  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    applyDarkMode()
  }

  function setDarkMode(value) {
    isDarkMode.value = value
    applyDarkMode()
  }

  function syncSystemTheme() {
    if (!mediaQuery) {return}
    const isSystemDark = mediaQuery.matches
    if (themeMode.value === 'system') {
      isDarkMode.value = isSystemDark
      applyDarkMode()
    }
  }

  function initDarkMode() {
    if (mediaQuery) {
      mediaQuery.removeEventListener('change', syncSystemTheme)
    }
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemThemeListener = syncSystemTheme
    mediaQuery.addEventListener('change', syncSystemTheme)

    if (themeMode.value === 'system') {
      isDarkMode.value = mediaQuery.matches
    }
    applyDarkMode()
  }

  function setThemeMode(mode) {
    themeMode.value = mode
    if (mode === 'system') {
      syncSystemTheme()
    } else {
      isDarkMode.value = mode === 'dark'
      applyDarkMode()
    }
  }

  return {
    menuWidth,
    isDarkMode,
    themeMode,
    handleMenuWidth,
    setMenuWidth,
    toggleDarkMode,
    applyDarkMode,
    setDarkMode,
    initDarkMode,
    setThemeMode,
    broadcastThemeChange
  }
})
