<template>
  <div class="sticky top-24 space-y-6">
    <UserInfoCard></UserInfoCard>

    <!-- 文章分类 -->
    <div class="bg-white border border-gray-200 rounded-lg p-5 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
      <h2 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white">分类</h2>
      <div class="space-y-2">
        <a
          v-for="item in categories"
          :key="item.id"
          class="flex items-center py-2 px-3 rounded-md hover:bg-gray-100 hover:text-blue-700 dark:hover:bg-gray-700 dark:hover:text-blue-300 transition-colors cursor-pointer"
          @click="$emit('go-category', item.id, item.name)"
        >
          <svg class="w-4 h-4 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 21 18">
            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="0.9" d="M2.539 17h12.476l4-9H5m-2.461 9a1 1 0 0 1-.914-1.406L5 8m-2.461 9H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.443a1 1 0 0 1 .8.4l2.7 3.6H16a1 1 0 0 1 1 1v2H5" />
          </svg>
          {{ item.name }}
        </a>
      </div>
    </div>

    <!-- 文章标签 -->
    <div class="bg-white border border-gray-200 rounded-lg p-5 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
      <h2 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white">标签</h2>
      <div class="flex flex-wrap gap-2">
        <div
          v-for="item in tags"
          :key="item.id"
          class="inline-flex items-center px-3 py-1 rounded-full bg-gray-100 text-gray-700 text-xs font-medium hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600 transition-colors cursor-pointer"
          @click="$emit('go-tag', item.id, item.name)"
        >
          {{ item.name }}
        </div>
      </div>
    </div>

    <!-- 相关文章推荐 -->
    <div class="bg-white border border-gray-200 rounded-lg p-5 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
      <h2 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white">相关推荐</h2>
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
        <div v-for="item in relatedArticles" :key="item.id" class="related-article-item">
          <a
            class="flex gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors group"
            @click="$emit('go-article', item.id)"
          >
            <div class="w-20 h-16 flex-shrink-0 overflow-hidden rounded-md">
              <img
                :src="'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2280%22 height=%2264%22 viewBox=%220 0 80 64%22 fill=%22none%22%3E%3Crect width=%2280%22 height=%2264%22 rx=%224%22 fill=%22%23f3f4f6%22/%3E%3Cpath d=%22M30 40L40 32L30 24V40Z%22 fill=%22%239ca3af%22/%3E%3C/svg%3E'"
                :data-src="item.thumbnail || '/default-thumbnail.svg'"
                :alt="item.title"
                class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110 lazyload"
              >
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="text-sm font-medium text-gray-900 dark:text-white truncate group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                {{ item.title }}
              </h4>
              <p class="text-xs text-gray-600 dark:text-gray-400 line-clamp-2 mt-1">
                {{ item.summary || truncateContent(item.content) }}
              </p>
              <div class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                {{ item.updateTime }}
              </div>
            </div>
          </a>
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
  if (!html) {return ''}
  const text = html.replace(/<[^>]*>/g, '')
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}
</script>
