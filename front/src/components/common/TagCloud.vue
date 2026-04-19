<template>
    <div class="bg-white border border-gray-200 rounded-lg p-5 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
        <h2 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
            <TagIcon class="w-5 h-5 text-primary-color" />
            {{ title }}
        </h2>
        <div class="flex flex-wrap gap-2">
            <span
                v-for="item in tags"
                :key="item.id"
                class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium transition-colors cursor-pointer"
                :class="getTagClass(item)"
                @click="handleClick(item)"
            >
                {{ item.name }}
                <span v-if="showCount && item.articleCount !== undefined" class="ml-1 opacity-70">
                    {{ item.articleCount }}
                </span>
            </span>
            <div v-if="tags.length === 0" class="text-center py-6 w-full">
                <TagIcon class="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-2" />
                <p class="text-gray-500 dark:text-gray-400 text-sm">暂无标签</p>
                <p class="text-gray-400 dark:text-gray-500 text-xs mt-1">标签会在发布文章时自动创建</p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { TagIcon } from '@heroicons/vue/24/outline'
import { useRoute } from 'vue-router'

const route = useRoute()

const props = defineProps({
    tags: {
        type: Array,
        default: () => []
    },
    title: {
        type: String,
        default: '标签'
    },
    showCount: {
        type: Boolean,
        default: false
    },
    activeId: {
        type: [String, Number],
        default: null
    },
    // 标签云模式 - 根据热度显示不同大小
    cloudMode: {
        type: Boolean,
        default: false
    }
})

const emit = defineEmits(['tag-click'])

const handleClick = (item) => {
    emit('tag-click', item.id, item.name)
}

const isActive = (item) => {
    if (props.activeId) {
        return item.id === props.activeId
    }
    return route.params.id === String(item.id) || route.query.tag === String(item.id)
}

// 获取标签样式
const getTagClass = (item) => {
    const isActiveTag = isActive(item)

    if (isActiveTag) {
        return 'bg-blue-500 text-white hover:bg-blue-600'
    }

    // 标签云模式 - 根据热度调整
    if (props.cloudMode && item.articleCount) {
        const count = item.articleCount
        if (count >= 10) {
            return 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 hover:bg-blue-200 dark:hover:bg-blue-900/50 text-sm'
        } else if (count >= 5) {
            return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300 hover:bg-green-200 dark:hover:bg-green-900/50'
        }
    }

    return 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
}
</script>
