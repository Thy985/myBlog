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
                <div class="grid grid-cols-1 lg:grid-cols-4 gap-10">
                    <div class="lg:col-span-3">
                        <div class="section-header-inline mb-8">
                            <h2 class="text-2xl font-bold flex items-center gap-3">
                                <span class="w-1 h-8 bg-primary rounded-full"></span>
                                最新文章
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
                                @pageChange="(page) => articlePagination.fetchData(page)"
                                @sizeChange="(newSize) => articlePagination.changeSize(newSize)"
                            />
                        </div>
                    </div>

                    <div class="lg:col-span-1">
                        <div class="sticky top-24 space-y-8">
                            <UserInfoCard></UserInfoCard>

                            <HotArticles :articles="hotArticles" :limit="5" />

                            <div class="bg-card border border-border-color rounded-xl p-5">
                                <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
                                    <svg class="w-5 h-5 text-primary" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 21 18">
                                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="0.9" d="M2.539 17h12.476l4-9H5m-2.461 9a1 1 0 0 1-.914-1.406L5 8m-2.461 9H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.443a1 1 0 0 1 .8.4l2.7 3.6H16a1 1 0 0 1 1 1v2H5" />
                                    </svg>
                                    文章分类
                                </h3>
                                <div class="space-y-2">
                                    <a
                                        v-for="item in categories"
                                        :key="item.id"
                                        class="flex items-center justify-between block w-full px-4 py-3 rounded-lg cursor-pointer text-secondary hover:text-primary hover:bg-primary/10 transition-all duration-300"
                                        @click="goCategoryArticleListPage(item.id, item.name)"
                                    >
                                        <span class="font-medium">{{ item.name }}</span>
                                        <span class="text-xs px-2 py-1 rounded-full bg-primary/10 text-primary">{{ item.articleCount || 0 }}</span>
                                    </a>
                                    <EmptyState
                                        v-if="categories.length === 0"
                                        icon="folder"
                                        title="暂无分类"
                                        :show-action="false"
                                        :show-tip="false"
                                        :compact="true"
                                    />
                                </div>
                            </div>

                            <div class="bg-card border border-border-color rounded-xl p-5">
                                <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
                                    <svg class="w-5 h-5 text-primary" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-3l-4 4z" />
                                    </svg>
                                    热门标签
                                </h3>
                                <div class="flex flex-wrap gap-2">
                                    <span
                                        v-for="item in tags"
                                        :key="item.id"
                                        class="inline-block text-sm px-3 py-1.5 rounded-lg cursor-pointer bg-primary/10 text-primary hover:bg-primary hover:text-white transition-all duration-300"
                                        @click="goTagArticleListPage(item.id, item.name)"
                                    >
                                        {{ item.name }}
                                    </span>
                                    <EmptyState
                                        v-if="tags.length === 0"
                                        icon="star"
                                        title="暂无标签"
                                        :show-action="false"
                                        :show-tip="false"
                                        :compact="true"
                                    />
                                </div>
                            </div>
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
import { defineAsyncComponent, ref, onMounted, computed } from 'vue'

import { useMainStore } from '@/stores'
import { useRouter } from 'vue-router'
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
import { getTags } from '@/api/frontend/tag'
import { getHotArticles, getRecommendedArticles } from '@/api/modules/article'
import { request, requestWithCache } from '@/composables/api'
import { useAsyncData, usePaginationData } from '@/composables/useAsyncData'
import logger from '@/utils/logger'

const store = useMainStore()
const router = useRouter()

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
        const res = await getIndexArticles(params.current, params.size)
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
</style>
