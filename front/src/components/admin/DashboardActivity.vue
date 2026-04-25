<template>
  <el-row :gutter="20" class="mb-6">
    <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :offset="0">
      <el-card shadow="never" class="activity-card border-1 rounded-xl">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-semibold">最近文章</span>
            <el-button type="text" @click="navigateTo('/admin/articles')">查看全部</el-button>
          </div>
        </template>
        <div v-if="recentArticles.length > 0">
          <el-table :data="recentArticles" style="width: 100%" size="small" class="activity-table">
            <el-table-column prop="title" label="标题" min-width="200">
              <template #default="scope">
                <span class="article-title">{{ scope.row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发布时间" width="120"></el-table-column>
            <el-table-column prop="readCount" label="浏览量" width="80">
              <template #default="scope">
                <span class="read-count">{{ scope.row.readCount || scope.row.viewCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button type="text" size="small" @click="navigateTo(`/admin/articles/edit/${scope.$index + 1}`)">
                  编辑
                </el-button>
                <el-button type="text" size="small" class="text-blue-500" @click="viewArticle(scope.row.title)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-state py-12 flex flex-col items-center justify-center">
          <el-icon class="text-4xl text-gray-400 mb-4"><Document /></el-icon>
          <p class="text-gray-500">暂无文章数据</p>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :offset="0">
      <el-card shadow="never" class="activity-card border-1 rounded-xl">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-semibold">最近评论</span>
            <el-button type="text" @click="navigateTo('/admin/comments')">查看全部</el-button>
          </div>
        </template>
        <div v-if="recentComments.length > 0">
          <el-table :data="recentComments" style="width: 100%" size="small" class="activity-table">
            <el-table-column prop="content" label="内容" min-width="200">
              <template #default="scope">
                <span class="comment-content">{{ scope.row.content }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="120"></el-table-column>
            <el-table-column prop="userName" label="用户" width="80">
              <template #default="scope">
                <span class="user-name">{{ scope.row.userName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button type="text" size="small" @click="replyComment(scope.row.userName)">
                  回复
                </el-button>
                <el-button type="text" size="small" class="text-blue-500" @click="viewComment(scope.row.content)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-state py-12 flex flex-col items-center justify-center">
          <el-icon class="text-4xl text-gray-400 mb-4"><ChatLineRound /></el-icon>
          <p class="text-gray-500">暂无评论数据</p>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { showMessage } from '@/utils'

defineProps({
  recentArticles: {
    type: Array,
    default: () => []
  },
  recentComments: {
    type: Array,
    default: () => []
  }
})

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}

const viewArticle = (title) => {
  showMessage(`查看文章：${title}`, 'info')
}

const replyComment = (userName) => {
  showMessage(`回复用户：${userName}`, 'info')
}

const viewComment = (content) => {
  showMessage(`查看评论：${content}`, 'info')
}
</script>

<style scoped>
.activity-card {
  border-radius: 12px;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  background-color: var(--background-light);
}

.activity-table {
  border-radius: 8px;
  overflow: hidden;
}

.activity-table :deep(.el-table__header-wrapper) {
  background-color: var(--background-light);
}

.activity-table :deep(.el-table__row:hover) {
  background-color: var(--border-color);
}

.article-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.article-title:hover {
  color: var(--primary-color);
}

.read-count {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
}

.comment-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
}
</style>
