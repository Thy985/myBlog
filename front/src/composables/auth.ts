import { useCookies } from '@vueuse/integrations/useCookies'
import type { UserProfile } from '@/types/user'

const TOKEN_KEY = 'Authorization'
const REFRESH_TOKEN_KEY = 'RefreshToken'
const REMEMBER_ME_KEY = 'RememberMe'
const CSRF_TOKEN_KEY = 'XSRF-TOKEN'

const cookie = useCookies()

const secureCookieOptions = {
  secure: import.meta.env.PROD,
  sameSite: 'lax' as const,
  path: '/',
  maxAge: 7 * 24 * 60 * 60
}

interface MfaTempInfo {
  token: string | null
  user: UserProfile | Record<string, never>
  mfaType: string | null
  username: string | null
}

const MFA_SESSION_KEYS = {
  TEMP_TOKEN: 'mfa_temp_token',
  USER_INFO: 'mfa_user_info',
  MFA_TYPE: 'mfa_type',
  USERNAME: 'mfa_username'
}

export function getToken(): string | undefined {
  return cookie.get(TOKEN_KEY)
}

export function getRefreshToken(): string | undefined {
  return cookie.get(REFRESH_TOKEN_KEY)
}

export function setToken(token: string): void {
  cookie.set(TOKEN_KEY, token, secureCookieOptions)
}

export function setRefreshToken(refreshToken: string): void {
  cookie.set(REFRESH_TOKEN_KEY, refreshToken, secureCookieOptions)
}

export function removeToken(): void {
  cookie.remove(TOKEN_KEY, { path: '/' })
}

export function removeRefreshToken(): void {
  cookie.remove(REFRESH_TOKEN_KEY, { path: '/' })
}

export function clearAuthInfo(): void {
  removeToken()
  removeRefreshToken()
  cookie.remove(REMEMBER_ME_KEY, { path: '/' })
  removeCsrfToken()
}

export function clearTempAuthInfo(): void {
  sessionStorage.removeItem(MFA_SESSION_KEYS.TEMP_TOKEN)
  sessionStorage.removeItem(MFA_SESSION_KEYS.USER_INFO)
  sessionStorage.removeItem(MFA_SESSION_KEYS.MFA_TYPE)
  sessionStorage.removeItem(MFA_SESSION_KEYS.USERNAME)
}

export function saveMfaTempInfo(token: string, user: UserProfile, mfaType: string, username: string): void {
  sessionStorage.setItem(MFA_SESSION_KEYS.TEMP_TOKEN, token)
  sessionStorage.setItem(MFA_SESSION_KEYS.USER_INFO, JSON.stringify(user))
  sessionStorage.setItem(MFA_SESSION_KEYS.MFA_TYPE, mfaType)
  sessionStorage.setItem(MFA_SESSION_KEYS.USERNAME, username)
}

export function getMfaTempInfo(): MfaTempInfo {
  return {
    token: sessionStorage.getItem(MFA_SESSION_KEYS.TEMP_TOKEN),
    user: JSON.parse(sessionStorage.getItem(MFA_SESSION_KEYS.USER_INFO) || '{}'),
    mfaType: sessionStorage.getItem(MFA_SESSION_KEYS.MFA_TYPE),
    username: sessionStorage.getItem(MFA_SESSION_KEYS.USERNAME)
  }
}

export function clearRedirectUrl(): void {
  sessionStorage.removeItem('redirectUrl')
}

export function getRedirectUrl(): string {
  return sessionStorage.getItem('redirectUrl') || '/'
}

export function setRedirectUrl(url: string): void {
  sessionStorage.setItem('redirectUrl', url)
}

export function getRememberMe(): boolean {
  return cookie.get(REMEMBER_ME_KEY) === 'true'
}

export function setRememberMe(remember: boolean): void {
  cookie.set(REMEMBER_ME_KEY, remember ? 'true' : 'false', secureCookieOptions)
}

export function saveAuthInfo(token: string, refreshToken: string, rememberMe: boolean): void {
  cookie.set(TOKEN_KEY, token, secureCookieOptions)
  cookie.set(REFRESH_TOKEN_KEY, refreshToken, secureCookieOptions)
  setRememberMe(rememberMe)
}

export function isAuthenticated(): boolean {
  return !!getToken()
}

export function getCsrfToken(): string | undefined {
  return cookie.get(CSRF_TOKEN_KEY)
}

export function setCsrfToken(token: string): void {
  cookie.set(CSRF_TOKEN_KEY, token, secureCookieOptions)
}

export function removeCsrfToken(): void {
  cookie.remove(CSRF_TOKEN_KEY, { path: '/' })
}