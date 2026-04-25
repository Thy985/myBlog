<template>
  <div class="login-page" @keydown.enter="handleLogin">
    <div class="login-container">
      <div class="login-header">
        <div class="logo">
          <i class="el-icon-sunny login-logo-icon"></i>
          <h1 class="login-logo-text">XingChen博客</h1>
        </div>
        <h2 class="login-title">用户登录</h2>
        <p class="login-subtitle">欢迎回来，请登录您的账号</p>
      </div>

      <LoginForm
        ref="loginFormRef"
        :is-loading="isLoading"
        @submit="handleLoginSubmit"
        @forgot-password="goToForgotPassword"
        @register="goToRegister"
        @social-login="handleSocialLogin"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { saveAuthInfo, clearTempAuthInfo, saveMfaTempInfo, getRedirectUrl } from '@/composables/auth'
import { useAuthStore } from '@/stores/auth'
import { useApiError } from '@/composables/api-error'
import logger from '@/utils/logger'
import LoginForm from '@/components/common/LoginForm.vue'

const router = useRouter()
const loginFormRef = ref(null)
const isLoading = ref(false)
const authStore = useAuthStore()
const { handleApiError, handleBusinessError } = useApiError()

const handleLoginSubmit = async (formData) => {
  try {
    isLoading.value = true

    const response = await login({
      account: formData.account,
      password: formData.password,
      remember: formData.remember,
      captcha: formData.captcha
    })

    if (!response || !response.token) {
      handleBusinessError(response, '登录失败，请稍后重试')
      return
    }

    if (response.requiresMfa) {
      saveMfaTempInfo(response.token, response.user, response.mfaType, response.user.username)

      ElMessage.info('请完成多因素认证')
      router.push('/verify-mfa')
    } else {
      saveAuthInfo(response.token, response.refreshToken, formData.remember)

      authStore.setUser(response.user)

      ElMessage.success('登录成功')
      clearTempAuthInfo()

      const redirectUrl = getRedirectUrl()
      if (redirectUrl !== '/login' && redirectUrl !== '/admin/login') {
        router.push(redirectUrl)
      } else {
        router.push('/')
      }
    }
  } catch (error) {
    handleApiError(error)
    logger.error('登录失败:', error)
  } finally {
    isLoading.value = false
  }
}

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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.login-container {
  width: 100%;
  max-width: 450px;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  backdrop-filter: blur(10px);
}

.login-container:hover {
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.15);
  transform: translateY(-5px);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  animation: fadeInDown 0.6s ease;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.login-logo-icon {
  font-size: 32px;
  color: #667eea;
  margin-right: 12px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.login-logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

@media (max-width: 768px) {
  .login-container {
    padding: 30px 24px;
    max-width: 100%;
  }

  .login-title {
    font-size: 24px;
  }

  .login-logo-text {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 16px;
  }

  .login-container {
    padding: 24px 20px;
  }

  .login-title {
    font-size: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-page {
    animation: none;
  }

  .login-logo-icon {
    animation: none;
  }
}

@media (prefers-contrast: high) {
  .login-container {
    border: 2px solid #000;
  }
}
</style>
