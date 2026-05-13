import axios from 'axios'
import router from '@/router'
import { showMessage } from '@/utils'
import { clearAuthInfo, setRedirectUrl, getCsrfToken } from '@/composables/auth'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const TECHNICAL_ERROR_PATTERNS = [
  'NullPointerException',
  'SQLException',
  'IOException',
  'ClassNotFoundException',
  'ArrayIndexOutOfBoundsException',
  'ConcurrentModificationException',
  'StackOverflowError',
  'OutOfMemoryError',
  'InternalError',
  'ServiceException',
  'DaoException',
  'ServletException',
  'ReflectionException',
  'InvocationTargetException',
  'MalformedURLException',
  'ClassCastException',
  'IllegalArgumentException',
  'IndexOutOfBoundsException'
]

const TECHNICAL_MESSAGES = [
  '数据处理异常',
  '系统内部错误',
  '服务器处理异常',
  '数据访问异常',
  '业务处理异常'
]

function isTechnicalError(message) {
  if (!message) {return false}
  return TECHNICAL_ERROR_PATTERNS.some(pattern =>
    message.includes(pattern) || message.toLowerCase().includes(pattern.toLowerCase())
  )
}

function getUserFriendlyMessage(message) {
  if (!message) {return '操作失败，请稍后重试'}

  if (isTechnicalError(message)) {
    const randomMsg = TECHNICAL_MESSAGES[Math.floor(Math.random() * TECHNICAL_MESSAGES.length)]
    logger.warn(`技术错误已过滤: ${message} -> ${randomMsg}`)
    return randomMsg
  }

  if (message.length > 100) {
    return '操作失败，请稍后重试'
  }

  return message
}

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
      } else if (code === API_STATUS.SUCCESS) {
      } else {
        const friendlyMessage = getUserFriendlyMessage(message)
        showMessage(friendlyMessage, 'error')
      }
    }
  } else {
    if (error.code === 'ECONNABORTED') {
      showMessage('请求超时，请检查网络连接', 'error')
    } else if (error.code === 'ERR_NETWORK') {
      showMessage('网络连接失败，请检查网络设置', 'error')
    } else {
      showMessage('网络错误，请稍后重试', 'error')
    }
  }

  return Promise.reject(error)
})

export function invalidateTokenCache() {
  cachedCsrfToken = null
}

export default instance
