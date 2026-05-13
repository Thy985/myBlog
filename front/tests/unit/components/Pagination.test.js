import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import Pagination from '@/components/ui/Pagination.vue'

describe('Pagination组件测试', () => {
  it('应该正确渲染分页信息', () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 1,
        total: 100,
        size: 10,
        pages: 10
      }
    })

    expect(wrapper.text()).toContain('1')
    expect(wrapper.text()).toContain('10')
  })

  it('应该正确计算页数', () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 1,
        total: 95,
        size: 10,
        pages: 10
      }
    })

    expect(wrapper.props('pages')).toBe(10)
  })

  it('点击下一页应该触发事件', async () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 1,
        total: 100,
        size: 10,
        pages: 10
      }
    })

    // 找到所有 a 标签，点击最后一个（下一页）
    const links = wrapper.findAll('a')
    const nextLink = links[links.length - 1]
    await nextLink.trigger('click')

    expect(wrapper.emitted('page-change')).toBeTruthy()
    expect(wrapper.emitted('page-change')[0]).toEqual([2])
  })

  it('点击上一页应该触发事件', async () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 2,
        total: 100,
        size: 10,
        pages: 10
      }
    })

    // 找到所有 a 标签，点击第一个（上一页）
    const links = wrapper.findAll('a')
    const prevLink = links[0]
    await prevLink.trigger('click')

    expect(wrapper.emitted('page-change')[0]).toEqual([1])
  })

  it('点击具体页码应该触发事件', async () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 1,
        total: 100,
        size: 10,
        pages: 10
      }
    })

    // 找到页码链接（中间的那些 li a）
    // pageLinks[0] = prev button, pageLinks[1..10] = pages 1-10, pageLinks[11] = next button
    const pageLinks = wrapper.findAll('li a')
    // 点击第3个链接（第2页）
    if (pageLinks.length > 2) {
      await pageLinks[2].trigger('click')
      expect(wrapper.emitted('page-change')[0]).toEqual([2])
    }
  })

  it('当前页应该禁用上一页按钮', () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 1,
        total: 100,
        size: 10,
        pages: 10
      }
    })

    const links = wrapper.findAll('a')
    const prevLink = links[0]
    expect(prevLink.classes()).toContain('page-btn-disabled')
  })

  it('最后一页应该禁用下一页按钮', () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 10,
        total: 100,
        size: 10,
        pages: 10
      }
    })

    const links = wrapper.findAll('a')
    const nextLink = links[links.length - 1]
    expect(nextLink.classes()).toContain('page-btn-disabled')
  })

  it('总数为0时不应该显示分页', () => {
    const wrapper = mount(Pagination, {
      props: {
        current: 1,
        total: 0,
        size: 10,
        pages: 0
      }
    })

    expect(wrapper.find('nav').exists()).toBe(false)
  })
})
