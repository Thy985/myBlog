<template>
    <div class="bg-white border border-gray-200 rounded-lg p-5 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
        <h3 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
            <svg class="w-5 h-5 text-primary-color" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            相关推荐
        </h3>

        <!-- 加载状态 -->
        <div v-if="loading" class="animate-pulse space-y-4">
            <div class="flex gap-3">
                <div class="w-20 h-16 bg-gray-200 rounded dark:bg-gray-700"></div>
                <div class="flex-1">
                    <div class="h-4 bg-gray-200 rounded dark:bg-gray-700 mb-2"></div>
                    <div class="h-3 bg-gray-200 rounded dark:bg-gray-700 w-3/4"></div>
                    <div class="h-3 bg-gray-200 rounded dark:bg-gray-700 w-1/2 mt-2"></div>
                </div>
            </div>
            <div class="flex gap-3">
                <div class="w-20 h-16 bg-gray-200 rounded dark:bg-gray-700"></div>
                <div class="flex-1">
                    <div class="h-4 bg-gray-200 rounded dark:bg-gray-700 mb-2"></div>
                    <div class="h-3 bg-gray-200 rounded dark:bg-gray-700 w-3/4"></div>
                    <div class="h-3 bg-gray-200 rounded dark:bg-gray-700 w-1/2 mt-2"></div>
                </div>
            </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="articles.length === 0" class="text-center py-6 text-gray-500 dark:text-gray-400">
            暂无相关推荐
        </div>

        <!-- 文章列表 -->
        <div v-else class="space-y-4">
            <div v-for="item in articles" :key="item.id" class="related-article-item">
                <a class="flex gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors cursor-pointer group" @click="$emit('article-click', item.id)">
                    <div class="w-20 h-16 flex-shrink-0 overflow-hidden rounded-md">
                        <img
                            :src="item.thumbnail || '/default-thumbnail.svg'"
                            :alt="item.title"
                            class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
                        />
                    </div>
                    <div class="flex-1 min-w-0">
                        <h4 class="text-sm font-medium text-gray-900 dark:text-white truncate group-hover:text-primary-color transition-colors">
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
</template>

<script setup>
defineProps({
    articles: {
        type: Array,
        default: () => []
    },
    loading: {
        type: Boolean,
        default: false
    }
})

defineEmits(['article-click'])

function truncateContent(html, maxLength = 60) {
    if (!html) {return ''}
    const text = html.replace(/<[^>]*>/g, '')
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}
</script>

<style scoped>
.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
