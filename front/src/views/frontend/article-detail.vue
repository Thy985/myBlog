<template>
  <div class="article-detail-page">
    <Header></Header>

    <!-- 文章详情 -->
    <div class="container mx-auto max-w-screen-xl px-4 md:px-6 lg:px-8 py-8">
      <!-- 方案A：2-6-4 网格布局 -->
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8">
        <!-- 左侧边栏 - 目录导航 (2列) -->
        <div v-if="tocItems.length > 0" class="hidden lg:block lg:col-span-2">
          <div class="sticky top-24">
            <ArticleToc
              :toc-items="tocItems"
              :active-index="activeTocIndex"
              @toc-click="handleTocClick"
            />
          </div>
        </div>

        <!-- 主内容区 (6列) - 限制最大宽度提升阅读体验 -->
        <div class="lg:col-span-6">
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
          <div v-else class="bg-white border border-gray-200 rounded-xl p-6 md:p-8 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
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

            <!-- 文章内容 - 限制最大宽度 -->
            <div class="article-content-wrapper">
              <MarkdownRenderer
                :content="processedContent"
                @rendered="handleContentRendered"
              />
            </div>
          </div>

          <!-- 上下篇 -->
          <ArticlePrevNext
            :prev-article="article.preArticleId ? { id: article.preArticleId, title: article.preArticleTitle, thumbnail: article.preArticleThumbnail } : null"
            :next-article="article.nextArticleId ? { id: article.nextArticleId, title: article.nextArticleTitle, thumbnail: article.nextArticleThumbnail } : null"
            @go-article="goArticleDetail"
          />

          <!-- 评论模块 -->
          <ArticleComments
            ref="commentListRef"
            :article-id="Number(route.params.id)"
            @submit-comment="handleCommentSubmit"
          />
        </div>

        <!-- 右侧边栏 (4列) - 更宽更实用 -->
        <div class="lg:col-span-4">
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
import { ref, onMounted, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useArticleDetail } from '@/composables/useArticleDetail'
import { publishComment } from '@/api/frontend/comment'

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

// Real template ref for comment list
const commentListRef = ref(null)

// Mobile TOC state
const showMobileToc = ref(false)

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

// Component mount
onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
/* 文章详情页基础样式 */
.article-detail-page {
  min-height: 100vh;
  background-color: var(--bg-primary, #f8fafc);
}

/* 文章内容区域 - 限制最大宽度为640px */
.article-content-wrapper {
  max-width: 640px;
  margin: 0 auto;
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
    max-width: 600px;
  }
}
</style>
