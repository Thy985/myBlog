import { ElMessage, ElLoading } from 'element-plus'
import { showPageLoading, hidePageLoading } from '@/utils/loading'
import logger from '@/utils/logger'

const CACHE_PREFIX = 'blog_cache_'

export const API_STATUS = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  TOO_MANY_REQUESTS: 429,
  SERVER_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503
} as const

export const API_MESSAGE = {
  SUCCESS: '操作成功',
  BAD_REQUEST: '请求参数有误，请检查后重试',
  UNAUTHORIZED: '身份验证失败，请重新登录',
  FORBIDDEN: '没有权限执行此操作',
  NOT_FOUND: '请求的资源不存在',
  CONFLICT: '数据冲突，请检查后重试',
  TOO_MANY_REQUESTS: '操作过于频繁，请稍后重试',
  SERVER_ERROR: '服务器内部错误，请稍后重试',
  BAD_GATEWAY: '服务器网关错误，请稍后重试',
  SERVICE_UNAVAILABLE: '服务暂时不可用，请稍后重试',
  NETWORK_ERROR: '网络连接失败，请检查网络设置',
  DEFAULT_ERROR: '操作失败，请稍后重试'
} as const

export interface RequestOptions {
  showError?: boolean
  showSuccess?: boolean
  successMessage?: string
  showLoading?: boolean
  loadingMessage?: string
  showPageLoading?: boolean
  enableRetry?: boolean
}

const MAX_RETRIES = 2
const RETRY_DELAY = 1000

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

async function retryRequest<T>(apiFunc: () => Promise<T>, params: unknown, retries = 0): Promise<T> {
  try {
    return await apiFunc()
  } catch (err: any) {
    if (retries < MAX_RETRIES && isRetryableError(err)) {
      logger.warn(`请求失败，${RETRY_DELAY * (retries + 1)}ms 后重试...`)
      await delay(RETRY_DELAY * (retries + 1))
      return retryRequest(apiFunc, params, retries + 1)
    }
    throw err
  }
}

function isRetryableError(err: any): boolean {
  if (!err.response) {
    return true
  }
  const status = err.response.status
  return status === API_STATUS.SERVER_ERROR ||
         status === API_STATUS.BAD_GATEWAY ||
         status === API_STATUS.SERVICE_UNAVAILABLE
}

export async function request<T>(apiFunc: () => Promise<T>, params?: unknown, options: RequestOptions = {}): Promise<T> {
  const {
    showError = true,
    showSuccess = false,
    successMessage = API_MESSAGE.SUCCESS,
    showLoading = false,
    loadingMessage = '加载中...',
    showPageLoading: shouldShowPageLoading = false,
    enableRetry = false
  } = options

  let loadingInstance: ReturnType<typeof ElLoading.service> | null = null

  try {
    if (shouldShowPageLoading) {
      showPageLoading()
    } else if (showLoading) {
      loadingInstance = ElLoading.service({
        lock: true,
        text: loadingMessage,
        background: 'rgba(0, 0, 0, 0.7)'
      })
    }

    const apiCall = () => apiFunc()
    const res = enableRetry
      ? await retryRequest(apiCall, params)
      : await apiCall()

    if (res && (res as any).code === API_STATUS.SUCCESS) {
      if (showSuccess) {
        ElMessage.success(successMessage)
      }
      return res
    } else {
      logger.error('API请求失败：响应格式错误', res)
      if (showError) {
        ElMessage.warning((res as any)?.message || API_MESSAGE.DEFAULT_ERROR)
      }
      throw new Error((res as any)?.message || API_MESSAGE.DEFAULT_ERROR)
    }
  } catch (err: any) {
    logger.error('API请求出错:', err)
    if (showError) {
      ElMessage.error(err.message || API_MESSAGE.NETWORK_ERROR)
    }
    throw err
  } finally {
    if (shouldShowPageLoading) {
      hidePageLoading()
    } else if (loadingInstance) {
      loadingInstance.close()
    }
  }
}

function safeJsonParse(str: string): Record<string, unknown> | null {
  try {
    return JSON.parse(str)
  } catch (e: any) {
    logger.error('JSON解析失败:', e.message)
    return null
  }
}

interface CacheOptions extends RequestOptions {
  cacheDuration?: number
}

export async function requestWithCache<T>(
  apiFunc: () => Promise<T>,
  cacheKey: string,
  params: unknown = {},
  options: CacheOptions = {}
): Promise<T> {
  const { cacheDuration = 5 * 60 * 1000 } = options
  const prefixedKey = `${CACHE_PREFIX}${cacheKey}`
  const cacheTimeKey = `${prefixedKey}_time`

  try {
    const cachedData = localStorage.getItem(prefixedKey)
    const cacheTime = localStorage.getItem(cacheTimeKey)

    if (cachedData && cacheTime) {
      const isExpired = (Date.now() - parseInt(cacheTime, 10)) > cacheDuration
      if (!isExpired) {
        logger.debug(`Cache hit for key: ${cacheKey}`)
        const parsed = safeJsonParse(cachedData)
        if (parsed) {
          return parsed as T
        }
      } else {
        logger.debug(`Cache expired for key: ${cacheKey}`)
        localStorage.removeItem(prefixedKey)
        localStorage.removeItem(cacheTimeKey)
      }
    }

    const res = await request(apiFunc, params, options)

    if (res && (res as any).data) {
      try {
        localStorage.setItem(prefixedKey, JSON.stringify(res))
        localStorage.setItem(cacheTimeKey, Date.now().toString())
      } catch (e: any) {
        logger.warn('缓存写入失败，可能超出存储限制:', e.message)
      }
    }

    return res
  } catch (err: any) {
    logger.error('带缓存的API请求出错:', err)
    const cachedData = localStorage.getItem(prefixedKey)
    if (cachedData) {
      logger.warn(`Returning expired cache for key: ${cacheKey} due to request failure`)
      const parsed = safeJsonParse(cachedData)
      if (parsed) {
        return parsed as T
      }
    }
    throw err
  }
}

export function clearCache(cacheKey: string): void {
  const prefixedKey = `${CACHE_PREFIX}${cacheKey}`
  localStorage.removeItem(prefixedKey)
  localStorage.removeItem(`${prefixedKey}_time`)
}

export function clearAllCache(): void {
  const appKeys = [
    'blog_cache_theme',
    'blog_cache_searchHistory',
    'blog_cache_blogSetting',
    'blog_cache_agentSessionState',
    'blog_cache_article_drafts',
    'blog_cache_current_draft'
  ]

  const keysToRemove: string[] = []

  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && (
      key.startsWith(CACHE_PREFIX) ||
      appKeys.includes(key) ||
      key.endsWith('_time')
    )) {
      keysToRemove.push(key)
    }
  }

  keysToRemove.forEach(key => localStorage.removeItem(key))
  logger.debug(`已清除 ${keysToRemove.length} 个缓存项`)
}