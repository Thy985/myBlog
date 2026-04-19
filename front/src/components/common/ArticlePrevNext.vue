<template>
  <div class="mt-6 bg-white border border-gray-200 rounded-lg p-6 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
    <h3 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white">相关阅读</h3>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- 上一篇 -->
      <div v-if="prevArticle?.id" class="prev-article">
        <a
          class="flex items-start gap-4 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors group"
          @click="$emit('go-article', prevArticle.id)"
        >
          <div class="w-24 h-16 flex-shrink-0 overflow-hidden rounded-md">
            <img
              :src="prevArticle.thumbnail || '/default-thumbnail.svg'"
              :alt="prevArticle.title"
              class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
            >
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center mb-1">
              <svg class="w-4 h-4 text-gray-400 dark:text-gray-500 mr-1 group-hover:text-blue-500 transition-colors" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 8 14">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 1 1.3 6.326a.91.91 0 0 0 0 1.348L7 13" />
              </svg>
              <span class="text-xs text-gray-500 dark:text-gray-500">上一篇</span>
            </div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-white truncate group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              {{ prevArticle.title }}
            </h4>
          </div>
        </a>
      </div>
      <div v-else class="prev-article p-3 rounded-lg bg-gray-50 dark:bg-gray-700">
        <div class="flex items-center mb-1">
          <svg class="w-4 h-4 text-gray-400 dark:text-gray-500 mr-1" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 8 14">
            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 1 1.3 6.326a.91.91 0 0 0 0 1.348L7 13" />
          </svg>
          <span class="text-xs text-gray-500 dark:text-gray-500">上一篇</span>
        </div>
        <h4 class="text-sm font-medium text-gray-400 dark:text-gray-500">暂无</h4>
      </div>

      <!-- 下一篇 -->
      <div v-if="nextArticle?.id" class="next-article">
        <a
          class="flex items-start gap-4 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors group"
          @click="$emit('go-article', nextArticle.id)"
        >
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-end mb-1">
              <span class="text-xs text-gray-500 dark:text-gray-500">下一篇</span>
              <svg class="w-4 h-4 text-gray-400 dark:text-gray-500 ml-1 group-hover:text-blue-500 transition-colors" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 8 14">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 13 5.7-5.326a.909.909 0 0 0 0-1.348L1 1" />
              </svg>
            </div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-white truncate text-right group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              {{ nextArticle.title }}
            </h4>
          </div>
          <div class="w-24 h-16 flex-shrink-0 overflow-hidden rounded-md">
            <img
              :src="nextArticle.thumbnail || '/default-thumbnail.svg'"
              :alt="nextArticle.title"
              class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
            >
          </div>
        </a>
      </div>
      <div v-else class="next-article p-3 rounded-lg bg-gray-50 dark:bg-gray-700">
        <div class="flex items-center justify-end mb-1">
          <span class="text-xs text-gray-500 dark:text-gray-500">下一篇</span>
          <svg class="w-4 h-4 text-gray-400 dark:text-gray-500 ml-1" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 8 14">
            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 13 5.7-5.326a.909.909 0 0 0 0-1.348L1 1" />
          </svg>
        </div>
        <h4 class="text-sm font-medium text-gray-400 dark:text-gray-500 text-right">暂无</h4>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  prevArticle: {
    type: Object,
    default: null
  },
  nextArticle: {
    type: Object,
    default: null
  }
})

defineEmits(['go-article'])
</script>
