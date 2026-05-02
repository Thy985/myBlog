import type { UserProfile } from '@/types/user'

const TOKEN_COOKIE_NAME = 'X-Auth-Token'
const REFRESH_TOKEN_KEY = 'RefreshToken'
const REMEMBER_ME_KEY = 'RememberMe'
const CSRF_TOKEN_KEY = 'XSRF-TOKEN'

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

function parseCookies(cookieString: string): Record<string, string> {
  const cookies: Record<string, string> = {}
  if (!cookieString) return cookies

  cookieString.split(';').forEach(cookie => {
    const [name, ...valueParts] = cookie.trim().split('=')
    if (name && valueParts.length > 0) {
      cookies[name] = decodeURIComponent(valueParts.join('='))
    }
  })
  return cookies
}

function getCookieToken(): string | null {
  const cookies = parseCookies(document.cookie)
  return cookies[TOKEN_COOKIE_NAME] || null
}

export function getToken(): string | undefined {
  return getCookieToken() || undefined
}

export function getRefreshToken(): string | undefined {
  const sessionToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)
  if (sessionToken) return sessionToken
  return localStorage.getItem(REFRESH_TOKEN_KEY) || undefined
}

export function setToken(token: string, rememberMe: boolean = false): void {
  console.warn('Token is now stored in HttpOnly Cookie. This function is kept for compatibility but will not persist token locally.')
}

export function setRefreshToken(refreshToken: string, rememberMe: boolean = false): void {
  if (rememberMe) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  } else {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
}

export function removeToken(): void {
  document.cookie = `${TOKEN_COOKIE_NAME}=; path=/; max-age=0; sameSite=strict`
}

export function removeRefreshToken(): void {
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function clearAuthInfo(): void {
  removeToken()
  removeRefreshToken()
  localStorage.removeItem(REMEMBER_ME_KEY)
  sessionStorage.removeItem(TOKEN_COOKIE_NAME)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
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
  return localStorage.getItem(REMEMBER_ME_KEY) === 'true'
}

export function setRememberMe(remember: boolean): void {
  if (remember) {
    localStorage.setItem(REMEMBER_ME_KEY, 'true')
  } else {
    localStorage.removeItem(REMEMBER_ME_KEY)
  }
}

export function saveAuthInfo(token: string, refreshToken: string, rememberMe: boolean): void {
  setToken(token, rememberMe)
  setRefreshToken(refreshToken, rememberMe)
  setRememberMe(rememberMe)
}

export function isAuthenticated(): boolean {
  return !!getToken()
}

export function getCsrfToken(): string | undefined {
  return localStorage.getItem(CSRF_TOKEN_KEY) || undefined
}

export function setCsrfToken(token: string): void {
  localStorage.setItem(CSRF_TOKEN_KEY, token)
}

export function removeCsrfToken(): void {
  localStorage.removeItem(CSRF_TOKEN_KEY)
}
