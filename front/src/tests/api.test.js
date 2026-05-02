import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { request, requestWithCache, clearCache, clearAllCache } from '@/composables/api'
import { ElMessage, ElLoading } from 'element-plus'
import { showPageLoading, hidePageLoading } from '@/utils/loading'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  ElLoading: {
    service: vi.fn(() => ({
      close: vi.fn()
    }))
  }
}))

vi.mock('@/utils/loading', () => ({
  showPageLoading: vi.fn(),
  hidePageLoading: vi.fn()
}))

vi.spyOn(localStorage, 'getItem').mockImplementation()
vi.spyOn(localStorage, 'setItem').mockImplementation()
vi.spyOn(localStorage, 'removeItem').mockImplementation()
vi.spyOn(localStorage, 'clear').mockImplementation()

describe('API请求处理测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('应该处理成功的API请求', async () => {
    const mockApiFunc = vi.fn().mockResolvedValue({ code: 200, data: { id: 1, name: '测试数据' } })

    const result = await request(mockApiFunc)

    expect(mockApiFunc).toHaveBeenCalled()
    expect(result).toEqual({ code: 200, data: { id: 1, name: '测试数据' } })
  })

  it('应该处理失败的API请求', async () => {
    const mockApiFunc = vi.fn().mockResolvedValue({ code: 500, message: '请求失败' })

    await expect(request(mockApiFunc)).rejects.toThrow('请求失败')
    expect(ElMessage.warning).toHaveBeenCalledWith('请求失败')
  })

  it('应该处理API请求异常', async () => {
    const mockApiFunc = vi.fn().mockRejectedValue(new Error('网络错误'))

    await expect(request(mockApiFunc)).rejects.toThrow('网络错误')
    expect(ElMessage.error).toHaveBeenCalledWith('网络错误')
  })

  it('应该显示加载动画', async () => {
    const mockApiFunc = vi.fn().mockResolvedValue({ code: 200, data: { id: 1, name: '测试数据' } })

    await request(mockApiFunc, undefined, { showLoading: true, loadingMessage: '加载中...' })

    expect(ElLoading.service).toHaveBeenCalledWith({
      lock: true,
      text: '加载中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
  })

  it('应该显示页面级加载动画', async () => {
    const mockApiFunc = vi.fn().mockResolvedValue({ code: 200, data: { id: 1, name: '测试数据' } })

    await request(mockApiFunc, undefined, { showPageLoading: true })

    expect(showPageLoading).toHaveBeenCalled()
    expect(hidePageLoading).toHaveBeenCalled()
  })

  it('应该显示成功提示', async () => {
    const mockApiFunc = vi.fn().mockResolvedValue({ code: 200, data: { id: 1, name: '测试数据' } })

    await request(mockApiFunc, undefined, { showSuccess: true, successMessage: '操作成功' })

    expect(ElMessage.success).toHaveBeenCalledWith('操作成功')
  })

  it('应该使用缓存数据', async () => {
    const mockApiFunc = vi.fn().mockResolvedValue({ code: 200, data: { id: 1, name: '测试数据' } })
    const mockCachedData = JSON.stringify({ code: 200, data: { id: 1, name: '缓存数据' } })

    localStorage.getItem.mockImplementation((key) => {
      if (key === 'blog_cache_test-cache') {
        return mockCachedData
      }
      if (key === 'blog_cache_test-cache_time') {
        return Date.now().toString()
      }
      return null
    })

    const result = await requestWithCache(mockApiFunc, 'test-cache')

    expect(localStorage.getItem).toHaveBeenCalledWith('blog_cache_test-cache')
    expect(mockApiFunc).not.toHaveBeenCalled()
    expect(result).toEqual({ code: 200, data: { id: 1, name: '缓存数据' } })
  })

  it('应该缓存API响应数据', async () => {
    const mockApiFunc = vi.fn().mockResolvedValue({ code: 200, data: { id: 1, name: '测试数据' } })

    localStorage.getItem.mockReturnValue(null)

    const result = await requestWithCache(mockApiFunc, 'test-cache')

    expect(mockApiFunc).toHaveBeenCalled()
    expect(localStorage.setItem).toHaveBeenCalledWith('blog_cache_test-cache', JSON.stringify({ code: 200, data: { id: 1, name: '测试数据' } }))
    expect(result).toEqual({ code: 200, data: { id: 1, name: '测试数据' } })
  })

  it('应该清除指定缓存', () => {
    clearCache('test-cache')
    expect(localStorage.removeItem).toHaveBeenCalledWith('blog_cache_test-cache')
    expect(localStorage.removeItem).toHaveBeenCalledWith('blog_cache_test-cache_time')
  })

  it('应该清除所有缓存', () => {
    localStorage.getItem.mockImplementation((key) => {
      if (key === 'blog_cache_theme') {
        return '{"theme":"dark"}'
      }
      if (key === 'blog_cache_blogSetting') {
        return '{"blogName":"test"}'
      }
      return null
    })

    Object.defineProperty(localStorage, 'length', {
      value: 2,
      writable: true
    })

    localStorage.key = (i) => {
      if (i === 0) return 'blog_cache_theme'
      if (i === 1) return 'blog_cache_blogSetting'
      return null
    }

    clearAllCache()

    expect(localStorage.removeItem).toHaveBeenCalledWith('blog_cache_theme')
    expect(localStorage.removeItem).toHaveBeenCalledWith('blog_cache_blogSetting')
  })
})
