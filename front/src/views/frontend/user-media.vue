<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden user-page-card">
            <div class="bg-primary-subtle border-b border-border-color p-6 card-header">
                <h1 class="text-2xl font-bold text-text-primary">我的媒体库</h1>
                <p class="text-text-secondary mt-2">管理您上传的文件</p>
            </div>
            <div class="p-6 card-body">
                <div class="flex justify-end mb-6 user-page-mb-6">
                    <button
                        class="btn btn-primary px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                        @click="showUploadDialog = true"
                    >
                        <svg class="w-4 h-4 mr-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                        </svg>
                        上传文件
                    </button>
                </div>

                <div v-if="loading" class="user-page-loading">
                    <div class="user-page-loading-spinner"></div>
                </div>

                <div v-else class="space-y-4">
                    <div v-if="files.length === 0" class="user-page-empty">
                        <svg class="user-page-empty-icon" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-6l-2-2H5a2 2 0 0 0-2 2Z" />
                        </svg>
                        <h3 class="user-page-empty-title">暂无文件</h3>
                        <p class="user-page-empty-desc">点击上方按钮上传您的第一个文件</p>
                    </div>
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        <div v-for="file in files" :key="file.id" class="border border-border-color rounded-lg overflow-hidden hover:shadow-md transition-all duration-300">
                            <div class="p-4">
                                <div class="flex items-start gap-3">
                                    <div class="w-12 h-12 rounded-lg bg-primary-subtle flex items-center justify-center flex-shrink-0">
                                        <svg v-if="file.type?.startsWith('image/')" class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 0 1 2.828 0L16 16m-2-2 1.586-1.586a2 2 0 0 1 2.828 0L20 14m-6-6h.01M6 20h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2Z" />
                                        </svg>
                                        <svg v-else-if="file.type?.startsWith('video/')" class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0 1 21 8.618v2.764a1 1 0 0 1-1.447.894L15 14M3 8a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8Z" />
                                        </svg>
                                        <svg v-else class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 0 0 2-2V9.414a1 1 0 0 0-.293-.707l-5.414-5.414A1 1 0 0 0 12.586 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2Z" />
                                        </svg>
                                    </div>
                                    <div class="flex-1 min-w-0">
                                        <h3 class="font-medium text-text-primary text-sm truncate" :title="file.name">{{ file.name }}</h3>
                                        <p class="text-xs text-text-secondary mt-1">
                                            {{ formatFileSize(file.size) }}
                                        </p>
                                    </div>
                                </div>
                                <div class="flex gap-2 mt-3">
                                    <button
                                        v-if="file.type?.startsWith('image/')"
                                        class="btn btn-outline btn-sm px-2 py-1 text-xs flex-1"
                                        @click="previewFile(file)"
                                    >
                                        预览
                                    </button>
                                    <button
                                        class="btn btn-outline btn-sm px-2 py-1 text-xs flex-1"
                                        @click="copyFileUrl(file)"
                                    >
                                        复制链接
                                    </button>
                                    <button
                                        class="btn btn-outline btn-sm px-2 py-1 text-xs text-danger-color flex-1"
                                        @click="handleDeleteFile(file)"
                                    >
                                        删除
                                    </button>
                                </div>
                            </div>
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

        <el-dialog v-model="previewVisible" title="文件预览" width="800px">
            <div class="flex justify-center">
                <img v-if="previewFile?.type?.startsWith('image/')" :src="previewFile.url" :alt="previewFile.name" class="max-w-full" />
                <video v-else-if="previewFile?.type?.startsWith('video/')" :src="previewFile.url" controls class="max-w-full"></video>
                <div v-else class="text-center py-8">
                    <p class="text-text-secondary">该文件类型不支持预览</p>
                </div>
            </div>
        </el-dialog>

        <el-dialog v-model="showUploadDialog" title="上传文件" width="500px">
            <div class="py-4">
                <el-upload
                    class="upload-demo"
                    drag
                    :action="uploadUrl"
                    :headers="{ Authorization: 'Bearer ' + token }"
                    :on-success="handleUploadSuccess"
                    :on-error="handleUploadError"
                    multiple
                >
                    <div class="el-upload__text">
                        拖拽文件到此处或 <em>点击上传</em>
                    </div>
                </el-upload>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { confirmDelete, showSuccess } from '@/utils'
import { getUserMediaList, deleteMedia as apiDeleteMedia } from '@/api/frontend/user'
import { API_STATUS } from '@/composables/api'
import '@/assets/css/common-user-pages.css'

const files = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const loading = ref(false)
const showUploadDialog = ref(false)
const previewVisible = ref(false)
const previewFile = ref(null)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const uploadUrl = import.meta.env.VITE_APP_BASE_URL + '/file/upload'
const token = localStorage.getItem('token') || ''

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

const formatFileSize = (bytes) => {
    if (bytes === 0) {return '0 B'}
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const fetchFiles = async () => {
    try {
        loading.value = true
        const res = await getUserMediaList({
            page: currentPage.value,
            size: pageSize.value
        })
        if (res.code === API_STATUS.SUCCESS && res.data) {
            files.value = res.data.list || []
            total.value = res.data.total || 0
        }
    } catch (err) {
        logger.error('获取文件列表失败:', err.message)
        ElMessage.error('获取文件列表失败')
    } finally {
        loading.value = false
    }
}

const changePage = (page) => {
    currentPage.value = page
    fetchFiles()
}

const previewFile = (file) => {
    previewFile.value = file
    previewVisible.value = true
}

const copyFileUrl = (file) => {
    if (file.url) {
        navigator.clipboard.writeText(file.url).then(() => {
            ElMessage.success('文件链接已复制到剪贴板')
        }).catch(() => {
            ElMessage.error('复制失败')
        })
    }
}

const handleDeleteFile = async (file) => {
    try {
        await confirmDelete(file.name, '文件')
        await apiDeleteMedia(file.id)
        showSuccess('删除成功')
        fetchFiles()
    } catch (err) {
        if (err !== 'cancel') {
            logger.error('删除文件失败:', err.message)
            ElMessage.error('删除文件失败')
        }
    }
}

const handleUploadSuccess = () => {
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    fetchFiles()
}

const handleUploadError = () => {
    ElMessage.error('上传失败')
}

onMounted(async () => {
    if (!store.user?.id) {
        await store.getAdminInfo()
    }
    fetchFiles()
})
</script>

<style scoped>
/* 已使用公共样式文件 common-user-pages.css */
</style>
