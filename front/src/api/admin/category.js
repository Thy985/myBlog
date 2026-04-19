import request from '@/axios'

/**
 * 添加分类
 * @param {Object} data - 分类数据
 * @param {string} data.name - 分类名称
 * @param {string} [data.description] - 分类描述
 * @returns {Promise}
 */
export function addCategory(data) {
  // 后端接口: POST /api/category
  return request.post('/category', data)
}

/**
 * 获取分类列表（分页）
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getCategoryPageList(params = {}) {
  // 后端接口: GET /api/category/list
  return request.get('/category/list', { params })
}

/**
 * 获取所有分类列表（不分页）
 * @returns {Promise}
 */
export function getCategoryList() {
  // 后端接口: GET /api/category/list
  return request.get('/category/list')
}

/**
 * 获取发现页分类列表
 * @returns {Promise}
 */
export function getDiscoverCategoryList() {
  // 后端接口: GET /api/category/discover
  return request.get('/category/discover')
}

/**
 * 获取当前用户分类列表
 * @returns {Promise}
 */
export function getUserCategoryList() {
  // 后端接口: GET /api/category/user
  return request.get('/category/user')
}

/**
 * 获取分类详情
 * @param {number} id - 分类ID
 * @returns {Promise}
 */
export function getCategoryById(id) {
  // 后端接口: GET /api/category/{id}
  return request.get(`/category/${id}`)
}

/**
 * 删除分类
 * @param {number} categoryId - 分类ID
 * @returns {Promise}
 */
export function deleteCategory(categoryId) {
  // 后端接口: DELETE /api/category/{id}
  return request.delete(`/category/${categoryId}`)
}

/**
 * 获取分类下拉选择列表
 * @returns {Promise}
 */
export function getCategorySelect() {
  // 使用获取分类列表接口
  return request.get('/category/list')
}

/**
 * 更新分类
 * @param {number} id - 分类ID
 * @param {Object} data - 分类数据
 * @returns {Promise}
 */
export function updateCategory(id, data) {
  // 后端接口: PUT /api/category/{id}
  return request.put(`/category/${id}`, data)
}

// 为了保持向后兼容，保留旧的方法名
export const getCategoryPageListLegacy = getCategoryPageList
