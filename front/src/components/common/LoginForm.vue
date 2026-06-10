<template>
  <div class="login-form-container">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="top"
      class="login-form"
      @keydown.enter="handleSubmit"
    >
      <el-form-item prop="account">
        <el-input
          v-model="formData.account"
          placeholder="请输入用户名/邮箱/手机号"
          size="large"
          prefix-icon="User"
          tabindex="1"
          autocomplete="username"
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
          autocomplete="current-password"
        />
      </el-form-item>

      <el-form-item prop="captcha">
        <LoginCaptcha
          ref="captchaRef"
          v-model="formData.captcha"
          @update:captcha-id="formData.captchaId = $event"
          @submit="handleSubmit"
        />
      </el-form-item>

      <div class="login-form-actions">
        <el-checkbox
          v-model="formData.remember"
          class="remember-checkbox"
          tabindex="3"
        >保持登录状态</el-checkbox>
        <el-link
          type="primary"
          class="forgot-password-link"
          tabindex="4"
          @click="$emit('forgot-password')"
        >忘记密码？</el-link>
      </div>

      <el-form-item>
        <el-button
          type="primary"
          class="login-button"
          :loading="isLoading"
          size="large"
          tabindex="5"
          :disabled="isLoading"
          @click="handleSubmit"
        >
          <template v-if="isLoading">
            <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
            登录中...
          </template>
          <template v-else>登录</template>
        </el-button>
      </el-form-item>

      <el-form-item class="register-link">
        <span>还没有账号？</span>
        <el-link
          type="primary"
          class="register-button"
          tabindex="6"
          @click="$emit('register')"
        >立即注册</el-link>
      </el-form-item>

    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useValidationRules } from '@/composables/useValidation'
import LoginCaptcha from './LoginCaptcha.vue'

const { account, password, captcha } = useValidationRules()

defineProps<{
  isLoading?: boolean
}>()

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'forgot-password'): void
  (e: 'register'): void
}>()

const formRef = ref(null)
const captchaRef = ref(null)

const formData = reactive({
  account: '',
  password: '',
  remember: false,
  captcha: '',
  captchaId: ''
})

const formRules = {
  account: [account()],
  password: [password()]
}

const handleSubmit = async () => {
  if (!formRef.value) {return}

  try {
    await formRef.value.validate()
    emit('submit', {
      account: formData.account,
      password: formData.password,
      remember: formData.remember,
      captcha: formData.captcha,
      captchaId: formData.captchaId
    })
  } catch (error) {
    // 表单验证失败
  }
}

const validate = () => {
  return formRef.value?.validate()
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const refreshCaptcha = () => {
  if (captchaRef.value) {
    captchaRef.value.refreshCaptcha()
  }
}

defineExpose({
  validate,
  resetForm,
  refreshCaptcha,
  formData
})
</script>

<style scoped>
.login-form-container {
  width: 100%;
}

.login-form {
  width: 100%;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 1px var(--border-color);
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-hover);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary-subtle), 0 0 0 1px var(--color-primary) !important;
}

.login-form :deep(.el-input__wrapper.is-focus .el-input__prefix-icon) {
  color: var(--color-primary);
  transition: color var(--transition-fast);
}

.login-form :deep(.el-input__prefix-icon) {
  color: var(--text-muted);
  transition: color var(--transition-fast);
}

.login-form :deep(.el-form-item__error) {
  font-size: 12px;
  padding-top: 4px;
  animation: shake 0.4s ease;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-4px); }
  40%, 80% { transform: translateX(4px); }
}

.login-form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.remember-checkbox {
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.remember-checkbox:hover {
  color: var(--color-primary);
}

.forgot-password-link {
  font-size: 14px;
  color: var(--color-primary);
}

.forgot-password-link:hover {
  text-decoration: underline;
}

.login-button {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 500;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  border: none;
  color: white;
  transition: background var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);
}

.login-button:hover:not(:disabled) {
  background: var(--color-primary-hover);
  box-shadow: var(--shadow-primary);
  transform: translateY(-1px);
}

.login-button:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.login-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.register-link span {
  color: var(--text-muted);
  margin-right: 8px;
}

.register-button {
  font-weight: 500;
  color: var(--color-primary);
}

.register-button:hover {
  text-decoration: underline;
}

.is-loading {
  margin-right: 8px;
}

@media (max-width: 768px) {
  .login-form-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .forgot-password-link {
    align-self: flex-end;
  }
}
</style>
