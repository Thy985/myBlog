<template>
  <div class="bg-card border border-border-color rounded-xl p-5 transition-all duration-300 hover:border-border-hover">
    <h2 class="text-lg font-bold text-text-primary mb-4 flex items-center">
      <svg class="w-5 h-5 mr-2 text-primary-color" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h3m0 0v3m0-3l-3 3m-3 0H7m0 0v-3m0 3l3-3m-2 8 4-4m0 0-4-4m4 4H6" />
      </svg>
      热门文章
    </h2>
    
    <div class="hot-articles-list space-y-3">
      <div 
        v-for="(article, index) in hotArticles" 
        :key="article.id"
        class="hot-article-item flex items-start space-x-3 p-2 rounded-lg hover:bg-border-color transition-colors duration-200 cursor-pointer animate-fade-in"
        :style="{ animationDelay: `${index * 0.1}s` }"
        @click="goToArticle(article.id)"
      >
        <!-- 排名序号 -->
        <div class="hot-article-rank flex-shrink-0 w-6 h-6 rounded-full bg-primary-light text-primary-color flex items-center justify-center font-bold text-sm">
          {{ index + 1 }}
        </div>
        
        <!-- 文章信息 -->
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-medium text-text-primary line-clamp-2 mb-1 hover:text-primary-color transition-colors duration-200">
            {{ article.title }}
          </h3>
          <div class="flex items-center text-xs text-text-tertiary">
            <span class="mr-3">{{ formatDate(article.createdAt) }}</span>
            <span>{{ article.readCount || 0 }} 阅读</span>
          </div>
        </div>
      </div>
      
      <!-- 无数据提示 -->
      <EmptyState
        v-if="hotArticles.length === 0"
        icon="star"
        title="暂无热门文章"
        description="暂时没有热门文章内容"
        :action-text="'去看看'"
        :show-action="true"
        :show-tip="false"
        :compact="true"
        @action="viewMore"
      />
    </div>
    
    <!-- 查看更多按钮 -->
    <div v-if="hotArticles.length > 0" class="mt-4 text-center">
      <button
        class="text-sm text-primary-color hover:text-primary-color/90 transition-colors duration-200"
        @click="viewMore"
        aria-label="查看更多文章"
      >
        查看更多 <svg class="w-4 h-4 inline ml-1" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
          <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7l3-3 3 3m0 6l-3 3-3-3" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/ui/EmptyState.vue'
import { getHotArticles } from '@/api/modules/article'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'

// Props
const props = defineProps({
  limit: {
    type: Number,
    default: 5
  }
})

const router = useRouter()

// 热门文章数据
const hotArticles = ref([])
const loading = ref(false)

// 获取热门文章
const fetchHotArticles = async () => {
  loading.value = true
  try {
    const res = await getHotArticles(props.limit)
    if (res && res.code === API_STATUS.SUCCESS && res.data) {
      hotArticles.value = res.data
    }
  } catch (error) {
    logger.error('获取热门文章失败:', error)
  } finally {
    loading.value = false
  }
}

// 初始化获取热门文章
onMounted(() => {
  fetchHotArticles()
})

// 方法
const goToArticle = (articleId) => {
  if (articleId) {
    router.push({ name: 'article', params: { id: String(articleId) } })
  }
}

const formatDate = (dateString) => {
  if (!dateString) {return ''}
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const viewMore = () => {
  // 这里可以跳转到一个专门的热门文章页面
  // 暂时跳转到首页
  router.push('/')
}
</script>

<style scoped>
.hot-articles-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.hot-article-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 0.5rem;
  transition: background-color 0.2s ease;
  cursor: pointer;
}

.hot-article-item:hover {
  background: var(--bg-tertiary);
}

.hot-article-rank {
  flex-shrink: 0;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.75rem;
}

.hot-article-info {
  flex: 1;
  min-width: 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  h2 {
    font-size: 1rem;
  }

  .hot-article-item {
    padding: 0.4rem;
  }

  .hot-article-rank {
    width: 1.25rem;
    height: 1.25rem;
    font-size: 0.625rem;
  }

  .hot-article-title {
    font-size: 0.875rem;
  }

  .hot-article-meta {
    font-size: 0.75rem;
  }
}

/* 淡入动画 */
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.5s ease-out forwards;
}

/* 减少动画模式支持 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }

  .animate-fade-in {
    animation: none !important;
  }
}
</style>
