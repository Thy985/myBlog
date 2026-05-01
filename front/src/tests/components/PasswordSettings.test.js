import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PasswordSettings from '@/components/common/PasswordSettings.vue'

describe('PasswordSettings组件测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('表单渲染', () => {
    it('应该正确渲染当前密码、新密码和确认密码输入框', () => {
      const wrapper = mount(PasswordSettings, {
        props: {}
      })

      expect(wrapper.find('input[placeholder="请输入当前密码"]').exists()).toBe(true)
      expect(wrapper.find('input[placeholder="请输入新密码"]').exists()).toBe(true)
      expect(wrapper.find('input[placeholder="请再次输入新密码"]').exists()).toBe(true)
    })

    it('应该渲染密码显示/隐藏切换按钮', () => {
      const wrapper = mount(PasswordSettings, {
        props: {}
      })

      const toggleButtons = wrapper.findAll('button[type="button"]')
      expect(toggleButtons.length).toBeGreaterThanOrEqual(3)
    })

    it('应该渲染提交和取消按钮', () => {
      const wrapper = mount(PasswordSettings, {
        props: {}
      })

      expect(wrapper.find('button[type="submit"]').exists()).toBe(true)
      expect(wrapper.find('button[type="button"]').exists()).toBe(true)
    })
  })

  describe('密码强度指示器', () => {
    it('空密码不显示强度指示器', () => {
      const wrapper = mount(PasswordSettings, {
        props: {}
      })

      expect(wrapper.find('.flex.gap-1.mb-1').exists()).toBe(false)
    })

    it('输入密码后显示强度指示器', async () => {
      const wrapper = mount(PasswordSettings, {
        props: {}
      })

      await wrapper.find('input[placeholder="请输入新密码"]').setValue('Test1234')
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.flex.gap-1.mb-1').exists()).toBe(true)
    })

    it('强度文本应该正确显示', async () => {
      const wrapper = mount(PasswordSettings, {
        props: {}
      })

      // 使用8字符但无其他复杂度要求的密码，强度为1（太弱）
      await wrapper.find('input[placeholder="请输入新密码"]').setValue('abcdefgh')
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.strengthText).toBe('太弱')
      expect(wrapper.vm.strength).toBe(1)
    })
  })

  describe('密码可见性切换', () => {
    it('点击切换按钮应该改变输入类型', async () => {
      const wrapper = mount(PasswordSettings, {
        props: {}
      })

      const currentInput = wrapper.find('input[placeholder="请输入当前密码"]')
      expect(currentInput.attributes('type')).toBe('password')

      const toggleButtons = wrapper.findAll('button[type="button"]')
      await toggleButtons[0].trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.find('input[placeholder="请输入当前密码"]').attributes('type')).toBe('text')
    })
  })

  describe('Loading状态', () => {
    it('loading时提交按钮应该禁用', async () => {
      const wrapper = mount(PasswordSettings, {
        props: { loading: true }
      })

      const submitButton = wrapper.find('button[type="submit"]')
      expect(submitButton.attributes('disabled')).toBeDefined()
      expect(submitButton.text()).toBe('修改中...')
    })
  })

  describe('Props响应式', () => {
    it('loading变化应该更新按钮文本', async () => {
      const wrapper = mount(PasswordSettings, {
        props: { loading: false }
      })

      expect(wrapper.find('button[type="submit"]').text()).toBe('修改密码')

      await wrapper.setProps({ loading: true })
      await wrapper.vm.$nextTick()

      expect(wrapper.find('button[type="submit"]').text()).toBe('修改中...')
    })
  })
})