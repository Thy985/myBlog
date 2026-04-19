import request from '@/axios'

/**
 * 获取仪表盘概览统计数据
 * @returns {Promise}
 */
export function getDashboardOverview() {
  return request.get('/admin/dashboard/stats/overview')
}

/**
 * 获取文章发布趋势
 * @param {number} days - 时间范围天数，默认7天
 * @returns {Promise}
 */
export function getArticleTrend(days = 7) {
  return request.get('/admin/dashboard/stats/article-trend', { params: { days } })
}

/**
 * 获取用户增长趋势
 * @param {number} days - 时间范围天数，默认7天
 * @returns {Promise}
 */
export function getUserTrend(days = 7) {
  return request.get('/admin/dashboard/stats/user-trend', { params: { days } })
}

/**
 * 获取访问量趋势
 * @param {number} days - 时间范围天数，默认7天
 * @returns {Promise}
 */
export function getVisitTrend(days = 7) {
  return request.get('/admin/dashboard/stats/visit-trend', { params: { days } })
}

/**
 * 获取分类分布统计
 * @returns {Promise}
 */
export function getCategoryDistribution() {
  return request.get('/admin/dashboard/stats/category-distribution')
}

/**
 * 获取标签分布统计
 * @returns {Promise}
 */
export function getTagDistribution() {
  return request.get('/admin/dashboard/stats/tag-distribution')
}

/**
 * 获取热门文章排行
 * @param {number} limit - 返回数量，默认10条
 * @returns {Promise}
 */
export function getHotArticles(limit = 10) {
  return request.get('/admin/dashboard/stats/hot-articles', { params: { limit } })
}

/**
 * 获取活跃用户排行
 * @param {number} limit - 返回数量，默认10条
 * @returns {Promise}
 */
export function getActiveUsers(limit = 10) {
  return request.get('/admin/dashboard/stats/active-users', { params: { limit } })
}

/**
 * 获取待审核内容统计
 * @returns {Promise}
 */
export function getPendingReviewStats() {
  return request.get('/admin/dashboard/stats/pending-review')
}

/**
 * 获取系统运行状态
 * @returns {Promise}
 */
export function getSystemStatus() {
  return request.get('/admin/dashboard/stats/system-status')
}

/**
 * 获取评论趋势
 * @param {number} days - 时间范围天数，默认7天
 * @returns {Promise}
 */
export function getCommentTrend(days = 7) {
  return request.get('/admin/dashboard/stats/comment-trend', { params: { days } })
}

/**
 * 获取用户角色分布
 * @returns {Promise}
 */
export function getUserRoleDistribution() {
  return request.get('/admin/dashboard/stats/user-role-distribution')
}

// 为了保持向后兼容，保留旧的方法名，但指向新的接口
export const getDashboardArticleStatisticsInfo = getArticleTrend
export const getDashboardPublishArticleStatisticsInfo = getArticleTrend
export const getDashboardPVStatisticsInfo = getVisitTrend
