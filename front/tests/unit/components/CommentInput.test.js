import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import CommentInput from '@/components/common/CommentInput.vue'

describe('CommentInput组件测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该使用默认props正确渲染组件', () => {
    const wrapper = mount(CommentInput)

    // 检查组件是否渲染
    expect(wrapper.find('.comment-input').exists()).toBe(true)
    // 检查textarea是否存在
    const textarea = wrapper.find('textarea')
    expect(textarea.exists()).toBe(true)
    // 检查初始状态下actions不显示（因为未聚焦且内容为空）
    expect(wrapper.find('.input-actions').exists()).toBe(false)
  })

  it('textarea存在并且可以输入内容', async () => {
    const wrapper = mount(CommentInput)
    const textarea = wrapper.find('textarea')

    // 模拟输入
    await textarea.setValue('这是一条评论')
    expect(wrapper.vm.content).toBe('这是一条评论')
  })

  it('应该显示字数统计元素', () => {
    const wrapper = mount(CommentInput, {
      props: { maxLength: 500 }
    })

    // CommentInput passes show-word-limit to el-input, verify textarea has maxlength attribute
    const textarea = wrapper.find('textarea')
    expect(textarea.attributes('maxlength')).toBe('500')
  })

  it('内容为空时提交按钮应该禁用', async () => {
    const wrapper = mount(CommentInput, {
      props: { showCancel: true }
    })

    // 先聚焦以显示按钮区域
    await wrapper.find('textarea').trigger('focus')
    await wrapper.vm.$nextTick()

    const submitBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(btn => btn.props('type') === 'primary')
    expect(submitBtn.props('disabled')).toBe(true)
  })

  it('内容不为空时提交按钮应该启用', async () => {
    const wrapper = mount(CommentInput, {
      props: { showCancel: true }
    })

    const textarea = wrapper.find('textarea')
    await textarea.setValue('测试评论内容')

    // 聚焦以显示按钮区域
    await textarea.trigger('focus')
    await wrapper.vm.$nextTick()

    const submitBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(btn => btn.props('type') === 'primary')
    expect(submitBtn.props('disabled')).toBe(false)
  })

  it('提交时应该触发submit事件并传递内容', async () => {
    const wrapper = mount(CommentInput)
    const textarea = wrapper.find('textarea')

    await textarea.setValue('这是一条测试评论')
    await textarea.trigger('focus')
    await wrapper.vm.$nextTick()

    // 获取提交按钮并点击
    const submitBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(btn => btn.props('type') === 'primary')
    await submitBtn.trigger('click')

    expect(wrapper.emitted('submit')).toBeTruthy()
    expect(wrapper.emitted('submit')[0]).toEqual(['这是一条测试评论'])
  })

  it('字数超过maxLength时应该被限制', async () => {
    const maxLength = 10
    const wrapper = mount(CommentInput, {
      props: { maxLength }
    })

    const textarea = wrapper.find('textarea')
    // 检查maxlength属性是否设置
    expect(textarea.attributes('maxlength')).toBe(String(maxLength))
  })

  it('点击表情按钮应该插入表情到内容中', async () => {
    const wrapper = mount(CommentInput)

    // 聚焦以显示操作区域
    await wrapper.find('textarea').trigger('focus')
    await wrapper.vm.$nextTick()

    // 通过文本查找表情按钮
    const emojiBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(btn => btn.text().includes('表情'))
    await emojiBtn.trigger('click')
    await wrapper.vm.$nextTick()

    // 检查表情选择器是否显示
    expect(wrapper.find('.emoji-picker').exists()).toBe(true)

    // 点击第一个表情
    const emojiItem = wrapper.find('.emoji-item')
    const emoji = emojiItem.text().trim()
    await emojiItem.trigger('click')
    await wrapper.vm.$nextTick()

    // 检查表情是否被插入到内容中
    expect(wrapper.vm.content).toContain(emoji)
  })

  it('showCancel为true时点击取消按钮应该触发cancel事件', async () => {
    const wrapper = mount(CommentInput, {
      props: { showCancel: true }
    })

    // 先聚焦以显示按钮区域
    await wrapper.find('textarea').trigger('focus')
    await wrapper.vm.$nextTick()

    // 找到取消按钮并点击（通过文本内容匹配）
    const cancelBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(btn => btn.text().includes('取消'))
    await cancelBtn.trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('应该显示自定义placeholder', () => {
    const customPlaceholder = '写下你的想法...'
    const wrapper = mount(CommentInput, {
      props: { placeholder: customPlaceholder }
    })

    const textarea = wrapper.find('textarea')
    expect(textarea.attributes('placeholder')).toBe(customPlaceholder)
  })

  it('应该应用自定义rows到textarea', () => {
    const wrapper = mount(CommentInput, {
      props: { rows: 5 }
    })

    const textarea = wrapper.find('textarea')
    expect(textarea.attributes('rows')).toBe('5')
  })

  it('提交空内容时不应该触发submit事件', async () => {
    const wrapper = mount(CommentInput)
    const textarea = wrapper.find('textarea')

    await textarea.setValue('   ') // 只有空格
    await textarea.trigger('focus')
    await wrapper.vm.$nextTick()

    const submitBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(btn => btn.props('type') === 'primary')
    await submitBtn.trigger('click')

    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('提交后应该清空内容', async () => {
    const wrapper = mount(CommentInput)
    const textarea = wrapper.find('textarea')

    await textarea.setValue('测试评论')
    await textarea.trigger('focus')
    await wrapper.vm.$nextTick()

    const submitBtn = wrapper.findAllComponents({ name: 'ElButton' }).find(btn => btn.props('type') === 'primary')
    await submitBtn.trigger('click')

    // 提交后内容应该被清空
    expect(wrapper.vm.content).toBe('')
  })

  it('聚焦时应该显示操作按钮区域', async () => {
    const wrapper = mount(CommentInput)

    // 初始状态不应该显示操作按钮
    expect(wrapper.find('.input-actions').exists()).toBe(false)

    // 聚焦后应该显示
    await wrapper.find('textarea').trigger('focus')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.input-actions').exists()).toBe(true)
  })

  it('自定义submitText应该显示在提交按钮上', async () => {
    const wrapper = mount(CommentInput, {
      props: {
        showCancel: true,
        submitText: '发布评论'
      }
    })

    await wrapper.find('textarea').trigger('focus')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('发布评论')
  })
})
