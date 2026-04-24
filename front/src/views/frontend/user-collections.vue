<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">我的收藏</h1>
                <p class="text-text-secondary mt-2">管理您收藏的文章</p>
            </div>
            <div class="p-6">
                <div class="flex justify-between items-center mb-6">
                    <div class="flex gap-2">
                        <input 
                            v-model="searchKeyword"
                            type="text"
                            placeholder="搜索收藏"
                            class="input input-outline px-3 py-2 text-sm"
                        >
                        <button 
                            class="btn btn-outline px-3 py-2 text-sm"
                            @click="searchCollections"
                        >
                            搜索
                        </button>
                    </div>
                </div>
                <div class="space-y-4">
                    <div v-if="collections.length === 0" class="py-12 text-center text-text-secondary">
                        暂无收藏
                    </div>
                    <div v-for="collection in collections" :key="collection.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300">
                        <div class="flex justify-between items-start mb-3">
                            <div class="flex-1">
                                <h3 class="font-medium text-text-primary mb-1">
                                    <router-link :to="`/article/detail?id=${collection.articleId}`" class="hover:underline">
                                        {{ collection.articleTitle }}
                                    </router-link>
                                </h3>
                                <p class="text-sm text-text-secondary mb-2">
                                    {{ collection.articleExcerpt }}
                                </p>
                                <div class="flex flex-wrap gap-2">
                                    <span class="text-xs text-text-tertiary bg-primary-subtle px-2 py-1 rounded-full">
                                        {{ collection.category || '未分类' }}
                                    </span>
                                    <span class="text-xs text-text-tertiary bg-primary-subtle px-2 py-1 rounded-full">
                                        {{ collection.tags?.join(', ') || '无标签' }}
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="flex justify-between items-center">
                            <p class="text-sm text-text-secondary">
                                收藏于 {{ formatDate(collection.collectedAt) }}
                            </p>
                            <button 
                                class="btn btn-outline btn-sm px-3 py-1 text-xs text-danger-color"
                                @click="removeCollection(collection.id)"
                            >
                                取消收藏
                            </button>
                        </div>
                    </div>
                </div>
                <div class="mt-6 flex justify-between items-center">
                    <p class="text-sm text-text-secondary">
                        共 {{ total }} 条记录
                    </p>
                    <div class="flex gap-1">
                        <button 
                            v-for="page in totalPages" 
                            :key="page"
                            :class="[
                                'px-3 py-1 rounded text-sm',
                                currentPage === page ? 'bg-primary-color text-white' : 'border border-border-color hover:bg-primary-subtle'
                            ]"
                            @click="changePage(page)"
                        >
                            {{ page }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import logger from '@/utils/logger'

// 收藏数据
const collections = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')

// 计算总页数
const totalPages = ref(0)

// 格式化日期
const formatDate = (dateString) => {
    if (!dateString) {return '未知'}
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    })
}

// 搜索收藏
const searchCollections = () => {
    currentPage.value = 1
    fetchCollections()
}

// 切换页码
const changePage = (page) => {
    currentPage.value = page
    fetchCollections()
}

// 移除收藏
const removeCollection = (id) => {
    // TODO: 实现移除收藏的逻辑
    logger.debug('移除收藏:', id)
    // 模拟移除收藏
    collections.value = collections.value.filter(item => item.id !== id)
    total.value = collections.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 模拟获取收藏列表
const fetchCollections = () => {
    // 这里应该通过API获取真实数据
    collections.value = [
        {
            id: 1,
            articleId: 1,
            articleTitle: 'Vue 3 组合式 API 最佳实践',
            articleExcerpt: '本文介绍了 Vue 3 组合式 API 的使用方法和最佳实践，帮助开发者更好地理解和应用这一特性。',
            category: '前端开发',
            tags: ['Vue', '前端', '组合式 API'],
            collectedAt: '2024-01-15T10:00:00Z'
        },
        {
            id: 2,
            articleId: 2,
            articleTitle: 'Spring Boot 4.0 新特性详解',
            articleExcerpt: 'Spring Boot 4.0 带来了许多新特性和改进，本文详细介绍了这些变化及其对开发的影响。',
            category: '后端开发',
            tags: ['Spring Boot', 'Java', '后端'],
            collectedAt: '2024-01-10T14:30:00Z'
        },
        {
            id: 3,
            articleId: 3,
            articleTitle: 'Tailwind CSS 3.0 入门指南',
            articleExcerpt: 'Tailwind CSS 3.0 是一个实用优先的 CSS 框架，本文将帮助你快速上手并掌握其核心功能。',
            category: '前端开发',
            tags: ['Tailwind CSS', 'CSS', '前端'],
            collectedAt: '2024-01-05T09:15:00Z'
        }
    ]
    total.value = collections.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 组件挂载时获取数据
onMounted(() => {
    fetchCollections()
})
</script>

<style scoped>
/* 响应式调整 */
@media (max-width: 768px) {
    .p-6 {
        padding: 1rem;
    }
    
    .p-4 {
        padding: 0.75rem;
    }
    
    .text-sm {
        font-size: 0.875rem;
    }
    
    .text-xs {
        font-size: 0.75rem;
    }
    
    .gap-2 {
        gap: 0.5rem;
    }
    
    .mt-6 {
        margin-top: 1.5rem;
    }
    
    .mb-6 {
        margin-bottom: 1.5rem;
    }
    
    .mb-3 {
        margin-bottom: 0.75rem;
    }
    
    .mb-2 {
        margin-bottom: 0.5rem;
    }
    
    .py-12 {
        padding: 3rem 0;
    }
}
</style>
