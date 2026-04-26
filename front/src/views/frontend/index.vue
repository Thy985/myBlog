<template>
    <div class="index-page min-h-screen">
        <Header></Header>

        <main>
            <HeroSection
                v-if="!loading || articles.length > 0"
                :stats="heroStats"
                @browse-articles="scrollToArticles"
                @learn-more="$router.push('/about')"
            />

            <FeaturedArticles
                :articles="featuredArticles"
                :loading="loadingFeatured"
                @article-click="goArticleDetail"
            />

            <div class="container mx-auto max-w-screen-xl px-4 py-16">
                <div class="flex flex-col lg:flex-row gap-10">
                    <div class="flex-1 min-w-0">
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

                        <div class="section-header-inline mb-8">
                            <h2 class="text-2xl font-bold flex items-center gap-3">
                                <span class="w-1 h-8 bg-primary rounded-full"></span>
                                {{ selectedCategoryId ? (categories.find(c => c.id === selectedCategoryId)?.name || '文章列表') : '发现文章' }}
                            </h2>
                            <div class="h-1 w-24 bg-primary rounded-full mt-2"></div>
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

                    <div class="hidden lg:block w-80 flex-shrink-0">
                        <div class="sticky top-24 space-y-8">
                            <UserInfoCard></UserInfoCard>

                            <HotArticles :articles="hotArticles" :limit="5" />
                        </div>
                    </div>
                </div>
            </div>

            <NewsletterSection @subscribe="handleSubscribe" />
        </main>

        <Footer></Footer>
    </div>
</template>

<script setup>
import { defineAsyncComponent, ref, watch, onMounted, computed } from 'vue'

import { useMainStore, useAuthStore } from '@/stores'
import { useRouter, useRoute } from 'vue-router'
import Header from '@/layouts/components/Header.vue'
import Footer from '@/layouts/components/Footer.vue'
import HeroSection from '@/components/frontend/HeroSection.vue'
import FeaturedArticles from '@/components/frontend/FeaturedArticles.vue'
import NewsletterSection from '@/components/frontend/NewsletterSection.vue'
import { ElMessage } from 'element-plus'

const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))
const ArticleCard = defineAsyncComponent(() => import('@/components/common/ArticleCard.vue'))
const Pagination = defineAsyncComponent(() => import('@/components/ui/Pagination.vue'))
const HotArticles = defineAsyncComponent(() => import('@/components/common/HotArticles.vue'))
const EmptyState = defineAsyncComponent(() => import('@/components/ui/EmptyState.vue'))
const SkeletonLoader = defineAsyncComponent(() => import('@/components/ui/SkeletonLoader.vue'))

import { getIndexArticles } from '@/api/frontend/index'
import { getCategories } from '@/api/frontend/category'
import { getHotArticles, getRecommendedArticles } from '@/api/frontend/article'
import { request, requestWithCache } from '@/composables/api'
import { useAsyncData, usePaginationData } from '@/composables/useAsyncData'
import logger from '@/utils/logger'

const store = useMainStore()
const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

// Developer ID (hardcoded as 1)
const DEVELOPER_ID = 1

// Filter state
const selectedCategoryId = ref(null)
const sortBy = ref('createdTime')

// Author filter: show developer's articles if not logged in, user's articles if logged in
const authorId = computed(() => {
    if (authStore.user?.id) {
        return authStore.user.id
    }
    return DEVELOPER_ID
})

const filterParams = computed(() => ({
  ...(selectedCategoryId.value && { categoryId: selectedCategoryId.value }),
  ...(sortBy.value !== 'createdTime' && { sortBy: sortBy.value }),
  authorId: authorId.value
}))

// Clear all filters
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

const goArticleDetail = (articleId) => {
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

const articlePagination = usePaginationData(
    async (params) => {
        const res = await getIndexArticles({ current: params.current, size: params.size, ...filterParams.value })
        return res.data || { list: [], total: 0, pages: 0 }
    },
    {
        defaultPageSize: 10,
        cacheKey: 'index-articles',
        cacheTTL: 2 * 60 * 1000
    }
)

const articles = computed(() => articlePagination.data.value?.list || [])
const loading = computed(() => articlePagination.loading.value)
const current = computed(() => articlePagination.currentPage.value)
const total = computed(() => articlePagination.total.value)
const size = computed(() => articlePagination.pageSize.value)
const pages = computed(() => articlePagination.pages.value)

const heroStats = computed(() => ({
    articleCount: total.value || 128,
    totalViews: 50000,
    subscriberCount: 2800
}))

const featuredData = useAsyncData(
    async () => {
        const res = await request(getRecommendedArticles, { limit: 6 }, { showError: false })
        return res.data || []
    },
    {
        cacheKey: 'featured-articles',
        cacheTTL: 5 * 60 * 1000
    }
)
const featuredArticles = computed(() => featuredData.data.value || [])
const loadingFeatured = computed(() => featuredData.loading.value)

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

const scrollToArticles = () => {
    const element = document.querySelector('.section-header-inline')
    if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
    }
}

const handleSubscribe = (email) => {
    ElMessage.success(`订阅成功！我们会将更新发送到 ${email}`)
}

async function initData() {
  try {
    const results = await Promise.allSettled([
      featuredData.execute(),
      articlePagination.fetchData(1),
      hotArticlesData.execute(),
      getCategoriesData()
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

const updateMetaTags = (title, description, url) => {
    document.title = title
    const metaMap = {
        'description': description,
        'keywords': '技术博客,前端,后端,全栈,编程,学习',
        'og:title': title,
        'og:description': description,
        'og:type': 'website',
        'og:url': url,
        'twitter:card': 'summary_large_image',
        'twitter:title': title,
        'twitter:description': description
    }
    Object.entries(metaMap).forEach(([name, content]) => {
        let meta = document.querySelector(`meta[name="${name}"], meta[property="${name}"]`)
        if (!meta) {
            meta = document.createElement('meta')
            if (name.startsWith('og:') || name.startsWith('twitter:')) {
                meta.setAttribute('property', name)
            } else {
                meta.setAttribute('name', name)
            }
            document.head.appendChild(meta)
        }
        meta.setAttribute('content', content)
    })
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

    const blogName = store.setting?.blogName || 'XingChen Blog'
    const blogDesc = store.setting?.description || '记录学习，分享成长'
    updateMetaTags(blogName, blogDesc, window.location.origin)
})

const goCategoryArticleListPage = (id, name) => {
    router.push({
        name: 'category-articles',
        params: { id: String(id), name: encodeURIComponent(name) }
    })
}

const goTagArticleListPage = (id, name) => {
    router.push({
        name: 'tag-articles',
        params: { id: String(id), name: encodeURIComponent(name) }
    })
}
</script>

<style scoped>
.index-page {
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
