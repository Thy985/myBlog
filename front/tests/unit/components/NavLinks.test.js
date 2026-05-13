import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import NavLinks from '@/components/layout/NavLinks.vue'
import { createRouter, createWebHistory } from 'vue-router'

describe('NavLinks组件测试', () => {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', component: { template: '<div>Home</div>' }, meta: { title: '首页' } },
      { path: '/category', component: { template: '<div>Category</div>' }, meta: { title: '分类' } },
      { path: '/tag', component: { template: '<div>Tag</div>' }, meta: { title: '标签' } },
      { path: '/archive', component: { template: '<div>Archive</div>' }, meta: { title: '归档' } }
    ]
  })

  it('应该正确渲染所有导航链接', async () => {
    router.push('/')
    await router.isReady()

    const wrapper = mount(NavLinks, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('分类')
    expect(wrapper.text()).toContain('标签')
    expect(wrapper.text()).toContain('归档')
  })

  it('导航链接数量正确', () => {
    const wrapper = mount(NavLinks, {
      global: {
        plugins: [router]
      }
    })

    // NavLinks 渲染为 div 包含多个 router-link
    const div = wrapper.find('div')
    expect(div.exists()).toBe(true)
    // 检查是否有4个链接文本
    const text = wrapper.text()
    expect(text).toContain('首页')
    expect(text).toContain('分类')
    expect(text).toContain('标签')
    expect(text).toContain('归档')
  })
})
