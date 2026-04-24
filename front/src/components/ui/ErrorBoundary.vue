<template>
  <slot v-if="!hasError" />
  <div v-else class="error-boundary" :class="{ 'error-boundary-compact': compact }">
    <div class="error-boundary-content">
      <div class="error-boundary-icon-wrapper">
        <div class="error-boundary-icon-bg">
          <el-icon class="error-boundary-icon">
            <WarningFilled />
          </el-icon>
        </div>
      </div>
      <h3 class="error-boundary-title">组件加载失败</h3>
      <p class="error-boundary-description">{{ errorMessage || '抱歉，发生了未知错误' }}</p>
      <div class="error-boundary-actions">
        <button class="error-boundary-btn error-boundary-btn-primary" @click="handleRetry">
          <el-icon class="mr-1"><Refresh /></el-icon>
          重试
        </button>
        <button v-if="showHome" class="error-boundary-btn error-boundary-btn-secondary" @click="handleGoHome">
          <el-icon class="mr-1"><HomeFilled /></el-icon>
          返回首页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'
import { WarningFilled, Refresh, HomeFilled } from '@element-plus/icons-vue'

const props = defineProps({
  compact: {
    type: Boolean,
    default: false
  },
  showHome: {
    type: Boolean,
    default: true
  },
  onError: {
    type: Function,
    default: null
  }
})

const router = useRouter()
const hasError = ref(false)
const errorMessage = ref('')
const errorInfo = ref(null)

onErrorCaptured((err, instance, info) => {
  hasError.value = true
  errorMessage.value = err?.message || '未知错误'
  errorInfo.value = {
    message: err?.message,
    info,
    componentName: instance?.$options?.name || 'Unknown'
  }

  if (props.onError && typeof props.onError === 'function') {
    props.onError(err, errorInfo.value)
  }

  console.error('[ErrorBoundary]', err, info)
  return false
})

function handleRetry() {
  hasError.value = false
  errorMessage.value = ''
  errorInfo.value = null
}

function handleGoHome() {
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  padding: 48px 24px;
}

.error-boundary-compact {
  min-height: 150px;
  padding: 24px 16px;
}

.error-boundary-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 400px;
}

.error-boundary-icon-wrapper {
  margin-bottom: 16px;
}

.error-boundary-icon-bg {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-error-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
}

.error-boundary-compact .error-boundary-icon-bg {
  width: 48px;
  height: 48px;
}

.error-boundary-icon {
  font-size: 28px;
  color: var(--color-error);
}

.error-boundary-compact .error-boundary-icon {
  font-size: 20px;
}

.error-boundary-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.error-boundary-compact .error-boundary-title {
  font-size: 16px;
  margin-bottom: 6px;
}

.error-boundary-description {
  font-size: 14px;
  color: var(--text-tertiary);
  line-height: 1.6;
  margin: 0 0 20px 0;
  word-break: break-word;
}

.error-boundary-compact .error-boundary-description {
  font-size: 13px;
  margin-bottom: 16px;
}

.error-boundary-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.error-boundary-btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
  border: none;
  outline: none;
}

.error-boundary-btn:focus {
  box-shadow: 0 0 0 3px var(--primary-light);
}

.error-boundary-btn-primary {
  background: var(--color-primary);
  color: white;
}

.error-boundary-btn-primary:hover {
  background: var(--color-primary-hover);
}

.error-boundary-btn-secondary {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.error-boundary-btn-secondary:hover {
  background: var(--background-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.dark .error-boundary-title {
  color: var(--text-primary);
}

.dark .error-boundary-description {
  color: var(--text-tertiary);
}

.dark .error-boundary-btn-secondary {
  color: var(--text-secondary);
  border-color: var(--border-color);
  background: transparent;
}

.dark .error-boundary-btn-secondary:hover {
  background: var(--background-light);
}
</style>
