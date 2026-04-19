import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import TechBadge from '@/components/tech/TechBadge.vue'

describe('TechBadge组件测试', () => {
  it('应该正确渲染标签文本', () => {
    const wrapper = mount(TechBadge, {
      slots: {
        default: 'Vue3'
      }
    })

    expect(wrapper.text()).toContain('Vue3')
  })

  it('应该支持不同的颜色变体', () => {
    const wrapper = mount(TechBadge, {
      props: {
        variant: 'success'
      },
      slots: { default: '成功' }
    })

    expect(wrapper.classes()).toContain('tech-badge-success')
  })

  it('应该支持尺寸变体', () => {
    const wrapperSm = mount(TechBadge, {
      props: {
        size: 'sm'
      },
      slots: { default: '小' }
    })

    const wrapperLg = mount(TechBadge, {
      props: {
        size: 'lg'
      },
      slots: { default: '大' }
    })

    expect(wrapperSm.find('span').classes()).toContain('px-2')
    expect(wrapperLg.find('span').classes()).toContain('px-3')
  })

  it('点击应该触发click事件', async () => {
    const wrapper = mount(TechBadge, {
      slots: { default: '可点击' }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('应该支持发光效果', () => {
    const wrapper = mount(TechBadge, {
      props: {
        glow: true
      },
      slots: { default: '发光' }
    })

    expect(wrapper.classes()).toContain('tech-badge-glow')
  })

  it('应该支持dot指示器', () => {
    const wrapper = mount(TechBadge, {
      props: {
        dot: true
      },
      slots: { default: '带点' }
    })

    expect(wrapper.find('.badge-dot').exists()).toBe(true)
  })

  it('应该支持图标插槽', () => {
    const wrapper = mount(TechBadge, {
      slots: {
        icon: '<svg class="w-3 h-3"></svg>',
        default: '带图标'
      }
    })

    expect(wrapper.find('.badge-icon').exists()).toBe(true)
  })
})
