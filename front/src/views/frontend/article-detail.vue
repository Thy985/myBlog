<template>
  <div class="article-detail-page">
    <Header></Header>

    <!-- 阅读进度条 -->
    <div class="reading-progress-bar" :style="{ width: readingProgress + '%' }"></div>

    <!-- 回到顶部按钮 -->
    <Transition name="fade">
      <button
        v-if="showBackToTop"
        class="fixed bottom-8 right-8 w-12 h-12 bg-white dark:bg-gray-800 rounded-full shadow-lg dark:shadow-gray-900/50 border border-gray-200 dark:border-gray-700 flex items-center justify-center text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 hover:text-blue-600 dark:hover:text-blue-400 transition-all duration-300 z-50"
        title="回到顶部"
        @click="scrollToTop"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
        </svg>
      </button>
    </Transition>

    <!-- 文章详情 -->
    <div class="container mx-auto max-w-screen-xl px-4 md:px-6 lg:px-8 py-8">
      <div class="flex flex-col lg:flex-row gap-6 lg:gap-8">
        <!-- 左侧边栏 - 目录导航 -->
        <div v-if="tocItems.length > 0" class="hidden lg:block w-64 flex-shrink-0">
          <div class="sticky top-24">
            <ArticleToc
              :toc-items="tocItems"
              :active-index="activeTocIndex"
              @toc-click="handleTocClick"
            />
          </div>
        </div>

        <!-- 主内容区 -->
        <div class="flex-1 min-w-0">
          <!-- 加载状态 -->
          <SkeletonLoader v-if="loading" type="article-card" :count="1" />

          <!-- 错误状态 -->
          <ErrorState
            v-else-if="error"
            type="server"
            :title="errorMessage"
            :show-retry="true"
            :show-back-home="true"
            :on-retry="() => loadArticleDetail()"
          />

          <!-- 文章内容 -->
          <div v-else class="bg-white dark:bg-gray-800 rounded-xl p-6 md:p-8 shadow-md dark:shadow-gray-900/20 border border-gray-100 dark:border-gray-700">
            <!-- 面包屑 -->
            <ArticleBreadcrumb
              :title="article.title"
              :category-id="article.categoryId"
              :category-name="article.categoryName"
              @go-home="router.push('/')"
              @go-category="goCategoryArticleListPage"
            />

            <!-- 移动端目录导航 -->
            <ArticleMobileToc
              :toc-items="tocItems"
              :visible="showMobileToc"
              :active-index="activeTocIndex"
              @toggle="showMobileToc = !showMobileToc"
              @close="showMobileToc = false"
              @select="handleMobileTocSelect"
            />

            <!-- 文章头部 -->
            <ArticleHeader
              :title="article.title"
              :update-time="article.updatedAt"
              :read-num="article.readCount"
              :category-id="article.categoryId"
              :category-name="article.categoryName"
              @go-category="goCategoryArticleListPage"
            />

            <!-- 文章内容 -->
            <div ref="articleContentRef" class="article-content-wrapper">
              <MarkdownRenderer
                :content="processedContent"
                @rendered="handleContentRendered"
              />
            </div>

            <!-- 点赞收藏 -->
            <div class="flex items-center justify-center gap-6 mt-8 pt-6 border-t border-gray-200 dark:border-gray-700">
              <button
                :class="['flex items-center gap-2 px-4 py-2 rounded-lg transition-all duration-300', isLiked ? 'bg-red-50 dark:bg-red-900/30 text-red-600 dark:text-red-400' : 'bg-gray-100 dark:bg-gray-700/50 text-gray-600 dark:text-gray-400 hover:bg-red-50 dark:hover:bg-red-900/30 hover:text-red-600']"
                :disabled="actionLoading"
                @click="toggleLike"
              >
                <svg class="w-5 h-5" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
                <span class="font-medium">{{ likeCount > 0 ? likeCount : '点赞' }}</span>
              </button>

              <button
                :class="['flex items-center gap-2 px-4 py-2 rounded-lg transition-all duration-300', isCollected ? 'bg-yellow-50 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400' : 'bg-gray-100 dark:bg-gray-700/50 text-gray-600 dark:text-gray-400 hover:bg-yellow-50 dark:hover:bg-yellow-900/30 hover:text-yellow-600']"
                :disabled="actionLoading"
                @click="toggleCollect"
              >
                <svg class="w-5 h-5" :fill="isCollected ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
                <span class="font-medium">{{ collectCount > 0 ? collectCount : '收藏' }}</span>
              </button>
            </div>
          </div>

          <!-- 上下篇 -->
          <ArticlePrevNext
            :prev-article="article.preArticleId ? { id: article.preArticleId, title: article.preArticleTitle, thumbnail: article.preArticleThumbnail } : null"
            :next-article="article.nextArticleId ? { id: article.nextArticleId, title: article.nextArticleTitle, thumbnail: article.nextArticleThumbnail } : null"
            @go-article="goArticleDetail"
          />

          <!-- 评论模块 -->
          <div class="mt-6 bg-white dark:bg-gray-800 rounded-xl p-6 md:p-8 shadow-md dark:shadow-gray-900/20 border border-gray-100 dark:border-gray-700">
            <h3 class="text-lg font-semibold mb-6 text-gray-900 dark:text-white flex items-center gap-2">
              <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              评论
            </h3>
            <ArticleComments
              ref="commentListRef"
              :article-id="Number(route.params.id)"
              @submit-comment="handleCommentSubmit"
            />
          </div>
        </div>

        <!-- 右侧边栏 -->
        <div class="hidden lg:block w-72 flex-shrink-0">
          <div class="sticky top-24 space-y-6">
            <ArticleSidebar
              :categories="categories"
              :tags="tags"
              :related-articles="relatedArticles"
              :loading-related="loadingRelated"
              @go-category="goCategoryArticleListPage"
              @go-tag="goTagArticleListPage"
              @go-article="goArticleDetail"
            />
          </div>
        </div>
      </div>
    </div>

    <Footer></Footer>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, defineAsyncComponent, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useArticleDetail } from '@/composables/useArticleDetail'
import { publishComment } from '@/api/frontend/comment'
import { likeArticle, unlikeArticle, collectArticle, uncollectArticle } from '@/api/frontend/article'
import { isAuthenticated } from '@/composables/auth'

import Header from '@/layouts/components/Header.vue'
import Footer from '@/layouts/components/Footer.vue'

const ArticleBreadcrumb = defineAsyncComponent(() => import('@/components/common/ArticleBreadcrumb.vue'))
const ArticleHeader = defineAsyncComponent(() => import('@/components/common/ArticleHeader.vue'))
const ArticleMobileToc = defineAsyncComponent(() => import('@/components/common/ArticleMobileToc.vue'))
const ArticlePrevNext = defineAsyncComponent(() => import('@/components/common/ArticlePrevNext.vue'))
const ArticleComments = defineAsyncComponent(() => import('@/components/common/ArticleComments.vue'))
const ArticleToc = defineAsyncComponent(() => import('@/components/common/ArticleToc.vue'))
const ErrorState = defineAsyncComponent(() => import('@/components/ui/ErrorState.vue'))
const SkeletonLoader = defineAsyncComponent(() => import('@/components/ui/SkeletonLoader.vue'))
const MarkdownRenderer = defineAsyncComponent(() => import('@/components/business/MarkdownRenderer.vue'))
const ArticleSidebar = defineAsyncComponent(() => import('@/components/business/ArticleDetailSidebar.vue'))

const router = useRouter()
const route = useRoute()

const {
  article,
  loading,
  error,
  errorMessage,
  tocItems,
  activeTocIndex,
  categories,
  tags,
  relatedArticles,
  loadingRelated,
  processedContent,
  loadArticleDetail,
  goArticleDetail,
  goCategoryArticleListPage,
  goTagArticleListPage,
  loadAllData
} = useArticleDetail()

const commentListRef = ref(null)
const articleContentRef = ref(null)
const showMobileToc = ref(false)
const showBackToTop = ref(false)
const isLiked = ref(false)
const isCollected = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const actionLoading = ref(false)

// Sync like/collect counts from article data
watch(article, (newArticle) => {
  if (newArticle) {
    likeCount.value = newArticle.likeCount || 0
    collectCount.value = newArticle.collectCount || 0
  }
}, { immediate: true })

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 阅读进度条 - 基于文章正文区域计算
const readingProgress = ref(0)

function updateReadingProgress() {
  const scrollTop = window.scrollY
  showBackToTop.value = scrollTop > 500

  if (!articleContentRef.value) {
    readingProgress.value = 0
    return
  }

  const contentRect = articleContentRef.value.getBoundingClientRect()
  const contentTop = contentRect.top + scrollTop
  const contentHeight = contentRect.height
  const windowHeight = window.innerHeight

  if (contentHeight <= 0) {
    readingProgress.value = 0
    return
  }

  // 计算阅读进度：从文章顶部开始，到文章底部结束
  const scrollPosition = scrollTop + windowHeight / 2
  const startPosition = contentTop
  const endPosition = contentTop + contentHeight

  if (scrollPosition < startPosition) {
    readingProgress.value = 0
  } else if (scrollPosition > endPosition) {
    readingProgress.value = 100
  } else {
    readingProgress.value = ((scrollPosition - startPosition) / (endPosition - startPosition)) * 100
  }
}

function handleScroll() {
  updateReadingProgress()
}

// Handle TOC click from desktop component
function handleTocClick({ id, index }) {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
    activeTocIndex.value = index
  }
}

// Handle TOC select from mobile component
function handleMobileTocSelect(id, index) {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
    activeTocIndex.value = index
  }
  showMobileToc.value = false
}

// Handle content rendered event
function handleContentRendered() {
  // Content rendering is handled by MarkdownRenderer
}

// Handle comment submission
async function handleCommentSubmit(content) {
  const articleId = route.query.articleId
  if (!articleId) {
    ElMessage.warning('Article ID does not exist')
    return
  }

  try {
    await publishComment({
      articleId: Number(articleId),
      content: content,
      parentId: 0
    })
    ElMessage.success('Comment submitted successfully')
    if (commentListRef.value) {
      commentListRef.value.refresh()
    }
  } catch (err) {
    ElMessage.error('Failed to submit comment, please try again later')
  }
}

// Toggle like
async function toggleLike() {
  if (!isAuthenticated()) {
    ElMessage.warning('Please login first')
    router.push('/login')
    return
  }
  if (actionLoading.value) {return}
  const articleId = route.params.id
  if (!articleId) {return}

  actionLoading.value = true
  try {
    if (isLiked.value) {
      await unlikeArticle(Number(articleId))
      isLiked.value = false
      likeCount.value--
    } else {
      await likeArticle(Number(articleId))
      isLiked.value = true
      likeCount.value++
    }
  } catch (err) {
    ElMessage.error('Operation failed, please try again later')
  } finally {
    actionLoading.value = false
  }
}

// Toggle collect
async function toggleCollect() {
  if (!isAuthenticated()) {
    ElMessage.warning('Please login first')
    router.push('/login')
    return
  }
  if (actionLoading.value) {return}
  const articleId = route.params.id
  if (!articleId) {return}

  actionLoading.value = true
  try {
    if (isCollected.value) {
      await uncollectArticle(Number(articleId))
      isCollected.value = false
      collectCount.value--
    } else {
      await collectArticle(Number(articleId))
      isCollected.value = true
      collectCount.value++
    }
  } catch (err) {
    ElMessage.error('Operation failed, please try again later')
  } finally {
    actionLoading.value = false
  }
}

// Component mount
onMounted(() => {
  loadAllData()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadAllData()
    scrollToTop()
  }
})
</script>

<style scoped>
/* 文章详情页基础样式 */
.article-detail-page {
  min-height: 100vh;
  background-color: var(--bg-primary, #f8fafc);
}

/* 阅读进度条 */
.reading-progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  z-index: 9999;
  transition: width 0.1s ease-out;
}

/* 文章内容区域 - 优化宽度为 800px */
.article-content-wrapper {
  max-width: 800px;
  margin: 0 auto;
  font-size: 16px;
  line-height: 1.8;
}

/* 标题样式 */
.title {
  line-height: 1.3;
  word-wrap: break-word;
  font-weight: 700;
  color: var(--text-primary);
}

/* 文章元信息 */
.article-meta {
  margin-bottom: 1.5rem;
}

/* 文章底部 */
.article-footer {
  border-top: 1px solid var(--border-color);
  padding-top: 1rem;
  margin-top: 2rem;
}

/* 描述文字 */
.desc {
  display: block;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

/* 指针样式 */
.cursor-pointer {
  cursor: pointer;
}

/* 无障碍访问 */
a:focus,
button:focus,
input:focus,
textarea:focus,
select:focus {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .title {
    font-size: 1.75rem;
  }

  .article-content-wrapper {
    max-width: 100%;
  }
}

/* 大屏幕优化 */
@media (min-width: 1280px) {
  .article-content-wrapper {
    max-width: 720px;
  }
}

/* 回到顶部按钮动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
