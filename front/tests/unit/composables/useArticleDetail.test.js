import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { h, defineComponent } from 'vue'
import { useArticleDetail } from '@/composables/useArticleDetail'
import { getArticle as getArticleDetailApi, updateReadNum as updateReadNumApi, getArticleReadStats, getRelatedArticles } from '@/api/frontend/article'
import { getCategories } from '@/api/frontend/category'
import { getTags } from '@/api/frontend/tag'
import { API_STATUS } from '@/composables/api'
import { ElMessage } from 'element-plus'
import * as vueRouter from 'vue-router'

// Mock API 模块
vi.mock('@/api/frontend/article', () => ({
  getArticle: vi.fn(),
  updateReadNum: vi.fn(),
  getArticleReadStats: vi.fn(),
  getRelatedArticles: vi.fn()
}))

vi.mock('@/api/frontend/category', () => ({
  getCategories: vi.fn()
}))

vi.mock('@/api/frontend/tag', () => ({
  getTags: vi.fn()
}))

// Mock vue-router
vi.mock('vue-router', () => ({
  useRoute: vi.fn(),
  useRouter: vi.fn()
}))

// Mock logger
vi.mock('@/utils/logger', () => ({
  default: {
    debug: vi.fn(),
    error: vi.fn(),
    warn: vi.fn()
  }
}))

// Mock loading utility
vi.mock('@/utils/loading', () => ({
  showPageLoading: vi.fn(),
  hidePageLoading: vi.fn()
}))

/**
 * 辅助函数：在组件上下文中挂载 composable
 * 因为 useArticleDetail 内部使用了 onUnmounted 和 vue-router
 */
function mountComposable() {
  const TestComponent = defineComponent({
    setup() {
      const composable = useArticleDetail()
      return { ...composable }
    },
    render() {
      return h('div', 'test')
    }
  })
  return mount(TestComponent)
}

describe('useArticleDetail 组合式函数测试', () => {
  let mockRoute, mockRouter

  const mockArticleData = {
    id: 1,
    title: '测试文章标题',
    content: '<h1>文章标题</h1><h2>第一章</h2><p>内容</p><h3>1.1 小节</h3><p>更多内容</p>',
    description: '这是一篇测试文章的描述',
    titleImage: '/test-image.jpg',
    readCount: 100,
    likeCount: 10,
    commentCount: 5,
    collectCount: 3,
    isLiked: false,
    isCollected: false,
    isTop: false,
    status: 'published',
    publishTime: '2026-06-01',
    createdAt: '2026-06-01',
    updatedAt: '2026-06-02',
    author: { id: 1, name: '测试作者' },
    category: { id: 1, name: '技术分享' },
    tags: [
      { id: 1, name: 'Vue3' },
      { id: 2, name: 'JavaScript' }
    ],
    preArticle: { id: 0, title: '上一篇文章', titleImage: '/prev.jpg' },
    nextArticle: { id: 2, title: '下一篇文章', titleImage: '/next.jpg' }
  }

  const mockRelatedArticles = [
    { id: 2, title: '相关文章1', readCount: 50 },
    { id: 3, title: '相关文章2', readCount: 80 }
  ]

  const mockCategories = [
    { id: 1, name: '技术分享' },
    { id: 2, name: '生活随笔' }
  ]

  const mockTags = [
    { id: 1, name: 'Vue3' },
    { id: 2, name: 'JavaScript' }
  ]

  beforeEach(() => {
    vi.resetAllMocks()

    mockRoute = {
      params: { id: '1' },
      query: {}
    }
    mockRouter = {
      push: vi.fn()
    }

    vi.mocked(vueRouter.useRoute).mockReturnValue(mockRoute)
    vi.mocked(vueRouter.useRouter).mockReturnValue(mockRouter)

    getArticleDetailApi.mockResolvedValue({ data: mockArticleData })
    getCategories.mockResolvedValue({ data: mockCategories })
    getTags.mockResolvedValue({ data: mockTags })
    getRelatedArticles.mockResolvedValue({ code: API_STATUS.SUCCESS, data: mockRelatedArticles })
    updateReadNumApi.mockResolvedValue({ code: API_STATUS.SUCCESS })
    getArticleReadStats.mockResolvedValue({ code: API_STATUS.SUCCESS, data: { readCount: 100 } })

    ElMessage.error = vi.fn()
    ElMessage.warning = vi.fn()
    ElMessage.success = vi.fn()
  })

  // ========== loadArticleDetail - 成功加载 ==========
  describe('loadArticleDetail - 成功加载', () => {
    it('应该正确加载文章数据', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.article.title).toBe('测试文章标题')
      expect(wrapper.vm.article.description).toBe('这是一篇测试文章的描述')
      expect(wrapper.vm.article.titleImage).toBe('/test-image.jpg')
      expect(wrapper.vm.article.readCount).toBe(100)
      expect(wrapper.vm.article.likeCount).toBe(10)
      expect(wrapper.vm.article.commentCount).toBe(5)
      expect(wrapper.vm.article.collectCount).toBe(3)
      expect(wrapper.vm.article.categoryId).toBe(1)
      expect(wrapper.vm.article.categoryName).toBe('技术分享')
      expect(wrapper.vm.article.tags).toHaveLength(2)
      expect(wrapper.vm.article.author).toEqual({ id: 1, name: '测试作者' })
    })

    it('应该正确处理上一篇文章信息', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.article.preArticleId).toBe(0)
      expect(wrapper.vm.article.preArticleTitle).toBe('上一篇文章')
      expect(wrapper.vm.article.preArticleThumbnail).toBe('/prev.jpg')
    })

    it('应该正确处理下一篇文章信息', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.article.nextArticleId).toBe(2)
      expect(wrapper.vm.article.nextArticleTitle).toBe('下一篇文章')
      expect(wrapper.vm.article.nextArticleThumbnail).toBe('/next.jpg')
    })

    it('应该移除内容中的h1标签', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.article.content).not.toContain('<h1')
      expect(wrapper.vm.article.content).toContain('<h2>第一章</h2>')
    })

    it('应该在没有上一篇文章时设置为null/空值', async () => {
      const articleDataWithoutPrevNext = {
        ...mockArticleData,
        preArticle: null,
        nextArticle: null
      }
      getArticleDetailApi.mockResolvedValue({ data: articleDataWithoutPrevNext })

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.article.preArticleId).toBeNull()
      expect(wrapper.vm.article.preArticleTitle).toBe('')
      expect(wrapper.vm.article.preArticleThumbnail).toBe('')
      expect(wrapper.vm.article.nextArticleId).toBeNull()
    })

    it('应该使用query.articleId作为备选ID', async () => {
      mockRoute.params = {}
      mockRoute.query = { articleId: '99' }
      vi.mocked(vueRouter.useRoute).mockReturnValue(mockRoute)

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(getArticleDetailApi).toHaveBeenCalledWith(99)
      expect(wrapper.vm.article.title).toBe('测试文章标题')
    })
  })

  // ========== loadArticleDetail - 错误处理 ==========
  describe('loadArticleDetail - 错误处理', () => {
    it('应该处理API请求失败', async () => {
      const apiError = new Error('网络错误')
      getArticleDetailApi.mockRejectedValue(apiError)

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.error).toBe(true)
      expect(wrapper.vm.errorMessage).toContain('网络错误')
    })

    it('应该处理无效响应格式', async () => {
      getArticleDetailApi.mockResolvedValue({})

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.error).toBe(true)
      expect(wrapper.vm.errorMessage).toContain('invalid response format')
    })

    it('应该在文章ID不存在时显示警告', async () => {
      mockRoute.params = {}
      mockRoute.query = {}
      vi.mocked(vueRouter.useRoute).mockReturnValue(mockRoute)

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.error).toBe(true)
      expect(wrapper.vm.errorMessage).toContain('文章ID不存在')
      expect(ElMessage.warning).toHaveBeenCalledWith('文章ID不存在')
    })

    it('应该显示错误消息', async () => {
      getArticleDetailApi.mockRejectedValue(new Error('服务器内部错误'))

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(ElMessage.error).toHaveBeenCalledWith('服务器内部错误')
    })
  })

  // ========== loadArticleDetail - 加载状态 ==========
  describe('loadArticleDetail - 加载状态', () => {
    it('加载开始时loading应该为true', async () => {
      let loadingDuringCall = null
      getArticleDetailApi.mockImplementation(async () => {
        loadingDuringCall = true
        return { data: mockArticleData }
      })

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(loadingDuringCall).toBe(true)
    })

    it('加载完成后loading应该为false', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.loading).toBe(false)
    })

    it('加载成功时error应该为false', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.error).toBe(false)
    })

    it('加载失败后loading也应该为false', async () => {
      getArticleDetailApi.mockRejectedValue(new Error('失败'))

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.loading).toBe(false)
    })
  })

  // ========== TOC 目录生成测试 ==========
  describe('TOC 目录生成', () => {
    it('应该从文章内容生成TOC', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.tocItems).toHaveLength(2)
      expect(wrapper.vm.tocItems[0]).toMatchObject({
        id: 'heading-0',
        title: '第一章',
        level: 2
      })
      expect(wrapper.vm.tocItems[1]).toMatchObject({
        id: 'heading-1',
        title: '1.1 小节',
        level: 3
      })
    })

    it('应该重置activeTocIndex为0', async () => {
      const wrapper = mountComposable()
      wrapper.vm.activeTocIndex = 5

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.activeTocIndex).toBe(0)
    })

    it('没有标题的内容应该生成空TOC', async () => {
      getArticleDetailApi.mockResolvedValue({
        data: { ...mockArticleData, content: '<p>纯文本内容，没有标题</p>' }
      })

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.tocItems).toHaveLength(0)
    })

    it('应该正确剥离标题中的HTML标签', async () => {
      getArticleDetailApi.mockResolvedValue({
        data: {
          ...mockArticleData,
          content: '<h2>带<strong>加粗</strong>的标题</h2>'
        }
      })

      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.tocItems).toHaveLength(1)
      expect(wrapper.vm.tocItems[0].title).toBe('带加粗的标题')
    })

    it('应该为标题添加id属性', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.processedContent).toContain('id="heading-0"')
      expect(wrapper.vm.processedContent).toContain('id="heading-1"')
    })
  })

  // ========== goArticleDetail 导航测试 ==========
  describe('goArticleDetail 导航', () => {
    it('应该导航到文章详情页（使用params）', () => {
      const wrapper = mountComposable()

      wrapper.vm.goArticleDetail(123)

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'article',
        params: { id: '123' }
      })
    })

    it('应该支持字符串类型的articleId', () => {
      const wrapper = mountComposable()

      wrapper.vm.goArticleDetail('456')

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'article',
        params: { id: '456' }
      })
    })

    it('应该在articleId不存在时显示警告', () => {
      const wrapper = mountComposable()

      wrapper.vm.goArticleDetail(null)

      expect(ElMessage.warning).toHaveBeenCalledWith('Article ID does not exist')
      expect(mockRouter.push).not.toHaveBeenCalled()
    })

    it('应该在articleId为undefined时显示警告', () => {
      const wrapper = mountComposable()

      wrapper.vm.goArticleDetail(undefined)

      expect(ElMessage.warning).toHaveBeenCalledWith('Article ID does not exist')
      expect(mockRouter.push).not.toHaveBeenCalled()
    })

    it('应该在路由失败时使用query方式回退', () => {
      const wrapper = mountComposable()
      mockRouter.push.mockImplementationOnce(() => {
        throw new Error('路由跳转失败')
      })

      wrapper.vm.goArticleDetail(789)

      expect(mockRouter.push).toHaveBeenCalledTimes(2)
      expect(mockRouter.push).toHaveBeenLastCalledWith({
        path: '/article/detail',
        query: { articleId: '789' }
      })
    })
  })

  // ========== goCategoryArticleListPage 导航测试 ==========
  describe('goCategoryArticleListPage 导航', () => {
    it('应该导航到分类文章列表页', () => {
      const wrapper = mountComposable()

      wrapper.vm.goCategoryArticleListPage(1, '技术分享')

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'category-articles',
        params: { id: '1', name: '技术分享' }
      })
    })

    it('应该在name为空时使用空字符串', () => {
      const wrapper = mountComposable()

      wrapper.vm.goCategoryArticleListPage(1, '')

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'category-articles',
        params: { id: '1', name: '' }
      })
    })

    it('应该在name为null时使用空字符串', () => {
      const wrapper = mountComposable()

      wrapper.vm.goCategoryArticleListPage(1, null)

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'category-articles',
        params: { id: '1', name: '' }
      })
    })
  })

  // ========== goTagArticleListPage 导航测试 ==========
  describe('goTagArticleListPage 导航', () => {
    it('应该导航到标签文章列表页', () => {
      const wrapper = mountComposable()

      wrapper.vm.goTagArticleListPage(1, 'Vue3')

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'tag-articles',
        params: { id: '1', name: 'Vue3' }
      })
    })

    it('应该在name为空时使用空字符串', () => {
      const wrapper = mountComposable()

      wrapper.vm.goTagArticleListPage(2, '')

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'tag-articles',
        params: { id: '2', name: '' }
      })
    })

    it('应该在name为null时使用空字符串', () => {
      const wrapper = mountComposable()

      wrapper.vm.goTagArticleListPage(3, null)

      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'tag-articles',
        params: { id: '3', name: '' }
      })
    })
  })

  // ========== 相关文章加载测试 ==========
  describe('相关文章加载', () => {
    it('应该成功加载相关文章', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(getRelatedArticles).toHaveBeenCalledWith(1, 5)
      expect(wrapper.vm.relatedArticles).toHaveLength(2)
      expect(wrapper.vm.relatedArticles[0].title).toBe('相关文章1')
    })

    it('加载完成后loadingRelated应该为false', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.loadingRelated).toBe(false)
    })

    it('相关文章API失败时应设置为空数组', async () => {
      const wrapper = mountComposable()

      getRelatedArticles.mockRejectedValue(new Error('获取相关文章失败'))

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.relatedArticles).toEqual([])
    })

    it('相关文章API返回非成功状态时应设置为空数组', async () => {
      const wrapper = mountComposable()

      getRelatedArticles.mockResolvedValue({ code: 500, data: null })

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.relatedArticles).toEqual([])
    })

    it('相关文章API返回data为null时应设置为空数组', async () => {
      const wrapper = mountComposable()

      getRelatedArticles.mockResolvedValue({ code: API_STATUS.SUCCESS, data: null })

      await wrapper.vm.loadArticleDetail()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.relatedArticles).toEqual([])
    })
  })

  // ========== fetchCategories / fetchTags 测试 ==========
  describe('分类和标签加载', () => {
    it('应该成功加载分类', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.fetchCategories()
      await wrapper.vm.$nextTick()

      expect(getCategories).toHaveBeenCalled()
      expect(wrapper.vm.categories).toHaveLength(2)
      expect(wrapper.vm.categories[0].name).toBe('技术分享')
    })

    it('分类加载失败时不应报错', async () => {
      getCategories.mockRejectedValue(new Error('获取分类失败'))

      const wrapper = mountComposable()

      await wrapper.vm.fetchCategories()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.categories).toEqual([])
    })

    it('应该成功加载标签', async () => {
      const wrapper = mountComposable()

      await wrapper.vm.fetchTags()
      await wrapper.vm.$nextTick()

      expect(getTags).toHaveBeenCalled()
      expect(wrapper.vm.tags).toHaveLength(2)
      expect(wrapper.vm.tags[0].name).toBe('Vue3')
    })

    it('标签加载失败时不应报错', async () => {
      getTags.mockRejectedValue(new Error('获取标签失败'))

      const wrapper = mountComposable()

      await wrapper.vm.fetchTags()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.tags).toEqual([])
    })
  })

  // ========== 组合式函数返回接口测试 ==========
  describe('组合式函数返回接口', () => {
    it('应该返回所有预期的响应式状态', () => {
      const wrapper = mountComposable()

      expect(wrapper.vm).toHaveProperty('article')
      expect(wrapper.vm).toHaveProperty('loading')
      expect(wrapper.vm).toHaveProperty('error')
      expect(wrapper.vm).toHaveProperty('errorMessage')
      expect(wrapper.vm).toHaveProperty('tocItems')
      expect(wrapper.vm).toHaveProperty('activeTocIndex')
      expect(wrapper.vm).toHaveProperty('categories')
      expect(wrapper.vm).toHaveProperty('tags')
      expect(wrapper.vm).toHaveProperty('relatedArticles')
      expect(wrapper.vm).toHaveProperty('loadingRelated')
      expect(wrapper.vm).toHaveProperty('processedContent')
      expect(wrapper.vm).toHaveProperty('commentListRef')
    })

    it('应该返回所有预期的方法', () => {
      const wrapper = mountComposable()

      expect(typeof wrapper.vm.loadArticleDetail).toBe('function')
      expect(typeof wrapper.vm.goArticleDetail).toBe('function')
      expect(typeof wrapper.vm.fetchCategories).toBe('function')
      expect(typeof wrapper.vm.fetchTags).toBe('function')
      expect(typeof wrapper.vm.goCategoryArticleListPage).toBe('function')
      expect(typeof wrapper.vm.goTagArticleListPage).toBe('function')
      expect(typeof wrapper.vm.updateReadNum).toBe('function')
      expect(typeof wrapper.vm.getReadStats).toBe('function')
      expect(typeof wrapper.vm.fetchRelatedArticles).toBe('function')
      expect(typeof wrapper.vm.loadAllData).toBe('function')
      expect(typeof wrapper.vm.dispose).toBe('function')
    })

    it('初始状态应该正确', () => {
      const wrapper = mountComposable()

      expect(wrapper.vm.loading).toBe(true)
      expect(wrapper.vm.error).toBe(false)
      expect(wrapper.vm.errorMessage).toBe('')
      expect(wrapper.vm.tocItems).toEqual([])
      expect(wrapper.vm.relatedArticles).toEqual([])
      expect(wrapper.vm.loadingRelated).toBe(false)
    })
  })

  // ========== dispose 清理测试 ==========
  describe('dispose 清理', () => {
    it('dispose 应该能正常调用', () => {
      const wrapper = mountComposable()

      expect(() => wrapper.vm.dispose()).not.toThrow()
    })
  })
})
