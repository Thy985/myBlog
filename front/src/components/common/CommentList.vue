<template>
  <div class="comment-list">
    <!-- 评论统计 -->
    <div class="comment-header mb-6">
      <h3 class="comment-title">
        评论 <span class="comment-count">({{ total }})</span>
      </h3>
    </div>

    <!-- 评论列表 -->
    <div v-if="comments && comments.length > 0" class="space-y-6">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        @reply="handleReply"
        @like="handleLike"
        @delete="handleDelete"
        @load-replies="handleLoadReplies"
      />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else
      icon="chat"
      title="暂无评论"
      description="快来发表第一条评论吧~"
      action-text="去评论"
      :show-tip="false"
      @action="focusCommentInput"
    />

    <!-- 加载更多 -->
    <div v-if="hasMore" class="text-center mt-8">
      <el-button :loading="loading" @click="loadMore">
        {{ loading ? '加载中...' : '加载更多' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import CommentItem from './CommentItem.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { getCommentList, likeComment, unlikeComment, deleteComment as deleteCommentApi } from '@/api/frontend/comment'

import logger from '@/utils/logger'

interface CommentItemData {
  id: number
  articleId: number
  userId: number
  parentId: number
  rootId: number
  content: string
  likeCount: number
  replyCount: number
  status: number
  device?: string
  createdTime?: string
  articleTitle?: string
  username?: string
  nickname?: string
  avatar?: string
  replyTo?: string
  replies?: CommentItemData[]
  isLiked: boolean
  level: number
}

const props = defineProps<{
  articleId: number
}>()

const emit = defineEmits<{
  (e: 'reply', comment: CommentItemData): void
  (e: 'like', comment: CommentItemData): void
  (e: 'delete', comment: CommentItemData): void
  (e: 'refresh'): void
}>()

const comments = ref<CommentItemData[]>([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 是否还有更多评论
const hasMore = computed(() => {
  return currentPage.value < totalPages.value
})

// 将评论树扁平化为数组，并映射后端字段到前端字段
function flattenComments(commentList: CommentItemData[]): CommentItemData[] {
  const result: CommentItemData[] = []
  for (const comment of commentList) {
    const mappedComment = {
      id: comment.id,
      articleId: comment.articleId,
      userId: comment.userId,
      parentId: comment.parentId,
      rootId: comment.rootId,
      content: comment.content,
      likeCount: comment.likeNum || 0,
      replyCount: comment.replyNum || 0,
      status: comment.status,
      device: comment.device,
      createdTime: comment.createdTime,
      articleTitle: comment.articleTitle,
      username: comment.username,
      nickname: comment.nickname,
      avatar: comment.avatar,
      replyTo: comment.replyToNickname || comment.replyToUsername,
      replies: comment.replies,
      isLiked: comment.isLiked,
      level: comment.parentId === 0 || comment.rootId === 0 ? 1 : 2
    }
    result.push(mappedComment)
    if (comment.replies && comment.replies.length > 0) {
      result.push(...flattenComments(comment.replies))
    }
  }
  return result
}

// 加载评论列表
const loadComments = async (append = false) => {
  loading.value = true
  try {
    const response = await getCommentList(props.articleId, currentPage.value, pageSize.value)

    if (response.code === 200 && response.data) {
      const commentTree = Array.isArray(response.data.list) ? response.data.list : []

      if (append) {
        comments.value.push(...flattenComments(commentTree))
      } else {
        comments.value = flattenComments(commentTree)
      }
      total.value = response.data.total || comments.value.length
    }
  } catch (error) {
    logger.error('加载评论失败:', error)
    ElMessage.error('加载评论失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载更多评论
const loadMore = async () => {
  if (!hasMore.value) return
  currentPage.value++
  await loadComments(true)
}

// 聚焦评论输入框
const focusCommentInput = () => {
  emit('refresh')
}

// 处理回复
const handleReply = (comment: CommentItemData) => {
  emit('reply', comment)
}

// 处理点赞
const handleLike = async (comment: CommentItemData) => {
  const wasLiked = comment.isLiked
  const previousCount = comment.likeCount
  try {
    if (comment.isLiked) {
      await unlikeComment(comment.id)
      comment.likeCount--
    } else {
      await likeComment(comment.id)
      comment.likeCount++
    }
    comment.isLiked = !comment.isLiked
    emit('like', comment)
  } catch (error) {
    comment.isLiked = wasLiked
    comment.likeCount = previousCount
    logger.error('点赞失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

// 处理删除
const handleDelete = async (comment: CommentItemData) => {
  try {
    await deleteCommentApi(comment.id)

    const index = comments.value.findIndex(c => c.id === comment.id)
    if (index > -1) {
      comments.value.splice(index, 1)
      total.value--
    }

    ElMessage.success('评论已删除')
    emit('delete', comment)
  } catch (error) {
    logger.error('删除失败:', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

// 加载回复
const handleLoadReplies = async (comment: CommentItemData) => {
  try {
    const response = await getCommentList({
      articleId: props.articleId,
      page: 1,
      pageSize: 100,
      parentId: comment.id
    })

    if (response.code === 200 && response.data) {
      comment.replies = response.data.list || []
    }
  } catch (error) {
    logger.error('加载回复失败:', error)
    ElMessage.error('加载回复失败')
  }
}

// 刷新评论列表
const refresh = () => {
  currentPage.value = 1
  loadComments()
}

// 初始化加载
loadComments()

// 暴露方法给父组件
defineExpose({
  refresh,
  loadComments
})
</script>

<style scoped>
.comment-list {
  width: 100%;
}

.comment-header {
  border-bottom: 2px solid var(--border-color);
  padding-bottom: 12px;
}

.comment-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-count {
  color: var(--text-muted);
  font-size: 1rem;
}
</style>
