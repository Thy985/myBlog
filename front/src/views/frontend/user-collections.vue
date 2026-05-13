<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden user-page-card">
            <div class="bg-primary-subtle border-b border-border-color p-6 card-header">
                <h1 class="text-2xl font-bold text-text-primary">我的收藏</h1>
                <p class="text-text-secondary mt-2">管理您收藏的文章</p>
            </div>
            <div class="p-6 card-body">
                <div v-if="loading" class="user-page-loading">
                    <div class="user-page-loading-spinner"></div>
                </div>

                <div v-else class="space-y-4">
                    <div v-if="collections.length === 0" class="user-page-empty">
                        <svg class="user-page-empty-icon" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 0 0 0 6.364L12 20.364l7.682-7.682a4.5 4.5 0 0 0-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 0 0-6.364 0Z" />
                        </svg>
                        <h3 class="user-page-empty-title">暂无收藏</h3>
                        <p class="user-page-empty-desc">去发现喜欢的文章吧</p>
                    </div>
                    <div v-for="collection in collections" :key="collection.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300 user-page-item">
                        <div class="flex justify-between items-start mb-3 user-page-mb-3">
                            <div class="flex-1">
                                <h3 class="font-medium text-text-primary mb-1 user-page-mb-2">
                                    <router-link :to="`/article/detail?id=${collection.articleId}`" class="hover:underline">
                                        {{ collection.articleTitle }}
                                    </router-link>
                                </h3>
                                <p class="text-sm text-text-secondary mb-2 user-page-mb-2">
                                    {{ collection.articleExcerpt }}
                                </p>
                                <div class="flex flex-wrap gap-2">
                                    <span class="text-xs text-text-tertiary bg-primary-subtle px-2 py-1 rounded-full">
                                        {{ collection.category || '未分类' }}
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
                                @click="handleRemoveCollection(collection.id)"
                            >
                                取消收藏
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
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { confirmDelete, showSuccess } from '@/utils'
import { getUserCollects, uncollectArticle } from '@/api/frontend/article'
import { API_STATUS } from '@/composables/api'
import '@/assets/css/common-user-pages.css'

const collections = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

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
        day: '2-digit'
    })
}

const fetchCollections = async () => {
    try {
        loading.value = true
        const res = await getUserCollects({
            page: currentPage.value,
            size: pageSize.value
        })
        if (res.code === API_STATUS.SUCCESS && res.data) {
            collections.value = res.data.list || []
            total.value = res.data.total || 0
        }
    } catch (err) {
        logger.error('获取收藏列表失败:', err.message)
        ElMessage.error('获取收藏列表失败')
    } finally {
        loading.value = false
    }
}

const changePage = (page) => {
    currentPage.value = page
    fetchCollections()
}

const handleRemoveCollection = async (id) => {
    const collection = collections.value.find(c => c.id === id)
    if (!collection) {return}
    try {
        await confirmDelete(collection.articleTitle, '收藏')
        await uncollectArticle(id)
        showSuccess('取消收藏成功')
        fetchCollections()
    } catch (err) {
        if (err !== 'cancel') {
            logger.error('取消收藏失败:', err.message)
            ElMessage.error('取消收藏失败')
        }
    }
}

onMounted(async () => {
    if (!store.user?.id) {
        await store.getAdminInfo()
    }
    fetchCollections()
})
</script>

<style scoped>
/* 已使用公共样式文件 common-user-pages.css */
</style>
