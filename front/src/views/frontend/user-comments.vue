<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">我的评论</h1>
                <p class="text-text-secondary mt-2">管理您的评论</p>
            </div>
            <div class="p-6">
                <div class="flex justify-between items-center mb-6">
                    <div class="flex gap-2">
                        <input 
                            v-model="searchKeyword"
                            type="text"
                            placeholder="搜索评论"
                            class="input input-outline px-3 py-2 text-sm"
                        >
                        <button 
                            class="btn btn-outline px-3 py-2 text-sm"
                            @click="searchComments"
                        >
                            搜索
                        </button>
                    </div>
                </div>
                <div class="space-y-4">
                    <div v-if="comments.length === 0" class="py-12 text-center text-text-secondary">
                        暂无评论
                    </div>
                    <div v-for="comment in comments" :key="comment.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300">
                        <div class="flex justify-between items-start mb-3">
                            <div>
                                <h3 class="font-medium text-text-primary mb-1">
                                    <router-link :to="`/article/detail?id=${comment.articleId}`" class="hover:underline">
                                        {{ comment.articleTitle }}
                                    </router-link>
                                </h3>
                                <p class="text-sm text-text-secondary">
                                    发表于 {{ formatDate(comment.createdAt) }}
                                </p>
                            </div>
                            <span 
                                :class="[
                                    'px-2 py-1 rounded-full text-xs font-medium',
                                    comment.status === 'approved' ? 'bg-success-subtle text-success-color' : 'bg-warning-subtle text-warning-color'
                                ]"
                            >
                                {{ comment.status === 'approved' ? '已审核' : '待审核' }}
                            </span>
                        </div>
                        <div class="text-text-primary mb-3">
                            {{ comment.content }}
                        </div>
                        <div class="flex justify-end gap-2">
                            <button 
                                class="btn btn-outline btn-sm px-3 py-1 text-xs"
                                @click="editComment(comment.id)"
                            >
                                编辑
                            </button>
                            <button 
                                class="btn btn-outline btn-sm px-3 py-1 text-xs text-danger-color"
                                @click="deleteComment(comment.id)"
                            >
                                删除
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

const comments = ref([])
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
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

// 搜索评论
const searchComments = () => {
    currentPage.value = 1
    fetchComments()
}

// 切换页码
const changePage = (page) => {
    currentPage.value = page
    fetchComments()
}

// 编辑评论
const editComment = (id) => {
    // TODO: 实现编辑评论的逻辑
    logger.debug('编辑评论:', id)
}

const deleteComment = (id) => {
    // TODO: 实现删除评论的逻辑
    logger.debug('删除评论:', id)
}

// 模拟获取评论列表
const fetchComments = () => {
    // 这里应该通过API获取真实数据
    comments.value = [
        {
            id: 1,
            articleId: 1,
            articleTitle: 'Vue 3 组合式 API 最佳实践',
            content: '这篇文章写得非常好，对我理解 Vue 3 的组合式 API 很有帮助！',
            createdAt: '2024-01-15T10:00:00Z',
            status: 'approved'
        },
        {
            id: 2,
            articleId: 2,
            articleTitle: 'Spring Boot 4.0 新特性详解',
            content: 'Spring Boot 4.0 的新特性确实很强大，期待在项目中使用！',
            createdAt: '2024-01-10T14:30:00Z',
            status: 'approved'
        },
        {
            id: 3,
            articleId: 3,
            articleTitle: 'Tailwind CSS 3.0 入门指南',
            content: 'Tailwind CSS 3.0 的响应式设计非常方便，大大提高了开发效率。',
            createdAt: '2024-01-05T09:15:00Z',
            status: 'approved'
        }
    ]
    total.value = comments.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 组件挂载时获取数据
onMounted(() => {
    fetchComments()
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
    
    .py-12 {
        padding: 3rem 0;
    }
}
</style>
