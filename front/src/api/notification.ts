import request from '@/axios'
import logger from '@/utils/logger'
import type { ApiResponse } from '@/types/api'

export interface NotificationSettings {
  emailNotification: boolean
  pushNotification: boolean
  commentNotification: boolean
  likeNotification: boolean
}

// 获取通知列表
export function getNotificationList(page = 1, size = 20): Promise<ApiResponse<{ list: any[]; total: number }>> {
  return request.get('/notification/list', { params: { page, size } })
}

// 获取未读通知数量
export function getUnreadCount(): Promise<ApiResponse<{ count: number }>> {
  return request.get('/notification/unread-count')
}

// 标记通知为已读
export function markAsRead(id: number): Promise<ApiResponse<null>> {
  return request.put(`/notification/${id}/read`)
}

// 标记所有通知为已读
export function markAllAsRead(): Promise<ApiResponse<null>> {
  return request.put('/notification/read-all')
}

// 删除通知
export function deleteNotification(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/notification/${id}`)
}

// 批量删除通知
export function deleteNotifications(ids: number[]): Promise<ApiResponse<null>> {
  return request.delete('/notification/batch', { data: ids })
}

// 清空所有通知
export function clearAllNotifications(): Promise<ApiResponse<null>> {
  return request.delete('/notification/clear-all')
}

// 获取通知设置
export function getNotificationSettings(): Promise<ApiResponse<NotificationSettings>> {
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
  } as ApiResponse<NotificationSettings>)
}

// 更新通知设置
export function updateNotificationSettings(settings: NotificationSettings): Promise<ApiResponse<null>> {
  logger.warn('updateNotificationSettings: 后端接口暂未实现', settings)
  return Promise.resolve({ code: 200, message: 'success' } as ApiResponse<null>)
}

// 发送系统通知（管理员权限）
export function sendSystemNotification(data: { title: string; content: string; userId?: number }): Promise<ApiResponse<null>> {
  logger.warn('sendSystemNotification: 后端接口暂未实现', data)
  return Promise.resolve({ code: 200, message: 'success' } as ApiResponse<null>)
}