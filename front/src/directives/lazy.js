/**
 * 图片懒加载指令 - 增强版
 * 支持：懒加载、响应式图片、加载占位、错误处理
 */
import logger from '@/utils/logger'

// 默认占位图
const DEFAULT_PLACEHOLDER = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"%3E%3Crect fill="%23f3f4f6" width="400" height="300"/%3E%3Ctext fill="%239ca3af" font-family="sans-serif" font-size="16" dy="5" x="50%25" y="50%25" text-anchor="middle"%3E加载中...%3C/text%3E%3C/svg%3E'

// 错误占位图
const ERROR_PLACEHOLDER = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"%3E%3Crect fill="%23fef2f2" width="400" height="300"/%3E%3Ctext fill="%23ef4444" font-family="sans-serif" font-size="16" dy="5" x="50%25" y="50%25" text-anchor="middle"%3E加载失败%3C/text%3E%3C/svg%3E'

// 存储所有observer实例
const observers = new WeakMap()

// 创建Intersection Observer配置
const createObserverOptions = (options = {}) => ({
  root: options.root || null,
  rootMargin: options.rootMargin || '50px 0px',
  threshold: options.threshold || 0.01,
  ...options
})

// 加载图片
const loadImage = (el, src, options = {}) => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    
    img.onload = () => {
      el.src = src
      el.classList.add('lazy-loaded')
      el.classList.remove('lazy-loading', 'lazy-error')
      
      // 触发自定义事件
      el.dispatchEvent(new CustomEvent('lazyLoaded', { detail: { src } }))
      
      resolve(src)
    }
    
    img.onerror = () => {
      el.src = options.errorPlaceholder || ERROR_PLACEHOLDER
      el.classList.add('lazy-error')
      el.classList.remove('lazy-loading', 'lazy-loaded')
      
      // 触发自定义事件
      el.dispatchEvent(new CustomEvent('lazyError', { detail: { src } }))
      
      reject(new Error(`Failed to load image: ${src}`))
    }
    
    img.src = src
  })
}

// 处理响应式图片
const handleResponsiveImage = (el, binding) => {
  const { value } = binding

  if (typeof value === 'string') {
    // 简单字符串，直接作为src
    return { src: value, srcset: null, sizes: null }
  }
  
  if (typeof value === 'object') {
    // 对象形式，支持响应式图片
    return {
      src: value.src || value.url || DEFAULT_PLACEHOLDER,
      srcset: value.srcset || null,
      sizes: value.sizes || null,
      webp: value.webp || null,
      options: value.options || {}
    }
  }
  
  return { src: DEFAULT_PLACEHOLDER, srcset: null, sizes: null }
}

// 主指令对象
const lazyDirective = {
  // 元素挂载时
  mounted(el, binding) {
    // 如果不是img元素，跳过
    if (el.tagName !== 'IMG') {
      logger.warn('v-lazy directive should only be used on img elements')
      return
    }
    
    const { src, srcset, sizes, webp, options } = handleResponsiveImage(el, binding)
    const placeholder = binding.arg || options.placeholder || DEFAULT_PLACEHOLDER
    
    // 设置初始占位图
    el.src = placeholder
    el.classList.add('lazy-image', 'lazy-loading')
    
    // 设置响应式图片属性
    if (srcset) {
      el.setAttribute('data-srcset', srcset)
    }
    if (sizes) {
      el.setAttribute('data-sizes', sizes)
    }
    if (webp) {
      el.setAttribute('data-webp', webp)
    }
    el.setAttribute('data-src', src)
    
    // 创建Intersection Observer
    const observerOptions = createObserverOptions(options)
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          // 图片进入视口
          const dataSrc = el.getAttribute('data-src')
          const dataSrcset = el.getAttribute('data-srcset')
          const dataSizes = el.getAttribute('data-sizes')
          
          if (dataSrc) {
            loadImage(el, dataSrc, { ...options, errorPlaceholder: binding.arg })
              .then(() => {
                // 加载成功，设置srcset和sizes
                if (dataSrcset) {
                  el.srcset = dataSrcset
                }
                if (dataSizes) {
                  el.sizes = dataSizes
                }
              })
              .catch(err => {
                logger.error('[v-lazy] Image load failed:', err)
              })
              .finally(() => {
                // 停止观察
                observer.unobserve(el)
                observers.delete(el)
              })
          }
        }
      })
    }, observerOptions)
    
    // 开始观察
    observer.observe(el)
    observers.set(el, observer)
    
    // 存储数据用于后续更新
    el._lazyOptions = { src, srcset, sizes, options }
  },
  
  // 元素更新时
  updated(el, binding) {
    // 如果不是img元素，跳过
    if (el.tagName !== 'IMG') {return}
    
    const { src, srcset, sizes } = handleResponsiveImage(el, binding)
    
    // 如果src变化，重新加载
    if (el._lazyOptions && el._lazyOptions.src !== src) {
      // 更新存储的数据
      el._lazyOptions = { ...el._lazyOptions, src, srcset, sizes }
      
      // 更新data属性
      el.setAttribute('data-src', src)
      if (srcset) {
        el.setAttribute('data-srcset', srcset)
      }
      if (sizes) {
        el.setAttribute('data-sizes', sizes)
      }
      
      // 重置状态
      el.classList.remove('lazy-loaded', 'lazy-error')
      el.classList.add('lazy-loading')
      
      // 如果已经在视口内，直接加载
      const rect = el.getBoundingClientRect()
      const isInViewport = rect.top < window.innerHeight && rect.bottom > 0
      
      if (isInViewport) {
        loadImage(el, src, el._lazyOptions.options)
          .then(() => {
            if (srcset) {el.srcset = srcset}
            if (sizes) {el.sizes = sizes}
          })
      }
    }
  },
  
  // 元素卸载时
  unmounted(el) {
    // 如果不是img元素，跳过
    if (el.tagName !== 'IMG') {return}
    
    // 清理observer
    const observer = observers.get(el)
    if (observer) {
      observer.disconnect()
      observers.delete(el)
    }
    
    // 清理数据
    delete el._lazyOptions
  }
}

// 批量预加载图片（用于提前加载关键图片）
export const preloadImages = (urls, options = {}) => {
  const { concurrent = 3, onProgress, onComplete } = options
  
  let loaded = 0
  const total = urls.length
  const queue = [...urls]
  const loading = new Set()
  
  const loadNext = () => {
    if (queue.length === 0 && loading.size === 0) {
      onComplete?.()
      return
    }
    
    while (loading.size < concurrent && queue.length > 0) {
      const url = queue.shift()
      loading.add(url)
      
      const img = new Image()
      img.onload = img.onerror = () => {
        loading.delete(url)
        loaded++
        onProgress?.(loaded, total)
        loadNext()
      }
      img.src = url
    }
  }
  
  loadNext()
}

// 检查图片是否在视口内
export const isInViewport = (el, offset = 0) => {
  const rect = el.getBoundingClientRect()
  return (
    rect.top >= -offset &&
    rect.left >= 0 &&
    rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) + offset &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth)
  )
}

export default lazyDirective
