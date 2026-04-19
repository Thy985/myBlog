<template>
  <div class="article-comments mt-6">
    <!-- 评论输入框 -->
    <CommentInput
      :article-id="articleId"
      class="mb-6"
      @submit="$emit('submit-comment', $event)"
    />

    <!-- 评论列表 -->
    <CommentList
      ref="commentListRef"
      :article-id="articleId"
      @reply="$emit('reply-comment', $event)"
      @like="$emit('like-comment', $event)"
      @delete="$emit('delete-comment', $event)"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import CommentInput from './CommentInput.vue'
import CommentList from './CommentList.vue'

defineProps({
  articleId: {
    type: Number,
    required: true
  }
})

defineEmits(['submit-comment', 'reply-comment', 'like-comment', 'delete-comment'])

const commentListRef = ref(null)

defineExpose({
  refresh: () => commentListRef.value?.refresh()
})
</script>
