import request from '@/axios'

/**
 * 管理员登录
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise}
 */
export function login(username, password) {
  // 使用 AuthController 的登录接口
  return request.post('/auth/login', { username, password })
}

/**
 * 获取当前登录管理员信息
 * @returns {Promise}
 */
export function getAdminInfo() {
  // 使用 AuthController 的获取用户信息接口
  return request.get('/auth/info')
}

/**
 * 更新管理员密码
 * @param {Object} data - 密码数据
 * @param {string} data.oldPassword - 旧密码
 * @param {string} data.newPassword - 新密码
 * @returns {Promise}
 */
export function updateAdminPassword(data) {
  // 使用 UserController 的更新密码接口
  return request.put('/user/password', data)
}

/**
 * 获取用户列表（管理员权限）
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getUserList(params = {}) {
  return request.get('/admin/users', { params })
}

/**
 * 创建用户（管理员权限）
 * @param {Object} data - 用户数据
 * @returns {Promise}
 */
export function createUser(data) {
  return request.post('/admin/users', data)
}

/**
 * 更新用户状态（管理员权限）
 * @param {number} id - 用户ID
 * @param {string} status - 状态
 * @returns {Promise}
 */
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

/**
 * 删除用户（管理员权限）
 * @param {number} id - 用户ID
 * @returns {Promise}
 */
export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}

/**
 * 分配用户角色（管理员权限）
 * @param {number} id - 用户ID
 * @param {Array<number>} roleIds - 角色ID列表
 * @returns {Promise}
 */
export function assignRoles(id, roleIds) {
  return request.put(`/admin/users/${id}/roles`, roleIds)
}
