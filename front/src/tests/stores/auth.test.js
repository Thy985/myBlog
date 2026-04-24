import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

const mockGetToken = vi.fn(() => null)
const mockRemoveToken = vi.fn()

vi.mock('@/composables/auth', () => ({
  getToken: (...args) => mockGetToken(...args),
  removeToken: (...args) => mockRemoveToken(...args)
}))

vi.mock('@/api/auth', () => ({
  getUserInfo: vi.fn()
}))

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
    mockGetToken.mockClear()
    mockRemoveToken.mockClear()
    mockGetToken.mockReturnValue(null)
  })

  describe('isLoggedIn', () => {
    it('should return false when no token', () => {
      const store = useAuthStore()
      expect(store.isLoggedIn()).toBe(false)
    })

    it('should return false when token exists but no user data', () => {
      mockGetToken.mockReturnValueOnce('fake-token')
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
      const store = useAuthStore()
      store.setUser({ id: 1 })
      store.logout()
      expect(store.user).toEqual({})
      expect(store.token).toBeNull()
      expect(mockRemoveToken).toHaveBeenCalled()
    })
  })
})
