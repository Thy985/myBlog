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

      <div class="login-divider">
        <span>其他登录方式</span>
      </div>

      <div class="login-social">
        <el-button
          type="default"
          circle
          icon="el-icon-chat-dot-round"
          class="social-btn"
          tabindex="7"
          @click="$emit('social-login', 'chat')"
        />
        <el-button
          type="default"
          circle
          icon="el-icon-s-grid"
          class="social-btn"
          tabindex="8"
          @click="$emit('social-login', 'grid')"
        />
        <el-button
          type="default"
          circle
          icon="el-icon-video-camera"
          class="social-btn"
          tabindex="9"
          @click="$emit('social-login', 'video')"
        />
      </div>
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
  'register',
  'social-login'
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

const validate = async () => {
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
    transition: all 0.2s ease;
}

.remember-checkbox:hover {
    color: var(--color-primary);
}

.forgot-password-link {
  font-size: 14px;
  transition: all 0.2s ease;
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
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
    border: none;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
}

.login-button:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px var(--color-primary-glow);
}

.login-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: all 0.6s ease;
}

.login-button:hover::before {
  left: 100%;
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
  transition: all 0.2s ease;
}

.register-button:hover {
  text-decoration: underline;
}

.login-divider {
  display: flex;
  align-items: center;
  margin: 32px 0;
  text-align: center;
}

.login-divider::before,
.login-divider::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--border-color);
}

.login-divider span {
    padding: 0 16px;
    color: var(--text-muted);
    font-size: 12px;
}

.login-social {
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
    border: 1px solid var(--border-color);
    cursor: pointer;
}

.social-btn:hover {
    transform: translateY(-3px) scale(1.05);
    box-shadow: var(--shadow-md);
    border-color: var(--color-primary);
    color: var(--color-primary);
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
