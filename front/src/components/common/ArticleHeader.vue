<template>
  <div class="article-header mb-8">
    <h1 class="title text-2xl md:text-3xl lg:text-4xl font-bold text-gray-900 dark:text-gray-100 mb-4 leading-tight">
      {{ title }}
    </h1>
    <div class="text-gray-600 dark:text-gray-400 text-sm flex flex-wrap items-center gap-x-6 gap-y-2 article-meta">
      <div class="flex items-center">
        <svg class="inline w-4 h-4 mr-2 text-gray-400 dark:text-gray-500" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
          <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 1v3m5-3v3m5-3v3M1 7h18M5 11h10M2 3h16a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" />
        </svg>
        <span class="text-gray-500 dark:text-gray-400">发表于 {{ updateTime }}</span>
      </div>

      <div class="flex items-center">
        <svg class="inline w-4 h-4 mr-2 text-gray-400 dark:text-gray-500" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 18 18">
          <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M1 5v11a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1H1Zm0 0V2a1 1 0 0 1 1-1h5.443a1 1 0 0 1 .8.4l2.7 3.6H1Z" />
        </svg>
        <span class="text-gray-500 dark:text-gray-400">分类于&nbsp;</span>
        <a
          class="text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300 transition-colors cursor-pointer font-medium"
          @click="$emit('go-category', categoryId, categoryName)"
        >
          {{ categoryName }}
        </a>
      </div>

      <div class="flex items-center">
        <svg class="inline w-4 h-4 mr-2 text-gray-400 dark:text-gray-500" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 14">
          <g stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2">
            <path d="M10 10a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
            <path d="M10 13c4.97 0 9-2.686 9-6s-4.03-6-9-6-9 2.686-9 6 4.03 6 9 6Z" />
          </g>
        </svg>
        <span class="text-gray-500 dark:text-gray-400">阅读量</span>
        <span class="font-medium text-gray-700 dark:text-gray-300 ml-1">{{ readNum }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  updateTime: {
    type: String,
    default: ''
  },
  readNum: {
    type: [Number, String],
    default: 0
  },
  categoryId: {
    type: [Number, String],
    default: null
  },
  categoryName: {
    type: String,
    default: ''
  }
})

defineEmits(['go-category'])
</script>

<style scoped>
.title {
  line-height: 1.3;
  word-wrap: break-word;
  font-weight: 700;
  color: var(--text-primary);
}

.dark .title {
  color: var(--text-primary);
}

.article-meta {
  margin-bottom: 1.5rem;
}
</style>
