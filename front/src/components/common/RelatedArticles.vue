<template>
    <div class="related-articles">
        <h3 class="text-base font-semibold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
            <svg class="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            相关推荐
        </h3>

        <!-- 加载状态 -->
        <div v-if="loading" class="animate-pulse space-y-3">
            <div v-for="i in 3" :key="i" class="flex gap-3">
                <div class="w-20 h-16 bg-gray-200 rounded-lg dark:bg-gray-700 flex-shrink-0"></div>
                <div class="flex-1 space-y-2">
                    <div class="h-4 bg-gray-200 rounded dark:bg-gray-700 w-3/4"></div>
                    <div class="h-3 bg-gray-200 rounded dark:bg-gray-700 w-1/2"></div>
                </div>
            </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="articles.length === 0" class="text-center py-6">
            <svg class="w-12 h-12 mx-auto mb-3 text-gray-300 dark:text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            <p class="text-sm text-gray-500 dark:text-gray-400">暂无相关推荐</p>
        </div>

        <!-- 文章列表 -->
        <div v-else class="space-y-3">
            <div
                v-for="item in articles"
                :key="item.id"
                class="related-article-item group cursor-pointer"
                @click="$emit('article-click', item.id)"
            >
                <div class="flex gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                    <div class="w-20 h-16 flex-shrink-0 overflow-hidden rounded-lg shadow-sm">
                        <img
                            :src="item.thumbnail || '/default-thumbnail.svg'"
                            :alt="item.title"
                            class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
                            loading="lazy"
                        />
                    </div>
                    <div class="flex-1 min-w-0">
                        <h4 class="text-sm font-medium text-gray-900 dark:text-white line-clamp-2 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                            {{ item.title }}
                        </h4>
                        <p class="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-1">
                            {{ item.summary || truncateContent(item.content) }}
                        </p>
                    </div>
                </div>
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

function truncateContent(html, maxLength = 50) {
    if (!html) {return ''}
    const text = html.replace(/<[^>]*>/g, '')
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}
</script>

<style scoped>
.line-clamp-1 {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
