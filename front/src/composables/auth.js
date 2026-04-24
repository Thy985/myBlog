import { useCookies } from '@vueuse/integrations/useCookies'
import logger from '@/utils/logger'

const TOKEN_KEY = 'Authorization'
const REFRESH_TOKEN_KEY = 'RefreshToken'
const REMEMBER_ME_KEY = 'RememberMe'
const CSRF_TOKEN_KEY = 'XSRF-TOKEN'
const cookie = useCookies()

const secureCookieOptions = {
  secure: import.meta.env.PROD,
  sameSite: 'lax',
  path: '/',
  maxAge: 7 * 24 * 60 * 60
}

function checkSecurityEnvironment() {
  const issues = []

  if (!import.meta.env.PROD) {
    issues.push('开发环境: Token 存储在非 HttpOnly Cookie 中')
  }

  if (!window.isSecureContext) {
    issues.push('非安全上下文: 建议在 HTTPS 环境下使用')
  }

  if (issues.length > 0 && import.meta.env.PROD) {
    logger.warn('[安全警告] Token 存储存在以下问题:', issues)
  }

  return issues
}

const securityIssues = checkSecurityEnvironment()

if (securityIssues.length > 0 && import.meta.env.PROD) {
  logger.error('[严重安全警告] 当前环境不符合最佳安全实践，请确保后端正确配置 HttpOnly Cookie。详细问题:', securityIssues)
}

// 获取 Token
export function getToken() {
  return cookie.get(TOKEN_KEY)
}

// 获取 Refresh Token
export function getRefreshToken() {
  return cookie.get(REFRESH_TOKEN_KEY)
}

// 设置 Token（严重安全警告：前端无法设置真正的 HttpOnly Cookie，Token 存在被 XSS 攻击窃取的风险）
// 最佳实践：后端应在设置 Refresh Token 时使用 HttpOnly、Secure、SameSite=Strict Cookie
export function setToken(token) {
  if (import.meta.env.PROD) {
    logger.warn('[安全警告] 使用非 HttpOnly Cookie 存储 Token，建议后端使用 HttpOnly Cookie')
  }
  return cookie.set(TOKEN_KEY, token, secureCookieOptions)
}

// 设置 Refresh Token
export function setRefreshToken(refreshToken) {
  if (import.meta.env.PROD) {
    logger.warn('[安全警告] 使用非 HttpOnly Cookie 存储 Refresh Token，建议后端使用 HttpOnly Cookie')
  }
  return cookie.set(REFRESH_TOKEN_KEY, refreshToken, secureCookieOptions)
}

// 删除 Token
export function removeToken() {
  return cookie.remove(TOKEN_KEY, { path: '/' })
}

// 删除 Refresh Token
export function removeRefreshToken() {
  return cookie.remove(REFRESH_TOKEN_KEY, { path: '/' })
}

// 清除所有认证信息
export function clearAuthInfo() {
  removeToken()
  removeRefreshToken()
  cookie.remove(REMEMBER_ME_KEY, { path: '/' })
  removeCsrfToken()
}

// MFA 临时信息键名
const MFA_SESSION_KEYS = {
  TEMP_TOKEN: 'mfa_temp_token',
  USER_INFO: 'mfa_user_info',
  MFA_TYPE: 'mfa_type',
  USERNAME: 'mfa_username'
}

// 清除临时认证信息（MFA流程中断后清理残留数据）
// 使用 sessionStorage，关闭浏览器标签页后自动清除，更安全
export function clearTempAuthInfo() {
  sessionStorage.removeItem(MFA_SESSION_KEYS.TEMP_TOKEN)
  sessionStorage.removeItem(MFA_SESSION_KEYS.USER_INFO)
  sessionStorage.removeItem(MFA_SESSION_KEYS.MFA_TYPE)
  sessionStorage.removeItem(MFA_SESSION_KEYS.USERNAME)
}

// 保存 MFA 临时信息到 sessionStorage
export function saveMfaTempInfo(token, user, mfaType, username) {
  sessionStorage.setItem(MFA_SESSION_KEYS.TEMP_TOKEN, token)
  sessionStorage.setItem(MFA_SESSION_KEYS.USER_INFO, JSON.stringify(user))
  sessionStorage.setItem(MFA_SESSION_KEYS.MFA_TYPE, mfaType)
  sessionStorage.setItem(MFA_SESSION_KEYS.USERNAME, username)
}

// 获取 MFA 临时信息
export function getMfaTempInfo() {
  return {
    token: sessionStorage.getItem(MFA_SESSION_KEYS.TEMP_TOKEN),
    user: JSON.parse(sessionStorage.getItem(MFA_SESSION_KEYS.USER_INFO) || '{}'),
    mfaType: sessionStorage.getItem(MFA_SESSION_KEYS.MFA_TYPE),
    username: sessionStorage.getItem(MFA_SESSION_KEYS.USERNAME)
  }
}

// 清除重定向URL
export function clearRedirectUrl() {
  sessionStorage.removeItem('redirectUrl')
}

// 获取重定向URL
export function getRedirectUrl() {
  return sessionStorage.getItem('redirectUrl') || '/'
}

// 保存重定向URL
export function setRedirectUrl(url) {
  sessionStorage.setItem('redirectUrl', url)
}

// 获取记住我状态
export function getRememberMe() {
  return cookie.get(REMEMBER_ME_KEY) === 'true'
}

// 设置记住我状态
export function setRememberMe(remember) {
  cookie.set(REMEMBER_ME_KEY, remember ? 'true' : 'false', secureCookieOptions)
}

export function saveAuthInfo(token, refreshToken, rememberMe) {
  cookie.set(TOKEN_KEY, token, secureCookieOptions)
  cookie.set(REFRESH_TOKEN_KEY, refreshToken, secureCookieOptions)
  setRememberMe(rememberMe)
}

// 检查是否已登录
export function isAuthenticated() {
  return !!getToken()
}

// 获取CSRF令牌
export function getCsrfToken() {
  return cookie.get(CSRF_TOKEN_KEY)
}

// 设置CSRF令牌
export function setCsrfToken(token) {
  return cookie.set(CSRF_TOKEN_KEY, token, secureCookieOptions)
}

// 删除CSRF令牌
export function removeCsrfToken() {
  return cookie.remove(CSRF_TOKEN_KEY, { path: '/' })
}
