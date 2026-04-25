<template>
  <div class="article-sidebar">
    <!-- 作者信息卡片 -->
    <UserInfoCard></UserInfoCard>

    <!-- 文章分类 -->
    <div class="sidebar-section">
      <h3 class="sidebar-section-title">
        <svg class="w-4 h-4 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
        </svg>
        文章分类
      </h3>
      <div class="space-y-1">
        <a
          v-for="item in categories"
          :key="item.id"
          class="category-item"
          @click="$emit('go-category', item.id, item.name)"
        >
          <svg class="w-4 h-4 text-gray-400 dark:text-gray-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
          <span class="truncate">{{ item.name }}</span>
          <span v-if="item.count" class="ml-auto text-xs text-gray-400 dark:text-gray-500">({{ item.count }})</span>
        </a>
      </div>
    </div>

    <!-- 文章标签 -->
    <div v-if="tags && tags.length > 0" class="sidebar-section">
      <h3 class="sidebar-section-title">
        <svg class="w-4 h-4 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
        </svg>
        标签云
      </h3>
      <div class="flex flex-wrap gap-2">
        <span
          v-for="item in tags"
          :key="item.id"
          class="tag-item"
          @click="$emit('go-tag', item.id, item.name)"
        >
          {{ item.name }}
        </span>
      </div>
    </div>

    <!-- 相关推荐 -->
    <RelatedArticles
      :articles="relatedArticles"
      :loading="loadingRelated"
      @article-click="$emit('go-article', $event)"
    />
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))
const RelatedArticles = defineAsyncComponent(() => import('@/components/common/RelatedArticles.vue'))

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
</script>

<style scoped>
.article-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sidebar-section {
  padding: 1rem 0;
  border-bottom: 1px dashed #e5e7eb;
  dark:border-gray-700;
}

.sidebar-section:last-child {
  border-bottom: none;
}

.sidebar-section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dark .sidebar-section-title {
  color: #d1d5db;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
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

.tag-item {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
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
</style>
