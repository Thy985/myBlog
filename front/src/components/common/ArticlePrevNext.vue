<template>
  <div class="mt-6 bg-white dark:bg-gray-800 rounded-xl p-6 shadow-md dark:shadow-gray-900/20 border border-gray-100 dark:border-gray-700">
    <h3 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
      <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
      </svg>
      相关阅读
    </h3>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <!-- 上一篇 -->
      <div v-if="prevArticle?.id" class="prev-article group cursor-pointer" @click="$emit('go-article', prevArticle.id)">
        <div class="flex items-center gap-4 p-4 rounded-xl bg-gray-50 dark:bg-gray-700/50 hover:bg-blue-50 dark:hover:bg-blue-900/20 border border-gray-200 dark:border-gray-600 transition-colors duration-200">
          <div class="w-20 h-16 flex-shrink-0 overflow-hidden rounded-lg">
            <img
              :src="prevArticle.thumbnail || '/default-thumbnail.svg'"
              :alt="prevArticle.title"
              class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
            >
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center mb-1">
              <span class="text-xs font-medium text-blue-600 dark:text-blue-400 flex items-center gap-1">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                </svg>
                上一篇
              </span>
            </div>
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white truncate group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              {{ prevArticle.title }}
            </h4>
          </div>
        </div>
      </div>
      <div v-else class="prev-article p-4 rounded-xl bg-gray-50 dark:bg-gray-700/50 border border-gray-200 dark:border-gray-600">
        <div class="flex items-center mb-1">
          <span class="text-xs font-medium text-gray-400 dark:text-gray-500 flex items-center gap-1">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
            上一篇
          </span>
        </div>
        <h4 class="text-sm font-medium text-gray-400 dark:text-gray-500">暂无上一篇文章</h4>
      </div>

      <!-- 下一篇 -->
      <div v-if="nextArticle?.id" class="next-article group cursor-pointer" @click="$emit('go-article', nextArticle.id)">
        <div class="flex items-center gap-4 p-4 rounded-xl bg-gray-50 dark:bg-gray-700/50 hover:bg-blue-50 dark:hover:bg-blue-900/20 border border-gray-200 dark:border-gray-600 transition-colors duration-200">
          <div class="flex-1 min-w-0 text-right">
            <div class="flex items-center justify-end mb-1">
              <span class="text-xs font-medium text-blue-600 dark:text-blue-400 flex items-center gap-1">
                下一篇
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </span>
            </div>
            <h4 class="text-sm font-semibold text-gray-900 dark:text-white truncate group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
              {{ nextArticle.title }}
            </h4>
          </div>
          <div class="w-20 h-16 flex-shrink-0 overflow-hidden rounded-lg">
            <img
              :src="nextArticle.thumbnail || '/default-thumbnail.svg'"
              :alt="nextArticle.title"
              class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
            >
          </div>
        </div>
      </div>
      <div v-else class="next-article p-4 rounded-xl bg-gray-50 dark:bg-gray-700/50 border border-gray-200 dark:border-gray-600">
        <div class="flex items-center justify-end mb-1">
          <span class="text-xs font-medium text-gray-400 dark:text-gray-500 flex items-center gap-1">
            下一篇
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </span>
        </div>
        <h4 class="text-sm font-medium text-gray-400 dark:text-gray-500 text-right">暂无下一篇文章</h4>
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
