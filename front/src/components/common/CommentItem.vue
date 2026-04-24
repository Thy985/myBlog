<template>
  <div class="comment-item">
    <!-- 一级评论 -->
    <div class="comment-main flex gap-3">
      <!-- 头像 -->
      <div class="comment-avatar flex-shrink-0">
        <el-avatar :size="40" :src="comment.avatar">
          {{ comment.username.charAt(0) }}
        </el-avatar>
      </div>

      <!-- 评论内容 -->
      <div class="comment-content flex-1">
        <!-- 用户名和时间 -->
        <div class="comment-meta flex items-center gap-2 mb-2">
          <span class="username font-semibold text-gray-800">{{ comment.username }}</span>
          <span class="time text-sm text-gray-500">{{ formatTime(comment.createdTime) }}</span>
        </div>

        <!-- 评论文本 (使用 sanitizeHtml 防止 XSS) -->
        <div class="comment-text text-gray-700 mb-3">
          <span v-if="comment.replyTo" class="reply-to text-blue-500">
            @{{ comment.replyTo }}
          </span>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <span v-html="sanitizeHtml(comment.content)"></span>
        </div>

        <!-- 操作按钮 -->
        <div class="comment-actions flex items-center gap-4 text-sm">
          <!-- 点赞 -->
          <button
            class="action-btn flex items-center gap-1 hover:text-blue-500 transition-colors"
            :class="{ 'text-blue-500': comment.isLiked }"
            @click="handleLike"
            :aria-label="comment.isLiked ? '取消点赞' : '点赞'"
          >
            <el-icon><Star v-if="!comment.isLiked" /><StarFilled v-else /></el-icon>
            <span>{{ comment.likeCount > 0 ? comment.likeCount : '点赞' }}</span>
          </button>

          <!-- 回复 -->
          <button
            class="action-btn flex items-center gap-1 hover:text-blue-500 transition-colors"
            @click="toggleReply"
            aria-label="回复评论"
          >
            <el-icon><ChatLineRound /></el-icon>
            <span>回复</span>
          </button>

          <!-- 删除(仅自己的评论) -->
          <button
            v-if="canDelete"
            class="action-btn flex items-center gap-1 hover:text-red-500 transition-colors"
            @click="handleDelete"
            aria-label="删除评论"
          >
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </button>

          <!-- 查看回复 -->
          <button
            v-if="comment.replyCount > 0 && comment.level === 1"
            class="action-btn flex items-center gap-1 hover:text-blue-500 transition-colors"
            @click="toggleReplies"
            :aria-label="showReplies ? '收起回复' : '查看回复'"
          >
            <el-icon><ArrowDown v-if="!showReplies" /><ArrowUp v-else /></el-icon>
            <span>{{ showReplies ? '收起' : `查看${comment.replyCount}条回复` }}</span>
          </button>
        </div>

        <!-- 回复输入框 -->
        <div v-if="showReplyInput" class="reply-input mt-3">
          <CommentInput
            :placeholder="`回复 @${comment.username}`"
            :parent-id="comment.id"
            :root-id="comment.rootId || comment.id"
            @submit="handleReplySubmit"
            @cancel="showReplyInput = false"
          />
        </div>

        <!-- 回复列表 -->
        <div v-if="showReplies && comment.replies && comment.replies.length > 0" class="replies mt-4 space-y-4">
          <CommentItem
            v-for="reply in comment.replies"
            :key="reply.id"
            :comment="reply"
            :is-reply="true"
            @reply="$emit('reply', $event)"
            @like="$emit('like', $event)"
            @delete="$emit('delete', $event)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Star, StarFilled, ChatLineRound, Delete, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import CommentInput from './CommentInput.vue'
import { fromNow } from '@/utils/dayjs'
import { sanitizeHtml } from '@/utils/xss'

const props = defineProps({
  comment: {
    type: Object,
    required: true
  },
  isReply: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['reply', 'like', 'delete', 'load-replies'])

// 状态
const showReplyInput = ref(false)
const showReplies = ref(false)

// 是否可以删除(仅自己的评论)
const canDelete = computed(() => {
  // 暂时允许所有用户删除，后续根据用户权限调整
  return true
})

// 格式化时间
const formatTime = (time) => {
  return fromNow(time)
}

// 切换回复输入框
const toggleReply = () => {
  showReplyInput.value = !showReplyInput.value
}

// 切换回复列表
const toggleReplies = () => {
  showReplies.value = !showReplies.value

  // 如果是展开且还没有加载回复，则加载
  if (showReplies.value && (!props.comment.replies || props.comment.replies.length === 0)) {
    emit('load-replies', props.comment)
  }
}

// 处理点赞
const handleLike = () => {
  emit('like', props.comment)
}

// 处理删除
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评论吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    emit('delete', props.comment)
  } catch {
    // 用户取消删除
  }
}

// 处理回复提交
const handleReplySubmit = (content) => {
  emit('reply', {
    ...props.comment,
    replyContent: content
  })
  showReplyInput.value = false
}
</script>

<style scoped>
.comment-item {
  padding: 16px 0;
}

.comment-item:not(:last-child) {
  border-bottom: 1px solid var(--border-color);
}

.comment-avatar {
  cursor: pointer;
}

.username {
  font-size: 15px;
}

.time {
  font-size: 13px;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.reply-to {
  margin-right: 4px;
  cursor: pointer;
}

.reply-to:hover {
  text-decoration: underline;
}

.action-btn {
  color: var(--text-muted);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: background-color 0.2s ease, color 0.2s ease;
}

.action-btn:hover {
  background-color: var(--bg-tertiary);
}

.action-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.replies {
  padding-left: 48px;
  border-left: 2px solid var(--border-color);
}

/* 回复样式 */
.comment-item.is-reply {
  padding: 12px 0;
}
</style>
