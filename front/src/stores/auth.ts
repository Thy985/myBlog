import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'
import type { UserProfile } from '@/types/user'
import { getUserInfo as saGetUserInfo } from '@/api/auth'
import { removeToken } from '@/composables/auth'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const CACHE_DURATION = 5 * 60 * 1000

type AuthChangeListener = (isLoggedIn: boolean, user: UserProfile | null) => void

export interface AuthState {
  user: UserProfile | Record<string, never>
  token: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const user = shallowRef<UserProfile | Record<string, never>>({})
  const isAuthenticated = ref(false)
  let lastFetchTime = 0
  let fetchPromise: Promise<UserProfile | undefined> | null = null
  const listeners = new Set<AuthChangeListener>()

  function isLoggedIn(): boolean {
    return isAuthenticated.value
  }

  function hasValidCache(): boolean {
    if (Object.keys(user.value).length === 0) {
      return false
    }
    if (lastFetchTime === 0) {
      return true
    }
    return (Date.now() - lastFetchTime) < CACHE_DURATION
  }

  function addAuthChangeListener(listener: AuthChangeListener): void {
    listeners.add(listener)
  }

  function removeAuthChangeListener(listener: AuthChangeListener): void {
    listeners.delete(listener)
  }

  function notifyAuthChange(isLoggedIn: boolean): void {
    const currentUser = isLoggedIn ? user.value as UserProfile : null
    listeners.forEach(listener => {
      try {
        listener(isLoggedIn, currentUser)
      } catch (err) {
        logger.error('Auth listener error:', err)
      }
    })
  }

  async function getAdminInfo(forceRefresh = false): Promise<UserProfile | undefined> {
    if (!forceRefresh && hasValidCache()) {
      return user.value as UserProfile
    }
    if (fetchPromise && !forceRefresh) {
      return fetchPromise
    }
    try {
      fetchPromise = _doGetAdminInfo()
      const result = await fetchPromise
      return result
    } finally {
      fetchPromise = null
    }
  }

  async function _doGetAdminInfo(): Promise<UserProfile | undefined> {
    const res = await saGetUserInfo()
    if (res && res.code === API_STATUS.SUCCESS && res.data) {
      user.value = res.data
      lastFetchTime = Date.now()
      isAuthenticated.value = true
      notifyAuthChange(true)
      return res.data
    } else {
      logger.error('获取用户信息失败：响应格式错误')
      throw new Error('获取用户信息失败：响应格式错误')
    }
  }

  function logout(): void {
    try {
      removeToken()
      user.value = {}
      isAuthenticated.value = false
      lastFetchTime = 0
      notifyAuthChange(false)
    } catch (err: any) {
      logger.error('登出失败:', err.message)
    }
  }

  function setUser(userData: UserProfile | Record<string, never>): void {
    user.value = userData || {}
    isAuthenticated.value = Object.keys(userData || {}).length > 0
    notifyAuthChange(isAuthenticated.value)
  }

  function clearCache(): void {
    user.value = {}
    isAuthenticated.value = false
    lastFetchTime = 0
    notifyAuthChange(false)
  }

  function invalidateAndLogout(): void {
    clearCache()
    removeToken()
  }

  return {
    user,
    isAuthenticated,
    isLoggedIn,
    hasValidCache,
    getAdminInfo,
    logout,
    setUser,
    clearCache,
    invalidateAndLogout,
    addAuthChangeListener,
    removeAuthChangeListener
  }
})