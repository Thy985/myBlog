<template>
  <div class="article-detail-page">
    <Header></Header>

    <!-- 文章详情 -->
    <div class="container mx-auto max-w-screen-xl px-4 md:px-6 lg:px-8 py-8">
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <!-- 左侧边栏 - 目录导航 -->
        <div v-if="tocItems.length > 0" class="lg:col-span-2 hidden lg:block">
          <ArticleToc
            :toc-items="tocItems"
            :active-index="activeTocIndex"
            @toc-click="handleTocClick"
          />
        </div>

        <!-- 主内容区 -->
        <div class="lg:col-span-8 md:col-span-12">
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
          <div v-else class="bg-white border border-gray-200 rounded-lg p-6 md:p-8 dark:bg-gray-800 dark:border-gray-700 shadow-sm">
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
            <MarkdownRenderer
              :content="processedContent"
              @rendered="handleContentRendered"
            />
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

        <!-- 右侧边栏 -->
        <div class="lg:col-span-2">
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
.title {
  line-height: 1.3;
  word-wrap: break-word;
  font-weight: 700;
  color: var(--text-primary);
}

.article-meta {
  margin-bottom: 1.5rem;
}

.article-footer {
  border-top: 1px solid var(--border-color);
  padding-top: 1rem;
  margin-top: 2rem;
}

.desc {
  display: block;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.cursor-pointer {
  cursor: pointer;
}

/* Accessibility */
a:focus,
button:focus,
input:focus,
textarea:focus,
select:focus {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .title {
    font-size: 1.75rem;
  }
}
</style>
