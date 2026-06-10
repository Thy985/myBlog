import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import LoginForm from '@/components/common/LoginForm.vue'

describe('LoginForm组件测试', () => {
  // 通用挂载函数，stub LoginCaptcha子组件（避免异步API调用）
  const mountForm = (options = {}) => {
    return mount(LoginForm, {
      global: {
        stubs: {
          LoginCaptcha: {
            template: '<div class="login-captcha-stub"><input class="captcha-input" /></div>'
          }
        }
      },
      ...options
    })
  }

  describe('渲染测试', () => {
    it('应该使用默认状态正确渲染表单', () => {
      const wrapper = mountForm()

      expect(wrapper.find('.login-form-container').exists()).toBe(true)
      expect(wrapper.find('.login-form').exists()).toBe(true)
      expect(wrapper.findAll('.el-form-item').length).toBeGreaterThanOrEqual(3)
    })

    it('应该包含用户名输入框', () => {
      const wrapper = mountForm()

      const usernameInput = wrapper.find('input[autocomplete="username"]')
      expect(usernameInput.exists()).toBe(true)
    })

    it('应该包含密码输入框', () => {
      const wrapper = mountForm()

      const passwordInput = wrapper.find('input[autocomplete="current-password"]')
      expect(passwordInput.exists()).toBe(true)
    })

    it('应该包含验证码区域', () => {
      const wrapper = mountForm()

      expect(wrapper.find('.login-captcha-stub').exists()).toBe(true)
    })

    it('应该包含"记住我"复选框', () => {
      const wrapper = mountForm()

      expect(wrapper.find('.remember-checkbox').exists()).toBe(true)
      expect(wrapper.text()).toContain('保持登录状态')
    })

    it('应该包含忘记密码链接', () => {
      const wrapper = mountForm()

      const forgotLink = wrapper.find('.forgot-password-link')
      expect(forgotLink.exists()).toBe(true)
      expect(forgotLink.text()).toBe('忘记密码？')
    })

    it('应该包含注册链接区域', () => {
      const wrapper = mountForm()

      const registerItem = wrapper.find('.register-link')
      expect(registerItem.exists()).toBe(true)
      expect(wrapper.text()).toContain('还没有账号？')

      const registerButton = wrapper.find('.register-button')
      expect(registerButton.exists()).toBe(true)
      expect(registerButton.text()).toBe('立即注册')
    })
  })

  describe('表单验证测试', () => {
    it('用户名为空时点击登录应显示验证错误', async () => {
      const wrapper = mountForm()

      const loginButton = wrapper.find('.login-button')
      await loginButton.trigger('click')

      // 等待验证完成
      await wrapper.vm.$nextTick()

      // account 验证器在空值时会报错"此项为必填项"
      expect(wrapper.text()).toContain('此项为必填项')
    })

    it('密码少于6个字符时点击登录应显示验证错误', async () => {
      const wrapper = mountForm()

      // 填入有效的用户名
      const accountInput = wrapper.find('input[autocomplete="username"]')
      await accountInput.setValue('testuser')
      await accountInput.trigger('blur')

      // 填入短密码
      const passwordInput = wrapper.find('input[autocomplete="current-password"]')
      await passwordInput.setValue('123')
      await passwordInput.trigger('blur')

      const loginButton = wrapper.find('.login-button')
      await loginButton.trigger('click')

      await wrapper.vm.$nextTick()

      // password 验证器对长度 < 6 的密码报错
      expect(wrapper.text()).toContain('密码长度至少为6个字符')
    })

    it('填写有效表单信息时点击登录应触发submit事件', async () => {
      const wrapper = mountForm()

      // 填写用户名
      const accountInput = wrapper.find('input[autocomplete="username"]')
      await accountInput.setValue('testuser')

      // 填写密码
      const passwordInput = wrapper.find('input[autocomplete="current-password"]')
      await passwordInput.setValue('password123')

      const loginButton = wrapper.find('.login-button')
      await loginButton.trigger('click')

      await wrapper.vm.$nextTick()

      expect(wrapper.emitted('submit')).toBeTruthy()
      expect(wrapper.emitted('submit')[0][0]).toMatchObject({
        account: 'testuser',
        password: 'password123',
        remember: false
      })
    })
  })

  describe('事件测试', () => {
    it('点击忘记密码链接应触发forgot-password事件', async () => {
      const wrapper = mountForm()

      const forgotLink = wrapper.find('.forgot-password-link')
      await forgotLink.trigger('click')

      expect(wrapper.emitted('forgot-password')).toBeTruthy()
    })

    it('点击注册链接应触发register事件', async () => {
      const wrapper = mountForm()

      const registerButton = wrapper.find('.register-button')
      await registerButton.trigger('click')

      expect(wrapper.emitted('register')).toBeTruthy()
    })
  })

  describe('加载状态测试', () => {
    it('当isLoading为true时，按钮应显示禁用和加载状态', () => {
      const wrapper = mountForm({
        props: {
          isLoading: true
        }
      })

      const loginButton = wrapper.find('.login-button')
      expect(loginButton.attributes('disabled')).toBeDefined()
      expect(wrapper.text()).toContain('登录中...')
    })

    it('当isLoading为false时，按钮应显示正常登录文字', () => {
      const wrapper = mountForm({
        props: {
          isLoading: false
        }
      })

      const loginButton = wrapper.find('.login-button')
      expect(loginButton.attributes('disabled')).toBeUndefined()
      expect(wrapper.text()).toContain('登录')
      expect(wrapper.text()).not.toContain('登录中...')
    })
  })

  describe('键盘快捷键测试', () => {
    it('在表单上按Enter键应触发提交', async () => {
      const wrapper = mountForm()

      // 先填写有效数据
      const accountInput = wrapper.find('input[autocomplete="username"]')
      await accountInput.setValue('testuser')

      const passwordInput = wrapper.find('input[autocomplete="current-password"]')
      await passwordInput.setValue('password123')

      // 在表单上触发 Enter 键
      await wrapper.find('.login-form').trigger('keydown.enter')

      await wrapper.vm.$nextTick()

      expect(wrapper.emitted('submit')).toBeTruthy()
    })
  })

  describe('密码可见性切换测试', () => {
    it('密码输入框应该支持show-password属性', () => {
      const wrapper = mountForm()

      const passwordInput = wrapper.find('input[autocomplete="current-password"]')
      // Element Plus的el-input使用show-password属性时，渲染的input类型为password
      expect(passwordInput.attributes('type')).toBe('password')
    })
  })

  describe('记住我复选框测试', () => {
    it('记住我复选框默认应为未选中状态', () => {
      const wrapper = mountForm()

      // 通过wrapper.vm访问暴露的formData
      // 初始值应该为false
      expect(wrapper.vm.formData.remember).toBe(false)
    })

    it('复选框状态变化应更新表单数据', async () => {
      const wrapper = mountForm()

      const checkbox = wrapper.find('.remember-checkbox input[type="checkbox"]')
      if (checkbox.exists()) {
        await checkbox.setValue(true)
        await wrapper.vm.$nextTick()

        expect(wrapper.vm.formData.remember).toBe(true)
      }
    })
  })
})
