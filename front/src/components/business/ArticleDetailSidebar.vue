<template>
  <div class="article-sidebar">
    <!-- 作者信息卡片 -->
    <div class="sidebar-card">
      <UserInfoCard></UserInfoCard>
    </div>

    <!-- 文章信息汇总 -->
    <div class="sidebar-card">
      <h2 class="sidebar-card-title">
        <svg class="w-5 h-5 mr-2 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
        </svg>
        文章信息
      </h2>
      
      <!-- 分类 -->
      <div class="mb-5">
        <h3 class="sidebar-section-title">分类</h3>
        <div class="space-y-2">
          <a
            v-for="item in categories"
            :key="item.id"
            class="category-item"
            @click="$emit('go-category', item.id, item.name)"
          >
            <svg class="w-4 h-4 mr-2 text-gray-400 dark:text-gray-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path>
            </svg>
            <span class="truncate">{{ item.name }}</span>
          </a>
        </div>
      </div>

      <!-- 标签 -->
      <div v-if="tags && tags.length > 0">
        <h3 class="sidebar-section-title">标签</h3>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="item in tags"
            :key="item.id"
            class="tag-item"
            @click="$emit('go-tag', item.id, item.name)"
          >
            # {{ item.name }}
          </span>
        </div>
      </div>
    </div>

    <!-- 相关文章推荐 -->
    <div class="sidebar-card">
      <h2 class="sidebar-card-title">
        <svg class="w-5 h-5 mr-2 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path>
        </svg>
        相关推荐
      </h2>
      
      <SkeletonLoader v-if="loadingRelated" type="generic" text="加载中..." :compact="true" />
      
      <EmptyState
        v-else-if="relatedArticles.length === 0"
        icon="document"
        title="暂无相关推荐"
        description="暂无相关推荐内容"
        :show-action="false"
        :show-tip="false"
        :compact="true"
      />
      
      <div v-else class="space-y-4">
        <div 
          v-for="item in relatedArticles" 
          :key="item.id" 
          class="group cursor-pointer"
          @click="$emit('go-article', item.id)"
        >
          <div class="related-item">
            <!-- 缩略图 - 增大到80x60 -->
            <div class="related-thumb">
              <img
                :src="item.thumbnail || '/default-thumbnail.svg'"
                :alt="item.title"
                class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
                loading="lazy"
              />
            </div>
            <!-- 内容 -->
            <div class="flex-1 min-w-0">
              <h4 class="related-title">
                {{ item.title }}
              </h4>
              <p class="related-summary">
                {{ item.summary || truncateContent(item.content) }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))
const SkeletonLoader = defineAsyncComponent(() => import('@/components/ui/SkeletonLoader.vue'))
const EmptyState = defineAsyncComponent(() => import('@/components/ui/EmptyState.vue'))

defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  tags: {
    type: Array,
    default: () => []
  },
  relatedArticles: {
    type: Array,
    default: () => []
  },
  loadingRelated: {
    type: Boolean,
    default: false
  }
})

defineEmits(['go-category', 'go-tag', 'go-article'])

function truncateContent(html, maxLength = 60) {
  if (!html) return ''
  const text = html.replace(/<[^>]*>/g, '')
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}
</script>

<style scoped>
.article-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 侧边栏卡片 - 增大内边距和优化阴影 */
.sidebar-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dark .sidebar-card {
  background: #1f2937;
  border-color: #374151;
}

.sidebar-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
}

/* 卡片标题 */
.sidebar-card-title {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 1.25rem;
  color: #111827;
  display: flex;
  align-items: center;
}

.dark .sidebar-card-title {
  color: #f9fafb;
}

/* 小节标题 */
.sidebar-section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.75rem;
}

.dark .sidebar-section-title {
  color: #d1d5db;
}

/* 分类项 */
.category-item {
  display: flex;
  align-items: center;
  padding: 0.625rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dark .category-item {
  color: #d1d5db;
}

.category-item:hover {
  background: #eff6ff;
  color: #1d4ed8;
}

.dark .category-item:hover {
  background: #374151;
  color: #60a5fa;
}

/* 标签项 */
.tag-item {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 0.875rem;
  border-radius: 9999px;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dark .tag-item {
  background: #374151;
  color: #d1d5db;
}

.tag-item:hover {
  background: #dbeafe;
  color: #1d4ed8;
}

.dark .tag-item:hover {
  background: #1e3a5f;
  color: #60a5fa;
}

/* 相关推荐项 */
.related-item {
  display: flex;
  gap: 1rem;
  padding: 0.75rem;
  border-radius: 0.75rem;
  transition: background 0.2s ease;
}

.related-item:hover {
  background: #f9fafb;
}

.dark .related-item:hover {
  background: #374151;
}

/* 缩略图 - 增大到80x60 */
.related-thumb {
  width: 80px;
  height: 60px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 0.5rem;
  background: #f3f4f6;
}

.dark .related-thumb {
  background: #374151;
}

/* 相关推荐标题 */
.related-title {
  font-size: 0.9375rem;
  font-weight: 500;
  color: #111827;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s ease;
}

.group:hover .related-title {
  color: #2563eb;
}

.dark .related-title {
  color: #f9fafb;
}

.dark .group:hover .related-title {
  color: #60a5fa;
}

/* 相关推荐摘要 */
.related-summary {
  font-size: 0.8125rem;
  color: #6b7280;
  margin-top: 0.25rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dark .related-summary {
  color: #9ca3af;
}
</style>
