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
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { getUserCollects, uncollectArticle } from '@/api/frontend/article'
import { API_STATUS } from '@/composables/api'

const collections = ref([])
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
            totalPages.value = Math.ceil(total.value / pageSize.value)
        }
    } catch (err) {
        logger.error('获取收藏列表失败:', err.message)
        ElMessage.error('获取收藏列表失败')
    } finally {
        loading.value = false
    }
}

const searchCollections = () => {
    currentPage.value = 1
    fetchCollections()
}

const changePage = (page) => {
    currentPage.value = page
    fetchCollections()
}

const removeCollection = async (id) => {
    try {
        await uncollectArticle(id)
        ElMessage.success('取消收藏成功')
        fetchCollections()
    } catch (err) {
        logger.error('取消收藏失败:', err.message)
        ElMessage.error('取消收藏失败')
    }
}

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
