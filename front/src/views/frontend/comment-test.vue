<template>
  <div class="comment-test-page min-h-screen bg-gray-50 py-8">
    <div class="container mx-auto max-w-4xl px-4">
      <!-- 页面标题 -->
      <div class="bg-white rounded-lg shadow-sm p-6 mb-6">
        <h1 class="text-2xl font-bold text-gray-900 mb-2">评论系统测试页面</h1>
        <p class="text-gray-600">测试文章ID: {{ articleId }}</p>
        <div class="mt-4 flex gap-4">
          <el-button type="primary" @click="testPublish">测试发布评论</el-button>
          <el-button @click="refreshComments">刷新评论列表</el-button>
          <el-button type="danger" @click="clearTest">清空测试</el-button>
        </div>
      </div>

      <!-- 评论模块 -->
      <div class="bg-white rounded-lg shadow-sm p-6">
        <!-- 发表评论 -->
        <div class="mb-8">
          <h3 class="text-xl font-bold text-gray-900 mb-4">发表评论</h3>
          <CommentInput
            placeholder="写下你的评论..."
            submit-text="发表评论"
            :show-cancel="false"
            @submit="handleCommentSubmit"
          />
        </div>

        <!-- 评论列表 -->
        <CommentList
          ref="commentListRef"
          :article-id="articleId"
          @reply="handleCommentReply"
          @like="handleCommentLike"
          @delete="handleCommentDelete"
        />
      </div>

      <!-- 测试日志 -->
      <div class="bg-white rounded-lg shadow-sm p-6 mt-6">
        <h3 class="text-xl font-bold text-gray-900 mb-4">测试日志</h3>
        <div class="space-y-2 max-h-96 overflow-y-auto">
          <div
            v-for="(log, index) in testLogs"
            :key="index"
            class="p-3 rounded"
            :class="{
              'bg-green-50 text-green-800': log.type === 'success',
              'bg-red-50 text-red-800': log.type === 'error',
              'bg-blue-50 text-blue-800': log.type === 'info'
            }"
          >
            <div class="flex justify-between items-start">
              <span class="font-mono text-sm">{{ log.message }}</span>
              <span class="text-xs text-gray-500">{{ log.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElButton } from 'element-plus'
import CommentList from '@/components/common/CommentList.vue'
import CommentInput from '@/components/common/CommentInput.vue'
import { publishComment } from '@/api/frontend/comment'

import logger from '@/utils/logger'
// 测试用的文章ID
const articleId = ref(1)

// 评论列表引用
const commentListRef = ref(null)

// 测试日志
const testLogs = ref([])

// 添加日志
const addLog = (message, type = 'info') => {
  testLogs.value.unshift({
    message,
    type,
    time: new Date().toLocaleTimeString()
  })
}

// 处理评论提交
const handleCommentSubmit = async (content) => {
  addLog(`尝试发布评论: ${content.substring(0, 20)}...`, 'info')
  
  try {
    const response = await publishComment({
      articleId: articleId.value,
      content: content,
      parentId: 0
    })
    
    if (response.code === 200) {
      ElMessage.success('评论发表成功')
      addLog('评论发表成功', 'success')
      
      // 刷新评论列表
      commentListRef.value?.refresh()
    } else {
      ElMessage.error(response.message || '发表评论失败')
      addLog(`发表失败: ${response.message}`, 'error')
    }
  } catch (error) {
    logger.error('发表评论失败:', error)
    ElMessage.error('发表评论失败，请稍后重试')
    addLog(`发表失败: ${error.message}`, 'error')
  }
}

// 处理回复评论
const handleCommentReply = async (comment) => {
  addLog(`尝试回复评论ID: ${comment.id}`, 'info')
  
  try {
    const response = await publishComment({
      articleId: articleId.value,
      content: comment.replyContent,
      parentId: comment.id
    })
    
    if (response.code === 200) {
      ElMessage.success('回复成功')
      addLog('回复成功', 'success')
      commentListRef.value?.refresh()
    } else {
      ElMessage.error(response.message || '回复失败')
      addLog(`回复失败: ${response.message}`, 'error')
    }
  } catch (error) {
    logger.error('回复失败:', error)
    ElMessage.error('回复失败，请稍后重试')
    addLog(`回复失败: ${error.message}`, 'error')
  }
}

// 处理点赞
const handleCommentLike = (comment) => {
  addLog(`点赞评论ID: ${comment.id}, 当前状态: ${comment.isLiked ? '已点赞' : '未点赞'}`, 'info')
}

// 处理删除
const handleCommentDelete = (comment) => {
  addLog(`删除评论ID: ${comment.id}`, 'success')
}

// 测试发布评论
const testPublish = async () => {
  const testContent = `这是一条测试评论 - ${new Date().toLocaleString()}`
  await handleCommentSubmit(testContent)
}

// 刷新评论列表
const refreshComments = () => {
  addLog('手动刷新评论列表', 'info')
  commentListRef.value?.refresh()
}

// 清空测试
const clearTest = () => {
  testLogs.value = []
  addLog('测试日志已清空', 'info')
}

// 初始化日志
addLog('评论系统测试页面已加载', 'success')
</script>

<style scoped>
.comment-test-page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
