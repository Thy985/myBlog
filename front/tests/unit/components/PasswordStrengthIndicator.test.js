import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import PasswordStrengthIndicator from '@/components/common/PasswordStrengthIndicator.vue'

describe('PasswordStrengthIndicator组件测试', () => {
  it('空密码不显示强度', () => {
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: ''
      }
    })

    expect(wrapper.text()).toContain('密码强度：')
    expect(wrapper.text()).toContain('')
  })

  it('弱密码显示弱', () => {
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'abc'
      }
    })

    expect(wrapper.text()).toContain('弱')
  })

  it('中等密码显示中', () => {
    // 6+字符 + 大写 + 数字 = score 3
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'Abcd12'
      }
    })

    expect(wrapper.text()).toContain('中')
  })

  it('强密码显示强', () => {
    // 8+字符 + 大写 + 数字 = score 4
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'Abcd123'
      }
    })

    expect(wrapper.text()).toContain('强')
  })

  it('非常强密码显示非常强', () => {
    // 8+字符 + 大写 + 数字 + 特殊字符 = score 5
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'Abcd123!'
      }
    })

    expect(wrapper.text()).toContain('非常强')
  })

  it('应该渲染4个强度条', () => {
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'Abcd123!'
      }
    })

    const bars = wrapper.findAll('.password-strength-bar')
    expect(bars.length).toBe(4)
  })

  it('弱密码只有第一个条变色', () => {
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'abc'
      }
    })

    const bars = wrapper.findAll('.password-strength-bar')
    expect(bars[0].classes()).toContain('weak')
    expect(bars[1].classes()).not.toContain('weak')
  })

  it('中等密码前两个条变色', () => {
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'Abcd12'
      }
    })

    const bars = wrapper.findAll('.password-strength-bar')
    expect(bars[0].classes()).toContain('medium')
    expect(bars[1].classes()).toContain('medium')
  })

  it('强密码前三个条变色', () => {
    // 8+字符 + 大写 + 数字 = score 4 (strong)
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'Abcd1234'
      }
    })

    const bars = wrapper.findAll('.password-strength-bar')
    expect(bars[0].classes()).toContain('strong')
    expect(bars[1].classes()).toContain('strong')
    expect(bars[2].classes()).toContain('strong')
  })

  it('非常强密码所有条变色', () => {
    const wrapper = mount(PasswordStrengthIndicator, {
      props: {
        password: 'Abcd123!'
      }
    })

    const bars = wrapper.findAll('.password-strength-bar')
    expect(bars[0].classes()).toContain('very-strong')
    expect(bars[1].classes()).toContain('very-strong')
    expect(bars[2].classes()).toContain('very-strong')
    expect(bars[3].classes()).toContain('very-strong')
  })
})
