import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import CommentList from '@/components/common/CommentList.vue'
import * as commentApi from '@/api/frontend/comment'

// Mock comment API
vi.mock('@/api/frontend/comment', () => ({
  getCommentList: vi.fn(),
  likeComment: vi.fn(),
  unlikeComment: vi.fn(),
  deleteComment: vi.fn()
}))

describe('CommentList组件测试', () => {
  const mockComments = [
    {
      id: 1,
      articleId: 100,
      userId: 1,
      parentId: 0,
      rootId: 0,
      content: '这是一条测试评论',
      likeNum: 5,
      replyNum: 2,
      status: 1,
      createdTime: '2026-02-11',
      username: 'user1',
      nickname: '用户1',
      avatar: '/avatar1.jpg',
      isLiked: false,
      level: 1
    },
    {
      id: 2,
      articleId: 100,
      userId: 2,
      parentId: 0,
      rootId: 0,
      content: '另一条评论',
      likeNum: 3,
      replyNum: 0,
      status: 1,
      createdTime: '2026-02-10',
      username: 'user2',
      nickname: '用户2',
      avatar: '/avatar2.jpg',
      isLiked: true,
      level: 1
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    commentApi.getCommentList.mockResolvedValue({
      code: 200,
      data: {
        list: mockComments,
        total: 2
      }
    })
    commentApi.likeComment.mockResolvedValue({ code: 200 })
    commentApi.unlikeComment.mockResolvedValue({ code: 200 })
    commentApi.deleteComment.mockResolvedValue({ code: 200 })
  })

  it('应该正确渲染有评论的列表', async () => {
    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(commentApi.getCommentList).toHaveBeenCalledWith(100, 1, 10)

    const commentItems = wrapper.findAll('.comment-item')
    expect(commentItems.length).toBeGreaterThan(0)
  })

  it('应该渲染评论条目', async () => {
    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    const commentItems = wrapper.findAll('.comment-item')
    expect(commentItems.length).toBe(2)
    expect(wrapper.text()).toContain('这是一条测试评论')
    expect(wrapper.text()).toContain('另一条评论')
  })

  it('应该正确显示评论数量', async () => {
    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('(2)')
  })

  it('没有评论时应该显示空状态', async () => {
    commentApi.getCommentList.mockResolvedValue({
      code: 200,
      data: { list: [], total: 0 }
    })

    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })

  it('hasMore为true时应该显示加载更多按钮', async () => {
    commentApi.getCommentList.mockResolvedValue({
      code: 200,
      data: { list: mockComments, total: 25 }
    })

    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('加载更多')
  })

  it('点击加载更多应该触发加载', async () => {
    commentApi.getCommentList.mockResolvedValueOnce({
      code: 200,
      data: { list: mockComments, total: 25 }
    })

    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    commentApi.getCommentList.mockResolvedValueOnce({
      code: 200,
      data: {
        list: [{
          id: 3, articleId: 100, userId: 3, parentId: 0, rootId: 0,
          content: '第三条评论', likeNum: 0, replyNum: 0, status: 1,
          createdTime: '2026-02-09', username: 'user3', nickname: '用户3',
          avatar: '/avatar3.jpg', isLiked: false, level: 1
        }],
        total: 25
      }
    })

    const loadMoreBtn = wrapper.find('.el-button')
    await loadMoreBtn.trigger('click')
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(commentApi.getCommentList).toHaveBeenCalledWith(100, 2, 10)
  })

  it('评论点赞应该触发handleLike', async () => {
    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    const commentItem = wrapper.findComponent({ name: 'CommentItem' })
    const commentData = { id: 1, isLiked: false, likeCount: 5 }
    commentItem.vm.$emit('like', commentData)
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(commentApi.likeComment).toHaveBeenCalledWith(1)
  })

  it('评论删除应该触发handleDelete', async () => {
    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    const commentItem = wrapper.findComponent({ name: 'CommentItem' })
    const commentData = { id: 1 }
    commentItem.vm.$emit('delete', commentData)
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(commentApi.deleteComment).toHaveBeenCalledWith(1)
  })

  it('评论回复应该触发handleReply', async () => {
    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    const commentItem = wrapper.findComponent({ name: 'CommentItem' })
    const commentData = { id: 1, content: 'test reply' }
    commentItem.vm.$emit('reply', commentData)
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('reply')).toBeTruthy()
    expect(wrapper.emitted('reply')[0]).toEqual([commentData])
  })

  it('refresh()方法应该重新加载评论', async () => {
    const wrapper = mount(CommentList, {
      props: { articleId: 100 }
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    commentApi.getCommentList.mockClear()

    const vm = wrapper.vm
    vm.refresh()

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(commentApi.getCommentList).toHaveBeenCalledWith(100, 1, 10)
  })
})
