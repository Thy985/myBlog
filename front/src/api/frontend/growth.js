import request from '@/axios'

// ============================================================================
// Growth Cycle API
// ============================================================================

/**
 * 获取可用增长周期
 * @returns {Promise}
 */
export function getGrowthCycles() {
  return request.get('/growth/cycles')
}

// ============================================================================
// Growth Task API
// ============================================================================

/**
 * 调度增长任务
 * @param {Object} data - { cycle, delayMs, options }
 * @returns {Promise}
 */
export function scheduleGrowthTask(data) {
  return request.post('/growth/task/schedule', data)
}

/**
 * 立即执行增长任务
 * @param {Object} data - { cycle }
 * @returns {Promise}
 */
export function executeGrowthTask(data) {
  return request.post('/growth/task/execute', data)
}

/**
 * 获取用户任务列表
 * @returns {Promise}
 */
export function getGrowthTaskList() {
  return request.get('/growth/task/list')
}

/**
 * 获取任务结果
 * @param {string} taskId - 任务ID
 * @returns {Promise}
 */
export function getTaskResult(taskId) {
  return request.get(`/growth/task/${taskId}`)
}

/**
 * 取消任务
 * @param {string} taskId - 任务ID
 * @returns {Promise}
 */
export function cancelTask(taskId) {
  return request.delete(`/growth/task/${taskId}`)
}

// ============================================================================
// Growth Report API
// ============================================================================

/**
 * 获取增长报告
 * @param {string} period - 周期类型 daily/weekly/monthly
 * @returns {Promise}
 */
export function getGrowthReport(period = 'daily') {
  return request.get('/growth/report', { params: { period } })
}

/**
 * 发现机会
 * @returns {Promise}
 */
export function discoverOpportunities() {
  return request.get('/growth/opportunities')
}

// ============================================================================
// RAG API
// ============================================================================

/**
 * RAG 搜索
 * @param {Object} data - { query, topK }
 * @returns {Promise}
 */
export function ragSearch(data) {
  return request.get('/growth/rag/search', { params: data })
}

/**
 * RAG 内容生成
 * @param {Object} data - { topic, keywords }
 * @returns {Promise}
 */
export function ragGenerateContent(data) {
  return request.post('/growth/rag/content/generate', data)
}

/**
 * RAG 内容优化
 * @param {Object} data - { originalContent, optimizationGoal }
 * @returns {Promise}
 */
export function ragOptimizeContent(data) {
  return request.post('/growth/rag/content/optimize', data)
}

/**
 * RAG 主题推荐
 * @param {Object} data - { topic, limit }
 * @returns {Promise}
 */
export function ragSuggestTopics(data) {
  return request.get('/growth/rag/topics/suggest', { params: data })
}

// ============================================================================
// Content API
// ============================================================================

/**
 * 生成内容
 * @param {Object} data - { topic, tags, category }
 * @returns {Promise}
 */
export function generateContent(data) {
  return request.post('/growth/content/generate', data)
}

/**
 * 优化文章
 * @param {number} articleId - 文章ID
 * @param {Object} data - 优化参数
 * @returns {Promise}
 */
export function optimizeArticle(articleId, data) {
  return request.post(`/growth/article/${articleId}/optimize`, data)
}
