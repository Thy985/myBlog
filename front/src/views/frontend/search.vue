<template>
    <Header></Header>

    <div class="container mx-auto max-w-screen-xl mt-5">
        <div class="grid grid-cols-4">
            <!-- 左边栏 -->
            <div class="col-span-4 px-3 md:col-span-3 sm:col-span-4">
                <!-- 搜索结果标题 -->
                <div class="bg-white border border-gray-200 p-5 rounded-lg dark:bg-gray-800 dark:border-gray-700 mb-4">
                    <h1 class="text-xl font-bold">搜索结果: {{ keyword }}</h1>
                    <p class="text-gray-500 dark:text-gray-400 mt-1">找到 {{ total }} 篇相关文章</p>
                </div>

                <!-- 加载状态 -->
                <SkeletonLoader v-if="loading" type="article-card" :count="4" />

                <!-- 无结果状态 -->
                <EmptyState
                    v-else-if="articles.length === 0"
                    icon="search"
                    title="未找到相关文章"
                    :description="`抱歉，没有找到与 &quot;${keyword}&quot; 相关的文章。请尝试其他关键词。`"
                    action-text="返回首页"
                    :show-action="true"
                    :show-tip="false"
                    @action="router.push('/')"
                />

                <!-- 搜索结果列表 -->
                <div v-else class="grid grid-cols-1 sm:grid-cols-1 md:grid-cols-2 gap-4">
                    <ArticleCard 
                        v-for="(article, index) in articles" 
                        :key="index"
                        :article="article"
                        @goArticleDetail="goArticleDetail"
                        @goTagArticleListPage="goTagArticleListPage"
                        @goCategoryArticleListPage="goCategoryArticleListPage"
                    />
                </div>

                <!-- 分页 -->
                <Pagination 
                    :current="current" 
                    :total="total" 
                    :size="size" 
                    :pages="pages"
                    @page-change="searchArticles"
                />

            </div>
            <!-- 右边栏 -->
            <div class="col-span-4 px-3 md:col-span-1 sm:col-span-4">
                <div class="sticky top-21">
                    <UserInfoCard></UserInfoCard>

                    <!-- 文章分类 -->
                    <div
                        class="mb-3 w-full font-medium p-5 bg-white border border-gray-200 rounded-lg dark:bg-gray-800 dark:border-gray-700">
                        <h2 class="mb-2 font-bold text-gray-900 uppercase dark:text-white">分类</h2>
                        <div
                            class="text-sm font-medium text-gray-900 bg-white rounded-lg dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                            <a
v-for="(item, index) in categories" :key="index"
                                class="flex items-end block w-full px-4 py-2 rounded-lg cursor-pointer hover:bg-gray-100 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-700 focus:text-blue-700 dark:border-gray-600 dark:hover:bg-gray-600 dark:hover:text-white dark:focus:ring-gray-500 dark:focus:text-blue-700"
                                @click="goCategoryArticleListPage(item.id, item.name)">
                                <svg
class="w-4 h-4 mr-2 mb-2px text-gray-800 inline dark:text-white" aria-hidden="true"
                                    xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 21 18">
                                    <path
stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                                        stroke-width="0.9"
                                        d="M2.539 17h12.476l4-9H5m-2.461 9a1 1 0 0 1-.914-1.406L5 8m-2.461 9H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.443a1 1 0 0 1 .8.4l2.7 3.6H16a1 1 0 0 1 1 1v2H5" />
                                </svg>
                                {{ item.name }}
                            </a>
                        </div>

                    </div>

                    <!-- 文章标签 -->
                    <div
                        class="mb-3 w-full font-medium p-5 bg-white border border-gray-200 rounded-lg dark:bg-gray-800 dark:border-gray-700">
                        <h2 class="mb-2 font-bold text-gray-900 uppercase dark:text-white">标签</h2>
                        <div
v-for="(item, index) in tags" :key="index" class="inline-block bg-green-100 text-green-800 text-xs font-medium mr-2 mb-1 px-2.5 py-0.5 rounded hover:bg-green-200 hover:text-green-900 dark:hover:bg-green-800 dark:hover:text-green-300 dark:bg-green-900 dark:text-green-300"
                            @click="goTagArticleListPage(item.id, item.name)">
                            {{ item.name }}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <Footer></Footer>
</template>

<script setup>
import { defineAsyncComponent, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Header from '@/layouts/components/Header.vue'
import Footer from '@/layouts/components/Footer.vue'
// 组件懒加载
const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))
const ArticleCard = defineAsyncComponent(() => import('@/components/common/ArticleCard.vue'))
const Pagination = defineAsyncComponent(() => import('@/components/ui/Pagination.vue'))
const SkeletonLoader = defineAsyncComponent(() => import('@/components/ui/SkeletonLoader.vue'))
const EmptyState = defineAsyncComponent(() => import('@/components/ui/EmptyState.vue'))
import { searchArticles as searchArticlesApi } from '@/api/frontend/index'
import { getCategories } from '@/api/frontend/category'
import { getTags } from '@/api/frontend/tag'
import { request } from '@/composables/api'

import logger from '@/utils/logger'
// 路由实例
const router = useRouter()
const route = useRoute()

// 搜索关键词
const keyword = ref(route.query.keyword || '')

// 文章列表数据
const articles = ref([])
// 当前页码
const current = ref(1)
// 总文章数
const total = ref(0)
// 每页显示数量
const size = ref(10)
// 总页数
const pages = ref(0)
// 加载状态
const loading = ref(false)

/**
 * 搜索文章
 * @param {number} currentNo - 当前页码
 * @returns {Promise<void>}
 */
async function searchArticles(currentNo) {
    if (!currentNo || currentNo < 1) {
        currentNo = 1
    }
    if (!keyword.value) {
        return
    }
    // 开始加载
    loading.value = true
    try {
        const res = await request(
            () => searchArticlesApi(keyword.value, currentNo, size.value),
            null,
            { showError: true }
        )
        if (res) {
            articles.value = res.data || []
            current.value = res.current || 1
            total.value = res.total || 0
            size.value = res.size || 10
            pages.value = res.pages || 0
        }
    } catch (err) {
        logger.error('搜索文章出错:', err)
        articles.value = []
    } finally {
        // 结束加载
        loading.value = false
    }
}

/**
 * 跳转到文章详情页
 * @param {number} articleId - 文章ID
 */
const goArticleDetail = (articleId) => {
    if (!articleId) {
        return
    }
    router.push({ path: '/article/detail', query: { articleId: articleId } })
}

/**
 * 跳转到分类文章列表页
 * @param {number} id - 分类ID
 * @param {string} name - 分类名称
 */
const goCategoryArticleListPage = (id, name) => {
    router.push({ path: '/category/list', query: { id: id, name: name } })
}

/**
 * 跳转到标签文章列表页
 * @param {number} id - 标签ID
 * @param {string} name - 标签名称
 */
const goTagArticleListPage = (id, name) => {
    router.push({ path: '/tag/list', query: { id: id, name: name } })
}

// 分类列表数据
const categories = ref([])

/**
 * 获取分类列表数据
 * @returns {Promise<void>}
 */
async function getCategoriesData() {
    try {
        const res = await request(getCategories, {}, {
            showError: false
        })
        if (res && res.data) {
            categories.value = res.data
        }
    } catch (err) {
        logger.error('获取分类出错:', err)
        categories.value = []
    }
}

// 标签列表数据
const tags = ref([])

/**
 * 获取标签列表数据
 * @returns {Promise<void>}
 */
async function getTagsData() {
    try {
        const res = await request(getTags, {}, {
            showError: false
        })
        if (res && res.data) {
            tags.value = res.data
        }
    } catch (err) {
        logger.error('获取标签出错:', err)
        tags.value = []
    }
}

// 初始化数据
onMounted(() => {
    if (keyword.value) {
        searchArticles(1)
    }
    Promise.all([
        getCategoriesData(),
        getTagsData()
    ])
})

</script>

<style>
.container {
    max-width: 1230px;
}

.article-img {
    height: 100%;
}

.two-line-clamp {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
}

.el-menu--horizontal .el-menu-item:not(.is-disabled):focus,
.el-menu--horizontal .el-menu-item:not(.is-disabled):hover {
    outline: 0;
    color: var(--el-menu-text-color);
    ;
    background-color: #fff;
    border-bottom: 2px solid #409eff;
    ;
}

.category-item:hover {
    text-decoration: underline;
    cursor: pointer;
}

.tag-item:hover {
    cursor: pointer;
}

.el-tag:hover {
    background-color: var(--el-color-info-light-8);
}

.cursor-pointer {
    cursor: pointer;
}
</style>
