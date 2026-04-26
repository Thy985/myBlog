import request from '@/axios'
import type { ApiResponse } from '@/types/api'
import type { UserProfile, PasswordChangeForm, UserProfileForm } from '@/types/user'

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
export function uploadAvatar(formData: FormData): Promise<ApiResponse<{ avatar: string }>> {
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
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
export function testAiConnection(data: { provider: string; apiKey: string }): Promise<ApiResponse<{ success: boolean }>> {
  return request.post('/ai/test-connection', data)
}

// 保存 AI 配置
export function saveAiConfig(data: { provider: string; apiKey: string; model?: string }): Promise<ApiResponse<null>> {
  return request.put('/ai/config', data)
}