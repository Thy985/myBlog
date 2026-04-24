<template>
  <div class="comment-input">
    <div class="input-wrapper">
      <!-- 文本输入框 -->
      <el-input
        v-model="content"
        type="textarea"
        :placeholder="placeholder"
        :rows="rows"
        :maxlength="maxLength"
        show-word-limit
        resize="none"
        @focus="handleFocus"
        @blur="handleBlur"
      />

      <!-- 操作按钮 -->
      <div v-if="isFocused || content" class="input-actions flex justify-between items-center mt-3">
        <!-- 表情和工具 -->
        <div class="tools flex items-center gap-2">
          <el-button
            size="small"
            text
            @click="showEmojiPicker = !showEmojiPicker"
            aria-label="选择表情"
          >
            <el-icon>
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
                <line x1="9" y1="9" x2="9.01" y2="9"/>
                <line x1="15" y1="9" x2="15.01" y2="9"/>
              </svg>
            </el-icon>
            表情
          </el-button>
        </div>

        <!-- 提交和取消 -->
        <div class="actions flex items-center gap-2">
          <el-button 
            v-if="showCancel"
            size="small" 
            @click="handleCancel"
          >
            取消
          </el-button>
          <el-button 
            type="primary" 
            size="small"
            :loading="submitting"
            :disabled="!content.trim()"
            @click="handleSubmit"
          >
            {{ submitText }}
          </el-button>
        </div>
      </div>

      <!-- 表情选择器 -->
      <div v-if="showEmojiPicker" class="emoji-picker mt-2 p-3 bg-white border rounded shadow-lg">
        <div class="emoji-list grid grid-cols-8 gap-2">
          <span
            v-for="emoji in emojiList"
            :key="emoji"
            class="emoji-item text-2xl cursor-pointer hover:bg-gray-100 p-2 rounded text-center"
            @click="insertEmoji(emoji)"
          >
            {{ emoji }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

import logger from '@/utils/logger'
const props = defineProps({
  placeholder: {
    type: String,
    default: '说点什么吧...'
  },
  rows: {
    type: Number,
    default: 3
  },
  maxLength: {
    type: Number,
    default: 500
  },
  submitText: {
    type: String,
    default: '发表评论'
  },
  showCancel: {
    type: Boolean,
    default: false
  },
  parentId: {
    type: Number,
    default: 0
  },
  rootId: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['submit', 'cancel'])

// 状态
const content = ref('')
const isFocused = ref(false)
const submitting = ref(false)
const showEmojiPicker = ref(false)

// 常用表情列表
const emojiList = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
  '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩',
  '😘', '😗', '😚', '😙', '😋', '😛', '😜', '🤪',
  '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐', '🤨',
  '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥',
  '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕',
  '🤢', '🤮', '🤧', '🥵', '🥶', '😵', '🤯', '🤠',
  '🥳', '😎', '🤓', '🧐', '😕', '😟', '🙁', '😮'
]

// 处理焦点
const handleFocus = () => {
  isFocused.value = true
}

const handleBlur = () => {
  // 延迟失焦，避免点击表情时输入框失焦
  setTimeout(() => {
    if (!showEmojiPicker.value) {
      isFocused.value = false
    }
  }, 200)
}

// 插入表情
const insertEmoji = (emoji) => {
  content.value += emoji
  showEmojiPicker.value = false
}

// 提交评论
const handleSubmit = () => {
  if (!content.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  if (content.value.length > props.maxLength) {
    ElMessage.warning(`评论内容不能超过${props.maxLength}个字符`)
    return
  }

  submitting.value = true
  try {
    emit('submit', content.value.trim())
    content.value = ''
    isFocused.value = false
  } catch (error) {
    logger.error('提交评论失败:', error)
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  content.value = ''
  isFocused.value = false
  showEmojiPicker.value = false
  emit('cancel')
}
</script>

<style scoped>
.comment-input {
  width: 100%;
}

.input-wrapper {
  position: relative;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  border-color: var(--border-color);
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-subtle);
}

.input-actions {
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.emoji-picker {
  position: absolute;
  z-index: 100;
  max-width: 400px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.emoji-item {
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.emoji-item:hover {
  transform: scale(1.2);
}
</style>
