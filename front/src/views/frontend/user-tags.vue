<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">我的标签</h1>
                <p class="text-text-secondary mt-2">管理您创建的标签</p>
            </div>
            <div class="p-6">
                <div class="flex justify-between items-center mb-6">
                    <div>
                        <button 
                            class="btn btn-primary px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                            @click="showCreateDialog = true"
                        >
                            <svg class="w-4 h-4 mr-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.394 2.08a1 1 0 0 0-.788 0l-7 3a1 1 0 0 0 0 1.84L5.25 8.051a.999.999 0 0 1 .356-.257l4-1.714a1 1 0 1 1 .788 1.838L7.667 9.088l1.94.831a1 1 0 0 0 .787 0l7-3a1 1 0 0 0 0-1.838l-7-3Z" />
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2 12a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v2Z" />
                            </svg>
                            创建标签
                        </button>
                    </div>
                    <div class="flex gap-2">
                        <input 
                            v-model="searchKeyword"
                            type="text"
                            placeholder="搜索标签"
                            class="input input-outline px-3 py-2 text-sm"
                        >
                        <button 
                            class="btn btn-outline px-3 py-2 text-sm"
                            @click="searchTags"
                        >
                            搜索
                        </button>
                    </div>
                </div>
                <div class="space-y-4">
                    <div v-if="tags.length === 0" class="py-12 text-center text-text-secondary">
                        暂无标签
                    </div>
                    <div v-for="tag in tags" :key="tag.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300">
                        <div class="flex justify-between items-start mb-3">
                            <div class="flex-1">
                                <h3 class="font-medium text-text-primary mb-1">{{ tag.name }}</h3>
                                <p class="text-sm text-text-secondary mb-2">{{ tag.description || '无描述' }}</p>
                                <div class="flex justify-between items-center">
                                    <span class="text-xs text-text-tertiary bg-primary-subtle px-2 py-1 rounded-full">
                                        文章数: {{ tag.articleCount }}
                                    </span>
                                    <span class="text-xs text-text-secondary">
                                        创建于 {{ formatDate(tag.createdAt) }}
                                    </span>
                                </div>
                            </div>
                            <div class="flex gap-2">
                                <button 
                                    class="btn btn-outline btn-sm px-3 py-1 text-xs"
                                    @click="editTag(tag)"
                                >
                                    编辑
                                </button>
                                <button 
                                    class="btn btn-outline btn-sm px-3 py-1 text-xs text-danger-color"
                                    @click="deleteTag(tag.id)"
                                >
                                    删除
                                </button>
                            </div>
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

const tags = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const showCreateDialog = ref(false)

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

// 搜索标签
const searchTags = () => {
    currentPage.value = 1
    fetchTags()
}

// 切换页码
const changePage = (page) => {
    currentPage.value = page
    fetchTags()
}

// 编辑标签
const editTag = (tag) => {
    // TODO: 实现编辑标签的逻辑
    logger.debug('编辑标签:', tag.id)
}

// 删除标签
const deleteTag = (id) => {
    // TODO: 实现删除标签的逻辑
    logger.debug('删除标签:', id)
    // 模拟删除标签
    tags.value = tags.value.filter(item => item.id !== id)
    total.value = tags.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 模拟获取标签列表
const fetchTags = () => {
    // 这里应该通过API获取真实数据
    tags.value = [
        {
            id: 1,
            name: 'Vue',
            description: 'Vue.js 相关技术',
            articleCount: 3,
            createdAt: '2024-01-15T10:00:00Z'
        },
        {
            id: 2,
            name: 'Spring Boot',
            description: 'Spring Boot 相关技术',
            articleCount: 2,
            createdAt: '2024-01-10T14:30:00Z'
        },
        {
            id: 3,
            name: 'Tailwind CSS',
            description: 'Tailwind CSS 相关技术',
            articleCount: 1,
            createdAt: '2024-01-05T09:15:00Z'
        },
        {
            id: 4,
            name: 'JavaScript',
            description: 'JavaScript 相关技术',
            articleCount: 4,
            createdAt: '2024-01-01T00:00:00Z'
        }
    ]
    total.value = tags.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 组件挂载时获取数据
onMounted(() => {
    fetchTags()
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
