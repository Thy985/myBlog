/**
 * 性能监控工具
 * 用于监控API调用响应时间和页面性能指标
 */
import logger from './logger'

// 性能监控数据
const performanceData = {
  apiCalls: {},
  pageLoad: {}
}

/**
 * 开始监控API调用
 * @param {string} apiName - API名称
 * @returns {string} - 监控ID
 */
export function startApiMonitor(apiName) {
  const monitorId = `${apiName}_${Date.now()}`
  performanceData.apiCalls[monitorId] = {
    name: apiName,
    startTime: performance.now(),
    endTime: null,
    duration: null,
    status: 'pending'
  }
  return monitorId
}

/**
 * 结束监控API调用
 * @param {string} monitorId - 监控ID
 * @param {boolean} success - 是否成功
 */
export function endApiMonitor(monitorId, success = true) {
  if (performanceData.apiCalls[monitorId]) {
    const data = performanceData.apiCalls[monitorId]
    data.endTime = performance.now()
    data.duration = data.endTime - data.startTime
    data.status = success ? 'success' : 'error'
    
    // 记录到控制台
    logger.debug(`[API Performance] ${data.name}: ${data.duration.toFixed(2)}ms (${data.status})`)
    
    // 如果响应时间过长，发出警告
    if (data.duration > 1000) {
      logger.warn(`[API Performance Warning] ${data.name} response time is too long: ${data.duration.toFixed(2)}ms`)
    }
  }
}

/**
 * 监控页面加载性能
 */
export function monitorPageLoad() {
  if (performance && performance.timing) {
    const timing = performance.timing
    const loadTime = timing.loadEventEnd - timing.navigationStart
    const domReadyTime = timing.domContentLoadedEventEnd - timing.navigationStart
    const firstPaint = performance.getEntriesByType('paint')[0]?.startTime || 0
    
    performanceData.pageLoad = {
      loadTime,
      domReadyTime,
      firstPaint
    }
    
    logger.debug('[Page Performance]', {
      loadTime: `${loadTime.toFixed(2)}ms`,
      domReadyTime: `${domReadyTime.toFixed(2)}ms`,
      firstPaint: `${firstPaint.toFixed(2)}ms`
    })
  }
}

/**
 * 获取性能监控数据
 * @returns {object} - 性能监控数据
 */
export function getPerformanceData() {
  return { ...performanceData }
}

/**
 * 清空性能监控数据
 */
export function clearPerformanceData() {
  performanceData.apiCalls = {}
  performanceData.pageLoad = {}
}

/**
 * 为API请求添加性能监控的包装函数
 * @param {Function} apiFn - API函数
 * @param {string} apiName - API名称
 * @returns {Function} - 包装后的API函数
 */
export function withPerformanceMonitor(apiFn, apiName) {
  return async function (...args) {
    const monitorId = startApiMonitor(apiName)
    try {
      const result = await apiFn(...args)
      endApiMonitor(monitorId, true)
      return result
    } catch (error) {
      endApiMonitor(monitorId, false)
      throw error
    }
  }
}
