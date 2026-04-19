/**
 * 数据采集工具
 * 用于收集用户行为数据并上报到后端
 */

import { recordPageView } from '@/api/admin/analytics'
import { useAuthStore } from '@/stores/auth'
import logger from '@/utils/logger'
/**
 * 生成会话ID
 */
function generateSessionId() {
  let sessionId = sessionStorage.getItem('sessionId')
  if (!sessionId) {
    sessionId = 'session_' + Date.now() + '_' + crypto.getRandomValues(new Uint32Array(1))[0].toString(36).slice(2, 11)
    sessionStorage.setItem('sessionId', sessionId)
  }
  return sessionId
}

/**
 * 获取设备类型
 */
function getDeviceType() {
  const ua = navigator.userAgent
  if (/(tablet|ipad|playbook|silk)|(android(?!.*mobi))/i.test(ua)) {
    return 'Tablet'
  }
  if (/Mobile|Android|iP(hone|od)|IEMobile|BlackBerry|Kindle|Silk-Accelerated|(hpw|web)OS|Opera M(obi|ini)/.test(ua)) {
    return 'Mobile'
  }
  return 'PC'
}

/**
 * 获取浏览器信息
 */
function getBrowser() {
  const ua = navigator.userAgent
  if (ua.indexOf('Chrome') > -1) {return 'Chrome'}
  if (ua.indexOf('Safari') > -1) {return 'Safari'}
  if (ua.indexOf('Firefox') > -1) {return 'Firefox'}
  if (ua.indexOf('Edge') > -1) {return 'Edge'}
  if (ua.indexOf('MSIE') > -1 || ua.indexOf('Trident') > -1) {return 'IE'}
  return 'Other'
}

/**
 * 获取操作系统
 */
function getOS() {
  const ua = navigator.userAgent
  if (ua.indexOf('Win') > -1) {return 'Windows'}
  if (ua.indexOf('Mac') > -1) {return 'MacOS'}
  if (ua.indexOf('Linux') > -1) {return 'Linux'}
  if (ua.indexOf('Android') > -1) {return 'Android'}
  if (ua.indexOf('iOS') > -1 || ua.indexOf('iPhone') > -1 || ua.indexOf('iPad') > -1) {return 'iOS'}
  return 'Other'
}

/**
 * 记录页面访问
 * @param {Object} options - 配置选项
 * @param {Number} options.articleId - 文章ID（可选）
 */
export function trackPageView(options = {}) {
  try {
    const params = {
      articleId: options.articleId || null,
      userId: getUserId(), // 从localStorage或store获取
      ipAddress: null, // 后端从请求中获取
      userAgent: navigator.userAgent,
      deviceType: getDeviceType(),
      browser: getBrowser(),
      os: getOS(),
      referer: document.referrer || null,
      sessionId: generateSessionId()
    }

    // 发送数据到后端
    recordPageView(params).catch(err => {
      logger.error('记录页面访问失败:', err)
    })

    logger.debug('页面访问已记录:', params)
  } catch (error) {
    logger.error('trackPageView error:', error)
  }
}

/**
 * 获取用户ID
 */
function getUserId() {
  try {
    const authStore = useAuthStore()
    return authStore.user?.id || null
  } catch (e) {
    logger.debug('获取用户ID失败:', e)
    return null
  }
}

/**
 * 记录文章阅读时长
 * @param {Number} articleId - 文章ID
 */
export function trackReadingTime(articleId) {
  const startTime = Date.now()
    
  const handleBeforeUnload = () => {
    const readingTime = Math.floor((Date.now() - startTime) / 1000)
        
    if (navigator.sendBeacon) {
      const data = JSON.stringify({
        articleId,
        readingTime,
        sessionId: generateSessionId()
      })
      navigator.sendBeacon('/api/analytics/reading-time', data)
    }
    window.removeEventListener('beforeunload', handleBeforeUnload)
  }
    
  window.addEventListener('beforeunload', handleBeforeUnload)
}

/**
 * 记录滚动深度
 * @param {Number} articleId - 文章ID
 */
export function trackScrollDepth(articleId) {
  let maxScrollDepth = 0
    
  const handleScroll = () => {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop
    const scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight
    const scrollDepth = Math.floor((scrollTop / scrollHeight) * 100)
        
    if (scrollDepth > maxScrollDepth) {
      maxScrollDepth = scrollDepth
    }
  }
    
  window.addEventListener('scroll', handleScroll, { passive: true })
    
  const handleBeforeUnload = () => {
    if (navigator.sendBeacon && maxScrollDepth > 0) {
      const data = JSON.stringify({
        articleId,
        scrollDepth: maxScrollDepth,
        sessionId: generateSessionId()
      })
      navigator.sendBeacon('/api/analytics/scroll-depth', data)
    }
        
    window.removeEventListener('scroll', handleScroll)
    window.removeEventListener('beforeunload', handleBeforeUnload)
  }
    
  window.addEventListener('beforeunload', handleBeforeUnload)
}

/**
 * 自动追踪（在路由守卫中使用）
 */
export function autoTrack() {
  // 在Vue Router的afterEach钩子中调用
  // 自动记录每个页面的访问
}

export default {
  trackPageView,
  trackReadingTime,
  trackScrollDepth,
  autoTrack
}
