<template>
  <el-card shadow="hover" :body-style="{ padding: '20px' }" class="mb-5 border-1 search-card">
    <div class="search-filter-bar">
      <div class="filter-items">
        <slot name="filters">
          <!-- 默认插槽，用于放置筛选条件 -->
        </slot>
      </div>
      <div class="filter-actions">
        <el-button
          type="primary"
          :icon="Search"
          :loading="loading"
          @click="$emit('search')"
        >
          查询
        </el-button>
        <el-button
          :icon="RefreshRight"
          @click="$emit('reset')"
        >
          重置
        </el-button>
        <slot name="extra-actions">
          <!-- 额外操作按钮插槽 -->
        </slot>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { Search, RefreshRight } from '@element-plus/icons-vue'

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['search', 'reset'])
</script>

<style scoped>
.search-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.search-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.search-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-items {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  flex: 1;
}

.filter-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.filter-actions :deep(.el-button) {
  transition: all 0.2s ease;
}

.filter-actions :deep(.el-button:hover) {
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .search-filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-items {
    justify-content: stretch;
  }

  .filter-actions {
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>
