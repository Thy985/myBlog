<template>
  <div class="forgot-password-page" @keydown.enter="handleResetPassword">
    <div class="forgot-password-container">
      <div class="forgot-password-header">
        <div class="logo">
          <i class="el-icon-sunny forgot-password-logo-icon"></i>
          <h1 class="forgot-password-logo-text">XingChen博客</h1>
        </div>
        <h2 class="forgot-password-title">忘记密码</h2>
        <p class="forgot-password-subtitle">重置您的密码，重新访问您的账号</p>
      </div>
      
      <el-form
        ref="forgotPasswordFormRef"
        :model="forgotPasswordForm"
        :rules="forgotPasswordFormRules"
        label-position="top"
        class="forgot-password-form"
        autocomplete="off"
      >
        <el-form-item prop="account">
          <el-input 
            v-model="forgotPasswordForm.account" 
            placeholder="请输入邮箱/手机号"
            size="large"
            :prefix-icon="isEmail ? 'el-icon-message' : 'el-icon-phone'"
            tabindex="1"
          />
        </el-form-item>
        
        <el-form-item prop="code">
          <div class="verification-code-container">
            <el-input
              v-model="forgotPasswordForm.code"
              placeholder="请输入验证码"
              size="large"
              prefix-icon="el-icon-key"
              tabindex="2"
            />
            <el-button
              type="primary"
              :disabled="!canSendCode || isSendingCode"
              class="verification-code-button"
              tabindex="3"
              @click="sendVerificationCode"
            >
              {{ isSendingCode ? `${countdown}s后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="forgotPasswordForm.password"
            type="password"
            placeholder="请输入新密码"
            show-password
            size="large"
            prefix-icon="el-icon-lock"
            tabindex="4"
          />
          
          <!-- 密码强度指示器 -->
          <div v-if="forgotPasswordForm.password" class="password-strength">
            <div class="password-strength-label">密码强度：</div>
            <div class="password-strength-bars">
              <div 
                v-for="n in 4" 
                :key="n"
                class="password-strength-bar"
                :class="{
                  'weak': passwordStrength === 'weak' && n <= 1,
                  'medium': passwordStrength === 'medium' && n <= 2,
                  'strong': passwordStrength === 'strong' && n <= 3,
                  'very-strong': passwordStrength === 'very-strong' && n <= 4
                }"
              ></div>
            </div>
            <div class="password-strength-text">{{ passwordStrengthText }}</div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="forgot-password-button"
            :loading="isLoading"
            size="large"
            tabindex="5"
            :disabled="isLoading"
            @click="handleResetPassword"
          >
            <template v-if="isLoading">
              <el-icon class="is-loading"><i class="el-icon-loading"></i></el-icon>
              重置中...
            </template>
            <template v-else>重置密码</template>
          </el-button>
        </el-form-item>
        
        <el-form-item class="login-link">
          <span>想起密码了？</span>
          <el-link 
            type="primary" 
            class="login-button" 
            tabindex="6"
            @click="goToLogin"
          >立即登录</el-link>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendVerificationCode as sendCodeApi, resetPassword as resetPasswordApi } from '@/api/auth'

import logger from '@/utils/logger'
const router = useRouter()
const forgotPasswordFormRef = ref(null)
const isLoading = ref(false)
const isSendingCode = ref(false)
const countdown = ref(60)

const forgotPasswordForm = reactive({
  account: '',
  code: '',
  password: ''
})

const forgotPasswordFormRules = {
  account: [
    { required: true, message: '请输入邮箱/手机号' }
  ],
  code: [
    { required: true, message: '请输入验证码' },
    { min: 6, max: 6, message: '验证码长度为6个字符' }
  ],
  password: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码长度至少为6个字符' }
  ]
}

// 判断输入的是邮箱还是手机号
const isEmail = computed(() => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(forgotPasswordForm.account)
})

const isPhone = computed(() => {
  return /^1[3-9]\d{9}$/.test(forgotPasswordForm.account)
})

// 计算是否可以发送验证码
const canSendCode = computed(() => {
  return (isEmail.value || isPhone.value) && forgotPasswordForm.account
})

// 密码强度计算
const passwordStrength = computed(() => {
  const password = forgotPasswordForm.password
  if (!password) {return ''}
  
  let score = 0
  if (password.length >= 6) {score++}
  if (password.length >= 8) {score++}
  if (/[A-Z]/.test(password)) {score++}
  if (/[0-9]/.test(password)) {score++}
  if (/[^A-Za-z0-9]/.test(password)) {score++}
  
  if (score <= 2) {return 'weak'}
  if (score === 3) {return 'medium'}
  if (score === 4) {return 'strong'}
  return 'very-strong'
})

const passwordStrengthText = computed(() => {
  switch (passwordStrength.value) {
    case 'weak': return '弱'
    case 'medium': return '中'
    case 'strong': return '强'
    case 'very-strong': return '非常强'
    default: return ''
  }
})

// 发送验证码
const sendVerificationCode = async () => {
  // 简单验证账号格式
  if (!forgotPasswordForm.account) {
    ElMessage.error('请输入邮箱/手机号')
    return
  }
  if (!isEmail.value && !isPhone.value) {
    ElMessage.error('请输入正确的邮箱或手机号格式')
    return
  }
  
  try {
    isSendingCode.value = true
    
    // 调用发送验证码API
    const response = await sendCodeApi({
      [isEmail.value ? 'email' : 'phone']: forgotPasswordForm.account,
      type: 'forgot'
    })
    
    if (response.code === 200) {
      ElMessage.success(`验证码发送成功，请查收${isEmail.value ? '邮箱' : '手机'}`)
      
      // 开始倒计时
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
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

// 处理重置密码
const handleResetPassword = async () => {
  if (!forgotPasswordFormRef.value) {return}
  
  try {
    // 验证表单
    await forgotPasswordFormRef.value.validate()
    
    isLoading.value = true
    
    // 调用重置密码API
    const response = await resetPasswordApi({
      [isEmail.value ? 'email' : 'phone']: forgotPasswordForm.account,
      code: forgotPasswordForm.code,
      password: forgotPasswordForm.password
    })
    
    if (response.code === 200) {
      ElMessage.success('密码重置成功')
      
      // 跳转到登录页面
      setTimeout(() => {
        router.push('/login')
      }, 1000)
    } else {
      ElMessage.error(response.message || '密码重置失败')
    }
  } catch (error) {
    // 处理网络错误
    if (error.response) {
      // 服务器返回错误状态码
      switch (error.response.status) {
        case 400:
          ElMessage.error('重置信息有误，请检查后重试')
          break
        case 401:
          ElMessage.error('验证码错误或已过期')
          break
        default:
          ElMessage.error('密码重置失败，请稍后重试')
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      ElMessage.error('网络连接失败，请检查网络设置')
    } else {
      // 请求配置出错
      ElMessage.error('密码重置失败，请稍后重试')
    }
    logger.error('重置密码失败:', error)
  } finally {
    isLoading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}

// 页面加载时的初始化
onMounted(() => {
  // autofocus is handled by el-input's tabindex
})
</script>

<style scoped>
.forgot-password-page {
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

.forgot-password-container {
  width: 100%;
  max-width: 450px;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  padding: 40px;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  backdrop-filter: blur(10px);
}

.forgot-password-container:hover {
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.15);
  transform: translateY(-5px);
}

.forgot-password-header {
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

.forgot-password-logo-icon {
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

.forgot-password-logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.forgot-password-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.forgot-password-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.forgot-password-form {
  width: 100%;
  animation: fadeIn 0.8s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.verification-code-container {
  display: flex;
  gap: 12px;
}

.verification-code-button {
  white-space: nowrap;
  min-width: 120px;
}

.forgot-password-button {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  position: relative;
  overflow: hidden;
}

.forgot-password-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.forgot-password-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.forgot-password-button:hover::before {
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
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.login-button:hover {
  text-decoration: underline;
}

/* 表单错误消息 */
.form-error-message {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
  animation: shake 0.5s ease;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-5px); }
  40%, 80% { transform: translateX(5px); }
}

/* 密码强度指示器 */
.password-strength {
  margin-top: 12px;
  font-size: 12px;
}

.password-strength-label {
  color: #666;
  margin-bottom: 6px;
}

.password-strength-bars {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
}

.password-strength-bar {
  flex: 1;
  height: 4px;
  background: #eaeaea;
  border-radius: 2px;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.password-strength-bar.weak {
  background: #f56c6c;
}

.password-strength-bar.medium {
  background: #e6a23c;
}

.password-strength-bar.strong {
  background: #67c23a;
}

.password-strength-bar.very-strong {
  background: #409eff;
}

.password-strength-text {
  color: #666;
  font-size: 11px;
}

/* 输入框聚焦效果 */
:deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

/* 按钮加载动画 */
.is-loading {
  margin-right: 8px;
}

@media (max-width: 768px) {
  .forgot-password-container {
    padding: 30px 24px;
    max-width: 100%;
  }
  
  .forgot-password-title {
    font-size: 24px;
  }
  
  .forgot-password-logo-text {
    font-size: 20px;
  }
  
  .verification-code-container {
    flex-direction: column;
  }
  
  .verification-code-button {
    width: 100%;
    min-width: unset;
  }
  
  .password-strength {
    margin-top: 10px;
  }
}

@media (max-width: 480px) {
  .forgot-password-page {
    padding: 16px;
  }
  
  .forgot-password-container {
    padding: 24px 20px;
  }
  
  .forgot-password-title {
    font-size: 22px;
  }
  
  .forgot-password-button {
    padding: 12px;
    font-size: 15px;
  }
}

/* 无障碍支持 */
@media (prefers-reduced-motion: reduce) {
  .forgot-password-page {
    animation: none;
  }
  
  .forgot-password-logo-icon {
    animation: none;
  }
  
  .form-error-message {
    animation: none;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .forgot-password-container {
    border: 2px solid #000;
  }
  
  .forgot-password-button {
    border: 2px solid #000;
  }
}
</style>
