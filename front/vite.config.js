import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    minify: 'esbuild',
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('element-plus')) {
              return 'element'
            }
            if (id.includes('echarts')) {
              return 'echarts'
            }
            if (id.includes('md-editor-v3')) {
              return 'markdown'
            }
            if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
              return 'vendor'
            }
            if (id.includes('axios')) {
              return 'axios'
            }
            if (id.includes('dayjs')) {
              return 'dayjs'
            }
            if (id.includes('viewerjs')) {
              return 'viewer'
            }
            if (id.includes('dompurify') || id.includes('nprogress') || id.includes('gsap') || id.includes('animate.css')) {
              return 'utils'
            }
          }
        },
        entryFileNames: 'assets/js/[name].[hash].js',
        chunkFileNames: 'assets/js/[name].[hash].js',
        assetFileNames: assetInfo => {
          if (/\.(png|jpe?g|gif|svg|webp|avif)$/i.test(assetInfo.name)) {
            return 'assets/images/[name].[hash].[ext]'
          }
          if (/\.(woff2?|eot|ttf|otf)$/i.test(assetInfo.name)) {
            return 'assets/fonts/[name].[hash].[ext]'
          }
          if (/\.css$/i.test(assetInfo.name)) {
            return 'assets/css/[name].[hash].[ext]'
          }
          return 'assets/[name].[hash].[ext]'
        }
      }
    },
    assetsDir: 'assets',
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 500,
    modulePreload: {
      polyfill: false
    },
    cssCodeSplit: true,
    manifest: true,
    assetsInlineLimit: 4096,
    target: 'esnext'
  },
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'element-plus',
      'axios',
      'dayjs',
      'mermaid'
    ],
    exclude: ['@vueuse/core']
  }
})
