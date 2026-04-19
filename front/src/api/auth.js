import request from '@/axios'
import logger from '@/utils/logger'

// ==================== 认证相关接口 ====================

// 登录接口 - 后端: POST /api/auth/login
export function login(data) {
  return request.post('/auth/login', data)
}

// 登出接口 - 后端: POST /api/auth/logout
export function logout() {
  return request.post('/auth/logout')
}

// 刷新Token - 后端: POST /api/auth/refresh
export function refreshToken() {
  return request.post('/auth/refresh')
}

// 获取用户信息 - 后端: GET /api/auth/info
export function getUserInfo() {
  return request.get('/auth/info')
}

// ==================== 用户注册接口 ====================

// 注册接口 - 后端: POST /api/user/register
export function register(data) {
  return request.post('/user/register', data)
}

// ==================== 验证码接口 ====================

// 获取验证码图片 (base64) - 后端: GET /api/captcha/base64
export function getCaptcha() {
  return request.get('/captcha/base64')
}

// 验证验证码 - 后端: GET /api/captcha/verify
export function verifyCaptcha(code) {
  return request.get('/captcha/verify', { params: { code } })
}

// ==================== 邮箱验证码接口 ====================

// 发送验证码 - 后端: POST /api/verification/send-code
export function sendVerificationCode(data) {
  return request.post('/verification/send-code', data)
}

// 验证邮箱验证码 - 后端: POST /api/verification/verify
export function verifyEmailCode(email, code, type) {
  return request.post('/verification/verify', null, {
    params: { email, code, type }
  })
}

// ==================== 密码重置接口 ====================

// 发送密码重置验证码 - 后端: POST /api/password/reset-code
export function sendResetCode(data) {
  return request.post('/password/reset-code', data)
}

// 验证密码重置验证码 - 后端: POST /api/password/verify-code
export function verifyResetCode(email, code) {
  return request.post('/password/verify-code', null, {
    params: { email, code }
  })
}

// 重置密码 - 后端: POST /api/password/reset
export function resetPassword(data) {
  return request.post('/password/reset', data)
}

// ==================== MFA 相关接口 ====================

// 设置 MFA - 后端: POST /api/mfa/setup (需要登录)
export function setupMfa() {
  return request.post('/mfa/setup')
}

// 验证并启用 MFA - 后端: POST /api/mfa/verify-and-enable (需要登录)
export function verifyAndEnableMfa(data) {
  return request.post('/mfa/verify-and-enable', data)
}

// 禁用 MFA - 后端: POST /api/mfa/disable (需要登录)
export function disableMfa(data) {
  return request.post('/mfa/disable', data)
}

// 获取 MFA 状态 - 后端: GET /api/mfa/status (需要登录)
export function getMfaStatus() {
  return request.get('/mfa/status')
}

// ==================== 兼容旧接口 (已废弃) ====================

// @deprecated 使用 verifyEmailCode 替代
// eslint-disable-next-line no-unused-vars
export function verifyMfa(_data) {
  logger.warn('verifyMfa: 请使用 verifyEmailCode 或 MFA 相关接口')
  return Promise.resolve({ code: 200, message: 'success' })
}
