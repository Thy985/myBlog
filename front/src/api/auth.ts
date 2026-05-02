import request from '@/axios'
import type {
  ApiResponse,
  LoginForm,
  RegisterForm
} from '@/types/api'
import type { UserProfile } from '@/types/user'

// ==================== 认证相关接口 ====================

export interface LoginData {
  username: string
  password: string
  captcha?: string
  captchaId?: string
  mfaCode?: string
}

export interface CaptchaResponse {
  image: string
  captchaId: string
}

export interface VerificationCodeData {
  email: string
  type: 'register' | 'reset' | 'bind'
}

export interface MfaSetupResponse {
  secret: string
  qrCodeUrl: string
}

export interface MfaVerifyData {
  code: string
}

export interface MfaStatusResponse {
  enabled: boolean
  secret?: string
}

// 登录接口 - 后端: POST /api/auth/login
export function login(data: LoginData): Promise<ApiResponse<{ token: string; userId: number; roles: string[] }>> {
  return request.post('/auth/login', data)
}

// 登出接口 - 后端: POST /api/auth/logout
export function logout(): Promise<ApiResponse<null>> {
  return request.post('/auth/logout')
}

// 刷新Token - 后端: POST /api/auth/refresh
export function refreshToken(): Promise<ApiResponse<{ token: string }>> {
  return request.post('/auth/refresh')
}

// 获取用户信息 - 后端: GET /api/auth/info
export function getUserInfo(): Promise<ApiResponse<UserProfile>> {
  return request.get('/auth/info')
}

// ==================== 用户注册接口 ====================

// 注册接口 - 后端: POST /api/user/register
export function register(data: RegisterForm): Promise<ApiResponse<{ userId: number }>> {
  return request.post('/user/register', data)
}

// ==================== 验证码接口 ====================

// 获取验证码图片 (base64) - 后端: GET /api/captcha/base64
export function getCaptcha(): Promise<ApiResponse<CaptchaResponse>> {
  return request.get('/captcha/base64')
}

// 验证验证码 - 后端: GET /api/captcha/verify
export function verifyCaptcha(code: string): Promise<ApiResponse<boolean>> {
  return request.get('/captcha/verify', { params: { code } })
}

// ==================== 邮箱验证码接口 ====================

// 发送验证码 - 后端: POST /api/verification/send-code
export function sendVerificationCode(data: VerificationCodeData): Promise<ApiResponse<null>> {
  return request.post('/verification/send-code', data)
}

// 验证邮箱验证码 - 后端: POST /api/verification/verify
export function verifyEmailCode(email: string, code: string, type: string): Promise<ApiResponse<null>> {
  return request.post('/verification/verify', null, {
    params: { email, code, type }
  })
}

// ==================== 密码重置接口 ====================

// 发送密码重置验证码 - 后端: POST /api/password/reset-code
export function sendResetCode(data: { email: string }): Promise<ApiResponse<null>> {
  return request.post('/password/reset-code', data)
}

// 验证密码重置验证码 - 后端: POST /api/password/verify-code
export function verifyResetCode(email: string, code: string): Promise<ApiResponse<null>> {
  return request.post('/password/verify-code', null, {
    params: { email, code }
  })
}

// 重置密码 - 后端: POST /api/password/reset
export function resetPassword(data: { email: string; code: string; password: string }): Promise<ApiResponse<null>> {
  return request.post('/password/reset', data)
}

// ==================== MFA 相关接口 ====================

// 设置 MFA - 后端: POST /api/mfa/setup (需要登录)
export function setupMfa(): Promise<ApiResponse<MfaSetupResponse>> {
  return request.post('/mfa/setup')
}

// 验证并启用 MFA - 后端: POST /api/mfa/verify-and-enable (需要登录)
export function verifyAndEnableMfa(data: MfaVerifyData): Promise<ApiResponse<{ enabled: boolean }>> {
  return request.post('/mfa/verify-and-enable', data)
}

// 禁用 MFA - 后端: POST /api/mfa/disable (需要登录)
export function disableMfa(data: MfaVerifyData): Promise<ApiResponse<null>> {
  return request.post('/mfa/disable', data)
}

// 获取 MFA 状态 - 后端: GET /api/mfa/status (需要登录)
export function getMfaStatus(): Promise<ApiResponse<MfaStatusResponse>> {
  return request.get('/mfa/status')
}