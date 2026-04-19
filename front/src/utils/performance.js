// 路由懒加载配置示例
// 在 router/index.js 中使用

// 图片懒加载指令
export const lazyLoadDirective = {
  mounted(el, binding) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const img = entry.target
          img.src = binding.value
          observer.unobserve(img)
        }
      })
    })
    observer.observe(el)
  }
}

// 代码分割配置（vite.config.js）
export const buildConfig = {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus'],
          'echarts': ['echarts'],
          'utils': ['dayjs', 'axios']
        }
      }
    },
    chunkSizeWarningLimit: 1000
  }
}
