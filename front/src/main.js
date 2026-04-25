import { createApp } from 'vue'
import { createPinia } from 'pinia'

// 设计系统 token - 必须在最前
import '@/assets/tokens/design-tokens.css'
import '@/assets/tech-theme.css'
import '@/assets/agent-theme.css'

import '@/assets/main.css'
// Element Plus 样式覆盖 - 必须在Element Plus之后
import '@/assets/styles/element-override.css'
// 响应式设计系统
import '@/assets/styles/responsive.css'
import 'animate.css'
import 'nprogress/nprogress.css'
import App from './App.vue'
import router from './router'

const pinia = createPinia()

// 代码高亮 - 按需引入常用语言
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml' // html
import json from 'highlight.js/lib/languages/json'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import markdown from 'highlight.js/lib/languages/markdown'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'

// 注册语言
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('css', css)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('go', go)
hljs.registerLanguage('rust', rust)

import 'highlight.js/styles/tokyo-night-dark.css'

// 导入插件
import { elementPlusPlugin, viewerPlugin, markedPlugin } from '@/plugins'

// 设置全局错误处理
import { setupGlobalErrorHandler } from '@/utils/error-handler'
import logger from '@/utils/logger'

// 全局指令
import { setupDirectives } from '@/directives'

const app = createApp(App)

app.use(pinia)
app.use(router)

// 使用插件
app.use(elementPlusPlugin)
app.use(viewerPlugin)
app.use(markedPlugin)

// 注册全局指令
setupDirectives(app)

// 代码高亮指令
app.directive('highlight', (el) => {
  const highlight = el.querySelectorAll('pre code')
  highlight.forEach(block => {
    hljs.highlightElement(block)
  })
})

// XSS过滤指令
import { createSanitizeDirective } from '@/utils/xss'
app.directive('sanitize', createSanitizeDirective())

// 设置全局错误处理
setupGlobalErrorHandler(app)

// 初始化博客设置信息 - 分离关键和非关键初始化
function initApp() {
  // 立即挂载应用，避免白屏（关键初始化）
  app.mount('#app')

  // 在应用挂载后导入权限控制，确保 pinia 和 router 已就绪
  import('@/permission').catch(err => {
    logger.error('权限控制模块加载失败:', err.message)
  })

  // 非关键初始化在挂载后执行，不阻塞应用渲染
  ;(async () => {
    try {
      const { useMainStore } = await import('@/stores')
      const mainStore = useMainStore()

      // 初始化深色模式
      mainStore.initDarkMode()

      // 同步 Element Plus 暗色主题
      const { watchDarkModeForElementPlus } = await import('@/utils/theme')
      watchDarkModeForElementPlus()

      // 获取博客设置（非关键，不阻塞应用渲染）
      mainStore.getBlogSetting().catch(err => {
        logger.warn('博客设置获取失败，不影响主功能:', err.message)
      })
      logger.debug('博客设置信息初始化成功')

      // 注册 Service Worker（PWA 离线支持）- 仅在生产环境
      if ('serviceWorker' in navigator && import.meta.env.PROD) {
        try {
          const reg = await navigator.serviceWorker.register('/sw.js', { scope: '/' })
          reg.addEventListener('updatefound', () => {
            const newWorker = reg.installing
            if (newWorker) {
              newWorker.addEventListener('statechange', () => {
                if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                  newWorker.postMessage({ type: 'SKIP_WAITING' })
                }
              })
            }
          })
        } catch (err) {
          logger.debug('Service Worker 注册失败:', err.message)
        }
      }
    } catch (err) {
      logger.error('初始化失败:', err.message)
    }
  })()
}

initApp()
