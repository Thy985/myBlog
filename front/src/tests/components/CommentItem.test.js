import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import CommentItem from '@/components/common/CommentItem.vue'

// Mock moment to avoid locale issues in tests
vi.mock('moment', () => ({
  default: () => ({
    fromNow: () => '刚刚'
  })
}))

describe('CommentItem组件测试', () => {
  const mockComment = {
    id: 1,
    content: '这是一条测试评论',
    createTime: '2026-02-11 10:00:00',
    avatar: '/avatar.jpg',
    username: 'testuser',
    articleId: 1,
    parentId: null,
    replyCount: 2,
    level: 1
  }

  it('应该正确渲染评论内容', () => {
    const wrapper = mount(CommentItem, {
      props: {
        comment: mockComment
      }
    })

    expect(wrapper.text()).toContain('这是一条测试评论')
  })

  it('应该正确渲染用户名', () => {
    const wrapper = mount(CommentItem, {
      props: {
        comment: mockComment
      }
    })

    expect(wrapper.text()).toContain('testuser')
  })

  it('应该显示回复数量', () => {
    const wrapper = mount(CommentItem, {
      props: {
        comment: mockComment
      }
    })

    expect(wrapper.text()).toContain('2')
  })

  it('评论有回复时应该显示展开按钮', () => {
    const wrapper = mount(CommentItem, {
      props: {
        comment: mockComment
      }
    })

    const toggleButton = wrapper.find('.action-btn:has(span)')
    expect(wrapper.text()).toContain('查看2条回复')
  })

  it('评论无回复时不应该显示展开回复按钮', () => {
    const commentWithoutReplies = {
      ...mockComment,
      replyCount: 0
    }

    const wrapper = mount(CommentItem, {
      props: {
        comment: commentWithoutReplies
      }
    })

    expect(wrapper.text()).not.toContain('查看')
  })
})
