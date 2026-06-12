import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import CommentItem from '@/components/common/CommentItem.vue'

vi.mock('moment', () => ({
  default: () => ({
    fromNow: () => '刚刚'
  })
}))

describe('CommentItem组件测试', () => {
  const mockComment = {
    id: 1,
    content: '这是一条测试评论',
    createdTime: '2026-02-11 10:00:00',
    avatar: '/avatar.jpg',
    username: 'testuser',
    articleId: 1,
    parentId: null,
    replyCount: 2,
    level: 1,
    likeCount: 0,
    isLiked: false
  }

  const createWrapper = (comment = mockComment) => {
    return mount(CommentItem, {
      props: { comment }
    })
  }

  it('应该正确渲染评论内容', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('这是一条测试评论')
  })

  it('应该正确渲染用户名', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.username').text()).toBe('testuser')
  })

  it('应该显示回复数量', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).toContain('查看2条回复')
  })

  it('评论有回复时应该显示查看回复按钮', () => {
    const wrapper = createWrapper()
    const buttons = wrapper.findAll('.action-btn')
    const viewRepliesBtn = buttons.find(btn => btn.text().includes('查看'))
    expect(viewRepliesBtn.exists()).toBe(true)
    expect(viewRepliesBtn.text()).toContain('查看2条回复')
  })

  it('评论无回复时不应该显示展开回复按钮', () => {
    const commentWithoutReplies = {
      ...mockComment,
      replyCount: 0
    }
    const wrapper = createWrapper(commentWithoutReplies)
    expect(wrapper.text()).not.toContain('查看')
  })
})
