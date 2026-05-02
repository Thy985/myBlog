<template>
    <Header></Header>

    <div class="container mx-auto max-w-screen-xl mt-5 px-4">
        <!-- 面包屑导航 -->
        <nav class="mb-6 breadcrumbs" aria-label="breadcrumb">
            <ul class="flex flex-wrap items-center space-x-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                <li>
                    <router-link to="/" class="hover:text-primary-color transition-colors duration-200" aria-label="首页">
                        <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"></path>
                        </svg>
                        首页
                    </router-link>
                </li>
                <li>
                    <span class="flex items-center">
                        <svg class="w-4 h-4 mx-2" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"></path>
                        </svg>
                        分类
                    </span>
                </li>
            </ul>
        </nav>

        <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
            <!-- 左边栏 -->
            <div class="md:col-span-3">
                <div class="mb-6 bg-white border border-gray-200 rounded-lg shadow-sm dark:bg-gray-800 dark:border-gray-700 overflow-hidden">
                    <div class="p-5 border-b border-gray-200 dark:border-gray-700">
                        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">分类</h1>
                        <p class="mt-2 text-gray-600 dark:text-gray-400">浏览所有文章分类</p>
                    </div>
                    
                    <!-- 加载状态 -->
                    <div v-if="loading" class="flex items-center justify-center py-12">
                        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-color"></div>
                    </div>
                    
                    <!-- 空状态 -->
                    <div v-else-if="categories.length === 0" class="p-8 text-center text-gray-500 dark:text-gray-400">
                        <svg class="mx-auto w-16 h-16 mb-4 opacity-50" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M2 2a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V2z"></path>
                            <path d="M15 7a2 2 0 11-4 0 2 2 0 014 0z"></path>
                        </svg>
                        <h3 class="text-lg font-medium">暂无分类数据</h3>
                        <p class="mt-2">分类将在您发布文章时创建</p>
                    </div>
                    
                    <!-- 分类列表 -->
                    <div v-else class="p-4">
                        <ul class="category-list">
                            <li 
                                v-for="item in sortedCategories" 
                                :key="item.id"
                                class="category-item"
                            >
                                <a 
                                    class="category-link"
                                    :aria-label="`查看分类 ${item.name} 的文章`"
                                    :tabindex="0"
                                    @click="goCategoryArticleListPage(item.id, item.name)"
                                    @keydown.enter="goCategoryArticleListPage(item.id, item.name)"
                                    @keydown.space.prevent="goCategoryArticleListPage(item.id, item.name)"
                                >
                                    <div class="category-icon">
                                        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                                            <path d="M2 2a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V2z"></path>
                                            <path d="M15 7a2 2 0 11-4 0 2 2 0 014 0z"></path>
                                        </svg>
                                    </div>
                                    <div class="category-info">
                                        <span class="category-name">{{ item.name }}</span>
                                        <span class="category-count">{{ item.articleCount || 0 }}</span>
                                    </div>
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
            <!-- 右边栏 -->
            <div class="md:col-span-1">
                <UserInfoCard></UserInfoCard>
            </div>
        </div>
    </div>

    <Footer></Footer>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

import Header from '@/layouts/components/Header.vue'
import Footer from '@/layouts/components/Footer.vue'
import { getCategories } from '@/api/frontend/category'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))

const router = useRouter()

const goCategoryArticleListPage = (id, name) => {
    router.push({
        name: 'category-articles',
        params: { id: String(id), name: name }
    })
}

const categories = ref([])
const loading = ref(false)

// 按文章数量排序分类
const sortedCategories = computed(() => {
    return [...categories.value].sort((a, b) => (b.count || 0) - (a.count || 0))
})

const fetchCategories = async () => {
    try {
        loading.value = true
        const res = await getCategories()

        if (res.code === API_STATUS.SUCCESS) {
            categories.value = res.data || []
        }
    } catch (error) {
        logger.error('获取分类列表失败:', error)
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchCategories()
})
</script>

<style scoped>
/* 分类列表样式 */
.category-list {
    list-style: none;
    margin: 0;
    padding: 0;
}

.category-item {
    margin-bottom: 0.5rem;
}

.category-link {
    display: flex;
    align-items: center;
    padding: 0.75rem 1rem;
    background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
    text-decoration: none;
    position: relative;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.category-link:hover {
    transform: translateX(4px);
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
    border-color: #cbd5e1;
}

.category-link:focus {
    outline: 2px solid rgba(59, 130, 246, 0.5);
    outline-offset: 2px;
}

.category-icon {
    margin-right: 1rem;
    color: #6b7280;
    transition: color 0.3s ease;
}

.category-link:hover .category-icon {
    color: #3b82f6;
}

.category-info {
    flex: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.category-name {
    font-size: 0.875rem;
    font-weight: 500;
    color: #4b5563;
    transition: color 0.3s ease;
}

.category-link:hover .category-name {
    color: #1f2937;
}

.category-count {
    background-color: rgba(107, 114, 128, 0.1);
    color: #6b7280;
    font-size: 0.75rem;
    font-weight: 600;
    padding: 0.125rem 0.5rem;
    border-radius: 9999px;
    min-width: 24px;
    text-align: center;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.category-link:hover .category-count {
    background-color: rgba(59, 130, 246, 0.1);
    color: #3b82f6;
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
    .category-link {
        background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
        border-color: #374151;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    }
    
    .category-link:hover {
        background: linear-gradient(135deg, #374151 0%, #1f2937 100%);
        border-color: #4b5563;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
    }
    
    .category-icon {
        color: #9ca3af;
    }
    
    .category-link:hover .category-icon {
        color: #60a5fa;
    }
    
    .category-name {
        color: #e5e7eb;
    }
    
    .category-link:hover .category-name {
        color: #ffffff;
    }
    
    .category-count {
        background-color: rgba(156, 163, 175, 0.1);
        color: #9ca3af;
    }
    
    .category-link:hover .category-count {
        background-color: rgba(96, 165, 250, 0.15);
        color: #60a5fa;
    }
}

/* 响应式调整 */
@media (max-width: 640px) {
    .category-link {
        padding: 0.625rem 0.875rem;
    }
    
    .category-icon {
        margin-right: 0.75rem;
    }
    
    .category-name {
        font-size: 0.8125rem;
    }
    
    .category-count {
        font-size: 0.6875rem;
        padding: 0.1rem 0.4rem;
    }
}

/* 动画效果 */
@media (prefers-reduced-motion: no-preference) {
    .category-item {
        animation: categoryFadeIn 0.3s ease-out;
    }
}

@keyframes categoryFadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 面包屑导航样式 */
.breadcrumbs {
    margin-bottom: 1.5rem;
}

.breadcrumbs ul {
    display: flex;
    flex-wrap: wrap;
    list-style: none;
    margin: 0;
    padding: 0;
}

.breadcrumbs li {
    display: flex;
    align-items: center;
}

.breadcrumbs a {
    text-decoration: none;
    color: #6b7280;
}

.breadcrumbs a:hover {
    color: #3b82f6;
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
    .category-link {
        border: 2px solid #000000;
    }
    
    .category-count {
        border: 1px solid #000000;
    }
}
</style>
