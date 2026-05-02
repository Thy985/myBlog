<template>
    <div class="discover-page min-h-screen">
        <Header></Header>

        <main class="container mx-auto max-w-screen-xl px-4 py-8">
            <div class="flex flex-col lg:flex-row gap-10">
                <div class="flex-1 min-w-0">
                    <!-- Page Title -->
                    <div class="section-header-inline mb-8">
                        <h2 class="text-2xl font-bold flex items-center gap-3">
                            <span class="w-1 h-8 bg-primary rounded-full"></span>
                            发现社区
                        </h2>
                        <div class="h-1 w-24 bg-primary rounded-full mt-2"></div>
                    </div>

                    <!-- Filter Bar -->
                    <div class="filter-bar mb-6 flex flex-wrap items-center gap-4 p-4 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700">
                        <!-- Sort Dropdown -->
                        <div class="flex items-center gap-2">
                            <span class="text-sm text-gray-500 dark:text-gray-400">排序:</span>
                            <el-select v-model="sortBy" size="default" style="width: 120px">
                                <el-option label="最新文章" value="createdTime" />
                                <el-option label="最多浏览" value="readNum" />
                                <el-option label="最多点赞" value="likeNum" />
                            </el-select>
                        </div>

                        <!-- Category Filter Chips -->
                        <div class="flex items-center gap-2 flex-wrap">
                            <span class="text-sm text-gray-500 dark:text-gray-400">分类:</span>
                            <button
                                class="filter-chip"
                                :class="{ 'active': selectedCategoryId === null }"
                                @click="selectedCategoryId = null"
                            >
                                全部
                            </button>
                            <button
                                v-for="cat in categories"
                                :key="cat.id"
                                class="filter-chip"
                                :class="{ 'active': selectedCategoryId === cat.id }"
                                @click="selectedCategoryId = cat.id"
                            >
                                {{ cat.name }}
                            </button>
                        </div>

                        <!-- Clear Filters -->
                        <button
                            v-if="selectedCategoryId || sortBy !== 'createdTime'"
                            class="text-sm text-primary hover:underline ml-auto"
                            @click="clearFilters"
                        >
                            清除筛选
                        </button>
                    </div>

                    <SkeletonLoader v-if="loading" type="article-card" :count="4" />
                    <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-8">
                        <ArticleCard
                            v-for="article in articles"
                            :key="article.id"
                            :article="article"
                            @goArticleDetail="goArticleDetail"
                            @goTagArticleListPage="goTagArticleListPage"
                            @goCategoryArticleListPage="goCategoryArticleListPage"
                        />
                        <EmptyState
                            v-if="articles.length === 0"
                            icon="document"
                            title="暂无文章数据"
                            description="暂时没有文章内容，敬请期待~"
                            :show-action="false"
                            :show-tip="false"
                            :compact="true"
                        />
                    </div>

                    <div v-if="articles.length > 0" class="mt-10">
                        <Pagination
                            :current="current"
                            :total="total"
                            :size="size"
                            :pages="pages"
                            @page-change="(page) => articlePagination.fetchData(page, articlePagination.pageSize.value, filterParams.value)"
                            @size-change="(newSize) => articlePagination.fetchData(1, newSize, filterParams.value)"
                        />
                    </div>
                </div>

                <!-- Sidebar -->
                <div class="hidden lg:block w-80 flex-shrink-0">
                    <div class="sticky top-24 space-y-8">
                        <div class="bg-white dark:bg-gray-800 rounded-xl p-5 border border-gray-200 dark:border-gray-700">
                            <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
                                <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                </svg>
                                社区成员
                            </h3>
                            <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
                                发现更多作者，探索精彩内容
                            </p>
                            <button
                                class="w-full py-2 px-4 bg-primary text-white rounded-lg hover:bg-primary-hover transition-colors"
                                @click="$router.push('/register')"
                            >
                                加入社区
                            </button>
                        </div>

                        <HotArticles :articles="hotArticles" :limit="5" />

                        <div class="bg-white dark:bg-gray-800 rounded-xl p-5 border border-gray-200 dark:border-gray-700">
                            <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
                                <svg class="w-5 h-5 text-primary" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="0.9" d="M2.539 17h12.476l4-9H5m-2.461 9a1 1 0 0 1-.914-1.406L5 8m-2.461 9H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.443a1 1 0 0 1 .8.4l2.7 3.6H16a1 1 0 0 1 1 1v2H5" />
                                </svg>
                                文章分类
                            </h3>
                            <div class="space-y-2">
                                <a
                                    v-for="item in categories"
                                    :key="item.id"
                                    class="flex items-center justify-between block w-full px-4 py-3 rounded-lg cursor-pointer text-secondary hover:text-primary hover:bg-primary/10 transition-all duration-300"
                                    @click="$router.push({ name: 'category-articles', params: { id: String(item.id), name: item.name } })"
                                >
                                    <span class="font-medium">{{ item.name }}</span>
                                    <span class="text-xs px-2 py-1 rounded-full bg-primary/10 text-primary">{{ item.articleCount || 0 }}</span>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>

        <Footer></Footer>
    </div>
</template>

<script setup>
import { defineAsyncComponent, ref, watch, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Header from '@/layouts/components/Header.vue'
import Footer from '@/layouts/components/Footer.vue'
import { ElMessage } from 'element-plus'

const ArticleCard = defineAsyncComponent(() => import('@/components/common/ArticleCard.vue'))
const Pagination = defineAsyncComponent(() => import('@/components/ui/Pagination.vue'))
const HotArticles = defineAsyncComponent(() => import('@/components/common/HotArticles.vue'))
const EmptyState = defineAsyncComponent(() => import('@/components/ui/EmptyState.vue'))
const SkeletonLoader = defineAsyncComponent(() => import('@/components/ui/SkeletonLoader.vue'))

import { getCategories } from '@/api/frontend/category'
import { getTags } from '@/api/frontend/tag'
import { getHotArticles } from '@/api/frontend/article'
import { getArticles } from '@/api/frontend/article'
import { request, requestWithCache } from '@/composables/api'
import { useAsyncData, usePaginationData } from '@/composables/useAsyncData'
import logger from '@/utils/logger'

const router = useRouter()
const route = useRoute()

// Filter state
const selectedCategoryId = ref(null)
const sortBy = ref('createdTime')

const filterParams = computed(() => ({
  ...(selectedCategoryId.value && { categoryId: selectedCategoryId.value }),
  ...(sortBy.value !== 'createdTime' && { sortBy: sortBy.value })
}))

// Article pagination - discover page shows ALL articles (no author filter)
const articlePagination = usePaginationData(
    async (params) => {
        const res = await getArticles({ current: params.current, size: params.size, ...filterParams.value })
        return res.data || { list: [], total: 0, pages: 0 }
    },
    {
        defaultPageSize: 10,
        cacheKey: 'discover-articles',
        cacheTTL: 2 * 60 * 1000
    }
)

const articles = computed(() => articlePagination.data.value?.list || [])
const loading = computed(() => articlePagination.loading.value)
const current = computed(() => articlePagination.currentPage.value)
const total = computed(() => articlePagination.total.value)
const size = computed(() => articlePagination.pageSize.value)
const pages = computed(() => articlePagination.pages.value)

const hotArticlesData = useAsyncData(
    async () => {
        const res = await request(getHotArticles, { limit: 5 }, { showError: false })
        return res.data || []
    },
    {
        cacheKey: 'hot-articles',
        cacheTTL: 5 * 60 * 1000
    }
)
const hotArticles = computed(() => hotArticlesData.data.value || [])

const categories = ref([])
const tags = ref([])

async function getCategoriesData() {
    try {
        const res = await requestWithCache(getCategories, 'categories', {}, { showError: false })
        if (res?.data) {
            categories.value = res.data
        }
    } catch (err) {
        logger.error('Fetch categories failed:', err.message)
    }
}

async function getTagsData() {
    try {
        const res = await requestWithCache(getTags, 'tags', {}, { showError: false })
        if (res?.data) {
            tags.value = res.data
        }
    } catch (err) {
        logger.error('Fetch tags failed:', err.message)
    }
}

function clearFilters() {
    selectedCategoryId.value = null
    sortBy.value = 'createdTime'
}

// Watch filter changes and update URL + refetch
watch([selectedCategoryId, sortBy], () => {
    router.replace({
        query: {
            ...(selectedCategoryId.value && { categoryId: String(selectedCategoryId.value) }),
            ...(sortBy.value !== 'createdTime' && { sortBy: sortBy.value })
        }
    })
    articlePagination.fetchData(1, articlePagination.pageSize.value, filterParams.value)
})

function goArticleDetail(articleId) {
    if (!articleId) {
        ElMessage.warning('文章ID不存在')
        return
    }
    try {
        router.push({ name: 'article', params: { id: String(articleId) } })
    } catch (err) {
        logger.error('Navigate to article failed:', err.message)
        ElMessage.error('跳转详情页失败，请稍后重试')
    }
}

function goCategoryArticleListPage(id, name) {
    router.push({
        name: 'category-articles',
        params: { id: String(id), name: encodeURIComponent(name) }
    })
}

function goTagArticleListPage(id, name) {
    router.push({
        name: 'tag-articles',
        params: { id: String(id), name: encodeURIComponent(name) }
    })
}

async function initData() {
  try {
    const results = await Promise.allSettled([
      articlePagination.fetchData(1),
      hotArticlesData.execute(),
      getCategoriesData(),
      getTagsData()
    ])

    const failedResults = results.filter(r => r.status === 'rejected')
    if (failedResults.length > 0) {
      logger.error('部分数据加载失败:', failedResults.map(r => r.reason?.message))
      ElMessage.warning('部分数据加载失败，请刷新页面重试')
    }
  } catch (err) {
    logger.error('Init data failed:', err.message)
    ElMessage.error('页面加载失败，请刷新页面重试')
  }
}

onMounted(() => {
    // Initialize filters from URL query params
    const query = route.query
    if (query.categoryId) {
        selectedCategoryId.value = Number(query.categoryId)
    }
    if (query.sortBy) {
        if (query.sortBy === 'createdTime' || query.sortBy === 'readNum' || query.sortBy === 'likeNum') {
            sortBy.value = query.sortBy
        }
    }

    initData()
})
</script>

<style scoped>
.discover-page {
    background: var(--bg-primary);
}

.filter-chip {
    padding: 0.375rem 0.75rem;
    font-size: 0.875rem;
    border-radius: 0.5rem;
    transition: all 0.2s;
    background: var(--bg-secondary);
    color: var(--text-secondary);
    cursor: pointer;
    border: none;
}

.filter-chip:hover {
    background: var(--primary-light);
    color: var(--primary);
}

.filter-chip.active {
    background: var(--primary);
    color: white;
}
</style>
