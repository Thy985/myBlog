import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// Mock the auth composable
vi.mock('@/composables/auth', () => ({
  getToken: vi.fn(() => null),
  removeToken: vi.fn()
}))

// Mock the API
vi.mock('@/api/auth', () => ({
  getUserInfo: vi.fn()
}))

// Mock logger
vi.mock('@/utils/logger', () => ({
  default: {
    error: vi.fn()
  }
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('isLoggedIn', () => {
    it('should return false when no token', () => {
      const store = useAuthStore()
      expect(store.isLoggedIn()).toBe(false)
    })

    it('should return false when token exists but no user data', () => {
      const { getToken } = vi.mocked('@/composables/auth')
      getToken.mockReturnValueOnce('fake-token')
      const store = useAuthStore()
      expect(store.isLoggedIn()).toBe(false)
    })
  })

  describe('hasValidCache', () => {
    it('should return false when no user data', () => {
      const store = useAuthStore()
      expect(store.hasValidCache()).toBe(false)
    })
  })

  describe('setUser', () => {
    it('should set user data', () => {
      const store = useAuthStore()
      const mockUser = { id: 1, username: 'test', avatar: 'url' }
      store.setUser(mockUser)
      expect(store.user).toEqual(mockUser)
    })

    it('should clear user when null passed', () => {
      const store = useAuthStore()
      store.setUser({ id: 1 })
      store.setUser(null)
      expect(store.user).toEqual({})
    })
  })

  describe('logout', () => {
    it('should clear user and token', () => {
      const { removeToken } = vi.mocked('@/composables/auth')
      const store = useAuthStore()
      store.setUser({ id: 1 })
      store.logout()
      expect(store.user).toEqual({})
      expect(store.token).toBeNull()
      expect(removeToken).toHaveBeenCalled()
    })
  })
})
