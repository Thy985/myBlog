<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-position="top"
    class="register-form"
    autocomplete="off"
    @keydown.enter="$emit('submit', formData)"
  >
    <el-form-item prop="username">
      <el-input
        v-model="formData.username"
        placeholder="请输入用户名"
        size="large"
        prefix-icon="el-icon-user"
        tabindex="1"
      />
    </el-form-item>

    <el-form-item prop="password">
      <el-input
        v-model="formData.password"
        type="password"
        placeholder="请输入密码"
        show-password
        size="large"
        prefix-icon="el-icon-lock"
        tabindex="2"
      />
      <PasswordStrengthIndicator v-if="formData.password" :password="formData.password" />
    </el-form-item>

    <el-form-item prop="email">
      <el-input
        v-model="formData.email"
        type="email"
        placeholder="请输入邮箱"
        size="large"
        prefix-icon="el-icon-message"
        tabindex="3"
      />
    </el-form-item>

    <el-form-item prop="phone">
      <el-input
        v-model="formData.phone"
        placeholder="请输入手机号"
        size="large"
        prefix-icon="el-icon-phone"
        tabindex="4"
      />
    </el-form-item>

    <el-form-item prop="code">
      <div class="verification-code-container">
        <el-input
          v-model="formData.code"
          placeholder="请输入验证码"
          size="large"
          prefix-icon="el-icon-key"
          tabindex="5"
        />
        <el-button
          type="primary"
          :disabled="!canSendCode || isSendingCode"
          class="verification-code-button"
          tabindex="6"
          @click="handleSendCode"
        >
          {{ isSendingCode ? `${countdown}s后重发` : '发送验证码' }}
        </el-button>
      </div>
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        class="register-button"
        :loading="isLoading"
        size="large"
        tabindex="7"
        :disabled="isLoading"
        @click="handleSubmit"
      >
        <template v-if="isLoading">
          <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
          注册中...
        </template>
        <template v-else>注册</template>
      </el-button>
    </el-form-item>

    <el-form-item class="login-link">
      <span>已有账号？</span>
      <el-link
        type="primary"
        class="login-button"
        tabindex="8"
        @click="$emit('login')"
      >立即登录</el-link>
    </el-form-item>

    <div class="register-divider">
      <span>其他注册方式</span>
    </div>

    <div class="register-social">
      <el-button
        type="default"
        circle
        icon="el-icon-chat-dot-round"
        class="social-btn"
        tabindex="9"
        @click="$emit('social-register', 'chat')"
      />
      <el-button
        type="default"
        circle
        icon="el-icon-s-grid"
        class="social-btn"
        tabindex="10"
        @click="$emit('social-register', 'grid')"
      />
      <el-button
        type="default"
        circle
        icon="el-icon-video-camera"
        class="social-btn"
        tabindex="11"
        @click="$emit('social-register', 'video')"
      />
    </div>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendVerificationCode } from '@/api/auth'
import logger from '@/utils/logger'
import PasswordStrengthIndicator from '@/components/common/PasswordStrengthIndicator.vue'

defineProps({
  isLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit', 'login', 'social-register'])

const formRef = ref(null)
const isSendingCode = ref(false)
const countdown = ref(60)
let countdownTimer = null

const formData = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  code: ''
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名' },
    { min: 2, max: 20, message: '用户名长度应在2-20个字符之间' }
  ],
  password: [
    { required: true, message: '请输入密码' },
    { min: 6, message: '密码长度至少为6个字符' }
  ],
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '请输入正确的邮箱格式' }
  ],
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式' }
  ],
  code: [
    { required: true, message: '请输入验证码' },
    { min: 6, max: 6, message: '验证码长度为6个字符' }
  ]
}

const canSendCode = computed(() => {
  return formData.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)
})

const handleSendCode = async () => {
  if (!formData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  try {
    isSendingCode.value = true

    const response = await sendVerificationCode({
      email: formData.email,
      type: 'register'
    })

    if (response.code === 200) {
      ElMessage.success('验证码发送成功，请查收邮箱')

      countdown.value = 60
      // 清理之前的定时器
      if (countdownTimer) {
        clearInterval(countdownTimer)
      }
      countdownTimer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(countdownTimer)
          countdownTimer = null
          isSendingCode.value = false
        }
      }, 1000)
    } else {
      ElMessage.error(response.message || '验证码发送失败')
      isSendingCode.value = false
    }
  } catch (error) {
    ElMessage.error('验证码发送失败，请稍后重试')
    isSendingCode.value = false
    logger.error('发送验证码失败:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) {return}

  try {
    await formRef.value.validate()
    emit('submit', { ...formData })
  } catch (error) {
    // 表单验证失败
  }
}

const validate = async () => {
  return formRef.value?.validate()
}

const resetForm = () => {
  formRef.value?.resetFields()
}

// 组件卸载时清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})

defineExpose({
  validate,
  resetForm,
  formData
})
</script>

<style scoped>
.register-form {
  width: 100%;
}

.verification-code-container {
  display: flex;
  gap: 12px;
}

.verification-code-button {
  white-space: nowrap;
  min-width: 120px;
}

.register-button {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.register-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: all 0.6s ease;
}

.register-button:hover::before {
  left: 100%;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.login-link span {
  color: #666;
  margin-right: 8px;
}

.login-button {
  font-weight: 500;
  transition: all 0.2s ease;
}

.login-button:hover {
  text-decoration: underline;
}

.register-divider {
  display: flex;
  align-items: center;
  margin: 32px 0;
  text-align: center;
}

.register-divider::before,
.register-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eaeaea;
}

.register-divider span {
  padding: 0 16px;
  color: #999;
  font-size: 12px;
}

.register-social {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
}

.social-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  border: 1px solid #eaeaea;
  cursor: pointer;
}

.social-btn:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.is-loading {
  margin-right: 8px;
}

@media (max-width: 768px) {
  .verification-code-container {
    flex-direction: column;
  }

  .verification-code-button {
    width: 100%;
    min-width: unset;
  }
}
</style>
