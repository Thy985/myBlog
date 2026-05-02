<template>
  <div class="login-captcha">
    <el-input
      v-model="captchaValue"
      placeholder="请输入验证码"
      size="large"
      prefix-icon="el-icon-circle-check"
      tabindex="1"
      @keyup.enter="$emit('submit')"
    />
    <div class="captcha-image" :class="{ 'is-loading': isLoading }" @click="refreshCaptcha">
      <img v-if="captchaImage && !isLoading" :src="captchaImage" alt="验证码" />
      <el-icon v-else class="captcha-loading-icon"><i class="el-icon-loading"></i></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getCaptcha } from '@/api/auth'
import logger from '@/utils/logger'

defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'submit', 'update:captchaId'])

const captchaValue = ref('')
const captchaImage = ref('')
const captchaId = ref('')
const isLoading = ref(false)

// 监听输入变化
watch(captchaValue, (newVal) => {
  emit('update:modelValue', newVal)
})

// 初始化获取验证码
onMounted(() => {
  refreshCaptcha()
})

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    isLoading.value = true
    captchaImage.value = ''
    captchaId.value = ''
    const response = await getCaptcha()
    if (response && response.code === 200 && response.data) {
      captchaImage.value = response.data.image
      captchaId.value = response.data.captchaId || ''
      emit('update:captchaId', captchaId.value)
    }
  } catch (error) {
    logger.error('获取验证码失败:', error)
  } finally {
    isLoading.value = false
  }
}

defineExpose({
  refreshCaptcha
})
</script>

<style scoped>
.login-captcha {
  display: flex;
  gap: 10px;
  align-items: center;
}

.login-captcha :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 1px var(--border-color);
  transition: box-shadow var(--transition-fast);
}

.login-captcha :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-hover);
}

.login-captcha :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary-subtle), 0 0 0 1px var(--color-primary) !important;
}

.login-captcha .el-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);
}

.captcha-image:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-glow-sm);
  transform: scale(1.02);
}

.captcha-image:active {
  transform: scale(0.98);
}

.captcha-image.is-loading {
  cursor: wait;
}

.captcha-loading-icon {
  font-size: 18px;
  color: var(--color-primary);
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
