import request from '@/axios'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'

/**
 * 添加标签
 * @param {Object} data - 标签数据
 * @param {string} data.name - 标签名称
 * @param {string} [data.color] - 标签颜色
 * @returns {Promise}
 */
export function addTags(data) {
  // 后端接口: POST /api/tag?name=xxx&color=xxx
  return request.post('/tag', null, {
    params: {
      name: data.name,
      color: data.color
    }
  })
}

/**
 * 获取标签列表（分页）
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getTagPageList(params = {}) {
  // 后端接口: GET /api/tag/list
  return request.get('/tag/list', { params })
}

/**
 * 获取所有标签列表（不分页）
 * @returns {Promise}
 */
export function getTagList() {
  // 后端接口: GET /api/tag/list
  return request.get('/tag/list')
}

/**
 * 获取发现页标签列表
 * @returns {Promise}
 */
export function getDiscoverTagList() {
  // 后端接口: GET /api/tag/discover
  return request.get('/tag/discover')
}

/**
 * 获取当前用户标签列表
 * @returns {Promise}
 */
export function getUserTagList() {
  // 后端接口: GET /api/tag/user
  return request.get('/tag/user')
}

/**
 * 获取热门标签
 * @param {number} limit - 返回数量，默认20
 * @returns {Promise}
 */
export function getHotTags(limit = 20) {
  // 后端接口: GET /api/tag/hot
  return request.get('/tag/hot', { params: { limit } })
}

/**
 * 获取标签详情
 * @param {number} id - 标签ID
 * @returns {Promise}
 */
export function getTagById(id) {
  // 后端接口: GET /api/tag/{id}
  return request.get(`/tag/${id}`)
}

/**
 * 删除标签
 * @param {number} tagId - 标签ID
 * @returns {Promise}
 */
export function deleteTag(tagId) {
  // 后端接口: DELETE /api/tag/{id}
  return request.delete(`/tag/${tagId}`)
}

/**
 * 搜索标签
 * @param {string} key - 搜索关键词
 * @returns {Promise}
 */
export function selectTags(key) {
  // 后端暂无搜索接口，使用获取列表后前端过滤
  logger.warn('selectTags: 后端搜索接口暂未实现，使用前端过滤')
  return request.get('/tag/list').then(res => {
    if (res.code === API_STATUS.SUCCESS && res.data) {
      const list = Array.isArray(res.data) ? res.data : res.data.list || []
      const filtered = list.filter(tag =>
        tag.name && tag.name.toLowerCase().includes(key.toLowerCase())
      )
      return {
        code: 200,
        data: filtered,
        message: 'success'
      }
    }
    return res
  })
}

/**
 * 获取标签下拉选择列表
 * @returns {Promise}
 */
export function getTagSelect() {
  // 使用获取标签列表接口
  return request.get('/tag/list')
}

/**
 * 更新标签
 * @param {number} id - 标签ID
 * @param {Object} data - 标签数据
 * @param {string} data.name - 标签名称
 * @param {string} [data.color] - 标签颜色
 * @returns {Promise}
 */
export function updateTag(id, data) {
  // 后端接口: PUT /api/tag/{id}
  return request.put(`/tag/${id}`, null, {
    params: {
      name: data.name,
      color: data.color
    }
  })
}
