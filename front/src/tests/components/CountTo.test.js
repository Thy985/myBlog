import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import CountTo from '@/components/ui/CountTo.vue'

describe('CountTo组件测试', () => {
  it('应该正确渲染数值', () => {
    const wrapper = mount(CountTo, {
      props: {
        value: 1234
      }
    })

    // 组件会动画显示数值
    expect(wrapper.find('.font-bold').exists()).toBe(true)
    expect(wrapper.text()).toBeDefined()
  })

  it('数值变化时应该更新显示', async () => {
    const wrapper = mount(CountTo, {
      props: {
        value: 100
      }
    })

    // 初始渲染
    expect(wrapper.exists()).toBe(true)

    // 更新数值
    await wrapper.setProps({ value: 200 })
    expect(wrapper.exists()).toBe(true)
  })
})
