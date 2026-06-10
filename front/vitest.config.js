import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['tests/unit/**/*.{js,ts,vue}'],
      exclude: ['src/**/*.d.ts', 'src/main.js', 'src/router/**', 'src/plugins/**']
    },
    setupFiles: ['./tests/unit/setup.js'],
    exclude: ['tests/e2e/**', 'node_modules/**', 'dist/**'],
    server: {
      deps: {
        inline: ['@vue/test-utils']
      }
    },
    deps: {
      optimizer: {
        web: {
          enabled: false
        }
      }
    },
    alias: [
      {
        find: /^@\/components\/ui\/EmptyState\.vue$/,
        replacement: fileURLToPath(new URL('./tests/unit/__mocks__/EmptyState.vue', import.meta.url))
      },
      {
        find: /^@\/components\/common\/CommentItem\.vue$/,
        replacement: fileURLToPath(new URL('./tests/unit/__mocks__/CommentItem.vue', import.meta.url))
      }
    ]
  }
})
