/**
 * 认证状态管理
 * 管理用户登录状态、用户信息
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo as saGetUserInfo } from '@/api/auth'
import { removeToken, getToken } from '@/composables/auth'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const CACHE_DURATION = 5 * 60 * 1000

export const useAuthStore = defineStore('auth', () => {
  const user = ref({})
  const token = ref(getToken())
  let lastFetchTime = 0
  let fetchPromise = null

  const isLoggedIn = () => !!getToken() && Object.keys(user.value).length > 0

  function hasValidCache() {
    return Object.keys(user.value).length > 0 && (Date.now() - lastFetchTime) < CACHE_DURATION
  }

  async function getAdminInfo(forceRefresh = false) {
    if (!forceRefresh && hasValidCache()) {
      return user.value
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

  async function _doGetAdminInfo() {
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

  function logout() {
    try {
      removeToken()
      user.value = {}
      token.value = null
      lastFetchTime = 0
    } catch (err) {
      logger.error('登出失败:', err.message)
    }
  }

  function setUser(userData) {
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
