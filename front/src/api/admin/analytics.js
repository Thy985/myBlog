import request from '@/axios'
import logger from '@/utils/logger'

/**
 * 获取统计概览
 * @returns {Promise}
 */
export function getAnalyticsOverview() {
  return request.get('/analytics/overview')
}

/**
 * 获取趋势数据
 * @param {string} startDate - 开始日期 (YYYY-MM-DD)
 * @param {string} endDate - 结束日期 (YYYY-MM-DD)
 * @returns {Promise}
 */
export function getTrendData(startDate, endDate) {
  return request.get('/analytics/trend', { params: { startDate, endDate } })
}

/**
 * 获取访问趋势（兼容旧接口，自动计算日期范围）
 * @param {number} days - 天数，默认7天
 * @returns {Promise}
 */
export function getVisitTrend(days = 7) {
  const endDate = new Date().toISOString().split('T')[0]
  const startDate = new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  return getTrendData(startDate, endDate)
}

/**
 * 获取热门文章
 * @param {number} limit - 返回数量，默认10条
 * @returns {Promise}
 */
export function getHotArticles(limit = 10) {
  return request.get('/analytics/hot-articles', { params: { limit } })
}

/**
 * 获取文章统计
 * @param {number} articleId - 文章ID
 * @param {number} _days - 天数，默认30天（预留参数）
 * @returns {Promise}
 */
// eslint-disable-next-line no-unused-vars
export function getArticleStats(articleId, _days = 30) {
  // 后端暂无此接口，返回模拟数据
  logger.warn('getArticleStats: 后端接口暂未实现')
  return Promise.resolve({
    code: 200,
    data: {
      articleId,
      views: 0,
      likes: 0,
      comments: 0
    },
    message: 'success'
  })
}

/**
 * 获取用户活动统计
 * @param {number} _days - 天数，默认7天（预留参数）
 * @returns {Promise}
 */
// eslint-disable-next-line no-unused-vars
export function getUserActivity(_days = 7) {
  // 后端暂无此接口，返回模拟数据
  logger.warn('getUserActivity: 后端接口暂未实现')
  return Promise.resolve({
    code: 200,
    data: [],
    message: 'success'
  })
}

/**
 * 获取来源分析
 * @param {number} _days - 天数，默认7天（预留参数）
 * @returns {Promise}
 */
// eslint-disable-next-line no-unused-vars
export function getSourceAnalysis(_days = 7) {
  // 后端暂无此接口，使用 /analytics/source 替代
  return request.get('/analytics/source')
}

/**
 * 获取设备分析
 * @param {number} _days - 天数，默认7天（预留参数）
 * @returns {Promise}
 */
// eslint-disable-next-line no-unused-vars
export function getDeviceAnalysis(_days = 7) {
  // 后端暂无此接口，使用 /analytics/device 替代
  return request.get('/analytics/device')
}

/**
 * 记录页面访问
 * @param {string} pageUrl - 页面URL
 * @returns {Promise}
 */
export function recordPageView(pageUrl) {
  return request.post('/analytics/pv', null, { params: { pageUrl } })
}

/**
 * 上报性能指标
 * @param {Object} data - 性能数据
 * @returns {Promise}
 */
export function recordPerformance(data) {
  return request.post('/analytics/performance', data)
}

/**
 * 上报性能错误
 * @param {Object} data - 错误数据
 * @returns {Promise}
 */
export function recordPerformanceErrors(data) {
  return request.post('/analytics/performance/errors', data)
}

/**
 * 获取实时统计
 * @returns {Promise}
 */
export function getRealtimeStats() {
  return request.get('/analytics/realtime')
}
