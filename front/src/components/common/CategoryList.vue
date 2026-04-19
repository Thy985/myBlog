<template>
    <div class="bg-white border border-gray-200 rounded-lg p-5 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
        <h2 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
            <FolderIcon class="w-5 h-5 text-primary-color" />
            {{ title }}
        </h2>
        <div class="space-y-1">
            <a
                v-for="item in categories"
                :key="item.id"
                class="flex items-center py-2 px-3 rounded-md hover:bg-gray-100 hover:text-blue-700 dark:hover:bg-gray-700 dark:hover:text-blue-300 transition-colors cursor-pointer"
                :class="{ 'bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300': isActive(item) }"
                @click="handleClick(item)"
            >
                <FolderIcon class="w-4 h-4 mr-2 text-gray-500 dark:text-gray-400 flex-shrink-0" />
                <span class="truncate">{{ item.name }}</span>
                <span v-if="showCount && item.articleCount !== undefined" class="ml-auto text-xs text-gray-400">
                    {{ item.articleCount }}
                </span>
            </a>
            <div v-if="categories.length === 0" class="text-center py-6">
                <FolderIcon class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-2" />
                <p class="text-gray-500 dark:text-gray-400 text-sm">暂无分类</p>
                <p class="text-gray-400 dark:text-gray-500 text-xs mt-1">管理员可以添加分类</p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { FolderIcon } from '@heroicons/vue/24/outline'
import { useRoute } from 'vue-router'

const route = useRoute()

const props = defineProps({
    categories: {
        type: Array,
        default: () => []
    },
    title: {
        type: String,
        default: '分类'
    },
    showCount: {
        type: Boolean,
        default: false
    },
    activeId: {
        type: [String, Number],
        default: null
    }
})

const emit = defineEmits(['category-click'])

const handleClick = (item) => {
    emit('category-click', item.id, item.name)
}

const isActive = (item) => {
    if (props.activeId) {
        return item.id === props.activeId
    }
    // 从路由参数判断
    return route.params.id === String(item.id) || route.query.category === String(item.id)
}
</script>
