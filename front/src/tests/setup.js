// 测试环境设置
import { vi } from 'vitest'

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