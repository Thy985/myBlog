<template>
  <div class="register-form-container">
    <!-- Step indicator -->
    <div class="register-steps">
      <div
        v-for="s in 3"
        :key="s"
        :class="['step', { active: currentStep === s, completed: currentStep > s }]"
      >
        <div class="step-dot">{{ s }}</div>
        <span class="step-label">{{ stepLabels[s - 1] }}</span>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="currentRules"
      label-position="top"
      class="register-form"
      autocomplete="off"
      @keydown.enter="handleNext"
    >
      <!-- Step 1: Account credentials -->
      <template v-if="currentStep === 1">
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
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
            prefix-icon="Lock"
            tabindex="2"
          />
          <PasswordStrengthIndicator v-if="formData.password" :password="formData.password" />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="请确认密码"
            show-password
            size="large"
            prefix-icon="Lock"
            tabindex="3"
          />
        </el-form-item>
      </template>

      <!-- Step 2: Contact info -->
      <template v-else-if="currentStep === 2">
        <el-form-item prop="email">
          <el-input
            v-model="formData.email"
            type="email"
            placeholder="请输入邮箱"
            size="large"
            prefix-icon="Message"
            tabindex="1"
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入手机号"
            size="large"
            prefix-icon="Phone"
            tabindex="2"
          />
        </el-form-item>
      </template>

      <!-- Step 3: Verification -->
      <template v-else>
        <el-form-item prop="code">
          <div class="verification-code-container">
            <el-input
              v-model="formData.code"
              placeholder="请输入验证码"
              size="large"
              prefix-icon="Key"
              tabindex="1"
            />
            <el-button
              type="primary"
              :disabled="!canSendCode || isSendingCode"
              class="verification-code-button"
              tabindex="2"
              @click="handleSendCode"
            >
              {{ isSendingCode ? `${countdown}s后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <p class="verification-hint">验证码已发送至 {{ formData.email }}</p>
      </template>

      <!-- Navigation buttons -->
      <el-form-item class="register-actions">
        <el-button
          v-if="currentStep > 1"
          type="default"
          class="back-button"
          size="large"
          tabindex="10"
          @click="currentStep--"
        >
          上一步
        </el-button>
        <el-button
          type="primary"
          class="register-button"
          :loading="isLoading"
          size="large"
          tabindex="11"
          :disabled="isLoading"
          @click="handleNext"
        >
          <template v-if="isLoading">
            <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
            注册中...
          </template>
          <template v-else>
            {{ currentStep === 3 ? '注册' : '下一步' }}
          </template>
        </el-button>
      </el-form-item>

      <el-form-item class="login-link">
        <span>已有账号？</span>
        <el-link
          type="primary"
          class="login-button"
          tabindex="12"
          @click="$emit('login')"
        >立即登录</el-link>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendVerificationCode } from '@/api/auth'
import logger from '@/utils/logger'
import PasswordStrengthIndicator from '@/components/common/PasswordStrengthIndicator.vue'

defineProps<{
  isLoading?: boolean
}>()

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'login'): void
}>()

const formRef = ref(null)
const currentStep = ref(1)
const isSendingCode = ref(false)
const countdown = ref(60)
let countdownTimer = null

const stepLabels = ['账户信息', '联系方式', '验证']

const formData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  code: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const step1Rules = {
  username: [
    { required: true, message: '请输入用户名' },
    { min: 2, max: 20, message: '用户名长度应在2-20个字符之间' }
  ],
  password: [
    { required: true, message: '请输入密码' },
    { min: 6, message: '密码长度至少为6个字符' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const step2Rules = {
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '请输入正确的邮箱格式' }
  ],
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式' }
  ]
}

const step3Rules = {
  code: [
    { required: true, message: '请输入验证码' },
    { min: 6, max: 6, message: '请输入6位数字验证码' }
  ]
}

const currentRules = computed(() => {
  if (currentStep.value === 1) { return step1Rules }
  if (currentStep.value === 2) { return step2Rules }
  return step3Rules
})

const canSendCode = computed(() => {
  return formData.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)
})

async function handleNext() {
  if (!formRef.value) { return }

  try {
    await formRef.value.validate()

    if (currentStep.value === 3) {
      emit('submit', {
        username: formData.username,
        password: formData.password,
        email: formData.email,
        phone: formData.phone,
        code: formData.code
      })
    } else if (currentStep.value === 2) {
      currentStep.value++
    } else {
      currentStep.value++
    }
  } catch (error) {
    // Validation failed
  }
}

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

const validate = () => {
  return formRef.value?.validate()
}

const resetForm = () => {
  formRef.value?.resetFields()
  currentStep.value = 1
}

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
.register-form-container {
  width: 100%;
}

.register-form {
  width: 100%;
}

/* Step indicator */
.register-steps {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0;
  margin-bottom: 32px;
  position: relative;
}

.register-steps::before {
  content: '';
  position: absolute;
  top: 14px;
  left: calc(50% - 80px);
  right: calc(50% - 80px);
  height: 2px;
  background: var(--border-color);
  z-index: 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
  z-index: 1;
  flex: 1;
  max-width: 100px;
}

.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: var(--bg-primary);
  border: 2px solid var(--border-color);
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.step.active .step-dot {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
  box-shadow: 0 0 0 4px var(--color-primary-subtle);
}

.step.completed .step-dot {
  background: var(--color-success);
  border-color: var(--color-success);
  color: white;
}

.step-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  transition: color var(--transition-fast);
}

.step.active .step-label {
  color: var(--color-primary);
}

.step.completed .step-label {
  color: var(--color-success);
}

/* Form items */
.el-form-item {
  margin-bottom: 20px;
}

.el-form-item :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 1px var(--border-color);
  transition: box-shadow var(--transition-fast);
}

.el-form-item :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-hover);
}

.el-form-item :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary-subtle), 0 0 0 1px var(--color-primary) !important;
}

/* Verification code */
.verification-code-container {
  display: flex;
  gap: 12px;
}

.verification-code-button {
  white-space: nowrap;
  min-width: 120px;
  border-radius: var(--radius-md);
}

.verification-hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: -8px;
  margin-bottom: 16px;
}

/* Buttons */
.register-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.register-button {
  flex: 1;
  padding: 14px;
  font-size: 16px;
  font-weight: 500;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  border: none;
  color: white;
  transition: background var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);
}

.register-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  box-shadow: var(--shadow-primary);
  transform: translateY(-1px);
}

.register-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.register-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.back-button {
  padding: 14px 24px;
  font-size: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  background: transparent;
  transition: border-color var(--transition-fast), color var(--transition-fast);
}

.back-button:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.login-link span {
  color: var(--text-muted);
  margin-right: 8px;
}

.login-button {
  font-weight: 500;
  color: var(--color-primary);
}

.login-button:hover {
  text-decoration: underline;
}

.is-loading {
  margin-right: 8px;
}

@media (max-width: 768px) {
  .register-steps::before {
    left: calc(50% - 60px);
    right: calc(50% - 60px);
  }

  .step {
    max-width: 70px;
  }

  .step-label {
    font-size: 11px;
  }

  .verification-code-container {
    flex-direction: column;
  }

  .verification-code-button {
    width: 100%;
    min-width: unset;
  }

  .register-actions {
    flex-direction: column;
  }

  .back-button {
    order: 1;
  }
}
</style>
