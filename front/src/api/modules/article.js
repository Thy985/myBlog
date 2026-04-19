import request from '@/axios'
import { transformArticleList } from '@/utils/transform'

/**
 * 获取文章列表
 * @param {Object} params - 查询参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页数量
 * @returns {Promise}
 */
export function getArticles(params) {
  return request.get('/article/list', { params })
}

/**
 * 获取热门文章
 * @param {number} [limit=10] - 返回数量
 * @returns {Promise}
 */
export function getHotArticles(limit = 10) {
  return request.get('/article/hot', { params: { limit } }).then(res => {
    if (res && res.data) {
      res.data = transformArticleList(res.data)
    }
    return res
  })
}

/**
 * 获取推荐文章列表（用于轮播图）
 * @param {Object} params - 查询参数
 * @param {number} params.limit - 限制数量，默认5
 * @returns {Promise}
 */
export function getRecommendedArticles(params = {}) {
  return request.get('/article/hot', { params: { limit: params.limit || 5 } }).then(res => {
    if (res && res.data) {
      res.data = transformArticleList(res.data)
    }
    return res
  })
}

/**
 * 获取相关文章推荐
 * @param {number} articleId - 文章ID
 * @param {number} [limit=5] - 返回数量
 * @returns {Promise}
 */
export function getRelatedArticles(articleId, limit = 5) {
  return request.get('/article/related', { params: { articleId, limit } }).then(res => {
    if (res && res.data) {
      res.data = transformArticleList(res.data)
    }
    return res
  })
}

/**
 * 获取文章详情
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function getArticle(id) {
  return request.get(`/article/${id}`)
}

/**
 * 搜索文章
 * @param {Object} params - 查询参数
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @returns {Promise}
 */
export function searchArticles(params) {
  return request.get('/article/search', { params })
}

/**
 * 创建文章
 * @param {Object} data - 文章数据
 * @returns {Promise}
 */
export function createArticle(data) {
  return request.post('/article', data)
}

/**
 * 更新文章
 * @param {number} id - 文章ID
 * @param {Object} data - 文章数据
 * @returns {Promise}
 */
export function updateArticle(id, data) {
  return request.put(`/article/${id}`, data)
}

/**
 * 删除文章
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function deleteArticle(id) {
  return request.delete(`/article/${id}`)
}

/**
 * 发布文章（将草稿变为正式文章）
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function publishArticleById(id) {
  return request.post(`/article/${id}/publish`)
}

/**
 * 下线文章
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function offlineArticle(id) {
  return request.post(`/article/${id}/offline`)
}

/**
 * 置顶/取消置顶文章
 * @param {number} id - 文章ID
 * @param {boolean} isTop - 是否置顶
 * @returns {Promise}
 */
export function topArticle(id, isTop) {
  return request.post(`/article/${id}/top`, null, { params: { isTop } })
}

/**
 * 点赞文章
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function likeArticle(id) {
  return request.post(`/article/${id}/like`)
}

/**
 * 取消点赞文章
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function unlikeArticle(id) {
  return request.delete(`/article/${id}/like`)
}

/**
 * 收藏文章
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function collectArticle(id) {
  return request.post(`/article/${id}/collect`)
}

/**
 * 取消收藏文章
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function uncollectArticle(id) {
  return request.delete(`/article/${id}/collect`)
}

/**
 * 获取用户收藏的文章列表
 * @param {Object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=10] - 每页数量
 * @returns {Promise}
 */
export function getUserCollects(params = {}) {
  return request.get('/article/user/collects', { params })
}

/**
 * 获取文章归档
 * @param {Object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=12] - 每页数量
 * @returns {Promise}
 */
export function getArticleArchive(params = {}) {
  return request.get('/article/archive', { params })
}

/**
 * 获取用户文章归档
 * @param {Object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=12] - 每页数量
 * @returns {Promise}
 */
export function getUserArticleArchive(params = {}) {
  return request.get('/article/user/archive', { params })
}

/**
 * 更新文章阅读量
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function updateReadNum(id) {
  return request.post(`/article/${id}/read`)
}

/**
 * 获取文章阅读统计
 * @param {number} id - 文章ID
 * @returns {Promise}
 */
export function getArticleReadStats(id) {
  return request.get(`/article/${id}/stats`)
}

/**
 * 获取用户的文章列表
 * @param {number} userId - 用户ID
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getUserArticles(userId, params = {}) {
  return request.get(`/article/user/${userId}`, { params })
}
