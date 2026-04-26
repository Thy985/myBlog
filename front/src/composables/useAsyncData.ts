import { ref, onUnmounted } from 'vue'
import logger from '@/utils/logger'

export interface UseAsyncDataOptions<T> {
  immediate?: boolean
  defaultValue?: T
  cacheKey?: string | null
  cacheTTL?: number
  showError?: boolean
  onError?: ((err: Error) => void) | null
}

export function useAsyncData<T>(
  apiFn: (...args: any[]) => Promise<T>,
  options: UseAsyncDataOptions<T> = {}
) {
  const {
    defaultValue = null as T,
    cacheKey = null as string | null,
    cacheTTL = 5 * 60 * 1000,
    showError = true,
    onError = null
  } = options

  const data = ref<T>(defaultValue)
  const loading = ref(false)
  const error = ref<Error | null>(null)
  let isUnmounted = false
  let currentAbortController: AbortController | null = null

  const cache = new Map<string, { data: T; timestamp: number }>()

  function getFromCache(key: string | null): T | null {
    if (!cacheKey || !key) {return null}
    const cached = cache.get(key)
    if (!cached) {return null}
    if (Date.now() - cached.timestamp > cacheTTL) {
      cache.delete(key)
      return null
    }
    return cached.data
  }

  function setToCache(key: string | null, value: T): void {
    if (!cacheKey || !key) {return}
    cache.set(key, { data: value, timestamp: Date.now() })
  }

  async function execute(...args: any[]) {
    if (isUnmounted) {return { data: data.value, loading: false, error: null }}

    const key = cacheKey ? `${cacheKey}:${JSON.stringify(args)}` : null
    const cached = getFromCache(key)
    if (cached !== null) {
      data.value = cached
      return { data: cached, loading: false, error: null }
    }

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

      if (key) {
        setToCache(key, result)
      }

      return { data: result, loading: false, error: null }
    } catch (err: any) {
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

  function reset(): void {
    data.value = defaultValue
    error.value = null
    loading.value = false
  }

  function clearCache(): void {
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

interface PageResult {
  records?: T[]
  total?: number
  pages?: number
  current?: number
  size?: number
}

export function usePaginationData<T>(
  apiFn: (params: { current: number; size: number }) => Promise<{ data: PageResult<T> }>,
  options: { defaultPageSize?: number } & UseAsyncDataOptions<PageResult<T>> = {}
) {
  const { defaultPageSize = 10, ...asyncOptions } = options

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

  function changeSize(newSize: number) {
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