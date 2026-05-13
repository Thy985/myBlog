<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden user-page-card">
            <div class="bg-primary-subtle border-b border-border-color p-6 card-header">
                <h1 class="text-2xl font-bold text-text-primary">我的评论</h1>
                <p class="text-text-secondary mt-2">管理您的评论</p>
            </div>
            <div class="p-6 card-body">
                <div v-if="loading" class="user-page-loading">
                    <div class="user-page-loading-spinner"></div>
                </div>

                <div v-else class="space-y-4">
                    <div v-if="comments.length === 0" class="user-page-empty">
                        <svg class="user-page-empty-icon" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 0 1-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8Z" />
                        </svg>
                        <h3 class="user-page-empty-title">暂无评论</h3>
                        <p class="user-page-empty-desc">去文章下发表您的第一条评论吧</p>
                    </div>
                    <div v-for="comment in comments" :key="comment.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300 user-page-item">
                        <div class="flex justify-between items-start mb-3 user-page-mb-3">
                            <div>
                                <h3 class="font-medium text-text-primary mb-1 user-page-mb-2">
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
                        <div class="text-text-primary mb-3 user-page-mb-3">
                            {{ comment.content }}
                        </div>
                        <div class="flex justify-end gap-2 user-page-gap-2">
                            <button
                                class="btn btn-outline btn-sm px-3 py-1 text-xs"
                                @click="openEditDialog(comment)"
                            >
                                编辑
                            </button>
                            <button
                                class="btn btn-outline btn-sm px-3 py-1 text-xs text-danger-color"
                                @click="handleDeleteComment(comment.id)"
                            >
                                删除
                            </button>
                        </div>
                    </div>
                </div>

                <div v-if="!loading && totalPages > 1" class="mt-6 flex justify-between items-center user-page-mt-6">
                    <p class="text-sm text-text-secondary">
                        共 {{ total }} 条记录
                    </p>
                    <div class="flex gap-1 user-page-gap-2">
                        <button
                            class="user-page-pagination-btn"
                            :disabled="currentPage === 1"
                            @click="changePage(currentPage - 1)"
                        >
                            上一页
                        </button>
                        <button
                            v-for="page in visiblePages"
                            :key="page"
                            :class="[
                                'user-page-pagination-btn',
                                page === currentPage ? 'active' : '',
                                page === '...' ? 'user-page-pagination-ellipsis' : ''
                            ]"
                            :disabled="page === '...'"
                            @click="page !== '...' && changePage(page)"
                        >
                            {{ page }}
                        </button>
                        <button
                            class="user-page-pagination-btn"
                            :disabled="currentPage === totalPages"
                            @click="changePage(currentPage + 1)"
                        >
                            下一页
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <el-dialog v-model="dialogVisible" title="编辑评论" width="500px">
            <el-form :model="formData" label-width="80px">
                <el-form-item label="评论内容" required>
                    <el-input
                        v-model="formData.content"
                        type="textarea"
                        :rows="4"
                        placeholder="请输入评论内容"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { confirmDelete, showSuccess } from '@/utils'
import { getUserCommentList, updateComment as apiUpdateComment } from '@/api/frontend/user'
import { deleteComment as apiDeleteComment } from '@/api/frontend/comment'
import { API_STATUS } from '@/composables/api'
import '@/assets/css/common-user-pages.css'

const comments = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const submitLoading = ref(false)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const dialogVisible = ref(false)
const editingComment = ref(null)
const formData = ref({
    content: ''
})

const visiblePages = computed(() => {
    const pages = []
    const total = totalPages.value
    const current = currentPage.value

    if (total <= 7) {
        for (let i = 1; i <= total; i++) {
            pages.push(i)
        }
    } else {
        pages.push(1)
        if (current > 3) {
            pages.push('...')
        }
        for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
            pages.push(i)
        }
        if (current < total - 2) {
            pages.push('...')
        }
        pages.push(total)
    }
    return pages
})

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
            page: currentPage.value,
            size: pageSize.value
        })
        if (res.code === API_STATUS.SUCCESS && res.data) {
            comments.value = res.data.list || []
            total.value = res.data.total || 0
        }
    } catch (err) {
        logger.error('获取评论列表失败:', err.message)
        ElMessage.error('获取评论列表失败')
    } finally {
        loading.value = false
    }
}

const changePage = (page) => {
    currentPage.value = page
    fetchComments()
}

const openEditDialog = (comment) => {
    editingComment.value = comment
    formData.value = { content: comment.content }
    dialogVisible.value = true
}

const handleSubmit = async () => {
    if (!formData.value.content || !formData.value.content.trim()) {
        ElMessage.error('评论内容不能为空')
        return
    }
    if (!editingComment.value) {return}
    submitLoading.value = true
    try {
        await apiUpdateComment(editingComment.value.id, { content: formData.value.content })
        showSuccess('更新成功')
        dialogVisible.value = false
        fetchComments()
    } catch (err) {
        logger.error('更新评论失败:', err.message)
        ElMessage.error('更新评论失败')
    } finally {
        submitLoading.value = false
    }
}

const handleDeleteComment = async (id) => {
    const comment = comments.value.find(c => c.id === id)
    if (!comment) {return}
    try {
        await confirmDelete(comment.content.substring(0, 20) + (comment.content.length > 20 ? '...' : ''), '评论')
        await apiDeleteComment(id)
        showSuccess('删除成功')
        fetchComments()
    } catch (err) {
        if (err !== 'cancel') {
            logger.error('删除评论失败:', err.message)
            ElMessage.error('删除评论失败')
        }
    }
}

onMounted(async () => {
    if (!store.user?.id) {
        await store.getAdminInfo()
    }
    fetchComments()
})
</script>

<style scoped>
/* 已使用公共样式文件 common-user-pages.css */
</style>
