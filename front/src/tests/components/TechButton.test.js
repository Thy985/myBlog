import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import TechButton from '@/components/tech/TechButton.vue'

describe('TechButton组件测试', () => {
  it('应该正确渲染按钮文本', () => {
    const wrapper = mount(TechButton, {
      slots: {
        default: '点击我'
      }
    })

    expect(wrapper.text()).toContain('点击我')
  })

  it('应该支持点击事件', async () => {
    const wrapper = mount(TechButton, {
      slots: { default: '点击我' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('应该支持disabled状态', () => {
    const wrapper = mount(TechButton, {
      props: {
        disabled: true
      },
      slots: { default: '点击我' }
    })

    const button = wrapper.find('button')
    expect(button.attributes('disabled')).toBeDefined()
  })

  it('disabled状态下点击不触发事件', async () => {
    const wrapper = mount(TechButton, {
      props: {
        disabled: true
      },
      slots: { default: '点击我' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })

  it('应该支持loading状态', () => {
    const wrapper = mount(TechButton, {
      props: {
        loading: true
      },
      slots: { default: '加载中' }
    })

    expect(wrapper.find('svg.animate-spin').exists()).toBe(true)
  })

  it('loading状态下点击不触发事件', async () => {
    const wrapper = mount(TechButton, {
      props: {
        loading: true
      },
      slots: { default: '加载中' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })

  it('应该支持不同的尺寸', () => {
    const wrapperSmall = mount(TechButton, {
      props: {
        size: 'sm'
      },
      slots: { default: '小按钮' }
    })

    const wrapperLarge = mount(TechButton, {
      props: {
        size: 'lg'
      },
      slots: { default: '大按钮' }
    })

    expect(wrapperSmall.find('button').classes()).toContain('px-3')
    expect(wrapperLarge.find('button').classes()).toContain('px-6')
  })

  it('应该支持发光效果', () => {
    const wrapper = mount(TechButton, {
      props: {
        glow: true
      },
      slots: { default: '发光按钮' }
    })

    expect(wrapper.find('button').classes()).toContain('glow-border')
  })

  it('应该支持主题变体', () => {
    const wrapper = mount(TechButton, {
      props: {
        variant: 'secondary'
      },
      slots: { default: '紫色主题' }
    })

    expect(wrapper.find('button').classes()).toContain('tech-btn-secondary')
  })

  it('应该渲染为button元素', () => {
    const wrapper = mount(TechButton, {
      slots: { default: '测试' }
    })

    expect(wrapper.find('button').exists()).toBe(true)
  })
})
