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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.register-container {
  width: 100%;
  max-width: 450px;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  backdrop-filter: blur(10px);
}

.register-container:hover {
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.15);
  transform: translateY(-5px);
}

.register-header {
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

.register-logo-icon {
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

.register-logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.register-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.register-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

@media (max-width: 768px) {
  .register-container {
    padding: 30px 24px;
    max-width: 100%;
  }

  .register-title {
    font-size: 24px;
  }

  .register-logo-text {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .register-page {
    padding: 16px;
  }

  .register-container {
    padding: 24px 20px;
  }

  .register-title {
    font-size: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .register-page {
    animation: none;
  }

  .register-logo-icon {
    animation: none;
  }
}

@media (prefers-contrast: high) {
  .register-container {
    border: 2px solid #000;
  }
}
</style>
