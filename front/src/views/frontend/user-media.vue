<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">我的媒体库</h1>
                <p class="text-text-secondary mt-2">管理您上传的文件</p>
            </div>
            <div class="p-6">
                <div class="flex justify-between items-center mb-6">
                    <div>
                        <button 
                            class="btn btn-primary px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                            @click="showUploadDialog = true"
                        >
                            <svg class="w-4 h-4 mr-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.994 19.944a1 1 0 0 0 1.05-.58l1.006-1.937a1 1 0 0 0-.364-1.293L16.273 15.09l-1.91-1.021a2.995 2.995 0 0 0-1.998-.524 2.995 2.995 0 0 0-1.998.524l-1.91 1.021-1.056 1.056a1 1 0 0 0-.28.618l-.154 1.543a1 1 0 0 0 1.182 1.182l1.543-.154a1 1 0 0 0 .618-.28l1.056-1.056 1.021 1.91a1 1 0 0 0 1.293.364l1.937-1.006a1 1 0 0 0 .58-1.05Z" />
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.05 3.05a2.5 2.5 0 0 1 3.535 0l1.414 1.414a2.5 2.5 0 0 1 0 3.535l-8.586 8.586A2 2 0 0 0 4 18h4a1 1 0 0 0 1-1v-4a2.5 2.5 0 0 1 0-3.535l1.414-1.414a2.5 2.5 0 0 1 3.535 0l4.243 4.243a1 1 0 0 0 1.414 0l2.829-2.829a1 1 0 0 0 0-1.414l-4.243-4.243a2.5 2.5 0 0 1 0-3.535l1.414-1.414a2.5 2.5 0 0 1 3.535 0l1.414 1.414a2.5 2.5 0 0 1 0 3.535l-.822.822" />
                            </svg>
                            上传文件
                        </button>
                    </div>
                    <div class="flex gap-2">
                        <input 
                            v-model="searchKeyword"
                            type="text"
                            placeholder="搜索文件"
                            class="input input-outline px-3 py-2 text-sm"
                        >
                        <button 
                            class="btn btn-outline px-3 py-2 text-sm"
                            @click="searchFiles"
                        >
                            搜索
                        </button>
                    </div>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    <div v-if="files.length === 0" class="col-span-full py-16 text-center text-text-secondary">
                        暂无文件
                    </div>
                    <div v-for="file in files" :key="file.id" class="border border-border-color rounded-lg overflow-hidden hover:shadow-md transition-all duration-300">
                        <div class="p-4">
                            <div class="flex justify-between items-start mb-3">
                                <div class="flex items-center gap-3">
                                    <div class="w-12 h-12 rounded-full bg-primary-subtle flex items-center justify-center">
                                        <svg v-if="file.type.startsWith('image/')" class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.994 19.944a1 1 0 0 0 1.05-.58l1.006-1.937a1 1 0 0 0-.364-1.293L16.273 15.09l-1.91-1.021a2.995 2.995 0 0 0-1.998-.524 2.995 2.995 0 0 0-1.998.524l-1.91 1.021-1.056 1.056a1 1 0 0 0-.28.618l-.154 1.543a1 1 0 0 0 1.182 1.182l1.543-.154a1 1 0 0 0 .618-.28l1.056-1.056 1.021 1.91a1 1 0 0 0 1.293.364l1.937-1.006a1 1 0 0 0 .58-1.05Z" />
                                        </svg>
                                        <svg v-else-if="file.type.startsWith('video/')" class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 15a.75.75 0 0 1-.75-.75v-7.5A.75.75 0 0 1 10 6h.008a2 2 0 0 1 1.992 2v1.056a2 2 0 0 1-.555 1.437l-.69 1.039a2 2 0 0 0-.555 1.44V14.25A.75.75 0 0 1 10 15Z" />
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 16.5v-2.25A2.25 2.25 0 0 1 5.25 12H10m0 0v3.75m0-3.75H5.25m10.5 3H19v-3a2 2 0 0 0-2-2H5.25a2 2 0 0 0-2 2v3m14.5 0v-2.25a2.25 2.25 0 0 0-2.25-2.25H15m0 0H8.75m4.5 0H15" />
                                        </svg>
                                        <svg v-else-if="file.type.startsWith('audio/')" class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 11.686a.5.5 0 0 1-.447.283l-7-2a.5.5 0 0 1-.316-.948l7-2a.5.5 0 0 1 .763.445v4.926Z" />
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12.5 6v10a1.5 1.5 0 0 1-3 0V6a1.5 1.5 0 0 1 3 0Z" />
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.5 6v10a1.5 1.5 0 0 1-3 0V6a1.5 1.5 0 0 1 3 0Z" />
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6.5 6v10a1.5 1.5 0 0 1-3 0V6a1.5 1.5 0 0 1 3 0Z" />
                                        </svg>
                                        <svg v-else class="w-6 h-6 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H4Zm12 12H4m10-7h-4m4 0-4-4m4 4-4 4" />
                                        </svg>
                                    </div>
                                    <div>
                                        <h3 class="font-medium text-text-primary text-sm truncate max-w-[200px]">{{ file.name }}</h3>
                                        <p class="text-xs text-text-secondary">
                                            {{ formatFileSize(file.size) }}
                                        </p>
                                    </div>
                                </div>
                                <div class="flex gap-1">
                                    <button 
                                        class="btn btn-outline btn-sm px-2 py-1 text-xs"
                                        @click="previewFile(file.id)"
                                    >
                                        预览
                                    </button>
                                    <button 
                                        class="btn btn-outline btn-sm px-2 py-1 text-xs"
                                        @click="copyFileUrl(file.id)"
                                    >
                                        复制链接
                                    </button>
                                    <button 
                                        class="btn btn-outline btn-sm px-2 py-1 text-xs text-danger-color"
                                        @click="deleteFile(file.id)"
                                    >
                                        删除
                                    </button>
                                </div>
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
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'

// 文件数据
const files = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchKeyword = ref('')
const showUploadDialog = ref(false)

// 计算总页数
const totalPages = ref(0)

// 格式化文件大小
const formatFileSize = (bytes) => {
    if (bytes === 0) {return '0 B'}
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 加载文件列表

// 搜索文件
const searchFiles = () => {
    currentPage.value = 1
    fetchFiles()
}

// 切换页码
const changePage = (page) => {
    currentPage.value = page
    fetchFiles()
}

// 预览文件
const previewFile = (id) => {
    // TODO: 实现文件预览的逻辑
    logger.debug('预览文件:', id)
}

// 复制文件链接
const copyFileUrl = (id) => {
    // TODO: 实现复制文件链接的逻辑
    logger.debug('复制文件链接:', id)
    // 模拟复制成功
    ElMessage.success('文件链接已复制到剪贴板')
}

// 删除文件
const deleteFile = (id) => {
    // TODO: 实现删除文件的逻辑
    logger.debug('删除文件:', id)
    // 模拟删除文件
    files.value = files.value.filter(item => item.id !== id)
    total.value = files.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 模拟获取文件列表
const fetchFiles = () => {
    // 这里应该通过API获取真实数据
    files.value = [
        {
            id: 1,
            name: 'vue3-logo.png',
            type: 'image/png',
            size: 102400,
            createdAt: '2024-01-15T10:00:00Z'
        },
        {
            id: 2,
            name: 'spring-boot-docs.pdf',
            type: 'application/pdf',
            size: 5242880,
            createdAt: '2024-01-10T14:30:00Z'
        },
        {
            id: 3,
            name: 'tailwind-css-cheatsheet.png',
            type: 'image/png',
            size: 204800,
            createdAt: '2024-01-05T09:15:00Z'
        },
        {
            id: 4,
            name: 'javascript-fundamentals.mp4',
            type: 'video/mp4',
            size: 104857600,
            createdAt: '2024-01-01T00:00:00Z'
        },
        {
            id: 5,
            name: 'api-design.md',
            type: 'text/markdown',
            size: 5120,
            createdAt: '2023-12-25T12:00:00Z'
        },
        {
            id: 6,
            name: 'profile-photo.jpg',
            type: 'image/jpeg',
            size: 153600,
            createdAt: '2023-12-20T08:00:00Z'
        }
    ]
    total.value = files.value.length
    totalPages.value = Math.ceil(total.value / pageSize.value)
}

// 组件挂载时获取数据
onMounted(() => {
    fetchFiles()
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
    
    .gap-1 {
        gap: 0.25rem;
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
    
    .grid-cols-1.md\:grid-cols-2.lg\:grid-cols-3 {
        grid-template-columns: repeat(2, 1fr);
    }
    
    .py-16 {
        padding: 4rem 0;
    }
}

@media (max-width: 480px) {
    .grid-cols-1.md\:grid-cols-2.lg\:grid-cols-3 {
        grid-template-columns: 1fr;
    }
    
    .max-w-\[200px\] {
        max-width: 150px;
    }
}
</style>
