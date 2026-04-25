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
                        标签
                    </span>
                </li>
            </ul>
        </nav>

        <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
            <!-- 左边栏 -->
            <div class="md:col-span-3">
                <div class="mb-6 bg-white border border-gray-200 rounded-lg shadow-sm dark:bg-gray-800 dark:border-gray-700 overflow-hidden">
                    <div class="p-5 border-b border-gray-200 dark:border-gray-700">
                        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">标签</h1>
                        <p class="mt-2 text-gray-600 dark:text-gray-400">发现并浏览所有文章标签</p>
                    </div>
                    
                    <!-- 加载状态 -->
                    <div v-if="loading" class="flex items-center justify-center py-12">
                        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-color"></div>
                    </div>
                    
                    <!-- 空状态 -->
                    <div v-else-if="tags.length === 0" class="p-8 text-center text-gray-500 dark:text-gray-400">
                        <svg class="mx-auto w-16 h-16 mb-4 opacity-50" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M2 10a8 8 0 018-8v8h8a8 8 0 11-16 0z"></path>
                            <path d="M12 2.252A8.014 8.014 0 0117.748 8H12V2.252z"></path>
                        </svg>
                        <h3 class="text-lg font-medium">暂无标签数据</h3>
                        <p class="mt-2">标签将在您发布文章时创建</p>
                    </div>
                    
                    <!-- 标签列表 -->
                    <div v-else class="p-5">
                        <div class="flex flex-wrap gap-3">
                            <div 
                                v-for="item in sortedTags" 
                                :key="item.id" 
                                class="tag-item"
                                :aria-label="`查看标签 ${item.name} 的文章`"
                                :tabindex="0"
                                @click="goTagArticleListPage(item.id, item.name)"
                                @keydown.enter="goTagArticleListPage(item.id, item.name)"
                                @keydown.space.prevent="goTagArticleListPage(item.id, item.name)"
                            >
                                <span class="tag-name">{{ item.name }}</span>
                                <span class="tag-count">{{ item.articleCount || 0 }}</span>
                            </div>
                        </div>
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
import { getTags } from '@/api/frontend/tag'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))

const router = useRouter()

const goTagArticleListPage = (id, name) => {
    router.push({ path: '/tag/list', query: { id, name } })
}

const tags = ref([])
const loading = ref(false)

// 按文章数量排序标签
const sortedTags = computed(() => {
    return [...tags.value].sort((a, b) => (b.count || 0) - (a.count || 0))
})

const fetchTags = async () => {
    try {
        loading.value = true
        const res = await getTags()

        if (res.code === API_STATUS.SUCCESS) {
            tags.value = res.data || []
        }
    } catch (error) {
        logger.error('获取标签列表失败:', error)
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchTags()
})
</script>

<style scoped>
/* 标签样式 */
.tag-item {
    display: inline-flex;
    align-items: center;
    padding: 0.5rem 1rem;
    background: linear-gradient(135deg, #f5f5f5 0%, #e5e5e5 100%);
    border: 1px solid #e2e8f0;
    border-radius: 9999px;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
    font-size: 0.875rem;
    font-weight: 500;
    color: #4b5563;
    position: relative;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.tag-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    background: linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%);
    border-color: #cbd5e1;
}

.tag-item:focus {
    outline: 2px solid rgba(59, 130, 246, 0.5);
    outline-offset: 2px;
}

.tag-name {
    margin-right: 0.5rem;
}

.tag-count {
    background-color: rgba(107, 114, 128, 0.1);
    color: #6b7280;
    font-size: 0.75rem;
    font-weight: 600;
    padding: 0.125rem 0.5rem;
    border-radius: 9999px;
    min-width: 20px;
    text-align: center;
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
    .tag-item {
        background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%);
        border-color: #4a5568;
        color: #e2e8f0;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    }
    
    .tag-item:hover {
        background: linear-gradient(135deg, #3a465b 0%, #2d3748 100%);
        border-color: #718096;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
    }
    
    .tag-count {
        background-color: rgba(173, 239, 209, 0.1);
        color: #a0aec0;
    }
}

/* 响应式调整 */
@media (max-width: 640px) {
    .tag-item {
        padding: 0.4rem 0.8rem;
        font-size: 0.8125rem;
    }
    
    .tag-count {
        font-size: 0.6875rem;
        padding: 0.1rem 0.4rem;
    }
}

/* 动画效果 */
@media (prefers-reduced-motion: no-preference) {
    .tag-item {
        animation: tagFadeIn 0.3s ease-out;
    }
}

@keyframes tagFadeIn {
    from {
        opacity: 0;
        transform: translateY(10px) scale(0.95);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
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
    .tag-item {
        border: 2px solid #000000;
    }
    
    .tag-count {
        border: 1px solid #000000;
    }
}
</style>
