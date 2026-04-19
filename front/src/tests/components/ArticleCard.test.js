import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import ArticleCard from '@/components/common/ArticleCard.vue'

describe('ArticleCard组件测试', () => {
  const mockArticle = {
    id: 1,
    title: '测试文章标题',
    description: '这是一篇测试文章的描述内容',
    titleImage: '/test-image.jpg',
    categoryName: '技术分享',
    tagNames: ['Vue3', 'JavaScript'],
    createdTime: '2026-02-11',
    readCount: 100
  }

  it('应该正确渲染文章信息', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })

    // 检查标题
    expect(wrapper.find('h2').text()).toBe('测试文章标题')

    // 检查描述
    expect(wrapper.find('.card-description').text()).toBe('这是一篇测试文章的描述内容')

    // 检查分类
    expect(wrapper.text()).toContain('技术分享')

    // 检查标签
    expect(wrapper.text()).toContain('Vue3')
    expect(wrapper.text()).toContain('JavaScript')

    // 检查阅读量
    expect(wrapper.text()).toContain('100')
  })

  it('应该正确渲染图片', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })

    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('alt')).toBe('测试文章标题')
  })

  it('点击标题应该触发goArticleDetail事件', async () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })

    await wrapper.find('h2').trigger('click')

    expect(wrapper.emitted('goArticleDetail')).toBeTruthy()
    expect(wrapper.emitted('goArticleDetail')[0]).toEqual([1])
  })

  it('点击标签应该触发goTagArticleListPage事件', async () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })

    const tags = wrapper.findAll('.tag-item')
    await tags[0].trigger('click')

    expect(wrapper.emitted('goTagArticleListPage')).toBeTruthy()
    expect(wrapper.emitted('goTagArticleListPage')[0]).toEqual([null, 'Vue3'])
  })

  it('应该正确处理无标签的情况', () => {
    const articleWithoutTags = {
      ...mockArticle,
      tagNames: []
    }

    const wrapper = mount(ArticleCard, {
      props: { article: articleWithoutTags }
    })

    const tags = wrapper.findAll('.tag-item')
    expect(tags.length).toBe(0)
  })

  it('应该正确显示默认阅读量', () => {
    const articleWithoutReadCount = {
      ...mockArticle,
      readCount: undefined
    }

    const wrapper = mount(ArticleCard, {
      props: { article: articleWithoutReadCount }
    })

    expect(wrapper.text()).toContain('0')
  })

  it('应该支持键盘导航', async () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })

    const tag = wrapper.find('.tag-item')

    // 模拟按下Enter键
    await tag.trigger('keydown.enter')
    expect(wrapper.emitted('goTagArticleListPage')).toBeTruthy()
  })

  it('应该有正确的ARIA属性', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })

    const tag = wrapper.find('.tag-item')
    expect(tag.attributes('role')).toBe('button')
    expect(tag.attributes('aria-label')).toContain('Vue3')
    expect(tag.attributes('tabindex')).toBe('0')
  })

  it('悬停时卡片应该有正确的样式类', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    })

    const card = wrapper.find('.article-card')
    expect(card.exists()).toBe(true)
  })
})
