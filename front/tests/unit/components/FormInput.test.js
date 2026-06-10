import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import FormInput from '@/components/form/FormInput.vue'

describe('FormInput组件测试', () => {
  it('应该正确渲染基础输入框', () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名',
        placeholder: '请输入用户名'
      }
    })

    expect(wrapper.find('label').text()).toContain('用户名')
    expect(wrapper.find('input').attributes('placeholder')).toBe('请输入用户名')
  })

  it('必填字段应该显示星号', () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '邮箱',
        required: true
      }
    })

    expect(wrapper.find('.text-error-color').exists()).toBe(true)
    expect(wrapper.find('.text-error-color').text()).toBe('*')
  })

  it('应该正确处理输入事件', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名'
      }
    })

    const input = wrapper.find('input')
    await input.setValue('testuser')

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['testuser'])
  })

  it('应该在失焦时触发验证', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名',
        required: true
      }
    })

    const input = wrapper.find('input')
    await input.trigger('blur')

    // 应该显示错误提示
    expect(wrapper.find('.form-feedback.is-invalid').exists()).toBe(true)
  })

  it('应该正确验证必填字段', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名',
        required: true
      }
    })

    // 触发失焦
    await wrapper.find('input').trigger('blur')

    // 应该显示错误
    expect(wrapper.find('.form-feedback.is-invalid').text()).toContain('不能为空')
  })

  it('应该正确执行自定义验证规则', async () => {
    const emailRule = v => /.+@.+\..+/.test(v) || '请输入有效的邮箱地址'

    const wrapper = mount(FormInput, {
      props: {
        modelValue: 'invalid-email',
        label: '邮箱',
        type: 'email',
        required: true,
        rules: [emailRule]
      }
    })

    // 触发失焦
    await wrapper.find('input').trigger('blur')

    // 应该显示自定义错误消息
    expect(wrapper.find('.form-feedback.is-invalid').text()).toBe('请输入有效的邮箱地址')
  })

  it('验证成功时应该显示成功图标', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: 'test@example.com',
        label: '邮箱',
        type: 'email',
        required: true,
        rules: [v => /.+@.+\..+/.test(v) || '请输入有效的邮箱地址']
      }
    })

    // 触发失焦
    await wrapper.find('input').trigger('blur')

    // 应该显示成功图标
    expect(wrapper.find('.text-success-color').exists()).toBe(true)
  })

  it('应该显示帮助文本', () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '密码',
        helpText: '密码至少8个字符'
      }
    })

    expect(wrapper.find('.text-text-tertiary').text()).toBe('密码至少8个字符')
  })

  it('应该显示成功提示消息', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: 'validuser',
        label: '用户名',
        required: true,
        successMessage: '用户名可用'
      }
    })

    await wrapper.find('input').trigger('blur')

    expect(wrapper.find('.form-feedback.is-valid').text()).toBe('用户名可用')
  })

  it('禁用状态应该正确工作', () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名',
        disabled: true
      }
    })

    const input = wrapper.find('input')
    expect(input.attributes('disabled')).toBeDefined()
    expect(input.classes()).toContain('input')
  })

  it('应该支持不同的输入类型', () => {
    const types = ['text', 'email', 'password', 'tel', 'number']

    types.forEach(type => {
      const wrapper = mount(FormInput, {
        props: {
          modelValue: '',
          label: '测试',
          type
        }
      })

      expect(wrapper.find('input').attributes('type')).toBe(type)
    })
  })

  it('暴露的validate方法应该正确工作', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名',
        required: true
      }
    })

    // 调用validate方法 - 使用 exposed
    const isValid = wrapper.vm.$.exposed.validate()

    // 等待DOM更新
    await wrapper.vm.$nextTick()

    expect(isValid).toBe(false)
    expect(wrapper.find('.form-feedback.is-invalid').exists()).toBe(true)
  })

  it('暴露的reset方法应该正确工作', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名',
        required: true
      }
    })

    // 先触发验证
    await wrapper.find('input').trigger('blur')
    expect(wrapper.find('.form-feedback.is-invalid').exists()).toBe(true)

    // 调用reset方法
    wrapper.vm.$.exposed.reset()

    // 错误提示应该消失
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.form-feedback.is-invalid').exists()).toBe(false)
  })

  it('应该触发validate事件', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: 'test',
        label: '用户名',
        required: true,
        rules: []
      }
    })

    // 直接调用暴露的validate方法
    const result = wrapper.vm.$.exposed.validate()
    await wrapper.vm.$nextTick()

    // validate返回true表示验证通过
    expect(result).toBe(true)

    // 验证emit已经被捕获
    const events = wrapper.emitted('validate')
    expect(events).toBeTruthy()
    expect(events[0]).toEqual([true])
  })

  it('应该有正确的ARIA属性', async () => {
    const wrapper = mount(FormInput, {
      props: {
        modelValue: '',
        label: '用户名',
        required: true
      }
    })

    await wrapper.find('input').trigger('blur')

    const input = wrapper.find('input')
    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-describedby')).toBeDefined()
  })
})
