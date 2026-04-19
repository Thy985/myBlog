import router from '@/router/index'
import { getToken, clearTempAuthInfo, setRedirectUrl, getRedirectUrl } from '@/composables/auth'
import { showMessage, showPageLoading, hidePageLoading } from '@/utils'
import logger from '@/utils/logger'

const API_TIMEOUT = 5000

let authStore = null
let settingsStore = null
let isStoresInitialized = false

const pendingRequests = new Map()

function withTimeout(promise, timeoutMs) {
  return Promise.race([
    promise,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error('请求超时')), timeoutMs)
    )
  ])
}

async function initStores() {
  if (isStoresInitialized && authStore && settingsStore) {
    return { authStore, settingsStore }
  }

  try {
    const { useAuthStore, useSettingsStore } = await import('@/stores')
    authStore = useAuthStore()
    settingsStore = useSettingsStore()
    isStoresInitialized = true
    return { authStore, settingsStore }
  } catch (err) {
    logger.error('Store init failed:', err.message)
    return { authStore: null, settingsStore: null }
  }
}

async function fetchUserInfoWithCache(store) {
  if (!store) {return null}

  if (store.hasValidCache()) {
    return store.user
  }

  const requestKey = 'userInfo'
  if (pendingRequests.has(requestKey)) {
    return pendingRequests.get(requestKey)
  }

  const requestPromise = (async () => {
    try {
      const result = withTimeout(store.getAdminInfo(), API_TIMEOUT)
      return await result
    } finally {
      pendingRequests.delete(requestKey)
    }
  })()

  pendingRequests.set(requestKey, requestPromise)
  return requestPromise
}

let navigationHeld = false
let pendingNavigation = null

router.beforeEach(async (to, from, next) => {
  logger.debug('Route guard:', to.path)

  if (navigationHeld && pendingNavigation) {
    pendingNavigation()
    navigationHeld = false
    pendingNavigation = null
  }

  showPageLoading()

  const token = getToken()

  const stores = await initStores()
  authStore = stores.authStore
  settingsStore = stores.settingsStore

  if (!authStore) {
    if (to.path.startsWith('/admin')) {
      showMessage('系统错误，请稍后重试', 'error')
      next({ path: '/' })
      return
    }
    next()
    return
  }

  if (token && authStore) {
    try {
      await fetchUserInfoWithCache(authStore)
    } catch (err) {
      logger.error('获取用户信息失败:', err.message)
    }
  }

  if (to.path === '/login') {
    clearTempAuthInfo()
  }

  if (!to.path.startsWith('/admin')) {
    if (to.meta.requiresAuth && !token) {
      setRedirectUrl(to.fullPath)
      showMessage('请先登录', 'warning')
      next({ path: '/login' })
      return
    }
    next()
    return
  }

  if (!token && to.path.startsWith('/admin')) {
    setRedirectUrl(to.fullPath)
    showMessage('请先登录', 'warning')
    next({ path: '/admin/login' })
    return
  }

  if (to.path === '/admin/login') {
    if (token) {
      const redirectUrl = getRedirectUrl()
      if (redirectUrl && redirectUrl !== '/login' && redirectUrl !== '/admin/login') {
        next({ path: redirectUrl })
        return
      }
      next({ path: from.path || '/' })
      return
    }
    next()
    return
  }

  if (to.path.startsWith('/admin') && token && authStore) {
    const user = authStore.user
    if (user && user.role === 'admin') {
      next()
      return
    }

    if (!user || Object.keys(user).length === 0) {
      try {
        await fetchUserInfoWithCache(authStore)
        if (authStore.user && authStore.user.role === 'admin') {
          next()
          return
        }
      } catch (error) {
        logger.error('获取管理员信息失败:', error.message)
        setRedirectUrl(to.fullPath)
        showMessage('请重新登录', 'warning')
        next({ path: '/admin/login' })
        return
      }
    }

    showMessage('权限不足，无法访问后台', 'error')
    next({ path: '/' })
    return
  }

  next()
})

router.afterEach((to) => {
  let title = (to.meta.title ? to.meta.title : '') + ' - 星辰博客'
  if (!to.meta.title) {
    title = '星辰博客'
  }
  document.title = title

  hidePageLoading()
  navigationHeld = false
  pendingNavigation = null
})

router.onError((error) => {
  hidePageLoading()
  logger.error('路由导航错误:', error.message)
  showMessage('页面导航失败，请稍后重试', 'error')
  navigationHeld = false
  pendingNavigation = null
})
