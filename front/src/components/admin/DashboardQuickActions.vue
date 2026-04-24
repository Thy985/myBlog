<template>
  <div class="quick-actions mb-6">
    <el-card shadow="never" class="border-1 rounded-xl">
      <div class="flex flex-wrap gap-4">
        <el-button type="primary" icon="Document" class="quick-action-btn" @click="navigateTo('/admin/articles/create')">
          发布文章
        </el-button>
        <el-button type="success" icon="FolderOpened" class="quick-action-btn" @click="navigateTo('/admin/categories')">
          管理分类
        </el-button>
        <el-button type="warning" icon="PriceTag" class="quick-action-btn" @click="navigateTo('/admin/tags')">
          管理标签
        </el-button>
        <el-button type="info" icon="ChatLineRound" class="quick-action-btn" @click="navigateTo('/admin/comments')">
          管理评论
        </el-button>
        <el-button type="default" icon="Setting" class="quick-action-btn" @click="navigateTo('/admin/blog-setting')">
          系统设置
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.quick-actions {
  margin-bottom: 24px;
}

.quick-action-btn {
  border-radius: 8px;
  font-weight: 500;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.quick-action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .quick-action-btn {
    font-size: 12px;
    padding: 8px 12px;
  }
}
</style>
