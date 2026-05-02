<template>
  <div class="login-page" @keydown.enter="handleLogin">
    <div class="login-container">
      <div class="login-header">
        <h2 class="login-title">登录</h2>
        <p class="login-subtitle">欢迎回来</p>
      </div>

      <Transition name="rate-limit-warning">
        <div v-if="isRateLimited" class="rate-limit-warning">
          <div class="rate-limit-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 8V12M12 16H12.01M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="rate-limit-content">
            <h4>登录过于频繁</h4>
            <p>请在 <strong>{{ rateLimitCountdown }}</strong> 秒后重试</p>
          </div>
        </div>
      </Transition>

      <LoginForm
        ref="loginFormRef"
        :is-loading="isLoading || isRateLimited"
        @submit="handleLoginSubmit"
        @forgot-password="goToForgotPassword"
        @register="goToRegister"
        @social-login="handleSocialLogin"
      />
    </div>

    <Transition name="success-overlay">
      <div v-if="showSuccessOverlay" class="success-overlay">
        <div class="success-content">
          <div class="success-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h3 class="success-title">登录成功</h3>
          <p class="success-message">欢迎回来，{{ loggedInUser || '用户' }}！</p>
          <p class="redirect-hint">正在跳转...</p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import { login } from '@/api/auth'
import { saveAuthInfo, clearTempAuthInfo, getRedirectUrl, setToken, clearRedirectUrl } from '@/composables/auth'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/api-error'
import logger from '@/utils/logger'
import LoginForm from '@/components/common/LoginForm.vue'

const router = useRouter()
const loginFormRef = ref(null)
const isLoading = ref(false)
const showSuccessOverlay = ref(false)
const loggedInUser = shallowRef('')
const authStore = useAuthStore()
const { handleApiError } = useApiError()

const isRateLimited = ref(false)
const rateLimitCountdown = ref(0)
let countdownTimer = null

const ERROR_MESSAGES = {
  'CAPTCHA_ERROR': '验证码错误，请重新输入',
  'CAPTCHA_EXPIRED': '验证码已过期，请刷新后重试',
  'USER_NOT_FOUND': '用户不存在，请检查账号',
  'PASSWORD_ERROR': '密码错误，请重新输入',
  'ACCOUNT_DISABLED': '账号已被禁用，请联系管理员',
  'TOO_MANY_ATTEMPTS': '登录尝试次数过多，请稍后再试',
  'MFA_REQUIRED': '需要两步验证',
  'MFA_ERROR': '两步验证码错误'
}

const CAPTCHA_ERROR_CODES = [40000, 40001, 40002, 'CAPTCHA_ERROR', 'CAPTCHA_EXPIRED']

function isCaptchaError(error) {
  const responseData = error.response?.data
  if (!responseData) return false
  const code = responseData.code
  const message = responseData.message
  if (CAPTCHA_ERROR_CODES.includes(code)) return true
  if (message && (message.includes('验证码') || message.includes('captcha'))) return true
  return false
}

function getFriendlyErrorMessage(error) {
  const responseData = error.response?.data
  if (!responseData) {
    if (!error.response) {
      return '网络连接失败，请检查网络设置'
    }
    return '登录失败，请稍后重试'
  }

  const code = responseData.code
  const message = responseData.message

  if (ERROR_MESSAGES[code]) {
    return ERROR_MESSAGES[code]
  }

  if (message) {
    if (message.includes('验证码') || message.includes(' captcha')) {
      return '验证码错误或已过期'
    }
    if (message.includes('密码') || message.includes('password')) {
      return '密码错误，请重新输入'
    }
    if (message.includes('用户') || message.includes('user')) {
      return '用户不存在，请检查账号'
    }
    if (message.includes('限流') || message.includes('429')) {
      return '登录尝试次数过多，请稍后再试'
    }
    return message
  }

  return '登录失败，请稍后重试'
}

function handleRateLimit(error) {
  const responseData = error.response?.data
  if (!responseData) return false

  if (responseData.code === 429 || responseData.message?.includes('限流')) {
    isRateLimited.value = true
    rateLimitCountdown.value = 60

    ElMessage.warning('登录尝试次数过多，请在稍后再试')

    if (countdownTimer) clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      rateLimitCountdown.value--
      if (rateLimitCountdown.value <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
        isRateLimited.value = false
        ElMessage.success('可以重新尝试登录了')
      }
    }, 1000)
    return true
  }
  return false
}

function refreshCaptcha() {
  if (loginFormRef.value?.refreshCaptcha) {
    loginFormRef.value.refreshCaptcha()
  }
  if (loginFormRef.value?.formData) {
    loginFormRef.value.formData.captcha = ''
  }
}

async function handleLoginSuccess(formData, response) {
  loggedInUser.value = response.user?.nickname || response.user?.username || ''

  if (response.refreshToken) {
    saveAuthInfo(response.token, response.refreshToken, formData.remember)
  } else {
    setToken(response.token)
  }

  authStore.setUser({ ...response.user, roles: response.roles })
  clearTempAuthInfo()
  clearRedirectUrl()

  if (formData.remember) {
    sessionStorage.setItem('savedAccount', formData.account)
  } else {
    sessionStorage.removeItem('savedAccount')
  }

  showSuccessOverlay.value = true

  await new Promise(resolve => setTimeout(resolve, 1200))

  const redirectUrl = getRedirectUrl()
  if (redirectUrl && redirectUrl !== '/login') {
    router.push(redirectUrl)
  } else {
    router.push('/')
  }
}

const handleLoginSubmit = async (formData) => {
  if (isRateLimited.value) {
    ElMessage.warning(`请等待 ${rateLimitCountdown.value} 秒后再试`)
    return
  }

  try {
    isLoading.value = true

    const res = await login({
      username: formData.account,
      password: formData.password,
      captcha: formData.captcha,
      captchaId: formData.captchaId
    })

    const response = res.data
    if (!response || !response.token) {
      ElMessage.error('登录失败，请稍后重试')
      return
    }

    await handleLoginSuccess(formData, response)
  } catch (error) {
    if (handleRateLimit(error)) {
      return
    }

    if (error.response?.data?.code === 7003) {
      try {
        const { value: mfaCode } = await ElMessageBox.prompt(
          '请输入两步验证码',
          '两步验证',
          {
            confirmButtonText: '验证',
            cancelButtonText: '取消',
            inputPlaceholder: '6位验证码',
            inputPattern: /^\d{6}$/,
            inputErrorMessage: '请输入6位数字验证码'
          }
        )
        const mfaRes = await login({
          username: formData.account,
          password: formData.password,
          captcha: formData.captcha,
          captchaId: formData.captchaId,
          mfaCode: mfaCode
        })
        const response = mfaRes.data
        if (response && response.token) {
          await handleLoginSuccess(formData, response)
        }
      } catch {
        // 用户取消 MFA 输入
      }
      return
    }

    const friendlyMessage = getFriendlyErrorMessage(error)
    ElMessage.error(friendlyMessage)
    logger.error('登录失败:', error)

    if (isCaptchaError(error)) {
      refreshCaptcha()
    }
  } finally {
    isLoading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})

const handleLogin = async () => {
  if (loginFormRef.value) {
    await loginFormRef.value.validate()
  }
}

const handleSocialLogin = (type) => {
  ElMessage.info(`${type} 登录开发中`)
}

const goToForgotPassword = () => {
  router.push('/forgot-password')
}

const goToRegister = () => {
  router.push('/register')
}

onMounted(() => {
  const savedAccount = sessionStorage.getItem('savedAccount')
  if (savedAccount && loginFormRef.value) {
    loginFormRef.value.formData.account = savedAccount
    loginFormRef.value.formData.remember = true
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 420px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  padding: 40px;
  opacity: 0;
  transform: translateY(16px);
  animation: container-enter 0.4s var(--ease-out-quart) forwards;
}

@keyframes container-enter {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  opacity: 0;
  animation: fade-in 0.3s var(--ease-out-quart) 0.15s forwards;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  opacity: 0;
  animation: fade-in 0.3s var(--ease-out-quart) 0.2s forwards;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
  opacity: 0;
  animation: fade-in 0.3s var(--ease-out-quart) 0.25s forwards;
}

@keyframes fade-in {
  to {
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .login-container {
    padding: 32px 24px;
    max-width: 100%;
    border-radius: var(--radius-lg);
  }

  .login-title {
    font-size: 22px;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 16px;
    align-items: flex-start;
    padding-top: 48px;
  }

  .login-container {
    padding: 28px 20px;
  }

  .login-title {
    font-size: 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-container,
  .login-header,
  .login-title,
  .login-subtitle {
    animation: none;
    opacity: 1;
    transform: none;
  }
}

.rate-limit-warning {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #f59e0b;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.rate-limit-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  color: #d97706;
}

.rate-limit-icon svg {
  width: 100%;
  height: 100%;
}

.rate-limit-content h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #92400e;
}

.rate-limit-content p {
  margin: 0;
  font-size: 13px;
  color: #a16207;
}

.rate-limit-content strong {
  color: #dc2626;
  font-size: 16px;
}

.rate-limit-warning-enter-active {
  animation: slide-down 0.3s ease-out;
}

.rate-limit-warning-leave-active {
  animation: slide-up 0.3s ease-in;
}

@keyframes slide-down {
  0% {
    opacity: 0;
    transform: translateY(-10px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slide-up {
  0% {
    opacity: 1;
    transform: translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateY(-10px);
  }
}

@media (prefers-color-scheme: dark) {
  .rate-limit-warning {
    background: linear-gradient(135deg, #422006 0%, #713f12 100%);
    border-color: #a16207;
  }

  .rate-limit-icon {
    color: #fbbf24;
  }

  .rate-limit-content h4 {
    color: #fef3c7;
  }

  .rate-limit-content p {
    color: #fde68a;
  }
}

.success-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.success-content {
  text-align: center;
  padding: 48px;
}

.success-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: success-pop 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 8px 32px rgba(16, 185, 129, 0.3);
}

.success-icon svg {
  width: 40px;
  height: 40px;
  color: white;
  animation: check-draw 0.4s ease-out 0.2s both;
}

@keyframes success-pop {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes check-draw {
  0% {
    stroke-dashoffset: 100;
    opacity: 0;
  }
  100% {
    stroke-dashoffset: 0;
    opacity: 1;
  }
}

.success-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
  animation: fade-up 0.4s ease-out 0.3s both;
}

.success-message {
  font-size: 18px;
  color: var(--text-secondary);
  margin: 0 0 8px;
  animation: fade-up 0.4s ease-out 0.4s both;
}

.redirect-hint {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
  animation: fade-up 0.4s ease-out 0.5s both, pulse 1.5s ease-in-out 0.9s infinite;
}

@keyframes fade-up {
  0% {
    opacity: 0;
    transform: translateY(16px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.success-overlay-enter-active {
  animation: overlay-in 0.3s ease-out;
}

.success-overlay-leave-active {
  animation: overlay-out 0.3s ease-in;
}

@keyframes overlay-in {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

@keyframes overlay-out {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}

@media (prefers-color-scheme: dark) {
  .success-overlay {
    background: rgba(15, 23, 42, 0.95);
  }

  .success-icon {
    box-shadow: 0 8px 32px rgba(16, 185, 129, 0.4);
  }

  .success-title {
    color: #f1f5f9;
  }

  .success-message {
    color: #94a3b8;
  }

  .redirect-hint {
    color: #64748b;
  }
}
</style>
