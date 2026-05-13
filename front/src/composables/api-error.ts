import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'

const HTTP_ERROR_MAP: Record<number, string> = {
  400: '请求参数有误，请检查后重试',
  401: '登录已过期，请重新登录',
  403: '没有权限执行此操作',
  404: '请求的资源不存在',
  409: '数据冲突，请稍后重试',
  429: '操作过于频繁，请稍后重试',
  500: '服务器繁忙，请稍后重试',
  502: '服务器网关错误，请稍后重试',
  503: '服务暂时不可用，请稍后重试'
}

const TECHNICAL_ERROR_PATTERNS = [
  'NullPointerException', 'SQLException', 'IOException',
  'ClassNotFoundException', 'StackOverflowError',
  'OutOfMemoryError', 'InternalError'
]

function isTechnicalError(message?: string): boolean {
  if (!message) {return false}
  return TECHNICAL_ERROR_PATTERNS.some(p =>
    message.includes(p) || message.toLowerCase().includes(p.toLowerCase())
  )
}

function getFriendlyMessage(message?: string): string {
  if (!message) {return '操作失败，请稍后重试'}
  if (isTechnicalError(message)) {
    logger.warn(`技术错误已过滤: ${message}`)
    return '操作失败，请稍后重试'
  }
  return message.length > 80 ? '操作失败，请稍后重试' : message
}

interface ApiError {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
  request?: unknown
  message?: string
  code?: string
}

export function useApiError(customErrorMap: Record<number, string> = {}) {
  const errorMap = { ...HTTP_ERROR_MAP, ...customErrorMap }

  function handleHttpError(error: ApiError): string | null {
    const status = error.response?.status
    if (status === undefined) {return null}
    const message = errorMap[status] || '操作失败，请稍后重试'
    ElMessage.error({ message, duration: 4000 })
    logger.error(`HTTP ${status}:`, error)
    return message
  }

  function handleNetworkError(error: ApiError): string | null {
    if (error.request) {
      if (error.code === 'ECONNABORTED') {
        ElMessage.error({ message: '请求超时，请检查网络连接', duration: 4000 })
      } else {
        ElMessage.error({ message: '网络连接失败，请检查网络设置', duration: 4000 })
      }
      logger.error('Network Error:', error)
      return '网络连接失败'
    }
    return null
  }

  function handleApiError(error: ApiError, fallbackMessage = '操作失败，请稍后重试'): void {
    if (handleHttpError(error)) {return}
    if (handleNetworkError(error)) {return}
    const friendlyMessage = getFriendlyMessage(error.message)
    ElMessage.error({ message: friendlyMessage, duration: 4000 })
    logger.error('API Error:', error)
  }

  function handleBusinessError(response: { message?: string; data?: { message?: string } }, fallbackMessage = '操作失败'): string {
    const message = response?.message || response?.data?.message || fallbackMessage
    ElMessage.error({ message: getFriendlyMessage(message), duration: 4000 })
    return message
  }

  return {
    handleHttpError,
    handleNetworkError,
    handleApiError,
    handleBusinessError
  }
}