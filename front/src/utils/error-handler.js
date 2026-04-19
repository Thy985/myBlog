/**
 * 全局错误处理
 */

import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'

const errorLogs = []
const MAX_ERROR_LOGS = 100
const ERROR_REPORT_QUEUE_SIZE = 10
let errorReportQueue = []
let isProcessingQueue = false

export const ErrorType = {
  VUE_ERROR: 'VUE_ERROR',
  PROMISE_ERROR: 'PROMISE_ERROR',
  RESOURCE_ERROR: 'RESOURCE_ERROR',
  NETWORK_ERROR: 'NETWORK_ERROR',
  SCRIPT_ERROR: 'SCRIPT_ERROR'
}

function formatError(error, type, info = '') {
  return {
    type,
    message: error?.message || String(error),
    stack: error?.stack || '',
    info,
    url: window.location.href,
    userAgent: navigator.userAgent,
    timestamp: new Date().toISOString(),
    userId: getAnonymousUserId()
  }
}

function getAnonymousUserId() {
  try {
    let anonymousId = localStorage.getItem('anonymous_user_id')
    if (!anonymousId) {
      anonymousId = 'anon_' + Date.now().toString(36) + Math.random().toString(36).substr(2, 9)
      localStorage.setItem('anonymous_user_id', anonymousId)
    }
    return anonymousId
  } catch {
    return 'unknown'
  }
}

async function reportError(errorInfo) {
  if (import.meta.env.DEV) {
    logger.error('[Error Report]', errorInfo)
    return
  }

  errorLogs.push(errorInfo)
  if (errorLogs.length > MAX_ERROR_LOGS) {
    errorLogs.shift()
  }

  errorReportQueue.push(errorInfo)
  if (errorReportQueue.length >= ERROR_REPORT_QUEUE_SIZE) {
    await flushErrorQueue()
  } else if (!isProcessingQueue) {
    setTimeout(() => flushErrorQueue(), 5000)
  }
}

async function flushErrorQueue() {
  if (isProcessingQueue || errorReportQueue.length === 0) {
    return
  }

  isProcessingQueue = true
  const queueToReport = [...errorReportQueue]
  errorReportQueue = []

  try {
    const payload = {
      errors: queueToReport,
      appVersion: import.meta.env.VITE_APP_VERSION || '1.0.0',
      buildTime: import.meta.env.VITE_BUILD_TIME || ''
    }

    if (navigator.sendBeacon) {
      const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' })
      navigator.sendBeacon('/api/error/report', blob)
    } else {
      await fetch('/api/error/report', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        keepalive: true
      })
    }
  } catch (err) {
    logger.error('[Error Report Failed]', err)
    errorReportQueue = [...queueToReport, ...errorReportQueue]
  } finally {
    isProcessingQueue = false
  }
}

export function setupVueErrorHandler(app) {
  app.config.errorHandler = (err, instance, info) => {
    logger.error('[Vue Error]', err, info)

    const errorInfo = formatError(err, ErrorType.VUE_ERROR, info)
    reportError(errorInfo)

    ElMessage.error({
      message: '系统错误，请稍后重试',
      duration: 3000
    })
  }

  app.config.warnHandler = (msg, instance, trace) => {
    if (import.meta.env.DEV) {
      logger.warn('[Vue Warning]', msg, trace)
    }
  }
}

export function setupPromiseErrorHandler() {
  window.addEventListener('unhandledrejection', event => {
    const error = event.reason
    logger.error('[Unhandled Promise Rejection]', error)

    const errorInfo = formatError(
      error,
      ErrorType.PROMISE_ERROR,
      'Unhandled Promise Rejection'
    )
    reportError(errorInfo)

    event.preventDefault()
  })
}

export function setupResourceErrorHandler() {
  window.addEventListener(
    'error',
    event => {
      if (event.target && (event.target.src || event.target.href)) {
        logger.error('[Resource Error]', event.target)

        const errorInfo = formatError(
          new Error(`Resource load failed: ${event.target.src || event.target.href}`),
          ErrorType.RESOURCE_ERROR,
          event.target.tagName
        )
        reportError(errorInfo)
      } else {
        logger.error('[Script Error]', event.error)

        const errorInfo = formatError(
          event.error || new Error(event.message),
          ErrorType.SCRIPT_ERROR,
          `${event.filename}:${event.lineno}:${event.colno}`
        )
        reportError(errorInfo)
      }
    },
    true
  )
}

export function setupNetworkErrorHandler() {
  window.addEventListener('online', () => {
    ElMessage.success('网络已连接')
  })

  window.addEventListener('offline', () => {
    ElMessage.error('网络已断开，请检查网络连接')
  })
}

export function getErrorLogs() {
  return [...errorLogs]
}

export function clearErrorLogs() {
  errorLogs.length = 0
}

export function setupGlobalErrorHandler(app) {
  setupVueErrorHandler(app)
  setupPromiseErrorHandler()
  setupResourceErrorHandler()
  setupNetworkErrorHandler()

  window.addEventListener('beforeunload', () => {
    flushErrorQueue()
  })

  logger.debug('[Global Error Handler] Initialized')
}
