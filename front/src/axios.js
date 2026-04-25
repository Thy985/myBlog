import axios from 'axios'
import router from '@/router'
import { showMessage } from '@/utils'
import { getToken, setToken, setRefreshToken, clearAuthInfo, setRedirectUrl, getCsrfToken } from '@/composables/auth'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const instance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 15000,
  withCredentials: true
})

let isRefreshing = false
let refreshSubscribers = []
let refreshPromise = null

let cachedToken = null
let cachedCsrfToken = null

function updateCachedTokens() {
  cachedToken = getToken()
  cachedCsrfToken = getCsrfToken()
}

function onRefreshTokenComplete(newToken, newRefreshToken) {
  setToken(newToken)
  setRefreshToken(newRefreshToken)
  updateCachedTokens()
  refreshSubscribers.forEach(callback => callback(newToken))
  refreshSubscribers = []
  isRefreshing = false
  refreshPromise = null
}

function onRefreshTokenFailed(error) {
  refreshSubscribers.forEach(callback => callback(Promise.reject(error)))
  refreshSubscribers = []
  isRefreshing = false
  refreshPromise = null
}

function addRefreshSubscriber(callback) {
  refreshSubscribers.push(callback)
}

function refreshToken() {
  if (isRefreshing && refreshPromise) {
    return new Promise((resolve, reject) => {
      addRefreshSubscriber(token => {
        if (token instanceof Error || token instanceof Promise) {
          reject(token)
        } else {
          resolve(token)
        }
      })
    })
  }

  isRefreshing = true

  refreshPromise = instance.post('/auth/refresh', {})
    .then(response => {
      const { token, refreshToken: newRefreshToken } = response.data
      onRefreshTokenComplete(token, newRefreshToken)
      return token
    })
    .catch(error => {
      logger.error('Token刷新失败:', error.message)
      clearAuthInfo()

      const currentPath = window.location.pathname + window.location.search
      if (currentPath && !currentPath.includes('/login') && !currentPath.includes('/admin/login')) {
        setRedirectUrl(currentPath)
        logger.info('已保存当前路径，登录后将自动返回:', currentPath)
      }

      if (!window.location.pathname.includes('/login')) {
        showMessage('登录已过期，请重新登录', 'warning')
        setTimeout(() => {
          router.push('/login')
        }, 1000)
      }

      onRefreshTokenFailed(error)
      return Promise.reject(error)
    })

  return refreshPromise
}

instance.interceptors.request.use((config) => {
  if (!cachedToken) {
    updateCachedTokens()
  }

  if (cachedToken) {
    config.headers.Authorization = `Bearer ${cachedToken}`
  }

  if (cachedCsrfToken && ['post', 'put', 'patch', 'delete'].includes(config.method)) {
    config.headers['X-XSRF-TOKEN'] = cachedCsrfToken
  }

  return config
}, (error) => {
  return Promise.reject(error)
})

instance.interceptors.response.use((response) => {
  return response.data
}, (error) => {
  const status = error.response?.status
  const config = error.config

  if (status === API_STATUS.UNAUTHORIZED && !config._isRetry) {
    config._isRetry = true
    return refreshToken().then(() => {
      updateCachedTokens()
      config.headers.Authorization = `Bearer ${cachedToken}`
      return instance(config)
    }).catch(() => {
      return Promise.reject(error)
    })
  }

  if (error.response?.data) {
    const { code, message } = error.response.data
    if (code !== API_STATUS.SUCCESS) {
      showMessage(message || '请求失败', 'error')
    }
  } else {
    showMessage('网络错误，请稍后重试', 'error')
  }

  return Promise.reject(error)
})

export function invalidateTokenCache() {
  cachedToken = null
  cachedCsrfToken = null
}

export default instance
