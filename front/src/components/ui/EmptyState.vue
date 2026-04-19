<template>
  <div class="empty-state-container" :class="{ 'empty-state-compact': compact }">
    <!-- 图标区域 -->
    <div class="empty-state-icon-wrapper">
      <div class="empty-state-icon-bg">
        <slot name="icon">
          <el-icon class="empty-state-icon">
            <component :is="iconComponent" />
          </el-icon>
        </slot>
      </div>
    </div>

    <!-- 标题 -->
    <h3 v-if="title" class="empty-state-title">{{ title }}</h3>

    <!-- 描述 -->
    <p v-if="description" class="empty-state-description">{{ description }}</p>

    <!-- 默认操作按钮 -->
    <div v-if="showAction && hasAction" class="empty-state-actions">
      <slot name="action">
        <button
          v-if="actionText"
          class="empty-state-btn empty-state-btn-primary"
          @click="handleAction"
        >
          {{ actionText }}
        </button>
        <button
          v-if="secondaryActionText"
          class="empty-state-btn empty-state-btn-secondary"
          @click="handleSecondaryAction"
        >
          {{ secondaryActionText }}
        </button>
      </slot>
    </div>

    <!-- 情感化提示 -->
    <div v-if="showTip" class="empty-state-tip">
      <slot name="tip">{{ tip }}</slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Document,
  ChatLineRound,
  Folder,
  Search,
  Bell,
  User,
  Picture,
  Link,
  Star
} from '@element-plus/icons-vue'

const props = defineProps({
  // 图标类型
  icon: {
    type: String,
    default: 'document',
    validator: (val) => [
      'document', 'chat', 'folder', 'search', 'bell', 'user', 'picture', 'link', 'star'
    ].includes(val)
  },
  // 标题
  title: {
    type: String,
    default: '暂无数据'
  },
  // 描述
  description: {
    type: String,
    default: ''
  },
  // 主要操作按钮文字
  actionText: {
    type: String,
    default: ''
  },
  // 次要操作按钮文字
  secondaryActionText: {
    type: String,
    default: ''
  },
  // 情感化提示
  tip: {
    type: String,
    default: ''
  },
  // 是否显示操作按钮区域
  showAction: {
    type: Boolean,
    default: true
  },
  // 是否显示情感化提示
  showTip: {
    type: Boolean,
    default: true
  },
  // 紧凑模式
  compact: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['action', 'secondary-action'])

const iconMap = {
  document: Document,
  chat: ChatLineRound,
  folder: Folder,
  search: Search,
  bell: Bell,
  user: User,
  picture: Picture,
  link: Link,
  star: Star
}

const iconComponent = computed(() => iconMap[props.icon] || Document)

const hasAction = computed(() => {
  return props.actionText || props.secondaryActionText
})

const handleAction = () => {
  emit('action')
}

const handleSecondaryAction = () => {
  emit('secondary-action')
}
</script>

<style scoped>
.empty-state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-state-container.empty-state-compact {
  padding: 24px 16px;
}

.empty-state-icon-wrapper {
  margin-bottom: 20px;
}

.empty-state-icon-bg {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-subtle) 0%, var(--color-primary) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px var(--color-primary-subtle);
  opacity: 0.9;
}

.empty-state-compact .empty-state-icon-bg {
  width: 56px;
  height: 56px;
}

.empty-state-icon {
  font-size: 36px;
  color: var(--color-primary);
}

.empty-state-compact .empty-state-icon {
  font-size: 24px;
}

.empty-state-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.empty-state-compact .empty-state-title {
  font-size: 16px;
  margin-bottom: 6px;
}

.empty-state-description {
  font-size: 14px;
  color: var(--text-tertiary);
  max-width: 320px;
  line-height: 1.6;
  margin: 0 0 20px 0;
}

.empty-state-compact .empty-state-description {
  font-size: 13px;
  margin-bottom: 16px;
}

.empty-state-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.empty-state-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  outline: none;
}

.empty-state-btn:focus {
  box-shadow: 0 0 0 3px var(--color-primary-subtle);
}

.empty-state-btn-primary {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary) 100%);
  color: white;
}

.empty-state-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px var(--color-primary-subtle);
}

.empty-state-btn-secondary {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.empty-state-btn-secondary:hover {
  background: var(--bg-secondary);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.empty-state-tip {
  margin-top: 16px;
  font-size: 12px;
  color: var(--text-muted);
}

/* 暗色模式 */
.dark .empty-state-title {
  color: var(--text-primary);
}

.dark .empty-state-description {
  color: var(--text-tertiary);
}

.dark .empty-state-icon-bg {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
}

.dark .empty-state-btn-secondary {
  color: var(--text-secondary);
  border-color: var(--border-color);
  background: transparent;
}

.dark .empty-state-btn-secondary:hover {
  background: var(--bg-secondary);
}
</style>
