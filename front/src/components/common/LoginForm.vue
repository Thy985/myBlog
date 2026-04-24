<template>
  <div class="login-form-container">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="top"
      class="login-form"
      autocomplete="off"
      @keydown.enter="handleSubmit"
    >
      <el-form-item prop="account">
        <el-input
          v-model="formData.account"
          placeholder="请输入用户名/邮箱/手机号"
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
      </el-form-item>

      <el-form-item prop="captcha">
        <LoginCaptcha
          v-model="formData.captcha"
          @submit="handleSubmit"
        />
      </el-form-item>

      <div class="login-form-actions">
        <el-checkbox
          v-model="formData.remember"
          class="remember-checkbox"
          tabindex="3"
        >记住密码</el-checkbox>
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

defineProps({
  isLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'submit',
  'forgot-password',
  'register'
])

const formRef = ref(null)

const formData = reactive({
  account: '',
  password: '',
  remember: false,
  captcha: ''
})

const formRules = {
  account: [account()],
  password: [password()],
  captcha: [captcha()]
}

const handleSubmit = async () => {
  if (!formRef.value) {return}

  try {
    await formRef.value.validate()
    emit('submit', {
      account: formData.account,
      password: formData.password,
      remember: formData.remember,
      captcha: formData.captcha
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

defineExpose({
  validate,
  resetForm,
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
    transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.remember-checkbox:hover {
    color: var(--color-primary);
}

.forgot-password-link {
  font-size: 14px;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.forgot-password-link:hover {
  text-decoration: underline;
}

.login-button {
    width: 100%;
    padding: 14px;
    font-size: 16px;
    font-weight: 500;
    border-radius: 8px;
    background: var(--color-primary);
    border: none;
    color: white;
    transition: background var(--transition-fast);
}

.login-button:hover:not(:disabled) {
    background: var(--color-primary-hover);
}

.login-button:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.register-link span {
    color: var(--text-secondary);
    margin-right: 8px;
}

.register-button {
  font-weight: 500;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
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
