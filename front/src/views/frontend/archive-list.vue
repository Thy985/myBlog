<template>
    <Header></Header>

    <div class="container mx-auto max-w-screen-xl mt-12 mb-12">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-8">
            <!-- 主要内容区 -->
            <div class="lg:col-span-3">
                <!-- 时间线 -->
                <div class="relative">
                    <!-- 时间左轴线 -->
                    <div class="absolute left-6 top-0 bottom-0 w-0.5 bg-gray-200 dark:bg-gray-700"></div>
                    
                    <!-- 年份分组 -->
                    <div v-for="(yearGroup, yearIndex) in groupedArchives" :key="yearIndex" class="mb-16">
                        <!-- 年份标题 -->
                        <div class="mb-8 pl-16">
                            <h2 :ref="el => yearRefs[yearGroup.year] = el" class="text-2xl font-bold text-gray-900 dark:text-white">{{ yearGroup.year }}</h2>
                        </div>
                        
                        <!-- 月份分组 -->
                        <div v-for="(monthGroup, monthIndex) in yearGroup.months" :key="monthIndex" class="relative mb-10">
                            <!-- 月份标记 -->
                            <div class="absolute left-6 transform -translate-x-1/2 w-6 h-6 bg-blue-500 rounded-full border-4 border-white dark:border-gray-800 z-10"></div>
                            
                            <!-- 月份标题 -->
                            <div class="pl-16 mb-4">
                                <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-300">{{ monthGroup.month }}</h3>
                            </div>
                            
                            <!-- 文章列表 -->
                            <div class="pl-16 space-y-4">
                                <div
v-for="(article, articleIndex) in monthGroup.articles" :key="articleIndex" 
                                    class="bg-white dark:bg-gray-800 rounded-lg shadow-sm hover:shadow-md transition-all duration-300 border border-gray-200 dark:border-gray-700 overflow-hidden transform hover:-translate-y-1">
                                    <a class="block p-5 hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors duration-200" @click="goArticleDetail(article.id)">
                                        <div class="flex flex-col md:flex-row md:items-center gap-5">
                                            <!-- 文章标题图 -->
                                            <div v-if="article.titleImage" class="w-full md:w-32 h-20 md:h-20 flex-shrink-0">
                                                <img :src="article.titleImage" :alt="article.title" class="w-full h-full object-cover rounded-md" loading="lazy" width="128" height="80" @error="(e) => e.target.style.display='none'" />
                                            </div>
                                            
                                            <!-- 文章信息 -->
                                            <div class="flex-1">
                                                <h4 class="text-lg font-medium text-gray-900 dark:text-white hover:text-blue-600 dark:hover:text-blue-400 transition-colors duration-200">{{ article.title }}</h4>
                                                <div class="flex items-center mt-3 text-xs text-gray-500 dark:text-gray-400">
                                                    <svg class="w-3 h-3 mr-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                                                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 1v3m5-3v3m5-3v3M1 7h18M5 11h10M2 3h16a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" />
                                                    </svg>
                                                    {{ article.createTime }}
                                                </div>
                                            </div>
                                        </div>
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- 分页 -->
                <nav v-if="total > 0" aria-label="Page navigation" class="mt-16">
                    <ul class="flex items-center justify-center -space-x-px h-10 text-base">
                        <li>
                            <a
v-if="current > 1" class="flex items-center justify-center px-4 h-10 ml-0 leading-tight text-gray-500 bg-white border border-gray-300 rounded-l-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white transition-colors duration-200"
                                @click="getArchiveList(current - 1)">
                                <span class="sr-only">上一页</span>
                                <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 1 1 5l4 4" />
                                </svg>
                            </a>
                            <a
v-else class="cursor-not-allowed flex items-center justify-center px-4 h-10 ml-0 leading-tight text-gray-400 bg-gray-100 border border-gray-300 rounded-l-lg dark:bg-gray-800 dark:border-gray-700 dark:text-gray-600"
                                @click="getArchiveList(current)">
                                <span class="sr-only">上一页</span>
                                <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 1 1 5l4 4" />
                                </svg>
                            </a>
                        </li>
                        
                        <li v-for="page in pageNumbers" :key="page">
                            <a
class="flex items-center justify-center px-4 h-10 leading-tight bg-white border dark:bg-gray-800 dark:border-gray-700 transition-colors duration-200"
                                :class="[page == current ? 'text-blue-600 bg-blue-50 border-blue-300 hover:bg-blue-100 hover:text-blue-700 dark:bg-blue-900/30 dark:border-blue-700 dark:text-blue-400' : 'text-gray-500 border-gray-300 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white']"
                                @click="getArchiveList(page)"
                                >
                                {{ page }}
                            </a>
                        </li>
                        
                        <li>
                            <a
v-if="current < totalPages" class="flex items-center justify-center px-4 h-10 leading-tight text-gray-500 bg-white border border-gray-300 rounded-r-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white transition-colors duration-200"
                                @click="getArchiveList(current + 1)">
                                <span class="sr-only">下一页</span>
                                <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 9 4-4-4-4" />
                                </svg>
                            </a>
                            <a
v-else class="cursor-not-allowed flex items-center justify-center px-4 h-10 leading-tight text-gray-400 bg-gray-100 border border-gray-300 rounded-r-lg dark:bg-gray-800 dark:border-gray-700 dark:text-gray-600"
                                @click="getArchiveList(current)">
                                <span class="sr-only">下一页</span>
                                <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 6 10">
                                    <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 9 4-4-4-4" />
                                </svg>
                            </a>
                        </li>
                    </ul>
                </nav>

            </div>
            
            <!-- 侧边栏 -->
            <div class="lg:col-span-1 space-y-6">
                <UserInfoCard></UserInfoCard>
                
                <!-- 归档统计 -->
                <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">归档统计</h3>
                    <div class="space-y-4">
                        <div class="flex justify-between items-center">
                            <span class="text-gray-600 dark:text-gray-400">总文章数</span>
                            <span class="text-gray-900 dark:text-white font-medium">{{ totalArticles }}</span>
                        </div>
                        <div class="flex justify-between items-center">
                            <span class="text-gray-600 dark:text-gray-400">归档年份</span>
                            <span class="text-gray-900 dark:text-white font-medium">{{ groupedArchives.length }}</span>
                        </div>
                    </div>
                </div>
                
                <!-- 年份导航 -->
                <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6">
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">年份导航</h3>
                    <div class="space-y-2">
                        <a
v-for="yearGroup in groupedArchives" :key="yearGroup.year" 
                            class="block px-4 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200"
                            @click="scrollToYear(yearGroup.year)">
                            {{ yearGroup.year }}
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <Footer></Footer>
</template>

<script setup>
import Header from '@/layouts/components/Header.vue'
import Footer from '@/layouts/components/Footer.vue'
const UserInfoCard = defineAsyncComponent(() => import('@/components/common/UserInfoCard.vue'))
import { defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { getArchives } from '@/api/frontend/archive'
import { ref, computed, onMounted } from 'vue'

import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'
const router = useRouter()

// 状态管理
const archives = ref([])
const current = ref(1)
const total = ref(0)
const size = ref(10)
const totalPages = ref(0)

// 计算属性：按年份和月份分组的归档数据
const groupedArchives = computed(() => {
    const grouped = {}
    
    // 按年份分组
    archives.value.forEach(item => {
        if (item && item.month) {
            const year = item.month.split('-')[0]
            if (!grouped[year]) {
                grouped[year] = { year, months: [] }
            }
            
            // 添加月份数据
            grouped[year].months.push({
                month: item.month,
                articles: item.articles || []
            })
        }
    })
    
    // 转换为数组并按年份降序排序
    return Object.values(grouped).sort((a, b) => b.year - a.year)
})

// 计算属性：总文章数
const totalArticles = computed(() => {
    return archives.value.reduce((sum, item) => sum + item.articles.length, 0)
})

// 计算属性：页码数组
const pageNumbers = computed(() => {
    const pages = []
    const maxPages = Math.min(totalPages.value, 5) // 最多显示5个页码
    let startPage = Math.max(1, current.value - Math.floor(maxPages / 2))
    const endPage = Math.min(totalPages.value, startPage + maxPages - 1)
    
    // 调整起始页码，确保显示足够的页码
    if (endPage - startPage + 1 < maxPages) {
        startPage = Math.max(1, endPage - maxPages + 1)
    }
    
    for (let i = startPage; i <= endPage; i++) {
        pages.push(i)
    }
    
    return pages
})

// 跳转到文章详情页
const goArticleDetail = (articleId) => {
    router.push({ path: '/article/detail', query: { articleId } })
}

// 滚动到指定年份
const yearRefs = ref({})
const scrollToYear = (year) => {
    const yearElement = yearRefs.value[year]
    if (yearElement) {
        yearElement.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
}

// 获取归档数据
const getArchiveList = async (currentPage) => {
    try {
        // 确保参数类型正确
        const params = {
            current: Number(currentPage),
            size: Number(size.value)
        }
        
        // 使用API函数获取归档数据
        const res = await getArchives(params)

        // 验证返回数据结构
        if (res && res.code === API_STATUS.SUCCESS) {
            // 验证必要字段存在
            const data = res.data || {}
            if (Array.isArray(data.list)) {
                archives.value = data.list
            } else {
                archives.value = []
            }

            if (data.page !== undefined) {
                current.value = Number(data.page)
            }

            if (data.total !== undefined) {
                total.value = Number(data.total)
            }

            if (data.size !== undefined) {
                size.value = Number(data.size)
            }

            if (data.pages !== undefined) {
                totalPages.value = Number(data.pages)
            }
        }
    } catch (error) {
        logger.error('获取归档数据失败:', error)
        // 仅在开发环境使用模拟数据
        if (import.meta.env.DEV) {
            archives.value = [{
                'month': '2026-02',
                'articles': [{
                    'id': 1,
                    'titleImage': 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=blog%20welcome%20banner%20with%20modern%20design&image_size=landscape_16_9',
                    'title': '欢迎 来到我的个人博客',
                    'createMonth': '2026-02',
                    'createTime': '2026-02-07'
                }]
            }]
            current.value = 1
            total.value = 1
            size.value = 10
            totalPages.value = 1
        }
    }
}

// 组件挂载时获取数据
onMounted(() => {
    getArchiveList(current.value)
})

</script>

<style scoped>
/* 自定义滚动条 */
::-webkit-scrollbar {
    width: 6px;
}

::-webkit-scrollbar-track {
    background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
    background: #888;
    border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
    background: #555;
}

/* 动画效果 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>
