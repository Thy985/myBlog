import request from '@/axios'

/**
 * 更新用户个人资料
 * @param {Object} data - { username, email, bio, avatar }
 * @returns {Promise}
 */
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

/**
 * 修改密码
 * @param {Object} data - { oldPassword, newPassword }
 * @returns {Promise}
 */
export function changePassword(data) {
  return request.put('/user/password', data)
}

/**
 * 更新隐私设置
 * @param {Object} data - 隐私设置数据
 * @returns {Promise}
 */
export function updatePrivacy(data) {
  return request.put('/user/privacy', data)
}

/**
 * 上传头像
 * @param {FormData} formData - 包含 avatar 字段的 FormData
 * @returns {Promise}
 */
export function uploadAvatar(formData) {
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 启用/禁用 MFA
 * @param {Object} data - { mfaEnabled, mfaType, mfaSecret }
 * @returns {Promise}
 */
export function updateMfaSetting(data) {
  return request.put('/user/mfa', data)
}

/**
 * 验证 MFA 验证码（用于启用 MFA 时的验证）
 * @param {Object} data - { code, mfaType }
 * @returns {Promise}
 */
export function verifyMfaCode(data) {
  return request.post('/user/mfa/verify', data)
}

/**
 * 生成备用验证码
 * @returns {Promise}
 */
export function generateBackupCodes() {
  return request.post('/user/mfa/backup-codes')
}

/**
 * 测试 AI 连接
 * @param {Object} data - AI 配置数据
 * @returns {Promise}
 */
export function testAiConnection(data) {
  return request.post('/user/apikey/test', data)
}

/**
 * 保存 AI 配置
 * @param {Object} data - AI 配置数据
 * @returns {Promise}
 */
export function saveAiConfig(data) {
  return request.put('/user/apikey', data)
}

export function getUserArticleList(userId, params = {}) {
  return request.get(`/article/user/${userId}`, { params })
}

export function getUserCategoryList() {
  return request.get('/category/user')
}

export function createCategory(data) {
  return request.post('/category', data)
}

export function updateCategory(id, data) {
  return request.put(`/category/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/category/${id}`)
}

export function getUserTagList() {
  return request.get('/tag/user')
}

export function createTag(data) {
  return request.post('/tag', data)
}

export function updateTag(id, data) {
  return request.put(`/tag/${id}`, data)
}

export function deleteTag(id) {
  return request.delete(`/tag/${id}`)
}

export function getUserCommentList(params = {}) {
  return request.get('/comment/user', { params })
}

export function updateComment(id, data) {
  return request.put(`/comment/${id}`, data)
}

export function getUserMediaList(params = {}) {
  return request.get('/file/list', { params })
}

export function deleteMedia(id) {
  return request.delete(`/file/${id}`)
}
