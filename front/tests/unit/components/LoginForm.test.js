import { mount, config } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import LoginForm from '@/components/common/LoginForm.vue'

describe('LoginForm组件测试', () => {
  // 创建带表单验证能力的 ElForm stub
  const createElFormStub = () => ({
    name: 'ElForm',
    props: ['model', 'rules', 'labelPosition'],
    template: '<form class="el-form"><slot></slot></form>',
    setup(props) {
      const validate = async () => {
        // 简单验证检查: 如果 model 的 account 或 password 为空，则拒绝
        if (props.model && !props.model.account) {
          throw new Error('validation failed')
        }
        if (props.model && !props.model.password) {
          throw new Error('validation failed')
        }
        if (props.model && props.model.password && props.model.password.length < 6) {
          throw new Error('validation failed')
        }
        return true
      }
      const resetFields = () => {}
      return { validate, resetFields }
    }
  })

  // 通用挂载函数
  const mountForm = (options = {}) => {
    return mount(LoginForm, {
      global: {
        stubs: {
          ElForm: createElFormStub(),
          ElFormItem: {
            name: 'ElFormItem',
            props: ['prop', 'label'],
            template: '<div class="el-form-item"><slot></slot></div>'
          },
          ElInput: {
            name: 'ElInput',
            props: ['modelValue', 'type', 'placeholder', 'size', 'prefixIcon', 'tabindex', 'autocomplete', 'showPassword'],
            emits: ['update:modelValue'],
            template: '<div class="el-input"><input :type="type || \'text\'" :autocomplete="autocomplete" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" /></div>'
          },
          LoginCaptcha: {
            template: '<div class="login-captcha-stub"><input class="captcha-input" /></div>'
          },
          ElCheckbox: {
            name: 'ElCheckbox',
            props: ['modelValue'],
            emits: ['update:modelValue'],
            template: '<label class="el-checkbox"><input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" /><slot></slot></label>'
          },
          ElLink: {
            name: 'ElLink',
            props: ['type'],
            template: '<a class="el-link" :class="`el-link--${type || \'default\'}`"><slot></slot></a>'
          },
          ElButton: {
            name: 'ElButton',
            props: ['type', 'loading', 'disabled', 'size'],
            template: '<button class="el-button" :class="`el-button--${type || \'default\'}`" :disabled="disabled || loading"><slot></slot></button>'
          },
          ElIcon: {
            name: 'ElIcon',
            template: '<span class="el-icon"><slot></slot></span>'
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
  })

  describe('表单验证测试', () => {
    it('用户名为空时点击登录不应触发submit事件', async () => {
      const wrapper = mountForm()
      const loginButton = wrapper.find('.login-button')
      await loginButton.trigger('click')
      await wrapper.vm.$nextTick()
      // 验证失败时不会emit submit
      expect(wrapper.emitted('submit')).toBeFalsy()
    })

    it('密码少于6个字符时点击登录不应触发submit事件', async () => {
      const wrapper = mountForm()
      wrapper.vm.formData.account = 'testuser'
      wrapper.vm.formData.password = '123'
      await wrapper.vm.$nextTick()

      const loginButton = wrapper.find('.login-button')
      await loginButton.trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.emitted('submit')).toBeFalsy()
    })

    it('填写有效表单信息时点击登录应触发submit事件', async () => {
      const wrapper = mountForm()
      wrapper.vm.formData.account = 'testuser'
      wrapper.vm.formData.password = 'password123'
      await wrapper.vm.$nextTick()

      const loginButton = wrapper.find('.login-button')
      await loginButton.trigger('click')
      await wrapper.vm.$nextTick()

      expect(wrapper.emitted('submit')).toBeTruthy()
      expect(wrapper.emitted('submit')[0][0]).toMatchObject({
        account: 'testuser',
        password: 'password123'
      })
    })
  })

  describe('事件测试', () => {
    it('点击忘记密码链接应触发forgot-password事件', async () => {
      const wrapper = mountForm()
      await wrapper.find('.forgot-password-link').trigger('click')
      expect(wrapper.emitted('forgot-password')).toBeTruthy()
    })

    it('点击注册链接应触发register事件', async () => {
      const wrapper = mountForm()
      await wrapper.find('.register-button').trigger('click')
      expect(wrapper.emitted('register')).toBeTruthy()
    })
  })

  describe('加载状态测试', () => {
    it('当isLoading为true时，按钮应显示禁用和加载状态', () => {
      const wrapper = mountForm({
        props: { isLoading: true }
      })
      const loginButton = wrapper.find('.login-button')
      expect(loginButton.attributes('disabled')).toBeDefined()
      expect(wrapper.text()).toContain('登录中...')
    })

    it('当isLoading为false时，按钮应显示正常登录文字', () => {
      const wrapper = mountForm({
        props: { isLoading: false }
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
      wrapper.vm.formData.account = 'testuser'
      wrapper.vm.formData.password = 'password123'
      await wrapper.vm.$nextTick()
      await wrapper.find('.login-form').trigger('keydown.enter')
      await wrapper.vm.$nextTick()
      expect(wrapper.emitted('submit')).toBeTruthy()
    })
  })

  describe('密码可见性测试', () => {
    it('密码输入框组件应该存在', () => {
      const wrapper = mountForm()
      const inputs = wrapper.findAllComponents({ name: 'ElInput' })
      // 应该有3个input: account, password, captcha(stubbed differently)
      expect(inputs.length).toBeGreaterThanOrEqual(2)
    })
  })

  describe('记住我复选框测试', () => {
    it('记住我复选框默认应为未选中状态', () => {
      const wrapper = mountForm()
      expect(wrapper.vm.formData.remember).toBe(false)
    })

    it('复选框状态变化应更新表单数据', async () => {
      const wrapper = mountForm()
      const checkbox = wrapper.find('.el-checkbox input[type="checkbox"]')
      expect(checkbox.exists()).toBe(true)
      await checkbox.setValue(true)
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.formData.remember).toBe(true)
    })
  })
})
