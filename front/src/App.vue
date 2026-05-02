<template>
  <el-config-provider :locale="locale">
    <ErrorBoundary>
      <router-view v-slot="{ Component, route }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </ErrorBoundary>
    <AgentFloatPanel />
  </el-config-provider>
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import AgentFloatPanel from '@/components/business/AgentFloatPanel.vue'
import ErrorBoundary from '@/components/ui/ErrorBoundary.vue'
import { onErrorCaptured } from 'vue'
import logger from '@/utils/logger'

const locale = zhCn

onErrorCaptured((err, instance, info) => {
  logger.error('[Global Error]', {
    error: err?.message || err,
    component: instance?.$options?.name || 'Unknown',
    info
  })
  return false
})
</script>

<style>
html, body, #app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system-font, BlinkMacSystemFont, "Helvetica Neue", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei UI", "Microsoft YaHei", Arial, sans-serif;
  color: var(--text-primary);
  font-size: 16px;
  background: var(--background-secondary);
  line-height: 1.8;
  letter-spacing: 0.02em;
}

#nprogress .bar {
   background: var(--primary-color) !important;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal) ease, transform var(--transition-normal) ease;
  will-change: opacity, transform;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (prefers-reduced-motion: reduce) {
  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}

.page-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
  color: var(--text-secondary, #666);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color, #e5e7eb);
  border-top-color: var(--primary-color, #667eea);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.page-loading p {
  margin-top: 16px;
  font-size: 14px;
}
</style>
