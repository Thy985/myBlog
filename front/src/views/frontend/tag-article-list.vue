<template>
    <Header></Header>

    <!-- 文章列表 -->
    <div class="container mx-auto max-w-screen-xl mt-5 px-4">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
            <!-- 左边栏 -->
            <div class="lg:col-span-3">
                <!-- 标签名称 -->
                <div class="flex items-center mb-6 text-gray-800 dark:text-white">
                    <svg class="w-6 h-6 mr-2 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 19 18">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.583 5.445h.01M8.86 16.71l-6.573-6.63a.993.993 0 0 1 0-1.4l7.329-7.394A.98.98 0 0 1 10.31 1l5.734.007A1.968 1.968 0 0 1 18 2.983v5.5a.994.994 0 0 1-.316.727l-7.439 7.5a.975.975 0 0 1-1.385.001Z"/>
                    </svg>
                    <h1 class="text-2xl font-bold">{{ tagName }}</h1>
                    <span class="ml-3 text-sm text-gray-500 dark:text-gray-400">({{ articles.length }} 篇文章)</span>
                </div>

                <!-- 文章列表 -->
                <div v-if="articles && articles.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <ArticleCard 
                        v-for="(article, index) in articles" 
                        :key="index"
                        :article="article"
                        @goArticleDetail="goArticleDetail"
                        @goTagArticleListPage="goTagArticleListPage"
                        @goCategoryArticleListPage="goCategoryArticleListPage"
                    />
                </div>
                <div v-else>
                    <div class="flex flex-col items-center mb-5">
                        <svg
t="1687255143784" class="icon" viewBox="0 0 1576 1024" version="1.1"
                            xmlns="http://www.w3.org/2000/svg" p-id="1447" width="200" height="200">
                            <path
                                d="M1260.533 834.866h-134.81l9.387-15.697c6.31-11.08 9.388-23.7 9.388-37.088V168.82c0-19.698-7.849-37.858-21.237-52.016-13.389-13.388-32.318-21.237-52.016-21.237H567.093c-19.698 0-37.857 7.849-52.015 21.237-14.158 14.158-21.237 32.318-21.237 52.016v32.317h-73.253c-19.698 0-37.858 7.849-52.016 21.238-14.158 14.158-22.006 32.317-22.006 52.015v612.338c0 12.62 3.078 25.238 9.387 37.088l9.388 15.697H251.92c-5.54 0-10.31 4.77-10.31 10.31 0 2.31 0.77 5.541 3.232 7.08 1.539 1.539 4.77 3.078 7.079 3.078h533.545l54.324 54.324c5.54 5.54 14.158 9.387 22.006 9.387 7.849 0 16.62-3.078 22.007-9.387 10.31-10.311 11.85-26.008 4.77-38.627l-9.387-16.62h129.27c5.54 0 10.31-4.771 10.31-10.311s-4.77-10.311-10.31-10.311h-30.01l9.388-15.697c6.31-11.08 9.388-23.7 9.388-37.088v-32.318h263.156c5.54 0 10.31-4.77 10.31-10.31 0.154-4.156-4.616-8.157-10.156-8.157z m-283.624 52.016c0 29.086-23.7 52.785-52.785 52.785h-70.945l-73.252-74.022c-7.849-7.849-19.699-11.08-30.01-7.849l-6.309 1.54-30.778-30.78 5.54-7.078c42.628-57.556 33.856-138.657-19.699-186.056-53.554-47.245-135.579-44.167-186.055 7.079-50.477 50.476-52.786 132.347-6.31 186.825 46.475 53.555 127.73 63.096 184.363 20.468l7.08-5.54 30.778 30.778-1.54 6.31c-3.077 11.08 0 22.93 7.85 30.778l29.085 29.086H419.665c-29.086 0-52.786-23.7-52.786-52.785V273.775c0-28.317 22.93-52.016 51.247-52.016h507.537c28.316 0 51.246 23.7 51.246 52.016v613.107z m-286.086-65.404c-22.93 22.93-52.016 33.856-82.025 33.856s-59.094-11.08-82.024-33.856c-21.853-21.7-34.01-51.247-33.857-82.025 0-30.779 11.85-60.634 33.857-82.025 22.006-22.006 51.246-33.856 82.024-33.856s59.864 11.85 82.025 33.856c22.007 22.007 33.856 51.246 33.856 82.025s-11.85 60.018-33.856 82.025z m433.36-40.166c0.001 29.085-23.698 52.785-52.784 52.785h-74.022V274.544c0-19.698-7.849-37.857-21.238-52.016-13.388-13.388-32.317-21.237-52.015-21.237H514.308v-31.548c0-29.085 23.7-52.785 52.785-52.785h504.46c29.085 0 52.785 23.7 52.785 52.785v611.569zM167.436 940.436H41.397c-5.54 0-10.31 4.771-10.31 10.311 0 3.078 0.769 5.54 3.077 7.08 1.539 1.538 4.77 3.077 7.08 3.077H167.28c5.54 0 10.31-4.77 10.31-10.31s-3.846-10.158-10.156-10.158z m0 0"
                                fill="#bfbfbf" p-id="1448"></path>
                            <path
                                d="M482.76 327.33h230.993c5.54 0 10.31-4.772 10.31-10.312s-4.77-10.31-10.31-10.31H482.76c-5.54 0-10.31 4.77-10.31 10.31 0 2.309 0.77 5.54 3.078 7.08 1.692 2.462 4.77 3.231 7.232 3.231z m336.563 85.102H482.76c-5.54 0-10.31 4.77-10.31 10.31 0 3.078 0.77 5.54 3.078 7.08 1.538 1.538 4.77 3.077 7.079 3.077h336.562c5.54 0 10.311-4.77 10.311-10.31 0-5.387-4.77-10.157-10.157-10.157z m-189.288 105.57H482.607c-5.54 0-10.311 4.77-10.311 10.31 0 3.078 0.77 5.54 3.078 7.08 1.539 1.539 4.77 3.077 7.079 3.077h147.429c5.54 0 10.31-4.77 10.31-10.31s-4.616-10.157-10.157-10.157zM157.278 707.905h21.237c5.54 0 10.311 4.77 10.311 10.31s-4.77 10.312-10.31 10.312h-21.238v21.237c0 5.54-4.77 10.31-10.31 10.31-3.079 0-5.54-0.769-7.08-3.077-2.308-1.54-3.078-4.771-3.078-7.08V728.68h-21.39c-5.54 0-10.311-4.77-10.311-10.31s4.77-10.311 10.31-10.311h21.237v-21.237c0-5.54 4.771-10.311 10.311-10.311s10.311 4.77 10.311 10.31v21.084z m1387.032-85.102v-21.238c0-3.231-0.77-5.54-3.078-7.079-2.308-1.539-4.77-3.078-7.079-3.078-5.54 0-10.31 4.771-10.31 10.311v21.237h-21.238c-3.077 0-5.54 0.77-7.079 3.232-1.539 2.309-3.231 4.77-3.231 7.08 0 5.54 4.77 10.31 10.31 10.31h21.238v21.237c0 5.54 4.77 10.31 10.31 10.31s10.311-4.77 10.311-10.31V643.27h21.237c5.54 0 10.311-4.77 10.311-10.31s-4.77-10.311-10.31-10.311h-21.392zM267.62 47.553h31.548c8.618 0 15.697 7.079 15.697 15.697s-7.08 15.697-15.697 15.697h-31.548v31.548c0 8.618-7.08 15.697-15.697 15.697-4.001 0-7.849-1.54-11.08-4.77-3.078-2.31-4.771-6.31-4.771-11.081V79.1h-31.548c-4.001 0-7.849-1.54-11.08-4.771-3.078-2.308-4.77-6.31-4.77-11.08 0-8.618 7.078-15.697 15.696-15.697h31.548V16.005c0-8.618 7.08-15.697 15.697-15.697s15.697 7.079 15.697 15.697v31.548zM62.634 274.544c-22.93 0-43.397 11.85-54.324 31.548-11.08 19.698-11.08 44.167 0 63.096 11.08 19.698 32.318 31.548 54.324 31.548 34.626 0 63.096-28.316 63.096-63.096s-28.47-63.096-63.096-63.096z m27.547 79.562c-5.54 9.388-15.697 15.697-27.547 15.697-17.39 0-31.548-14.158-31.548-31.548s14.158-31.547 31.548-31.547c11.08 0 21.237 6.31 27.547 15.697 5.54 9.695 5.54 21.39 0 31.701z m1275.306-205.754c-22.93 0-43.397 11.85-54.324 31.548-11.08 19.698-11.08 44.167 0 63.096 11.08 19.698 32.318 31.548 54.324 31.548 34.626 0 63.096-28.316 63.096-63.096-0.154-34.626-27.7-63.096-63.096-63.096z m27.547 78.793c-5.54 9.388-15.697 15.697-27.547 15.697-17.39 0-31.548-14.158-31.548-31.548s14.158-31.548 31.548-31.548c11.08 0 22.007 6.31 27.547 15.697 5.54 9.696 5.54 22.315 0 31.702z m0 0"
                                fill="#bfbfbf" p-id="1449"></path>
                        </svg>
                        <p class="text-gray-500 mt-5 text-lg font-blod">此标签下还未发布博客哟~</p>
                    </div>
                </div>

                <!-- 分页 -->
                <div v-if="total > 0" class="mt-8">
                    <Pagination 
                        :current="current"
                        :total="total"
                        :size="size"
                        :pages="pages"
                        @change="getArticles"
                    />
                </div>
            </div>

            <!-- 右边栏 -->
            <div class="lg:col-span-1">
                <div class="sticky top-24">
                    <UserInfoCard></UserInfoCard>

                    <!-- 文章分类 -->
                    <div class="mt-6 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm p-4">
                        <h4 class="mb-4 font-bold text-gray-900 dark:text-white uppercase text-sm">分类</h4>
                        <div class="space-y-2">
                            <a 
                                v-for="(item, index) in categories" 
                                :key="index"
                                class="flex items-center w-full px-3 py-2 rounded-lg cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200"
                                @click="goCategoryArticleListPage(item.id, item.name)"
                            >
                                <svg class="w-4 h-4 mr-2 text-gray-600 dark:text-gray-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 21 18">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="0.9" d="M2.539 17h12.476l4-9H5m-2.461 9a1 1 0 0 1-.914-1.406L5 8m-2.461 9H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.443a1 1 0 0 1 .8.4l2.7 3.6H16a1 1 0 0 1 1 1v2H5" />
                                </svg>
                                {{ item.name }}
                            </a>
                        </div>
                    </div>
                    
                    <!-- 热门标签 -->
                    <div class="mt-6 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm p-4">
                        <h4 class="mb-4 font-bold text-gray-900 dark:text-white uppercase text-sm">热门标签</h4>
                        <div class="flex flex-wrap gap-2">
                            <span 
                                v-for="(item, index) in hotTags" 
                                :key="index"
                                class="inline-block bg-green-100 text-green-800 text-xs font-medium px-2.5 py-0.5 rounded hover:bg-green-200 hover:text-green-900 dark:bg-green-900 dark:text-green-300 dark:hover:bg-green-800 cursor-pointer transition-colors duration-200"
                                @click="goTagArticleListPage(item.id, item.name)"
                            >
                                {{ item.name }}
                            </span>
                        </div>
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
const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))
const ArticleCard = defineAsyncComponent(() => import('@/components/common/ArticleCard.vue'))
const Pagination = defineAsyncComponent(() => import('@/components/ui/Pagination.vue'))
import { defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { getTagArticles, getTags } from '@/api/frontend/tag'
import { getCategories } from '@/api/frontend/category'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const router = useRouter()
const route = useRoute()

let currentAbortController = null
let isUnmounted = false

// ✅ 支持 RESTful params 和 query 双模式
const tagId = computed(() => route.params.id || route.query.id)
const tagName = ref(route.params.name || route.query.name)
const loading = ref(false)
const error = ref('')

function safeUpdate(callback: () => void) {
    if (!isUnmounted) {
        callback()
    }
}

function scrollToTop() {
    window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 导航方法 - 使用 RESTful URL
const goArticleDetail = (articleId) => {
    router.push({ name: 'article', params: { id: String(articleId) } })
}

const goTagArticleListPage = (tagId, tagName) => {
    router.push({
        name: 'tag-articles',
        params: { id: String(tagId), name: encodeURIComponent(tagName) }
    })
}

const goCategoryArticleListPage = (categoryId, categoryName) => {
    router.push({
        name: 'category-articles',
        params: { id: String(categoryId), name: encodeURIComponent(categoryName) }
    })
}

const articles = ref([])
// 当前页码
const current = ref(1)
const total = ref(0)
const size = ref(10)
const pages = ref(0)

// 获取分页数据
async function getArticles(currentNo) {
    if (isUnmounted) {return}

    if (currentAbortController) {
        currentAbortController.abort()
    }
    currentAbortController = new AbortController()

    loading.value = true
    error.value = ''

    try {
        const res = await getTagArticles({ current: currentNo, size: size.value, tagId: tagId.value })

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
            error.value = res.message || '获取文章列表失败'
        }
    } catch (err) {
        if (err.name === 'AbortError' || isUnmounted) {return}
        error.value = '网络错误，请稍后重试'
        logger.error('获取文章列表失败:', err)
    } finally {
        if (!isUnmounted) {
            loading.value = false
        }
    }
}

// 获取分类
const categories = ref([])
const getCategoryList = () => {
    getCategories().then((e) => {
        if (e.code === 200) {
            categories.value = e.data || []
        }
    }).catch((err) => {
        logger.error('获取分类列表失败:', err)
    })
}

// 获取热门标签
const hotTags = ref([])
const getHotTags = () => {
    getTags().then((e) => {
        if (e.code === 200) {
            hotTags.value = (e.data || []).slice(0, 10) // 只显示前10个标签
        }
    }).catch((err) => {
        logger.error('获取热门标签失败:', err)
    })
}

// 初始化数据
onMounted(() => {
    loadAllData()
})

onUnmounted(() => {
    isUnmounted = true
    if (currentAbortController) {
        currentAbortController.abort()
        currentAbortController = null
    }
})

// 监听路由参数变化 - 支持 params 和 query
watch([() => route.params.id, () => route.query.id], ([newParamsId, newQueryId], [oldParamsId, oldQueryId]) => {
    const newId = newParamsId || newQueryId
    const oldId = oldParamsId || oldQueryId
    if (newId !== oldId) {
        tagName.value = route.params.name || route.query.name
        current.value = 1
        loadAllData()
        scrollToTop()
    }
})

async function loadAllData() {
    await Promise.allSettled([
        getArticles(current.value),
        getCategoryList(),
        getHotTags()
    ])
}

</script>

<style scoped>
.container {
    max-width: 1230px;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
    width: 6px;
    height: 6px;
}

::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
}

::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
}

.dark ::-webkit-scrollbar-track {
    background: #333;
}

.dark ::-webkit-scrollbar-thumb {
    background: #666;
}

.dark ::-webkit-scrollbar-thumb:hover {
    background: #888;
}
</style>
