<template>
    <div class="container mx-auto max-w-screen-xl mt-8 px-4">
        <div class="bg-background-primary border border-border-color rounded-xl shadow-md overflow-hidden">
            <div class="bg-primary-subtle border-b border-border-color p-6">
                <h1 class="text-2xl font-bold text-text-primary">我的分类</h1>
                <p class="text-text-secondary mt-2">管理您创建的分类</p>
            </div>
            <div class="p-6">
                <div class="flex justify-between items-center mb-6">
                    <div>
                        <button 
                            class="btn btn-primary px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
                            @click="showCreateDialog = true"
                        >
                            <svg class="w-4 h-4 mr-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10H7m0 0a2 2 0 1 0 0 4h6a2 2 0 1 0 0-4ZM7 10a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2m0 0a2 2 0 1 0 0 4H9a2 2 0 1 0 0-4Z" />
                            </svg>
                            创建分类
                        </button>
                    </div>
                    <div class="flex gap-2">
                        <input 
                            v-model="searchKeyword"
                            type="text"
                            placeholder="搜索分类"
                            class="input input-outline px-3 py-2 text-sm"
                        >
                        <button 
                            class="btn btn-outline px-3 py-2 text-sm"
                            @click="searchCategories"
                        >
                            搜索
                        </button>
                    </div>
                </div>
                <div class="space-y-4">
                    <div v-if="categories.length === 0" class="py-12 text-center text-text-secondary">
                        暂无分类
                    </div>
                    <div v-for="category in categories" :key="category.id" class="border border-border-color rounded-lg p-4 hover:bg-primary-subtle transition-all duration-300">
                        <div class="flex justify-between items-start mb-3">
                            <div class="flex-1">
                                <h3 class="font-medium text-text-primary mb-1">{{ category.name }}</h3>
                                <p class="text-sm text-text-secondary mb-2">{{ category.description || '无描述' }}</p>
                                <div class="flex justify-between items-center">
                                    <span class="text-xs text-text-tertiary bg-primary-subtle px-2 py-1 rounded-full">
                                        文章数: {{ category.articleCount }}
                                    </span>
                                    <span class="text-xs text-text-secondary">
                                        创建于 {{ formatDate(category.createdAt) }}
                                    </span>
                                </div>
                            </div>
                            <div class="flex gap-2">
                                <button 
                                    class="btn btn-outline btn-sm px-3 py-1 text-xs"
                                    @click="editCategory(category)"
                                >
                                    编辑
                                </button>
                                <button 
                                    class="btn btn-outline btn-sm px-3 py-1 text-xs text-danger-color"
                                    @click="deleteCategory(category.id)"
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
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { getUserCategoryList, updateCategory, deleteCategory as apiDeleteCategory } from '@/api/frontend/user'
import { API_STATUS } from '@/composables/api'

const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const showCreateDialog = ref(false)
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

const fetchCategories = async () => {
    try {
        loading.value = true
        const res = await getUserCategoryList()
        if (res.code === API_STATUS.SUCCESS && res.data) {
            categories.value = res.data || []
            total.value = categories.value.length
            totalPages.value = 1
        }
    } catch (err) {
        logger.error('获取分类列表失败:', err.message)
        ElMessage.error('获取分类列表失败')
    } finally {
        loading.value = false
    }
}

const searchCategories = () => {
    currentPage.value = 1
    fetchCategories()
}

const changePage = (page) => {
    currentPage.value = page
    fetchCategories()
}

const editCategory = async (category) => {
    try {
        await updateCategory(category.id, { name: category.name, description: category.description })
        ElMessage.success('更新成功')
        fetchCategories()
    } catch (err) {
        logger.error('更新分类失败:', err.message)
        ElMessage.error('更新分类失败')
    }
}

const deleteCategory = async (id) => {
    try {
        await apiDeleteCategory(id)
        ElMessage.success('删除成功')
        fetchCategories()
    } catch (err) {
        logger.error('删除分类失败:', err.message)
        ElMessage.error('删除分类失败')
    }
}

onMounted(() => {
    fetchCategories()
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
