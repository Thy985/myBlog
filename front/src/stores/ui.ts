import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import logger from '@/utils/logger'

const STORAGE_KEYS = {
  MENU_WIDTH: 'menu_width',
  DARK_MODE: 'darkMode',
  THEME_MODE: 'themeMode'
}

type ThemeMode = 'light' | 'dark' | 'system'

export const useUIStore = defineStore('ui', () => {
  const getStoredValue = (key: string, defaultValue: string): string => {
    try {
      const stored = localStorage.getItem(key)
      return stored !== null ? stored : defaultValue
    } catch (e) {
      return defaultValue
    }
  }

  const menuWidth = ref<string>(getStoredValue(STORAGE_KEYS.MENU_WIDTH, '250px'))
  const isDarkMode = ref<boolean>(getStoredValue(STORAGE_KEYS.DARK_MODE, 'false') === 'true')
  const themeMode = ref<ThemeMode>(getStoredValue(STORAGE_KEYS.THEME_MODE, 'light') as ThemeMode)

  let mediaQuery: MediaQueryList | null = null
  let systemThemeListener: ((e: MediaQueryListEvent) => void) | null = null

  watch(menuWidth, (newValue: string) => {
    try {
      localStorage.setItem(STORAGE_KEYS.MENU_WIDTH, newValue)
    } catch (e: any) {
      logger.error('Failed to save menu width:', e)
    }
  })

  watch(isDarkMode, (newValue: boolean) => {
    try {
      localStorage.setItem(STORAGE_KEYS.DARK_MODE, String(newValue))
    } catch (e: any) {
      logger.error('Failed to save dark mode:', e)
    }
  })

  watch(themeMode, (newValue: ThemeMode) => {
    try {
      localStorage.setItem(STORAGE_KEYS.THEME_MODE, newValue)
    } catch (e: any) {
      logger.error('Failed to save theme mode:', e)
    }
  })

  function handleMenuWidth(): void {
    menuWidth.value = menuWidth.value === '250px' ? '64px' : '250px'
  }

  function setMenuWidth(width: string): void {
    menuWidth.value = width
  }

  function broadcastThemeChange(theme: string): void {
    window.dispatchEvent(new CustomEvent('themeChange', {
      detail: { theme, isDark: theme === 'dark' }
    }))
  }

  function applyDarkMode(): void {
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

  function toggleDarkMode(): void {
    isDarkMode.value = !isDarkMode.value
    applyDarkMode()
  }

  function setDarkMode(value: boolean): void {
    isDarkMode.value = value
    applyDarkMode()
  }

  function syncSystemTheme(): void {
    if (!mediaQuery) {return}
    const isSystemDark = mediaQuery.matches
    if (themeMode.value === 'system') {
      isDarkMode.value = isSystemDark
      applyDarkMode()
    }
  }

  function initDarkMode(): void {
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

  function setThemeMode(mode: ThemeMode): void {
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