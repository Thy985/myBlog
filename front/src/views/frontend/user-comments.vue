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
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { getUserCommentList, updateComment as apiUpdateComment, deleteComment as apiDeleteComment } from '@/api/frontend/user'
import { API_STATUS } from '@/composables/api'

const comments = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const loading = ref(false)

const totalPages = ref(0)

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

const fetchComments = async () => {
    try {
        loading.value = true
        const res = await getUserCommentList({
            current: currentPage.value,
            size: pageSize.value,
            keyword: searchKeyword.value
        })
        if (res.code === API_STATUS.SUCCESS && res.data) {
            comments.value = res.data.list || []
            total.value = res.data.total || 0
            totalPages.value = Math.ceil(total.value / pageSize.value)
        }
    } catch (err) {
        logger.error('获取评论列表失败:', err.message)
        ElMessage.error('获取评论列表失败')
    } finally {
        loading.value = false
    }
}

const searchComments = () => {
    currentPage.value = 1
    fetchComments()
}

const changePage = (page) => {
    currentPage.value = page
    fetchComments()
}

const editComment = async (id) => {
    try {
        const comment = comments.value.find(c => c.id === id)
        if (comment) {
            await apiUpdateComment(id, { content: comment.content })
            ElMessage.success('更新成功')
            fetchComments()
        }
    } catch (err) {
        logger.error('更新评论失败:', err.message)
        ElMessage.error('更新评论失败')
    }
}

const deleteComment = async (id) => {
    try {
        await apiDeleteComment(id)
        ElMessage.success('删除成功')
        fetchComments()
    } catch (err) {
        logger.error('删除评论失败:', err.message)
        ElMessage.error('删除评论失败')
    }
}

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
