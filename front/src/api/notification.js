import request from '@/axios'
import logger from '@/utils/logger'

/**
 * 获取通知列表
 * @param {number} page - 页码，默认1
 * @param {number} size - 每页数量，默认20（后端使用size而不是pageSize）
 * @returns {Promise}
 */
export function getNotificationList(page = 1, size = 20) {
  // 后端接口: GET /api/notification/list
  // 注意：后端使用 'size' 参数名，不是 'pageSize'
  return request.get('/notification/list', { params: { page, size } })
}

/**
 * 获取未读通知数量
 * @returns {Promise}
 */
export function getUnreadCount() {
  // 后端接口: GET /api/notification/unread-count
  return request.get('/notification/unread-count')
}

/**
 * 标记通知为已读
 * @param {number} id - 通知ID
 * @returns {Promise}
 */
export function markAsRead(id) {
  // 后端接口: PUT /api/notification/{id}/read
  return request.put(`/notification/${id}/read`)
}

/**
 * 标记所有通知为已读
 * @returns {Promise}
 */
export function markAllAsRead() {
  // 后端接口: PUT /api/notification/read-all
  return request.put('/notification/read-all')
}

/**
 * 删除通知
 * @param {number} id - 通知ID
 * @returns {Promise}
 */
export function deleteNotification(id) {
  // 后端接口: DELETE /api/notification/{id}
  return request.delete(`/notification/${id}`)
}

/**
 * 批量删除通知
 * @param {Array<number>} ids - 通知ID列表
 * @returns {Promise}
 */
export function deleteNotifications(ids) {
  // 后端接口: DELETE /api/notification/batch
  return request.delete('/notification/batch', { data: ids })
}

/**
 * 清空所有通知
 * @returns {Promise}
 */
export function clearAllNotifications() {
  // 后端接口: DELETE /api/notification/clear-all
  return request.delete('/notification/clear-all')
}

/**
 * 获取通知设置
 * @returns {Promise}
 */
export function getNotificationSettings() {
  // 后端暂无此接口
  logger.warn('getNotificationSettings: 后端接口暂未实现')
  return Promise.resolve({
    code: 200,
    data: {
      emailNotification: true,
      pushNotification: true,
      commentNotification: true,
      likeNotification: true
    },
    message: 'success'
  })
}

/**
 * 更新通知设置
 * @param {Object} settings - 设置对象
 * @returns {Promise}
 */
export function updateNotificationSettings(settings) {
  // 后端暂无此接口
  logger.warn('updateNotificationSettings: 后端接口暂未实现', settings)
  return Promise.resolve({ code: 200, message: 'success' })
}

/**
 * 发送系统通知（管理员权限）
 * @param {Object} data - 通知数据
 * @returns {Promise}
 */
export function sendSystemNotification(data) {
  // 后端暂无此接口
  logger.warn('sendSystemNotification: 后端接口暂未实现', data)
  return Promise.resolve({ code: 200, message: 'success' })
}
