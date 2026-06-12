<template>
  <div class="comment-item">
    <span class="comment-content">{{ comment.content }}</span>
  </div>
</template>

<script>
export default {
  name: 'CommentItem',
  props: ['comment', 'isReply'],
  emits: ['reply', 'like', 'delete', 'load-replies']
}
</script>
