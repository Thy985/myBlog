import router from '@/router/index'
import { getToken, clearTempAuthInfo, setRedirectUrl, getRedirectUrl } from '@/composables/auth'
import { showMessage, showPageLoading, hidePageLoading, resetPageLoading } from '@/utils'
import logger from '@/utils/logger'

const API_TIMEOUT = 5000

const storeState = {
  authStore: null,
  settingsStore: null,
  initPromise: null
}

let userInfoPromise = null

function withTimeout(promise, timeoutMs) {
  return Promise.race([
    promise,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error('请求超时')), timeoutMs)
    )
  ])
}

async function initStores() {
  if (storeState.authStore && storeState.settingsStore) {
    return { authStore: storeState.authStore, settingsStore: storeState.settingsStore }
  }

  if (storeState.initPromise) {
    return storeState.initPromise
  }

  storeState.initPromise = (async () => {
    try {
      const { useAuthStore, useSettingsStore } = await import('@/stores')
      storeState.authStore = useAuthStore()
      storeState.settingsStore = useSettingsStore()
      return { authStore: storeState.authStore, settingsStore: storeState.settingsStore }
    } catch (err) {
      logger.error('Store init failed:', err.message)
      return { authStore: null, settingsStore: null }
    }
  })()

  return storeState.initPromise
}

async function fetchUserInfoWithCache(store) {
  if (!store) {return null}

  if (store.hasValidCache()) {
    return store.user
  }

  if (userInfoPromise) {
    return userInfoPromise
  }

  userInfoPromise = withTimeout(store.getAdminInfo(), API_TIMEOUT)
    .finally(() => {
      userInfoPromise = null
    })

  return userInfoPromise
}

function handleAuthNavigation(to, from, next) {
  const token = getToken()
  const isAdminRoute = to.path.startsWith('/admin')

  if (navigationGuard) {
    pendingNavigation = { to, from, next }
    return
  }
  navigationGuard = true

  if (isAdminRoute) {
    showPageLoading()
  }

  ;(async () => {
    const stores = await initStores()
    const authStore = stores.authStore

    if (!authStore) {
      if (isAdminRoute) {
        showMessage('系统错误，请稍后重试', 'error')
        navigationGuard = false
        hidePageLoading()
        next({ path: '/' })
        return
      }
      navigationGuard = false
      hidePageLoading()
      next()
      return
    }

    if (to.path === '/login') {
      clearTempAuthInfo()
      if (token) {
        const redirectUrl = getRedirectUrl()
        if (redirectUrl && redirectUrl !== '/login' && redirectUrl !== '/admin/login') {
          navigationGuard = false
          hidePageLoading()
          next({ path: redirectUrl })
          return
        }
        navigationGuard = false
        hidePageLoading()
        next({ path: '/' })
        return
      }
      navigationGuard = false
      hidePageLoading()
      next()
      return
    }

    if (!isAdminRoute) {
      if (to.meta.requiresAuth && !token) {
        setRedirectUrl(to.fullPath)
        showMessage('请先登录', 'warning')
        navigationGuard = false
        hidePageLoading()
        next({ path: '/login' })
        return
      }
      navigationGuard = false
      hidePageLoading()
      next()
      return
    }

    if (!token && isAdminRoute) {
      setRedirectUrl(to.fullPath)
      showMessage('请先登录', 'warning')
      navigationGuard = false
      hidePageLoading()
      next({ path: '/admin/login' })
      return
    }

    if (to.path === '/admin/login') {
      if (token) {
        const redirectUrl = getRedirectUrl()
        if (redirectUrl && redirectUrl !== '/login' && redirectUrl !== '/admin/login') {
          navigationGuard = false
          hidePageLoading()
          next({ path: redirectUrl })
          return
        }
        navigationGuard = false
        hidePageLoading()
        next({ path: from.path || '/' })
        return
      }
      navigationGuard = false
      hidePageLoading()
      next()
      return
    }

    if (isAdminRoute && token && authStore) {
      let user = authStore.user

      if (user && user.role === 'admin') {
        navigationGuard = false
        hidePageLoading()
        next()
        return
      }

      if (!user || Object.keys(user).length === 0) {
        try {
          user = await fetchUserInfoWithCache(authStore)
        } catch (error) {
          logger.error('获取管理员信息失败:', error.message)
          if (error.message === '请求超时') {
            showMessage('获取用户信息超时，请检查网络', 'error')
          } else {
            showMessage('请重新登录', 'warning')
          }
          setRedirectUrl(to.fullPath)
          navigationGuard = false
          hidePageLoading()
          next({ path: '/admin/login' })
          return
        }
      }

      if (user && user.role === 'admin') {
        navigationGuard = false
        hidePageLoading()
        next()
        return
      }

      showMessage('权限不足，无法访问后台', 'error')
      navigationGuard = false
      hidePageLoading()
      next({ path: '/' })
      return
    }

    navigationGuard = false
    hidePageLoading()
    next()
  })()
}

let navigationGuard = false
let pendingNavigation = null

router.beforeEach((to, from, next) => {
  handleAuthNavigation(to, from, next)
})

router.afterEach((to) => {
  let title = (to.meta.title ? to.meta.title : '') + ' - 星辰博客'
  if (!to.meta.title) {
    title = '星辰博客'
  }
  document.title = title

  if (pendingNavigation) {
    const { to, from, next } = pendingNavigation
    pendingNavigation = null
    navigationGuard = false
    resetPageLoading()
    router.push(to.path).catch(() => {})
  } else {
    navigationGuard = false
  }
})

router.onError((error) => {
  hidePageLoading()
  resetPageLoading()
  logger.error('路由导航错误:', error.message)
  showMessage('页面导航失败，请稍后重试', 'error')
  navigationGuard = false
  pendingNavigation = null
})
