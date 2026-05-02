<template>
  <div class="register-page" @keydown.enter="handleRegister">
    <div class="register-container">
      <div class="register-header">
        <div class="logo">
          <i class="el-icon-sunny register-logo-icon"></i>
          <h1 class="register-logo-text">XingChen博客</h1>
        </div>
        <h2 class="register-title">用户注册</h2>
        <p class="register-subtitle">创建新账号，开始您的博客之旅</p>
      </div>

      <RegisterForm
        ref="registerFormRef"
        :is-loading="isLoading"
        @submit="handleRegisterSubmit"
        @login="goToLogin"
        @social-register="handleSocialRegister"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import RegisterForm from '@/components/common/RegisterForm.vue'
import logger from '@/utils/logger'

const router = useRouter()
const registerFormRef = ref(null)
const isLoading = ref(false)

const handleRegisterSubmit = async (formData) => {
  try {
    isLoading.value = true

    const response = await register({
      username: formData.username,
      password: formData.password,
      email: formData.email,
      phone: formData.phone,
      code: formData.code
    })

    if (response.code === 200) {
      ElMessage.success('注册成功')

      setTimeout(() => {
        router.push('/login')
      }, 1000)
    } else {
      ElMessage.error(response.message || '注册失败')
    }
  } catch (error) {
    if (error.response) {
      switch (error.response.status) {
        case 400:
          ElMessage.error('注册信息有误，请检查后重试')
          break
        case 409:
          ElMessage.error('用户名或邮箱已存在')
          break
        default:
          ElMessage.error('注册失败，请稍后重试')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络设置')
    } else {
      ElMessage.error('注册失败，请稍后重试')
    }
    logger.error('注册失败:', error)
  } finally {
    isLoading.value = false
  }
}

const handleRegister = async () => {
  if (registerFormRef.value) {
    await registerFormRef.value.validate()
  }
}

const handleSocialRegister = (type) => {
  ElMessage.info(`${type} 注册开发中`)
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  // autofocus is handled by el-input's tabindex
})
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.register-page::before {
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

.register-container {
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

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.register-logo-icon {
  font-size: 32px;
  color: var(--color-primary);
  margin-right: 10px;
}

.register-logo-text {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.02em;
}

.register-title {
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  letter-spacing: -0.01em;
}

.register-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

@media (max-width: 768px) {
  .register-container {
    padding: 32px 24px;
    max-width: 100%;
    border-radius: var(--radius-lg);
  }

  .register-title {
    font-size: 22px;
  }

  .register-logo-text {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .register-page {
    padding: 16px;
    align-items: flex-start;
    padding-top: 48px;
  }

  .register-container {
    padding: 28px 20px;
  }

  .register-title {
    font-size: 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .register-page::before {
    animation: none;
  }
}

@media (prefers-contrast: high) {
  .register-container {
    border: 2px solid var(--text-primary);
  }
}
</style>
