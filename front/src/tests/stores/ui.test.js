import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUIStore } from '@/stores/ui'

// Mock logger
vi.mock('@/utils/logger', () => ({
  default: {
    error: vi.fn()
  }
}))

// Mock document.classList
const mockClassList = {
  add: vi.fn(),
  remove: vi.fn()
}

// Mock window.dispatchEvent
const mockDispatchEvent = vi.fn()

describe('useUIStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()

    // Reset document.classList mock
    mockClassList.add.mockClear()
    mockClassList.remove.mockClear()
    mockDispatchEvent.mockClear()

    // Setup document mock
    Object.defineProperty(document, 'documentElement', {
      value: {
        classList: mockClassList
      },
      writable: true
    })

    global.window = {
      ...global.window,
      dispatchEvent: mockDispatchEvent,
      matchMedia: vi.fn(() => ({
        matches: false,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn()
      }))
    }
  })

  describe('initial state', () => {
    it('should have default menu width', () => {
      const store = useUIStore()
      expect(store.menuWidth).toBe('250px')
    })

    it('should have default dark mode as false', () => {
      const store = useUIStore()
      expect(store.isDarkMode).toBe(false)
    })

    it('should have default theme mode as light', () => {
      const store = useUIStore()
      expect(store.themeMode).toBe('light')
    })
  })

  describe('handleMenuWidth', () => {
    it('should toggle menu width between 250px and 64px', () => {
      const store = useUIStore()
      expect(store.menuWidth).toBe('250px')

      store.handleMenuWidth()
      expect(store.menuWidth).toBe('64px')

      store.handleMenuWidth()
      expect(store.menuWidth).toBe('250px')
    })
  })

  describe('setMenuWidth', () => {
    it('should set custom menu width', () => {
      const store = useUIStore()
      store.setMenuWidth('300px')
      expect(store.menuWidth).toBe('300px')
    })
  })

  describe('toggleDarkMode', () => {
    it('should toggle dark mode', () => {
      const store = useUIStore()
      expect(store.isDarkMode).toBe(false)

      store.toggleDarkMode()
      expect(store.isDarkMode).toBe(true)

      store.toggleDarkMode()
      expect(store.isDarkMode).toBe(false)
    })
  })

  describe('setDarkMode', () => {
    it('should set dark mode to specific value', () => {
      const store = useUIStore()
      store.setDarkMode(true)
      expect(store.isDarkMode).toBe(true)

      store.setDarkMode(false)
      expect(store.isDarkMode).toBe(false)
    })
  })

  describe('applyDarkMode', () => {
    it('should add dark class when dark mode enabled', () => {
      const store = useUIStore()
      store.isDarkMode = true
      store.applyDarkMode()
      expect(mockClassList.add).toHaveBeenCalledWith('dark')
      expect(mockClassList.remove).toHaveBeenCalledWith('light')
    })

    it('should remove dark class when dark mode disabled', () => {
      const store = useUIStore()
      store.isDarkMode = false
      store.applyDarkMode()
      expect(mockClassList.remove).toHaveBeenCalledWith('dark')
      expect(mockClassList.add).toHaveBeenCalledWith('light')
    })
  })

  describe('setThemeMode', () => {
    it('should set theme mode to dark', () => {
      const store = useUIStore()
      store.setThemeMode('dark')
      expect(store.themeMode).toBe('dark')
      expect(store.isDarkMode).toBe(true)
    })

    it('should set theme mode to light', () => {
      const store = useUIStore()
      store.setThemeMode('light')
      expect(store.themeMode).toBe('light')
      expect(store.isDarkMode).toBe(false)
    })
  })
})
