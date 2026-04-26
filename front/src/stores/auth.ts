import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserProfile } from '@/types/user'
import { getUserInfo as saGetUserInfo } from '@/api/auth'
import { removeToken, getToken } from '@/composables/auth'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const CACHE_DURATION = 5 * 60 * 1000

export interface AuthState {
  user: UserProfile | Record<string, never>
  token: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | Record<string, never>>({})
  const token = ref<string | null>(getToken())
  let lastFetchTime = 0
  let fetchPromise: Promise<UserProfile | undefined> | null = null

  function isLoggedIn(): boolean {
    return !!getToken() && Object.keys(user.value).length > 0
  }

  function hasValidCache(): boolean {
    return Object.keys(user.value).length > 0 && (Date.now() - lastFetchTime) < CACHE_DURATION
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
      token.value = null
      lastFetchTime = 0
    } catch (err: any) {
      logger.error('登出失败:', err.message)
    }
  }

  function setUser(userData: UserProfile | Record<string, never>): void {
    user.value = userData || {}
  }

  return {
    user,
    token,
    isLoggedIn,
    hasValidCache,
    getAdminInfo,
    logout,
    setUser
  }
})