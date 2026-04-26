import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'

const HTTP_ERROR_MAP: Record<number, string> = {
  400: '请求参数有误，请检查后重试',
  401: '身份验证失败，请重新登录',
  403: '没有权限执行此操作',
  404: '请求的资源不存在',
  409: '数据冲突，请检查后重试',
  429: '操作过于频繁，请稍后重试',
  500: '服务器内部错误，请稍后重试',
  502: '服务器网关错误，请稍后重试',
  503: '服务暂时不可用，请稍后重试'
}

interface ApiError {
  response?: {
    status?: number
  }
  request?: unknown
  message?: string
}

interface BusinessResponse {
  message?: string
  data?: {
    message?: string
  }
}

export function useApiError(customErrorMap: Record<number, string> = {}) {
  const errorMap = { ...HTTP_ERROR_MAP, ...customErrorMap }

  function handleHttpError(error: ApiError): string | null {
    const status = error.response?.status
    if (status === undefined) {return null}
    const message = errorMap[status] || '操作失败，请稍后重试'
    ElMessage.error(message)
    logger.error(`HTTP ${status}:`, error)
    return message
  }

  function handleNetworkError(error: ApiError): string | null {
    if (error.request) {
      ElMessage.error('网络连接失败，请检查网络设置')
      logger.error('Network Error:', error)
      return '网络连接失败'
    }
    return null
  }

  function handleApiError(error: ApiError, fallbackMessage = '操作失败，请稍后重试'): void {
    if (handleHttpError(error)) {return}
    if (handleNetworkError(error)) {return}
    ElMessage.error(error.message || fallbackMessage)
    logger.error('API Error:', error)
  }

  function handleBusinessError(response: BusinessResponse, fallbackMessage = '操作失败'): string {
    const message = response?.message || response?.data?.message || fallbackMessage
    ElMessage.error(message)
    return message
  }

  return {
    handleHttpError,
    handleNetworkError,
    handleApiError,
    handleBusinessError
  }
}