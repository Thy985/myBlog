<template>
  <div class="skeleton-loader" :class="{ 'skeleton-compact': compact }">
    <!-- 文章卡片骨架屏 -->
    <div v-if="type === 'article-card'" class="grid grid-cols-1 sm:grid-cols-1 md:grid-cols-2 gap-8">
      <div
        v-for="i in count"
        :key="i"
        class="skeleton-card"
      >
        <!-- 图片骨架 -->
        <div class="skeleton-image"></div>
        <!-- 内容骨架 -->
        <div class="skeleton-content">
          <!-- 标签骨架 -->
          <div class="skeleton-tags">
            <div class="skeleton-tag"></div>
            <div class="skeleton-tag skeleton-tag-short"></div>
          </div>
          <!-- 标题骨架 -->
          <div class="skeleton-title"></div>
          <div class="skeleton-title skeleton-title-short"></div>
          <!-- 描述骨架 -->
          <div class="skeleton-desc"></div>
          <div class="skeleton-desc"></div>
          <div class="skeleton-desc skeleton-desc-short"></div>
          <!-- Meta 骨架 -->
          <div class="skeleton-meta">
            <div class="skeleton-meta-item"></div>
            <div class="skeleton-meta-item skeleton-meta-item-short"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 侧边栏骨架屏 -->
    <div v-else-if="type === 'sidebar'" class="space-y-6">
      <!-- 分类/标签列表骨架 -->
      <div v-for="i in count" :key="i" class="sidebar-item">
        <div class="sidebar-icon"></div>
        <div class="sidebar-text"></div>
        <div class="sidebar-count"></div>
      </div>
    </div>

    <!-- 文章详情页骨架屏 -->
    <div v-else-if="type === 'article-detail'">
      <div class="detail-header">
        <div class="detail-title"></div>
        <div class="detail-meta">
          <div class="detail-meta-item"></div>
          <div class="detail-meta-item detail-meta-item-md"></div>
          <div class="detail-meta-item"></div>
        </div>
      </div>
      <div class="detail-content">
        <div v-for="i in 8" :key="i" class="detail-line" :class="{ 'detail-line-short': i === 5 || i === 8 }"></div>
      </div>
    </div>

    <!-- 通用骨架屏 -->
    <div v-else-if="type === 'generic'" class="flex flex-col items-center justify-center py-20">
      <div class="skeleton-spinner"></div>
      <span class="mt-4 text-text-muted">{{ text }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'article-card',
    validator: (val) => ['article-card', 'sidebar', 'article-detail', 'generic'].includes(val)
  },
  count: {
    type: Number,
    default: 4
  },
  text: {
    type: String,
    default: '加载中...'
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.skeleton-loader {
  width: 100%;
}

.skeleton-compact {
  /* 紧凑模式 */
}

/* 文章卡片骨架屏 */
.skeleton-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.skeleton-image {
  width: 100%;
  height: 200px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-content {
  padding: 20px;
}

.skeleton-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.skeleton-tag {
  width: 60px;
  height: 24px;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-tag-short {
  width: 40px;
}

.skeleton-title {
  height: 20px;
  border-radius: 4px;
  margin-bottom: 8px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-title-short {
  width: 60%;
}

.skeleton-desc {
  height: 14px;
  border-radius: 4px;
  margin-bottom: 8px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-desc-short {
  width: 80%;
}

.skeleton-meta {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.skeleton-meta-item {
  width: 80px;
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-meta-item-short {
  width: 50px;
}

/* 侧边栏骨架屏 */
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
}

.sidebar-icon {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.sidebar-text {
  flex: 1;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.sidebar-count {
  width: 30px;
  height: 18px;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

/* 文章详情页骨架屏 */
.detail-header {
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.detail-title {
  height: 32px;
  width: 70%;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.detail-meta {
  display: flex;
  gap: 24px;
}

.detail-meta-item {
  height: 14px;
  width: 80px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.detail-meta-item-md {
  width: 120px;
}

.detail-content {
  space-y: 3;
}

.detail-line {
  height: 16px;
  width: 100%;
  border-radius: 4px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, var(--border-color) 25%, var(--bg-tertiary) 50%, var(--border-color) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.detail-line-short {
  width: 65%;
}

/* 通用加载动画 */
.skeleton-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 动画 */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 减少动画模式支持 */
@media (prefers-reduced-motion: reduce) {
  .skeleton-image,
  .skeleton-tag,
  .skeleton-title,
  .skeleton-desc,
  .skeleton-meta-item,
  .skeleton-spinner,
  .sidebar-icon,
  .sidebar-text,
  .sidebar-count,
  .detail-title,
  .detail-meta-item,
  .detail-line {
    animation: none;
    background: var(--border-color);
  }
}
</style>
