<template>
    <Header></Header>

    <!-- 文章列表 -->
    <div class="container mx-auto max-w-screen-xl mt-5 px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
            <!-- 左边栏 - 文章列表 -->
            <div class="lg:col-span-3">
                <!-- 分类标题 -->
                <div class="flex items-center mb-6 text-2xl font-bold text-gray-900 dark:text-white">
                    <svg
class="w-6 h-6 mr-2 text-primary-color" aria-hidden="true"
                                    xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 21 18">
                                    <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                                        stroke-width="0.9"
                                        d="M2.539 17h12.476l4-9H5m-2.461 9a1 1 0 0 1-.914-1.406L5 8m-2.461 9H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.443a1 1 0 0 1 .8.4l2.7 3.6H16a1 1 0 0 1 1 1v2H5" />
                                </svg>
                    {{ categoryName }}
                </div>
                
                <!-- 加载状态 -->
                <div v-if="loading" class="flex items-center justify-center py-16">
                    <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-color"></div>
                </div>
                
                <!-- 文章列表 -->
                <div v-else-if="articles && articles.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <ArticleCard 
                        v-for="article in articles" 
                        :key="article.id"
                        :article="article"
                        @go-article-detail="goArticleDetail"
                        @go-tag-article-list-page="goTagArticleListPage"
                        @go-category-article-list-page="goCategoryArticleListPage"
                    />
                </div>
                
                <!-- 无文章提示 - 使用 EmptyState 组件 -->
                <EmptyState
                    v-else
                    icon="folder"
                    title="此分类下还未发布博客哟~"
                    description="您可以尝试查看其他分类，或者稍后再来看看是否有新的内容更新。"
                    action-text="返回首页"
                    tip="博客正在建设中，更多文章即将上线"
                    @action="goBack"
                />
                
                <!-- 分页 -->
                <nav v-if="total > 0" aria-label="Page navigation" class="mt-10">
                    <ul class="flex items-center justify-center space-x-1">
                        <li>
                            <a
v-if="current > 1" class="flex items-center justify-center px-3 h-10 leading-tight text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 transition-all duration-200"
                                @click="fetchArticles(current - 1)">
                                <span class="sr-only">Previous</span>
                                <svg class="w-4 h-4" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                                        stroke-width="2" d="M5 1 1 5l4 4" />
                                </svg>
                            </a>
                            <a
v-else
                                class="cursor-not-allowed flex items-center justify-center px-3 h-10 leading-tight text-gray-400 bg-gray-100 border border-gray-300 rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-500">
                                <span class="sr-only">Previous</span>
                                <svg class="w-4 h-4" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                                        stroke-width="2" d="M5 1 1 5l4 4" />
                                </svg>
                            </a>
                        </li>
                        <li v-for="page in displayPages" :key="page">
                            <a
class="flex items-center justify-center px-4 h-10 leading-tight bg-white border dark:bg-gray-800 dark:border-gray-700 transition-all duration-200"
                                :class="[page == current ? 'text-white bg-primary-color border-primary-color hover:bg-primary-hover' : 'text-gray-500 border-gray-300 hover:bg-gray-50 dark:text-gray-400 dark:hover:bg-gray-700']"
                                @click="fetchArticles(page)"
                                >
                                {{ page }}
                            </a>
                        </li>
                        <li>
                            <a
v-if="current < pages" class="flex items-center justify-center px-3 h-10 leading-tight text-gray-500 bg-white border border-gray-300 rounded-md hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 transition-all duration-200"
                                @click="fetchArticles(current + 1)">
                                <span class="sr-only">Next</span>
                                <svg class="w-4 h-4" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                                        stroke-width="2" d="m1 9 4-4-4-4" />
                                </svg>
                            </a>
                            <a
v-else
                                class="cursor-not-allowed flex items-center justify-center px-3 h-10 leading-tight text-gray-400 bg-gray-100 border border-gray-300 rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-500">
                                <span class="sr-only">Next</span>
                                <svg class="w-4 h-4" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                                        stroke-width="2" d="m1 9 4-4-4-4" />
                                </svg>
                            </a>
                        </li>
                    </ul>
                </nav>
            </div>
            
            <!-- 右边栏 - 侧边栏 -->
            <div class="lg:col-span-1">
                <div class="sticky top-24 space-y-6">
                    <!-- 用户信息卡片 -->
                    <UserInfoCard></UserInfoCard>

                    <!-- 文章标签 -->
                    <div class="card p-5">
                        <h4 class="mb-4 font-bold text-gray-900 uppercase dark:text-white">热门标签</h4>
                        <div class="flex flex-wrap gap-2">
                            <div
v-for="item in tags" :key="item.id" class="tag tag-primary text-xs font-medium px-3 py-1 rounded-full hover:shadow-sm transition-all duration-200"
                                @click="goTagArticleListPage(item.id, item.name)">
                                {{ item.name }}
                            </div>
                        </div>
                    </div>

                    <!-- 分类导航 -->
                    <div class="card p-5">
                        <h4 class="mb-4 font-bold text-gray-900 uppercase dark:text-white">所有分类</h4>
                        <ul class="space-y-2">
                            <li v-for="category in categories" :key="category.id">
                                <a
class="flex items-center justify-between p-2 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200"
                                    @click="goCategoryArticleListPage(category.id, category.name)">
                                    <span class="text-gray-700 dark:text-gray-300">{{ category.name }}</span>
                                    <span class="text-xs text-gray-500 dark:text-gray-400">{{ category.articleCount || 0 }}</span>
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <Footer></Footer>
</template>

<script setup lang="ts">
import Header from '@/layouts/components/Header.vue'
import Footer from '@/layouts/components/Footer.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ArticleCard from '@/components/common/ArticleCard.vue'
const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))
import { defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { getCategoryArticles, getCategories } from '@/api/frontend/category'
import { getTags } from '@/api/frontend/tag'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const router = useRouter()
const route = useRoute()

let currentAbortController = null
let isUnmounted = false

// 响应式数据
// ✅ 支持 RESTful params 和 query 双模式
const categoryId = computed(() => route.params.id || route.query.id)
const categoryName = ref(route.params.name || route.query.name || '分类')
const categories = ref([])
const articles = ref([])
const tags = ref([])
const current = ref(1)
const total = ref(0)
const size = ref(10)
const pages = ref(0)
const loading = ref(false)
const error = ref(false)

function safeUpdate(callback: () => void) {
    if (!isUnmounted) {
        callback()
    }
}

function scrollToTop() {
    window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function loadAllData() {
    const id = categoryId.value
    if (!id) {
        safeUpdate(() => {
            error.value = true
        })
        logger.error('分类ID不存在，无法获取文章列表')
        return
    }

    safeUpdate(() => {
        error.value = false
    })

    await Promise.allSettled([
        fetchArticles(current.value),
        fetchTags(),
        fetchCategories()
    ])
}
const displayPages = computed(() => {
    const pageArray = []
    for (let i = 1; i <= pages.value; i++) {
        pageArray.push(i)
    }
    return pageArray
})

// 导航方法 - 使用 RESTful URL
const goTagArticleListPage = (tagId, name) => {
    logger.debug('跳转至标签文章列表', { tagId, name })
    router.push({
        name: 'tag-articles',
        params: { id: String(tagId), name: encodeURIComponent(name) }
    })
}

const goArticleDetail = (articleId) => {
    router.push({ name: 'article', params: { id: String(articleId) } })
}

const goCategoryArticleListPage = (categoryId, categoryName) => {
    router.push({
        name: 'category-articles',
        params: { id: String(categoryId), name: encodeURIComponent(categoryName) }
    })
}

const goBack = () => {
    router.push('/')
}

// API调用方法
const fetchArticles = async (currentNo) => {
    if (isUnmounted) {return}

    if (currentAbortController) {
        currentAbortController.abort()
    }
    currentAbortController = new AbortController()

    try {
        loading.value = true
        const id = categoryId.value

        const params = {
            current: Number(currentNo),
            size: Number(size.value),
            categoryId: Number(id)
        }

        const res = await getCategoryArticles(params)

        if (isUnmounted) {return}

        if (res.code === API_STATUS.SUCCESS) {
            const data = res.data || {}
            safeUpdate(() => {
                articles.value = data.list || []
                current.value = data.page || 1
                total.value = data.total || 0
                size.value = data.size || 10
                pages.value = data.pages || 0
            })
        } else {
            logger.error('获取文章列表失败:', res.message)
        }
    } catch (error) {
        if (error.name === 'AbortError' || isUnmounted) {return}
        logger.error('获取文章列表失败:', error)
    } finally {
        if (!isUnmounted) {
            loading.value = false
        }
    }
}

const fetchTags = async () => {
    try {
        const res = await getTags()
        if (res.code === API_STATUS.SUCCESS) {
            tags.value = res.data || []
        }
    } catch (error) {
        logger.error('获取标签失败:', error)
    }
}

const fetchCategories = async () => {
    try {
        const res = await getCategories()
        if (res.code === API_STATUS.SUCCESS) {
            categories.value = res.data || []
        }
    } catch (error) {
        logger.error('获取分类失败:', error)
    }
}

onMounted(() => {
    logger.debug('加载分类页面数据', { params: route.params, query: route.query })
    loadAllData()
})

onUnmounted(() => {
    isUnmounted = true
    if (currentAbortController) {
        currentAbortController.abort()
        currentAbortController = null
    }
})

watch([() => route.params.id, () => route.query.id], ([newParamsId, newQueryId], [oldParamsId, oldQueryId]) => {
    const newId = newParamsId || newQueryId
    const oldId = oldParamsId || oldQueryId
    if (newId && newId !== oldId) {
        categoryName.value = route.params.name || route.query.name || '分类'
        current.value = 1
        loadAllData()
        scrollToTop()
    }
})
</script>

<style scoped>
/* 自定义样式 */
.text-primary-color {
    color: var(--primary-color);
}

.bg-primary-color {
    background-color: var(--primary-color);
}

.border-primary-color {
    border-color: var(--primary-color);
}

.bg-primary-hover {
    background-color: var(--primary-hover);
}
</style>
