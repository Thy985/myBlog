<template>
    <!-- 桌面端目录 -->
    <div v-if="tocItems.length > 0" class="hidden xl:block">
        <div class="sticky top-24">
            <div class="bg-white dark:bg-gray-800 rounded-xl p-5 shadow-lg dark:shadow-gray-900/30 border border-gray-100 dark:border-gray-700">
                <h3 class="text-base font-semibold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
                    <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7" />
                    </svg>
                    文章目录
                </h3>
                <div class="article-toc">
                    <ul class="space-y-1.5">
                        <li v-for="(item, index) in tocItems" :key="item.id || `toc-${index}`">
                            <a
                                :href="'#' + item.id"
                                :class="['block py-2 px-3 rounded-lg transition-all duration-200', props.activeIndex === index ? 'bg-primary/20 text-primary dark:bg-primary/30 font-medium' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700/50']"
                                :style="{ paddingLeft: (item.level - 2) * 16 + 12 + 'px' }"
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
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}
</style>
