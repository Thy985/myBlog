<template>
  <div class="verify-mfa-page" @keydown.enter="handleVerify">
    <div class="verify-mfa-container">
      <div class="verify-mfa-header">
        <div class="logo">
          <i class="el-icon-sunny login-logo-icon"></i>
          <h1 class="login-logo-text">XingChen博客</h1>
        </div>
        <h2 class="verify-mfa-title">多因素认证</h2>
        <p class="verify-mfa-subtitle">请完成额外的验证步骤以登录您的账号</p>
      </div>
      
      <el-form
        ref="verifyFormRef"
        :model="verifyForm"
        :rules="verifyFormRules"
        label-position="top"
        class="verify-mfa-form"
        autocomplete="off"
      >
        <el-form-item prop="code">
          <el-input 
            v-model="verifyForm.code" 
            placeholder="请输入验证码"
            size="large"
            prefix-icon="el-icon-key"
            maxlength="6"
            show-word-limit
            tabindex="1"
          />
        </el-form-item>
        
        <div class="verify-mfa-info">
          <p v-if="mfaType === 'sms'" class="verify-mfa-info-text">
            验证码已发送至您的手机：{{ maskedPhone }}
          </p>
          <p v-else-if="mfaType === 'email'" class="verify-mfa-info-text">
            验证码已发送至您的邮箱：{{ maskedEmail }}
          </p>
          <p v-else-if="mfaType === 'app'" class="verify-mfa-info-text">
            请使用认证器应用扫描二维码或输入验证码
          </p>
        </div>
        
        <el-form-item>
          <el-button
            type="primary"
            class="verify-mfa-button"
            :loading="isLoading"
            size="large"
            tabindex="2"
            :disabled="isLoading"
            @click="handleVerify"
          >
            <template v-if="isLoading">
              <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
              验证中...
            </template>
            <template v-else>验证</template>
          </el-button>
        </el-form-item>
        
        <div class="verify-mfa-actions">
          <el-link 
            type="primary" 
            class="resend-code-link" 
            :disabled="isResending"
            tabindex="3"
            @click="resendCode"
          >
            {{ resendText }}
          </el-link>
          <el-link 
            type="danger" 
            class="go-back-link" 
            tabindex="4"
            @click="goBack"
          >
            返回登录
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { verifyAndEnableMfa } from '@/api/auth'
import { setRedirectUrl, getRedirectUrl, saveAuthInfo, getMfaTempInfo, clearTempAuthInfo } from '@/composables/auth'
import { useAuthStore } from '@/stores/auth'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const router = useRouter()
const verifyFormRef = ref(null)
const isLoading = ref(false)
const isResending = ref(false)
const resendCountdown = ref(60)
const resendTimer = ref(null)
const authStore = useAuthStore()

// 从 sessionStorage 获取 MFA 信息
const mfaTempInfo = getMfaTempInfo()
const mfaType = ref(mfaTempInfo.mfaType || '')
const username = ref(mfaTempInfo.username || '')
const tempToken = ref(mfaTempInfo.token || '')

// 模拟获取用户的手机和邮箱
const phone = ref('186****0838')
const email = ref('185****838@qq.com')

const maskedPhone = computed(() => {
  return phone.value
})

const maskedEmail = computed(() => {
  return email.value
})

const resendText = computed(() => {
  if (isResending.value) {
    return `重新发送(${resendCountdown.value}s)`
  }
  return '重新发送验证码'
})

const verifyForm = reactive({
  code: ''
})

const verifyFormRules = {
  code: [
    { required: true, message: '请输入验证码' },
    { min: 6, max: 6, message: '验证码长度为6位' },
    { pattern: /^\d+$/, message: '验证码只能包含数字' }
  ]
}

const handleVerify = async () => {
  if (!verifyFormRef.value) {return}

  try {
    await verifyFormRef.value.validate()

    isLoading.value = true

    const response = await verifyAndEnableMfa({
      username: username.value,
      code: verifyForm.code,
      mfaType: mfaType.value,
      tempToken: tempToken.value
    })

    if (response.code === API_STATUS.SUCCESS && response.data) {
      const { token, refreshToken, user } = response.data
      if (!token) {
        ElMessage.error('验证响应异常：缺少token')
        return
      }

      saveAuthInfo(token, refreshToken || '', true)
      clearTempAuthInfo()

      if (user) {
        authStore.setUser(user)
      }

      ElMessage.success('验证成功，正在登录...')

      const redirectUrl = getRedirectUrl()
      if (redirectUrl !== '/login') {
        router.push(redirectUrl)
      } else {
        router.push('/')
      }
    } else {
      ElMessage.error(response.message || '验证失败')
    }
  } catch (error) {
    logger.error('MFA验证失败:', error)
    if (error.response) {
      switch (error.response.status) {
        case 400:
          ElMessage.error('验证码错误，请重新输入')
          break
        case 401:
          ElMessage.error('验证失败，请重新登录')
          router.push('/login')
          break
        case 403:
          ElMessage.error('账号已被禁用，请联系管理员')
          router.push('/login')
          break
        case 429:
          ElMessage.error('验证过于频繁，请稍后重试')
          break
        case 500:
          ElMessage.error('服务器内部错误，请稍后重试')
          break
        case 502:
          ElMessage.error('网关错误，请稍后重试')
          break
        case 503:
          ElMessage.error('服务暂时不可用，请稍后重试')
          break
        case 504:
          ElMessage.error('服务器响应超时，请检查网络设置')
          break
        default:
          ElMessage.error(error.response.data?.message || '验证失败，请稍后重试')
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      ElMessage.error('网络连接失败，请检查网络设置')
    } else {
      // 请求配置出错
      ElMessage.error('验证失败，请稍后重试')
    }
    logger.error('验证失败:', error)
  } finally {
    isLoading.value = false
  }
}

const resendCode = () => {
  if (isResending.value) {return}
  
  isResending.value = true
  resendCountdown.value = 60
  
  // 开始倒计时
  resendTimer.value = setInterval(() => {
    resendCountdown.value--
    if (resendCountdown.value <= 0) {
      clearInterval(resendTimer.value)
      isResending.value = false
    }
  }, 1000)
  
  // 模拟发送验证码
  ElMessage.info('验证码已重新发送')
}

const goBack = () => {
  // 清除临时数据
  localStorage.removeItem('tempToken')
  localStorage.removeItem('tempUserInfo')
  localStorage.removeItem('mfaType')
  localStorage.removeItem('mfaUsername')
  
  // 跳回登录页面
  router.push('/login')
}

// 页面加载时的初始化
onMounted(() => {
  // 检查是否有临时token，如果没有则跳回登录页面
  if (!tempToken.value || !mfaType.value || !username.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  // autofocus is handled by el-input's tabindex
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (resendTimer.value) {
    clearInterval(resendTimer.value)
    resendTimer.value = null
  }
})
</script>

<style scoped>
.verify-mfa-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.verify-mfa-page::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.1) 0%, transparent 50%),
              radial-gradient(circle at 70% 70%, rgba(236,72,153,0.15) 0%, transparent 40%);
  animation: shimmer 20s ease-in-out infinite;
  pointer-events: none;
}

@keyframes shimmer {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-5%, -5%) rotate(3deg); }
}

.verify-mfa-container {
  width: 100%;
  max-width: 420px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur-lg);
  -webkit-backdrop-filter: var(--glass-blur-lg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  padding: 40px;
  position: relative;
  z-index: 1;
}

.verify-mfa-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.login-logo-icon {
  font-size: 32px;
  color: var(--color-primary);
  margin-right: 10px;
}

.login-logo-text {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.02em;
}

.verify-mfa-title {
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  letter-spacing: -0.01em;
}

.verify-mfa-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.verify-mfa-form {
  width: 100%;
}

.verify-mfa-info {
  margin: 16px 0;
  padding: 12px;
  background: var(--color-primary-subtle);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-primary);
}

.verify-mfa-info-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.verify-mfa-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.resend-code-link {
  font-size: 14px;
  color: var(--color-primary);
}

.resend-code-link:hover {
  text-decoration: underline;
}

.go-back-link {
  font-size: 14px;
  color: var(--color-error);
}

.go-back-link:hover {
  text-decoration: underline;
}

.verify-mfa-button {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 500;
  border-radius: var(--radius-md);
  background: var(--gradient-primary);
  border: none;
  color: white;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}

.verify-mfa-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-primary);
}

.verify-mfa-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.is-loading {
  margin-right: 8px;
}

@media (max-width: 768px) {
  .verify-mfa-container {
    padding: 32px 24px;
    max-width: 100%;
    border-radius: var(--radius-lg);
  }

  .verify-mfa-title {
    font-size: 22px;
  }

  .login-logo-text {
    font-size: 18px;
  }

  .verify-mfa-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .go-back-link {
    align-self: flex-end;
  }
}

@media (max-width: 480px) {
  .verify-mfa-page {
    padding: 16px;
    align-items: flex-start;
    padding-top: 48px;
  }

  .verify-mfa-container {
    padding: 28px 20px;
  }

  .verify-mfa-title {
    font-size: 20px;
  }

  .verify-mfa-button {
    padding: 12px;
    font-size: 15px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .verify-mfa-page::before {
    animation: none;
  }
}

@media (prefers-contrast: high) {
  .verify-mfa-container {
    border: 2px solid var(--text-primary);
  }

  .verify-mfa-button {
    border: 2px solid var(--text-primary);
  }

  .verify-mfa-info {
    border: 2px solid var(--text-primary);
  }
}
</style>
