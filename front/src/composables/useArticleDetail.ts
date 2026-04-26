/**
 * Article Detail Composable
 * 封装文章详情页的数据获取和状态管理
 *
 * ✅ 已修复: 数据竞态条件、请求瀑布流优化
 */
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'
import { getArticle as getArticleDetailApi, updateReadNum as updateReadNumApi, getArticleReadStats, getRelatedArticles } from '@/api/frontend/article'
import { getCategories } from '@/api/frontend/category'
import { getTags } from '@/api/frontend/tag'
import { request, API_STATUS } from '@/composables/api'

export function useArticleDetail() {
  const route = useRoute()
  const router = useRouter()

  let currentAbortController = null
  let isUnmounted = false

  const tocItems = ref([])
  const activeTocIndex = ref(0)

  const loading = ref(true)
  const error = ref(false)
  const errorMessage = ref('')
  const article = reactive({
    title: '',
    content: '',
    updatedAt: '',
    readCount: 0,
    categoryId: null as number | null,
    categoryName: '',
    description: '',
    titleImage: '',
    likeCount: 0,
    commentCount: 0,
    collectCount: 0,
    isLiked: false,
    isCollected: false,
    isTop: false,
    status: '',
    publishTime: null as string | null,
    createdAt: '',
    author: null as any,
    preArticleId: null as number | null,
    preArticleTitle: '',
    preArticleThumbnail: '',
    nextArticleId: null as number | null,
    nextArticleTitle: '',
    nextArticleThumbnail: '',
    tags: [] as any[]
  })

  const loadingRelated = ref(false)
  const relatedArticles = ref([] as any[])
  const readStats = ref({} as any)
  const updatingReadNum = ref(false)

  const categories = ref([] as any[])
  const tags = ref([] as any[])

  let processedContentWithIds = ''

  const processedContent = computed(() => {
    return processedContentWithIds || article.content
  })

  function generateToc() {
    if (isUnmounted) {return}

    tocItems.value = []
    const content = article.content
    let headingIndex = 0

    processedContentWithIds = content.replace(/<(h[2-4])([^>]*)>(.*?)<\/\1>/g, (match: string, tag: string, attrs: string, text: string) => {
      const id = `heading-${headingIndex}`
      tocItems.value.push({
        id,
        title: text.replace(/<[^>]*>/g, ''),
        level: parseInt(tag.substring(1))
      })
      headingIndex++
      return `<${tag} id="${id}"${attrs}>${text}</${tag}>`
    })

    activeTocIndex.value = 0
  }

  function safeUpdate(callback: () => void) {
    if (!isUnmounted) {
      callback()
    }
  }

  async function loadArticleDetail() {
    const articleId = route.params.id || route.query.articleId
    if (!articleId) {
      safeUpdate(() => {
        error.value = true
        errorMessage.value = '文章ID不存在'
        loading.value = false
      })
      ElMessage.warning('文章ID不存在')
      return
    }

    if (currentAbortController) {
      currentAbortController.abort()
    }
    currentAbortController = new AbortController()
    const signal = currentAbortController.signal

    logger.debug('Fetching article detail', { articleId })
    safeUpdate(() => {
      loading.value = true
      error.value = false
    })

    try {
      const res = await getArticleDetailApi(Number(articleId))

      if (signal.aborted || isUnmounted) {return}

      if (res && res.data) {
        const d = res.data
        safeUpdate(() => {
          article.title = d.title || ''
          let content = d.content || ''
          content = content.replace(/<h1[^>]*>.*?<\/h1>/gi, '')
          article.content = content
          article.updatedAt = d.updatedAt || ''
          article.description = d.description || ''
          article.titleImage = d.titleImage || ''
          article.readCount = d.readCount || 0
          article.likeCount = d.likeCount || 0
          article.commentCount = d.commentCount || 0
          article.collectCount = d.collectCount || 0
          article.isLiked = !!d.isLiked
          article.isCollected = !!d.isCollected
          article.isTop = !!d.isTop
          article.status = d.status || ''
          article.publishTime = d.publishTime || null
          article.createdAt = d.createdAt || ''
          article.author = d.author || null
          article.categoryId = d.category?.id
          article.categoryName = d.category?.name || d.categoryName || ''
          article.tags = d.tags || []

          if (d.preArticle) {
            article.preArticleId = d.preArticle.id
            article.preArticleTitle = d.preArticle.title
            article.preArticleThumbnail = d.preArticle.titleImage || d.preArticle.thumbnail || ''
          } else {
            article.preArticleId = null
            article.preArticleTitle = ''
            article.preArticleThumbnail = ''
          }

          if (d.nextArticle) {
            article.nextArticleId = d.nextArticle.id
            article.nextArticleTitle = d.nextArticle.title
            article.nextArticleThumbnail = d.nextArticle.titleImage || d.nextArticle.thumbnail || ''
          } else {
            article.nextArticleId = null
            article.nextArticleTitle = ''
            article.nextArticleThumbnail = ''
          }
        })
      } else {
        throw new Error('Failed to get article detail: invalid response format')
      }
    } catch (err: any) {
      if (err.name === 'AbortError' || isUnmounted) {return}

      logger.error('Failed to get article detail:', err)
      safeUpdate(() => {
        error.value = true
        errorMessage.value = err.message || 'Failed to get article detail, please try again later'
        ElMessage.error(errorMessage.value)
      })
    } finally {
      safeUpdate(() => {
        loading.value = false
      })

      if (!isUnmounted) {
        generateToc()
        fetchRelatedArticles(Number(articleId))
      }
    }
  }

  function goArticleDetail(articleId: number | string) {
    if (!articleId) {
      ElMessage.warning('Article ID does not exist')
      return
    }
    try {
      router.push({ name: 'article', params: { id: String(articleId) } })
    } catch (err: any) {
      logger.error('Failed to navigate to article detail:', err)
      router.push({ path: '/article/detail', query: { articleId: String(articleId) } })
    }
  }

  async function fetchCategories() {
    if (isUnmounted) {return}
    try {
      const res = await getCategories()
      if (!isUnmounted && res && res.data) {
        categories.value = res.data
      }
    } catch (err: any) {
      if (!isUnmounted) {
        logger.error('Failed to fetch categories:', err)
      }
    }
  }

  async function fetchTags() {
    if (isUnmounted) {return}
    try {
      const res = await getTags()
      if (!isUnmounted && res && res.data) {
        tags.value = res.data
      }
    } catch (err: any) {
      if (!isUnmounted) {
        logger.error('Failed to fetch tags:', err)
      }
    }
  }

  function goCategoryArticleListPage(id: number, name: string) {
    router.push({ name: 'category-articles', params: { id: String(id), name: encodeURIComponent(name || '') } })
  }

  function goTagArticleListPage(id: number, name: string) {
    router.push({ name: 'tag-articles', params: { id: String(id), name: encodeURIComponent(name || '') } })
  }

  async function updateReadNum(articleId: number | string) {
    if (!articleId || isUnmounted) {return}

    updatingReadNum.value = true
    try {
      await request(() => updateReadNumApi(Number(articleId)), null, {
        showError: false
      })
      safeUpdate(() => {
        article.readCount = (article.readCount || 0) + 1
      })
    } catch (err: any) {
      logger.error('Failed to update read count:', err)
    } finally {
      if (!isUnmounted) {
        updatingReadNum.value = false
      }
    }
  }

  async function getReadStats(articleId: number | string) {
    if (!articleId || isUnmounted) {return}

    try {
      const res = await request(() => getArticleReadStats(Number(articleId)), null, {
        showError: true
      })
      if (!isUnmounted && res && res.data) {
        readStats.value = res.data
      }
    } catch (err: any) {
      if (!isUnmounted) {
        logger.error('Failed to get read stats:', err)
      }
    }
  }

  async function fetchRelatedArticles(articleId: number) {
    if (!articleId || isUnmounted) {return}

    loadingRelated.value = true
    try {
      const res = await getRelatedArticles(articleId, 5)
      if (!isUnmounted) {
        if (res && res.code === API_STATUS.SUCCESS && res.data) {
          relatedArticles.value = res.data
        } else {
          relatedArticles.value = []
        }
      }
    } catch (err: any) {
      if (!isUnmounted) {
        logger.error('Failed to get related articles:', err)
        relatedArticles.value = []
      }
    } finally {
      if (!isUnmounted) {
        loadingRelated.value = false
      }
    }
  }

  async function loadAllData() {
    const articleId = route.params.id || route.query.articleId
    if (!articleId) {return}

    const [articleResult] = await Promise.allSettled([
      loadArticleDetail(),
      fetchCategories(),
      fetchTags()
    ])

    if (!isUnmounted) {
      updateReadNum(articleId)
      getReadStats(articleId)
    }
  }

  onUnmounted(() => {
    isUnmounted = true
    if (currentAbortController) {
      currentAbortController.abort()
      currentAbortController = null
    }
  })

  return {
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
    commentListRef: ref(null),

    loadArticleDetail,
    goArticleDetail,
    fetchCategories,
    fetchTags,
    goCategoryArticleListPage,
    goTagArticleListPage,
    updateReadNum,
    getReadStats,
    fetchRelatedArticles,
    loadAllData,

    dispose: () => {
      isUnmounted = true
      if (currentAbortController) {
        currentAbortController.abort()
      }
    }
  }
}