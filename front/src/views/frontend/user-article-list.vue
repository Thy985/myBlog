<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4 py-6">
        <!-- 页面标题 -->
        <div class="mb-6">
            <h1 class="text-2xl font-bold text-gray-800 mb-2">我的文章</h1>
            <p class="text-gray-600">管理您发布的博客文章</p>
        </div>

        <!-- 操作和搜索区域 -->
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
            <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-4">
                <button 
                    class="btn btn-primary px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105 flex items-center gap-2"
                    @click="$router.push('/user/articles/create')"
                >
                    <svg class="w-4 h-4" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
                    </svg>
                    发布新文章
                </button>
                <div class="flex gap-2 flex-1 md:flex-none">
                    <input 
                        v-model="searchKeyword"
                        type="text"
                        placeholder="搜索文章标题"
                        class="input input-outline px-3 py-2 text-sm flex-1"
                    >
                    <button 
                        class="btn btn-outline px-3 py-2 text-sm"
                        @click="searchArticles"
                    >
                        搜索
                    </button>
                </div>
            </div>
        </div>

        <!-- 文章列表 -->
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <div class="overflow-x-auto">
                <table class="w-full">
                    <thead>
                        <tr class="bg-gray-50 border-b border-gray-200">
                            <th class="text-left py-3 px-6 font-medium text-gray-600">标题</th>
                            <th class="text-left py-3 px-6 font-medium text-gray-600">分类</th>
                            <th class="text-left py-3 px-6 font-medium text-gray-600">发布时间</th>
                            <th class="text-left py-3 px-6 font-medium text-gray-600">状态</th>
                            <th class="text-left py-3 px-6 font-medium text-gray-600">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-if="articles.length === 0" class="border-b border-gray-200">
                            <td colspan="5" class="py-12 text-center">
                                <div class="flex flex-col items-center">
                                    <svg class="w-12 h-12 text-gray-400 mb-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                    </svg>
                                    <h3 class="text-lg font-medium text-gray-700 mb-1">暂无文章</h3>
                                    <p class="text-gray-500 text-sm">点击上方按钮发布您的第一篇文章</p>
                                </div>
                            </td>
                        </tr>
                        <tr v-for="article in articles" :key="article.id" class="border-b border-gray-200 hover:bg-gray-50 transition-all duration-300">
                            <td class="py-4 px-6">
                                <router-link :to="`/article/detail?id=${article.id}`" class="text-primary hover:underline font-medium flex items-center gap-3">
                                    <span>{{ article.title }}</span>
                                </router-link>
                            </td>
                            <td class="py-4 px-6 text-gray-600">
                                {{ article.category || '未分类' }}
                            </td>
                            <td class="py-4 px-6 text-gray-600">
                                {{ formatDate(article.createdAt) }}
                            </td>
                            <td class="py-4 px-6">
                                <span 
                                    :class="[
                                        'px-3 py-1 rounded-full text-xs font-medium',
                                        article.status === 'published' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                                    ]"
                                >
                                    {{ article.status === 'published' ? '已发布' : '草稿' }}
                                </span>
                            </td>
                            <td class="py-4 px-6">
                                <div class="flex gap-2">
                                    <button 
                                        class="btn btn-outline btn-sm px-3 py-1 text-xs transition-all duration-300 hover:bg-primary hover:text-white"
                                        @click="editArticle(article.id)"
                                    >
                                        编辑
                                    </button>
                                    <button 
                                        class="btn btn-outline btn-sm px-3 py-1 text-xs text-red-600 border-red-200 hover:bg-red-50 transition-all duration-300"
                                        @click="deleteArticle(article.id)"
                                    >
                                        删除
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            
            <!-- 分页 -->
            <div class="p-4 border-t border-gray-200 flex justify-between items-center">
                <p class="text-sm text-gray-600">
                    共 {{ total }} 条记录
                </p>
                <div class="flex gap-1">
                    <button 
                        v-if="currentPage > 1"
                        class="px-3 py-1 rounded text-sm border border-gray-200 hover:bg-gray-50 transition-colors"
                        @click="changePage(currentPage - 1)"
                    >
                        上一页
                    </button>
                    <button 
                        v-for="page in totalPages" 
                        :key="page"
                        :class="[
                            'px-3 py-1 rounded text-sm transition-colors',
                            currentPage === page ? 'bg-primary text-white' : 'border border-gray-200 hover:bg-gray-50'
                        ]"
                        @click="changePage(page)"
                    >
                        {{ page }}
                    </button>
                    <button 
                        v-if="currentPage < totalPages"
                        class="px-3 py-1 rounded text-sm border border-gray-200 hover:bg-gray-50 transition-colors"
                        @click="changePage(currentPage + 1)"
                    >
                        下一页
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

import logger from '@/utils/logger'
// 文章数据
const articles = ref([])
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

// 搜索文章
const searchArticles = () => {
    currentPage.value = 1
    fetchArticles()
}

// 切换页码
const changePage = (page) => {
    currentPage.value = page
    fetchArticles()
}

// 编辑文章
const editArticle = (id) => {
    // 这里应该跳转到编辑页面
    logger.debug('编辑文章:', id)
}

// 删除文章
const deleteArticle = (id) => {
    // 这里应该实现删除逻辑
    logger.debug('删除文章:', id)
}

// 模拟获取文章列表
const fetchArticles = () => {
    // 这里应该通过API获取真实数据
    articles.value = [
        {
            id: 1,
            title: 'Vue 3 组合式 API 最佳实践',
            category: '前端开发',
            createdAt: '2024-01-15T10:00:00Z',
            status: 'published'
        },
        {
            id: 2,
            title: 'Spring Boot 4.0 新特性详解',
            category: '后端开发',
            createdAt: '2024-01-10T14:30:00Z',
            status: 'published'
        },
        {
            id: 3,
            title: 'Tailwind CSS 3.0 入门指南',
            category: '前端开发',
            createdAt: '2024-01-05T09:15:00Z',
            status: 'draft'
        }
    ]
    total.value = articles.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 组件挂载时获取数据
onMounted(() => {
    fetchArticles()
})
</script>

<style scoped>
/* 响应式调整 */
@media (max-width: 768px) {
    .p-6 {
        padding: 1rem;
    }
    
    .py-4 {
        padding: 0.75rem;
    }
    
    .px-4 {
        padding: 0.75rem;
    }
    
    .text-sm {
        font-size: 0.875rem;
    }
    
    .text-xs {
        font-size: 0.75rem;
    }
    
    .w-full {
        font-size: 0.875rem;
    }
}
</style>
