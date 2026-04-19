<template>
  <div class="error-state-container" :class="{ 'error-state-compact': compact }">
    <!-- 错误图标 -->
    <div class="error-state-icon-wrapper">
      <div class="error-state-icon-bg" :class="iconClass">
        <el-icon class="error-state-icon">
          <component :is="iconComponent" />
        </el-icon>
      </div>
    </div>

    <!-- 错误标题 -->
    <h3 class="error-state-title">{{ displayTitle }}</h3>

    <!-- 错误描述 -->
    <p class="error-state-description">{{ displayDescription }}</p>

    <!-- 操作按钮 -->
    <div v-if="showActions" class="error-state-actions">
      <button
        v-if="showRetry"
        class="error-state-btn error-state-btn-primary"
        @click="handleRetry"
      >
        <el-icon class="mr-2"><Refresh /></el-icon>
        重试
      </button>
      <button
        v-if="showBackHome"
        class="error-state-btn error-state-btn-secondary"
        @click="handleBackHome"
      >
        <el-icon class="mr-2"><HomeFilled /></el-icon>
        返回首页
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  WarningFilled,
  CircleCloseFilled,
  Link,
  HomeFilled,
  Refresh,
  Document
} from '@element-plus/icons-vue'

const props = defineProps({
  // 错误类型
  type: {
    type: String,
    default: 'generic',
    validator: (val) => [
      'generic', 'network', 'server', 'not-found', 'permission'
    ].includes(val)
  },
  // 自定义标题
  title: {
    type: String,
    default: ''
  },
  // 自定义描述
  description: {
    type: String,
    default: ''
  },
  // 是否显示重试按钮
  showRetry: {
    type: Boolean,
    default: true
  },
  // 是否显示返回首页按钮
  showBackHome: {
    type: Boolean,
    default: true
  },
  // 是否显示操作按钮区域
  showActions: {
    type: Boolean,
    default: true
  },
  // 紧凑模式
  compact: {
    type: Boolean,
    default: false
  },
  // 自定义重试回调
  onRetry: {
    type: Function,
    default: null
  }
})

const router = useRouter()

const iconMap = {
  generic: WarningFilled,
  network: Link,
  server: CircleCloseFilled,
  'not-found': Document,
  permission: WarningFilled
}

const errorMessages = {
  generic: {
    title: '加载失败',
    description: '抱歉，发生了未知错误，请稍后重试'
  },
  network: {
    title: '网络连接失败',
    description: '请检查您的网络连接后重试'
  },
  server: {
    title: '服务器错误',
    description: '服务器暂时无法响应，请稍后重试'
  },
  'not-found': {
    title: '内容不存在',
    description: '抱歉，您访问的内容不存在或已被删除'
  },
  permission: {
    title: '无权访问',
    description: '抱歉，您没有权限访问此内容'
  }
}

const iconComponent = computed(() => iconMap[props.type] || WarningFilled)

const iconClassMap = {
  generic: 'error-icon-generic',
  network: 'error-icon-network',
  server: 'error-icon-server',
  'not-found': 'error-icon-not-found',
  permission: 'error-icon-permission'
}

const iconClass = computed(() => iconClassMap[props.type] || 'error-icon-generic')

const displayTitle = computed(() => props.title || errorMessages[props.type].title)
const displayDescription = computed(() => props.description || errorMessages[props.type].description)

const handleRetry = () => {
  if (props.onRetry && typeof props.onRetry === 'function') {
    props.onRetry()
  } else {
    router.go(0)
  }
}

const handleBackHome = () => {
  router.push('/')
}
</script>

<style scoped>
.error-state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.error-state-container.error-state-compact {
  padding: 24px 16px;
}

.error-state-icon-wrapper {
  margin-bottom: 20px;
}

.error-state-icon-bg {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.9;
}

.error-state-compact .error-state-icon-bg {
  width: 56px;
  height: 56px;
}

.error-icon-generic {
  background: linear-gradient(135deg, #fef3c7 0%, #fcd34d 100%);
}

.error-icon-network {
  background: linear-gradient(135deg, #dbeafe 0%, #60a5fa 100%);
}

.error-icon-server {
  background: linear-gradient(135deg, #fee2e2 0%, #f87171 100%);
}

.error-icon-not-found {
  background: linear-gradient(135deg, #f3e8ff 0%, #a78bfa 100%);
}

.error-icon-permission {
  background: linear-gradient(135deg, #fef3c7 0%, #f59e0b 100%);
}

.error-state-icon {
  font-size: 36px;
  color: var(--text-primary);
}

.error-state-compact .error-state-icon {
  font-size: 24px;
}

.error-icon-generic .error-state-icon {
  color: #d97706;
}

.error-icon-network .error-state-icon {
  color: #2563eb;
}

.error-icon-server .error-state-icon {
  color: #dc2626;
}

.error-icon-not-found .error-state-icon {
  color: #7c3aed;
}

.error-icon-permission .error-state-icon {
  color: #d97706;
}

.error-state-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.error-state-compact .error-state-title {
  font-size: 16px;
  margin-bottom: 6px;
}

.error-state-description {
  font-size: 14px;
  color: var(--text-tertiary);
  max-width: 320px;
  line-height: 1.6;
  margin: 0 0 20px 0;
}

.error-state-compact .error-state-description {
  font-size: 13px;
  margin-bottom: 16px;
}

.error-state-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.error-state-btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  outline: none;
}

.error-state-btn:focus {
  box-shadow: 0 0 0 3px var(--primary-light);
}

.error-state-btn-primary {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
}

.error-state-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px var(--primary-light);
}

.error-state-btn-secondary {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.error-state-btn-secondary:hover {
  background: var(--background-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 暗色模式 */
.dark .error-state-title {
  color: var(--text-primary);
}

.dark .error-state-description {
  color: var(--text-tertiary);
}

.dark .error-state-btn-secondary {
  color: var(--text-secondary);
  border-color: var(--border-color);
  background: transparent;
}

.dark .error-state-btn-secondary:hover {
  background: var(--background-light);
}
</style>
