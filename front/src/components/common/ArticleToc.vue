<template>
    <!-- 桌面端目录 -->
    <div v-if="tocItems.length > 0" class="hidden lg:block">
        <div class="sticky top-24">
            <div class="bg-white rounded-lg border border-gray-200 p-5 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
                <h3 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
                    <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
                    </svg>
                    文章目录
                </h3>
                <div class="article-toc">
                    <ul class="space-y-2">
                        <li v-for="(item, index) in tocItems" :key="item.id || `toc-${index}`">
                            <a
                                :href="'#' + item.id"
                                :class="['block py-1 px-2 rounded transition-colors', props.activeIndex === index ? 'bg-primary/20 text-primary' : 'hover:bg-gray-100 dark:hover:bg-gray-700']"
                                :style="{ paddingLeft: (item.level - 2) * 16 + 'px' }"
                                @click.prevent="scrollToSection(item.id, index)"
                            >
                                {{ item.title }}
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
const props = defineProps({
    tocItems: {
        type: Array,
        default: () => []
    },
    activeIndex: {
        type: Number,
        default: 0
    }
})

const emit = defineEmits(['toc-click'])

const scrollToSection = (id, index) => {
    const element = document.getElementById(id)
    if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
        emit('toc-click', { id, index })
    }
}
</script>

<style scoped>
.article-toc a {
    @apply text-gray-700 dark:text-gray-300;
    transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.article-toc a:hover {
    color: var(--primary-color);
}
</style>
