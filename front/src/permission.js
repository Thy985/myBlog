import router from '@/router/index'
import { clearTempAuthInfo, setRedirectUrl, getRedirectUrl } from '@/composables/auth'
import { showMessage } from '@/utils'
import logger from '@/utils/logger'
import { getUserInfo } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

function isLoginPage(path) {
  return path === '/login'
}

async function checkAuthStatus() {
  try {
    const res = await getUserInfo()
    const isValid = res?.code === 200 && res?.data?.user != null
    logger.debug('checkAuthStatus:', { code: res?.code, hasUser: !!res?.data?.user, isValid })
    return isValid
  } catch (err) {
    logger.debug('checkAuthStatus error:', err.message)
    return false
  }
}

async function handleLoginNavigation(to, from, next) {
  logger.debug('handleLoginNavigation start', { to: to.path, from: from.path })

  const authStore = useAuthStore()
  logger.debug('authStore.isLoggedIn():', authStore.isLoggedIn())

  const isLoggedIn = await checkAuthStatus()
  logger.debug('checkAuthStatus result:', isLoggedIn)

  if (isLoggedIn) {
    const redirectUrl = getRedirectUrl()
    logger.debug('redirectUrl:', redirectUrl)
    if (redirectUrl && !isLoginPage(redirectUrl)) {
      logger.debug('Redirecting to redirectUrl:', redirectUrl)
      next({ path: redirectUrl })
    } else {
      logger.debug('Redirecting to /')
      next({ path: '/' })
    }
    return
  }

  logger.debug('Not logged in, clearing temp auth')
  clearTempAuthInfo()
  next()
}

async function handleAuthNavigation(to, from, next) {
  const requiresAuth = to.meta.requiresAuth
  const loginPage = isLoginPage(to.path)

  logger.debug('handleAuthNavigation:', { to: to.path, requiresAuth, loginPage })

  try {
    if (loginPage) {
      await handleLoginNavigation(to, from, next)
      return
    }

    if (requiresAuth) {
      const isLoggedIn = await checkAuthStatus()
      if (!isLoggedIn) {
        logger.debug('requiresAuth but not logged in, redirect to login')
        setRedirectUrl(to.fullPath)
        showMessage('请先登录', 'warning')
        next({ path: '/login' })
        return
      }
    }

    next()
  } catch (error) {
    logger.error('导航处理错误:', error.message)
    showMessage('页面导航失败，请稍后重试', 'error')
    next({ path: '/' })
  }
}

router.beforeEach(async (to, from, next) => {
  logger.debug('router.beforeEach:', { to: to.path, from: from.path })
  await handleAuthNavigation(to, from, next)
})

router.afterEach((to) => {
  let title = (to.meta.title ? to.meta.title : '') + ' - 星辰博客'
  if (!to.meta.title) {
    title = '星辰博客'
  }
  document.title = title
})

router.onError((error) => {
  logger.error('路由导航错误:', error.message)
  showMessage('页面导航失败，请稍后重试', 'error')
})
