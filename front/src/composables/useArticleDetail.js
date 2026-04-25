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
import { getArticle as getArticleDetailApi, updateReadNum as updateReadNumApi, getArticleReadStats, getRelatedArticles } from '@/api/modules/article'
import { getCategories } from '@/api/frontend/category'
import { getTags } from '@/api/frontend/tag'
import { request, API_STATUS } from '@/composables/api'

export function useArticleDetail() {
  const route = useRoute()
  const router = useRouter()

  // 竞态控制: 用于取消进行中的异步操作
  let currentAbortController = null
  let isUnmounted = false

  // TOC items
  const tocItems = ref([])
  const activeTocIndex = ref(0)

  // Article state
  const loading = ref(true)
  const error = ref(false)
  const errorMessage = ref('')
  const article = reactive({
    title: '',
    content: '',
    updatedAt: '',
    readCount: 0,
    categoryId: null,
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
    publishTime: null,
    createdAt: '',
    author: null,
    preArticleId: null,
    preArticleTitle: '',
    preArticleThumbnail: '',
    nextArticleId: null,
    nextArticleTitle: '',
    nextArticleThumbnail: '',
    tags: []
  })

  // Related articles state
  const loadingRelated = ref(false)
  const relatedArticles = ref([])
  const readStats = ref({})
  const updatingReadNum = ref(false)

  // Categories and tags
  const categories = ref([])
  const tags = ref([])

  // Storage for processed content with IDs
  let processedContentWithIds = ''

  // Get processed content with heading IDs for TOC
  const processedContent = computed(() => {
    return processedContentWithIds || article.content
  })

  // Generate TOC from content and add IDs to headings
  function generateToc() {
    if (isUnmounted) {return}

    tocItems.value = []
    const content = article.content
    let headingIndex = 0

    processedContentWithIds = content.replace(/<(h[2-4])([^>]*)>(.*?)<\/\1>/g, (match, tag, attrs, text) => {
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

  // 安全的状态更新（检查组件是否已卸载）
  function safeUpdate(callback) {
    if (!isUnmounted) {
      callback()
    }
  }

  // Load article detail (带竞态保护)
  async function loadArticleDetail() {
    // ✅ 支持 RESTful params 和 query 两种模式
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

    // 取消之前的请求
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
      const res = await getArticleDetailApi(articleId)

      // 检查是否被取消或组件已卸载
      if (signal.aborted || isUnmounted) {return}

      if (res && res.data) {
        const d = res.data
        safeUpdate(() => {
          article.title = d.title
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
    } catch (err) {
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
        fetchRelatedArticles(articleId)
      }
    }
  }

  // Navigate to article detail (使用 RESTful URL)
  function goArticleDetail(articleId) {
    if (!articleId) {
      ElMessage.warning('Article ID does not exist')
      return
    }
    try {
      // ✅ 使用 RESTful 风格路由: /article/123
      router.push({ name: 'article', params: { id: String(articleId) } })
    } catch (err) {
      logger.error('Failed to navigate to article detail:', err)
      // 回退到 query 模式（兼容旧路径）
      router.push({ path: '/article/detail', query: { articleId: String(articleId) } })
    }
  }

  // Fetch categories
  async function fetchCategories() {
    if (isUnmounted) {return}
    try {
      const res = await getCategories()
      if (!isUnmounted && res && res.data) {
        categories.value = res.data
      }
    } catch (err) {
      if (!isUnmounted) {
        logger.error('Failed to fetch categories:', err)
      }
    }
  }

  // Fetch tags
  async function fetchTags() {
    if (isUnmounted) {return}
    try {
      const res = await getTags()
      if (!isUnmounted && res && res.data) {
        tags.value = res.data
      }
    } catch (err) {
      if (!isUnmounted) {
        logger.error('Failed to fetch tags:', err)
      }
    }
  }

  // Navigate to category article list
  function goCategoryArticleListPage(id, name) {
    router.push({ name: 'category-articles', params: { id: String(id), name: encodeURIComponent(name || '') } })
  }

  // Navigate to tag article list
  function goTagArticleListPage(id, name) {
    router.push({ name: 'tag-articles', params: { id: String(id), name: encodeURIComponent(name || '') } })
  }

  // Update article read count
  async function updateReadNum(articleId) {
    if (!articleId || isUnmounted) {return}

    updatingReadNum.value = true
    try {
      await request(() => updateReadNumApi(articleId), null, {
        showError: false
      })
      safeUpdate(() => {
        article.readCount = (article.readCount || 0) + 1
      })
    } catch (err) {
      logger.error('Failed to update read count:', err)
    } finally {
      if (!isUnmounted) {
        updatingReadNum.value = false
      }
    }
  }

  // Get article read statistics
  async function getReadStats(articleId) {
    if (!articleId || isUnmounted) {return}

    try {
      const res = await request(() => getArticleReadStats(articleId), null, {
        showError: true
      })
      if (!isUnmounted && res && res.data) {
        readStats.value = res.data
      }
    } catch (err) {
      if (!isUnmounted) {
        logger.error('Failed to get read stats:', err)
      }
    }
  }

  // Get related articles
  async function fetchRelatedArticles(articleId) {
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
    } catch (err) {
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

  // Load all data - 优化版：全并行 + 非阻塞次要数据
  async function loadAllData() {
    const articleId = route.params.id || route.query.articleId
    if (!articleId) {return}

    // 核心数据并行加载（首屏关键）
    const [articleResult] = await Promise.allSettled([
      loadArticleDetail(),
      fetchCategories(),
      fetchTags()
    ])

    // 次要数据不阻塞渲染，fire-and-forget
    if (!isUnmounted) {
      updateReadNum(articleId)
      getReadStats(articleId)
    }
  }

  // 清理函数：组件卸载时调用
  onUnmounted(() => {
    isUnmounted = true
    if (currentAbortController) {
      currentAbortController.abort()
      currentAbortController = null
    }
  })

  return {
    // State
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

    // Methods
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

    // Lifecycle
    dispose: () => {
      isUnmounted = true
      if (currentAbortController) {
        currentAbortController.abort()
      }
    }
  }
}
