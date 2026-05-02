import axios from 'axios'
import router from '@/router'
import { showMessage } from '@/utils'
import { clearAuthInfo, setRedirectUrl, getCsrfToken } from '@/composables/auth'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const instance = axios.create({
  baseURL: import.meta.env.DEV ? '/api' : import.meta.env.VITE_APP_BASE_API,
  timeout: 15000,
  withCredentials: true
})

let isRefreshing = false
let refreshSubscribers = []
let refreshPromise = null

let cachedCsrfToken = null

function updateCachedCsrfToken() {
  cachedCsrfToken = getCsrfToken()
}

function onRefreshTokenComplete() {
  refreshSubscribers.forEach(callback => callback())
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

async function clearAuthStoreCache() {
  try {
    const { useAuthStore } = await import('@/stores')
    const authStore = useAuthStore()
    authStore.invalidateAndLogout?.() || authStore.clearCache?.()
  } catch (e) {
    // store 可能未初始化，忽略
  }
}

function refreshToken() {
  if (isRefreshing && refreshPromise) {
    return new Promise((resolve) => {
      addRefreshSubscriber(() => {
        resolve()
      })
    })
  }

  isRefreshing = true

  refreshPromise = instance.post('/auth/refresh', {})
    .then(() => {
      onRefreshTokenComplete()
    })
    .catch(async error => {
      logger.error('Token刷新失败:', error.message)
      clearAuthInfo()
      await clearAuthStoreCache()

      const currentPath = window.location.pathname + window.location.search
      if (currentPath && !currentPath.includes('/login')) {
        setRedirectUrl(currentPath)
        logger.info('已保存当前路径，登录后将自动返回:', currentPath)
      }

      if (!window.location.pathname.includes('/login')) {
        import('element-plus').then(({ ElMessageBox }) => {
          ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            router.push('/login')
          }).catch(() => {
          })
        })
      }

      onRefreshTokenFailed(error)
      return Promise.reject(error)
    })

  return refreshPromise
}

instance.interceptors.request.use((config) => {
  if (!cachedCsrfToken) {
    updateCachedCsrfToken()
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
      return instance(config)
    }).catch(() => {
      return Promise.reject(error)
    })
  }

  if (error.response?.data) {
    const { code, message } = error.response.data
    if (code !== API_STATUS.SUCCESS) {
      if (code === 429) {
        // 限流错误不显示消息，由调用者处理
      } else if (code === API_STATUS.SUCCESS) {
        // 成功不显示
      } else {
        showMessage(message || '请求失败', 'error')
      }
    }
  } else {
    showMessage('网络错误，请稍后重试', 'error')
  }

  return Promise.reject(error)
})

export function invalidateTokenCache() {
  cachedCsrfToken = null
}

export default instance
