// 测试环境设置
import { vi } from 'vitest'
import { config } from '@vue/test-utils'

// 模拟环境变量
vi.stubEnv('VITE_APP_BASE_API', 'http://localhost:8080/api')
vi.stubEnv('NODE_ENV', 'test')

// 模拟浏览器API
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn()
}))

// 模拟localStorage
const localStorageMock = (() => {
  let store = {}
  return {
    getItem(key) {
      return store[key] || null
    },
    setItem(key, value) {
      store[key] = value.toString()
    },
    removeItem(key) {
      delete store[key]
    },
    clear() {
      store = {}
    },
    key(index) {
      return Object.keys(store)[index] || null
    }
  }
})()

global.localStorage = localStorageMock

// 模拟document.cookie
document.cookie = ''
Object.defineProperty(document, 'cookie', {
  writable: true,
  value: ''
})

// Element Plus 组件模拟
vi.mock('element-plus', () => {
  const mockElMessage = {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  }

  return {
    ElMessage: mockElMessage,
    ElMessageBox: {
      confirm: vi.fn(),
      alert: vi.fn(),
      prompt: vi.fn()
    },
    ElLoading: {
      service: vi.fn(() => ({
        close: vi.fn()
      }))
    }
  }
})

// Element Plus Icons
vi.mock('@element-plus/icons-vue', () => ({
  Star: { template: '<span class="el-icon-star"></span>' },
  StarFilled: { template: '<span class="el-icon-star-filled"></span>' },
  ChatLineRound: { template: '<span class="el-icon-chat"></span>' },
  Delete: { template: '<span class="el-icon-delete"></span>' },
  ArrowDown: { template: '<span class="el-icon-arrow-down"></span>' },
  ArrowUp: { template: '<span class="el-icon-arrow-up"></span>' },
  Document: { template: '<span class="el-icon-document"></span>' },
  Folder: { template: '<span class="el-icon-folder"></span>' },
  Search: { template: '<span class="el-icon-search"></span>' },
  Bell: { template: '<span class="el-icon-bell"></span>' },
  User: { template: '<span class="el-icon-user"></span>' },
  Picture: { template: '<span class="el-icon-picture"></span>' },
  Link: { template: '<span class="el-icon-link"></span>' }
}))

// Mock child components that have TypeScript and cause parsing issues in vitest
// (Also aliased in vitest.config.js test.alias)
vi.mock('@/components/ui/EmptyState.vue', () => ({
  default: {
    name: 'EmptyState',
    props: ['icon', 'title', 'description', 'actionText', 'showTip', 'compact', 'secondaryActionText', 'showAction'],
    emits: ['action', 'secondary-action'],
    template: '<div class="empty-state"><slot></slot></div>'
  }
}))

vi.mock('@/components/common/CommentItem.vue', () => ({
  default: {
    name: 'CommentItem',
    props: ['comment', 'isReply'],
    emits: ['reply', 'like', 'delete', 'load-replies'],
    template: `
      <div class="comment-item">
        <div class="comment-meta">
          <span class="username">{{ comment.username }}</span>
        </div>
        <span class="comment-content">{{ comment.content }}</span>
        <button v-if="comment.replyCount > 0" class="action-btn">查看{{ comment.replyCount }}条回复</button>
      </div>
    `
  }
}))

// Global stubs for common child components
config.global.stubs = {
  CommentItem: {
    name: 'CommentItem',
    props: ['comment', 'isReply'],
    emits: ['reply', 'like', 'delete', 'load-replies'],
    template: '<div class="comment-item"><span class="comment-content">{{ comment.content }}</span></div>'
  },
  EmptyState: {
    name: 'EmptyState',
    props: ['icon', 'title', 'description', 'actionText', 'showTip', 'compact', 'secondaryActionText', 'showAction'],
    emits: ['action', 'secondary-action'],
    template: '<div class="empty-state"><slot></slot></div>'
  }
}

// Mock Element Plus components globally
const ElButton = {
  name: 'ElButton',
  props: ['type', 'size', 'loading', 'disabled', 'text', 'plain', 'circle', 'link'],
  inheritAttrs: false,
  template: `
    <button
      v-bind="$attrs"
      :class="[
        'el-button',
        type ? 'el-button--' + type : '',
        size ? 'el-button--' + size : '',
        { 'is-loading': loading, 'is-disabled': disabled || loading }
      ]"
      :disabled="disabled || loading"
    >
      <slot></slot>
    </button>
  `
}
const ElAvatar = {
  name: 'ElAvatar',
  props: ['size', 'src', 'shape'],
  template: '<span class="el-avatar"><slot></slot></span>'
}
const ElIcon = {
  name: 'ElIcon',
  template: '<span class="el-icon"><slot></slot></span>'
}
const ElInput = {
  name: 'ElInput',
  props: ['modelValue', 'type', 'placeholder', 'rows', 'maxlength', 'showWordLimit', 'show-word-limit', 'resize'],
  emits: ['update:modelValue', 'focus', 'blur'],
  inheritAttrs: false,
  template: `
    <div class="el-input" :class="{ 'el-textarea': type === 'textarea' }">
      <textarea
        v-if="type === 'textarea'"
        class="el-textarea__inner"
        :placeholder="placeholder"
        :rows="rows"
        :maxlength="maxlength"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        @focus="$emit('focus')"
        @blur="$emit('blur')"
      ></textarea>
      <input
        v-else
        class="el-input__inner"
        :placeholder="placeholder"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        @focus="$emit('focus')"
        @blur="$emit('blur')"
      />
      <div v-if="showWordLimit || $attrs['show-word-limit'] !== undefined || $attrs['showWordLimit'] !== undefined" class="el-input__count">
        <span class="el-input__count-inner">{{ (modelValue || '').length }} / {{ maxlength || '无限制' }}</span>
      </div>
    </div>
  `
}
config.global.components = { ElButton, ElAvatar, ElIcon, ElInput }

// 忽略 Vue Router 警告
config.global.config.warnHandler = (msg, instance, trace) => {
  if (msg.includes('Vue Router')) {
    return
  }
}
