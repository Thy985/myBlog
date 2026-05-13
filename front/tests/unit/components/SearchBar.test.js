import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import SearchBar from '@/components/layout/SearchBar.vue'
import { createRouter, createWebHistory } from 'vue-router'

describe('SearchBar组件测试', () => {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/search', component: { template: '<div>Search</div>' } }
    ]
  })

  it('应该正确渲染搜索输入框', () => {
    const wrapper = mount(SearchBar, {
      global: {
        plugins: [router]
      }
    })

    const input = wrapper.find('input[type="text"]')
    expect(input.exists()).toBe(true)
  })

  it('应该支持v-model绑定', async () => {
    const wrapper = mount(SearchBar, {
      props: {
        modelValue: '测试搜索'
      },
      global: {
        plugins: [router]
      }
    })

    const input = wrapper.find('input')
    expect(input.element.value).toBe('测试搜索')
  })

  it('按回车应该触发search事件', async () => {
    const wrapper = mount(SearchBar, {
      global: {
        plugins: [router]
      }
    })

    const input = wrapper.find('input')
    await input.setValue('测试')
    await input.trigger('keyup.enter')

    expect(wrapper.emitted('search')).toBeTruthy()
  })

  it('应该显示正确的placeholder', () => {
    const wrapper = mount(SearchBar, {
      global: {
        plugins: [router]
      }
    })

    const input = wrapper.find('input')
    expect(input.attributes('placeholder')).toContain('搜索...')
  })

  it('应该显示搜索图标', () => {
    const wrapper = mount(SearchBar, {
      global: {
        plugins: [router]
      }
    })

    const searchIcon = wrapper.find('svg')
    expect(searchIcon.exists()).toBe(true)
  })

  it('点击搜索按钮应该触发search事件', async () => {
    const wrapper = mount(SearchBar, {
      props: {
        modelValue: '测试'
      },
      global: {
        plugins: [router]
      }
    })

    const button = wrapper.find('button')
    await button.trigger('click')

    expect(wrapper.emitted('search')).toBeTruthy()
  })
})
