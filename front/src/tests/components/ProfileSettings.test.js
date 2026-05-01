import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ProfileSettings from '@/components/common/ProfileSettings.vue'

describe('ProfileSettings组件测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('表单渲染', () => {
    it('应该正确渲染用户名、邮箱和简介输入框', () => {
      const wrapper = mount(ProfileSettings, {
        props: {
          username: 'testuser',
          email: 'test@example.com',
          bio: '这是一段测试简介'
        }
      })

      expect(wrapper.find('#username').exists()).toBe(true)
      expect(wrapper.find('#email').exists()).toBe(true)
      expect(wrapper.find('#bio').exists()).toBe(true)
    })

    it('应该正确渲染必填标记星号', () => {
      const wrapper = mount(ProfileSettings, {
        props: {}
      })

      const spans = wrapper.findAll('span')
      const asteriskSpan = spans.find(span => span.text() === '*')
      expect(asteriskSpan).toBeDefined()
    })
  })

  describe('表单验证', () => {
    it('空用户名应该显示错误', async () => {
      const wrapper = mount(ProfileSettings, {
        props: {}
      })

      await wrapper.find('#username').setValue('')
      await wrapper.vm.$nextTick()

      // 等待防抖
      await new Promise(resolve => setTimeout(resolve, 400))

      expect(wrapper.vm.errors.username).toBeTruthy()
    })

    it('用户名少于2字符应该显示错误', async () => {
      const wrapper = mount(ProfileSettings, {
        props: {}
      })

      await wrapper.find('#username').setValue('a')
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 400))

      expect(wrapper.vm.errors.username).toBe('用户名至少2个字符')
    })

    it('无效邮箱应该显示错误', async () => {
      const wrapper = mount(ProfileSettings, {
        props: {}
      })

      await wrapper.find('#email').setValue('invalid-email')
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 400))

      expect(wrapper.vm.errors.email).toBe('请输入有效的邮箱地址')
    })
  })

  describe('重置功能', () => {
    it('点击取消按钮应该重置表单到初始值', async () => {
      const wrapper = mount(ProfileSettings, {
        props: {
          username: 'original',
          email: 'original@test.com',
          bio: 'original bio'
        }
      })

      // 修改表单
      await wrapper.find('#username').setValue('changed')
      await wrapper.find('#email').setValue('changed@test.com')
      await wrapper.find('#bio').setValue('changed bio')

      // 点击取消
      await wrapper.find('button[type="button"]').trigger('click')
      await wrapper.vm.$nextTick()

      // 验证重置
      expect(wrapper.vm.formData.username).toBe('original')
      expect(wrapper.vm.formData.email).toBe('original@test.com')
      expect(wrapper.vm.formData.bio).toBe('original bio')
    })

    it('取消按钮应该清空错误状态', async () => {
      const wrapper = mount(ProfileSettings, {
        props: {}
      })

      // 触发验证错误
      await wrapper.find('#username').setValue('')
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 400))

      expect(wrapper.vm.errors.username).toBeTruthy()

      // 点击取消
      await wrapper.find('button[type="button"]').trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.errors.username).toBe('')
    })
  })

  describe('Props响应式', () => {
    it('Props变化应该更新表单数据', async () => {
      const wrapper = mount(ProfileSettings, {
        props: {
          username: 'initial',
          email: 'initial@test.com',
          bio: 'initial bio'
        }
      })

      await wrapper.setProps({
        username: 'updated',
        email: 'updated@test.com',
        bio: 'updated bio'
      })
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.username).toBe('updated')
      expect(wrapper.vm.formData.email).toBe('updated@test.com')
      expect(wrapper.vm.formData.bio).toBe('updated bio')
    })
  })
})