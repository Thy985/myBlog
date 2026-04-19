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
    <div class="captcha-image" @click="refreshCaptcha">
      <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
      <span v-else class="captcha-placeholder">点击刷新</span>
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

const emit = defineEmits(['update:modelValue', 'submit'])

const captchaValue = ref('')
const captchaImage = ref('')
const captchaId = ref('')

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
    const response = await getCaptcha()
    if (response && response.code === 200) {
      captchaImage.value = response.data
      captchaId.value = Date.now().toString()
    }
  } catch (error) {
    logger.error('获取验证码失败:', error)
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

.login-captcha .el-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--background-light);
  transition: border-color 0.2s;
}

.captcha-image:hover {
  border-color: var(--primary-color);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
