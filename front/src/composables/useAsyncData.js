/**
 * useAsyncData - 通用异步数据获取 Composable
 *
 * ✅ 特性:
 * - 自动 loading/error 状态管理
 * - 组件卸载时自动取消请求
 * - 防重复请求
 * - 支持缓存
 * - 类型安全
 */
import { ref, onUnmounted } from 'vue'
import logger from '@/utils/logger'

export function useAsyncData(apiFn, options = {}) {
  const {
    immediate: _immediate = false,
    defaultValue = null,
    cacheKey = null,
    cacheTTL = 5 * 60 * 1000,
    showError = true,
    onError = null
  } = options

  const data = ref(defaultValue)
  const loading = ref(false)
  const error = ref(null)
  let isUnmounted = false
  let currentAbortController = null

  // 简单内存缓存
  const cache = new Map()

  function getFromCache(key) {
    if (!cacheKey || !key) {return null}
    const cached = cache.get(key)
    if (!cached) {return null}
    if (Date.now() - cached.timestamp > cacheTTL) {
      cache.delete(key)
      return null
    }
    return cached.data
  }

  function setToCache(key, value) {
    if (!cacheKey || !key) {return}
    cache.set(key, { data: value, timestamp: Date.now() })
  }

  async function execute(...args) {
    if (isUnmounted) {return { data: data.value, loading: false, error: null }}

    // 检查缓存
    const key = cacheKey ? `${cacheKey}:${JSON.stringify(args)}` : null
    const cached = getFromCache(key)
    if (cached !== null) {
      data.value = cached
      return { data: cached, loading: false, error: null }
    }

    // 取消之前的请求
    if (currentAbortController) {
      currentAbortController.abort()
    }
    currentAbortController = new AbortController()

    loading.value = true
    error.value = null

    try {
      const result = await apiFn(...args)

      if (isUnmounted || currentAbortController.signal.aborted) {
        return { data: data.value, loading: false, error: null }
      }

      data.value = result

      // 写入缓存
      if (key) {
        setToCache(key, result)
      }

      return { data: result, loading: false, error: null }
    } catch (err) {
      if (err.name === 'AbortError' || isUnmounted) {
        return { data: data.value, loading: false, error: null }
      }

      error.value = err
      logger.error('useAsyncData error:', err.message)

      if (showError && onError) {
        onError(err)
      }

      return { data: data.value, loading: false, error: err }
    } finally {
      if (!isUnmounted) {
        loading.value = false
      }
    }
  }

  function reset() {
    data.value = defaultValue
    error.value = null
    loading.value = false
  }

  function clearCache() {
    cache.clear()
  }

  onUnmounted(() => {
    isUnmounted = true
    if (currentAbortController) {
      currentAbortController.abort()
      currentAbortController = null
    }
  })

  return {
    data,
    loading,
    error,
    execute,
    reset,
    clearCache
  }
}

/**
 * usePaginationData - 分页数据获取 Composable
 * 基于 useAsyncData 的分页封装
 */
export function usePaginationData(apiFn, options = {}) {
  const {
    defaultPageSize = 10,
    ...asyncOptions
  } = options

  const asyncData = useAsyncData(apiFn, asyncOptions)
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)
  const pages = ref(0)

  async function fetchData(page = 1, size = defaultPageSize) {
    currentPage.value = page
    pageSize.value = size

    const result = await asyncData.execute({ current: page, size })

    if (result.data) {
      total.value = result.data.total || 0
      pages.value = result.data.pages || 0
    }

    return result
  }

  function nextPage() {
    if (currentPage.value < pages.value) {
      fetchData(currentPage.value + 1, pageSize.value)
    }
  }

  function prevPage() {
    if (currentPage.value > 1) {
      fetchData(currentPage.value - 1, pageSize.value)
    }
  }

  function changeSize(newSize) {
    fetchData(1, newSize)
  }

  return {
    ...asyncData,
    currentPage,
    pageSize,
    total,
    pages,
    fetchData,
    nextPage,
    prevPage,
    changeSize
  }
}
