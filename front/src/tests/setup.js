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
  ArrowUp: { template: '<span class="el-icon-arrow-up"></span>' }
}))

// 忽略 Vue Router 警告
config.global.config.warnHandler = (msg, instance, trace) => {
  if (msg.includes('Vue Router')) {
    return
  }
}
