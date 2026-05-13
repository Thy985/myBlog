import request from '@/axios'
import type { ApiResponse } from '@/types/api'
import type { UserProfile, PasswordChangeForm, UserProfileForm } from '@/types/user'
import type { Category, Tag } from '@/types/article'

export interface LoginHistory {
  id: number
  device: string
  ipAddress: string
  loginAt: string
  status: 'active' | 'expired'
}

// 更新用户个人资料
export function updateProfile(data: UserProfileForm): Promise<ApiResponse<UserProfile>> {
  return request.put('/user/profile', data)
}

// 修改密码
export function changePassword(data: PasswordChangeForm): Promise<ApiResponse<null>> {
  return request.put('/user/password', data)
}

// 更新隐私设置
export function updatePrivacy(data: { privacyLevel: string }): Promise<ApiResponse<null>> {
  return request.put('/user/privacy', data)
}

// 上传头像
export function uploadAvatar(formData: FormData): Promise<ApiResponse<{ fileUrl: string }>> {
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取登录历史
export function getLoginHistory(limit: number = 20): Promise<ApiResponse<LoginHistory[]>> {
  return request.get('/auth/login-history', { params: { limit } })
}

// 启用/禁用 MFA
export function updateMfaSetting(data: { mfaEnabled: boolean; mfaType: string; mfaSecret?: string }): Promise<ApiResponse<null>> {
  return request.put('/user/mfa', data)
}

// 验证 MFA 验证码
export function verifyMfaCode(data: { code: string; mfaType: string }): Promise<ApiResponse<boolean>> {
  return request.post('/user/mfa/verify', data)
}

// 生成备用验证码
export function generateBackupCodes(): Promise<ApiResponse<{ codes: string[] }>> {
  return request.post('/user/mfa/backup-codes')
}

// 测试 AI 连接
export function testAiConnection(data: { provider: string; apiKey: string; baseUrl?: string; model?: string }): Promise<ApiResponse<{ success: boolean; valid: boolean; message: string }>> {
  return request.post('/user/apikey/test', data)
}

// 保存 AI 配置
export function saveAiConfig(data: { provider: string; apiKey: string; baseUrl?: string; model?: string }): Promise<ApiResponse<null>> {
  return request.put('/user/apikey', data)
}

export interface UserArticleParams {
  page?: number
  size?: number
  keyword?: string
  status?: string
}

export interface UserCommentParams {
  page?: number
  size?: number
}

export interface UserCategoryParams {
  page?: number
  size?: number
}

export interface UserTagParams {
  page?: number
  size?: number
}

export interface UserMediaParams {
  page?: number
  size?: number
  type?: string
}

export interface MediaFile {
  id: number
  name: string
  url: string
  type: string
  size: number
  createdAt: string
}

export interface UserCategory {
  id: number
  name: string
  description: string
  articleCount: number
  createdAt: string
}

export interface UserTag {
  id: number
  name: string
  description: string
  articleCount: number
  createdAt: string
}

export interface UserComment {
  id: number
  articleId: number
  articleTitle: string
  content: string
  createdAt: string
  status: string
}

export interface UserArticle {
  id: number
  title: string
  category: string
  createdAt: string
  status: string
}

export function getUserArticleList(userId: number, params?: UserArticleParams): Promise<ApiResponse<{ list: UserArticle[]; total: number; page: number; size: number }>> {
  return request.get(`/article/user/${userId}`, { params })
}

export function getUserCategoryList(): Promise<ApiResponse<Category[]>> {
  return request.get('/category/user')
}

export function createCategory(data: { name: string; description?: string }): Promise<ApiResponse<Category>> {
  return request.post('/category', data)
}

export function updateCategory(id: number, data: { name: string; description?: string }): Promise<ApiResponse<Category>> {
  return request.put(`/category/${id}`, data)
}

export function deleteCategory(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/category/${id}`)
}

export function getUserTagList(): Promise<ApiResponse<Tag[]>> {
  return request.get('/tag/user')
}

export function createTag(data: { name: string; description?: string }): Promise<ApiResponse<Tag>> {
  return request.post('/tag', data)
}

export function updateTag(id: number, data: { name: string; description?: string }): Promise<ApiResponse<Tag>> {
  return request.put(`/tag/${id}`, data)
}

export function deleteTag(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/tag/${id}`)
}

export function getUserCommentList(params?: UserCommentParams): Promise<ApiResponse<{ list: UserComment[]; total: number; page: number; size: number }>> {
  return request.get('/comment/user', { params })
}

// 获取用户统计信息（GET /user/stats）- 需要登录
export interface UserStats {
  articleCount: number
  commentCount: number
  likeCount: number
}
export function getUserStats(): Promise<ApiResponse<UserStats>> {
  return request.get('/user/stats')
}

// 获取用户最近活动（GET /user/activities）- 需要登录
export interface UserActivity {
  type: string
  description: string
  createdAt: string
  articleId?: number
  commentId?: number
}
export function getUserActivities(): Promise<ApiResponse<UserActivity[]>> {
  return request.get('/user/activities')
}

export function updateComment(id: number, data: { content: string }): Promise<ApiResponse<UserComment>> {
  return request.put(`/comment/${id}`, data)
}

export function getUserMediaList(params?: UserMediaParams): Promise<ApiResponse<{ list: MediaFile[]; total: number; page: number; size: number }>> {
  return request.get('/file/list', { params })
}

export function deleteMedia(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/file/${id}`)
}