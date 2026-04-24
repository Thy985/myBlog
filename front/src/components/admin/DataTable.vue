<template>
  <el-card shadow="hover" class="border-1 data-table-card">
    <!-- 卡片头部插槽 -->
    <template #header>
      <div v-if="$slots.header || title" class="card-header">
        <slot name="header">
          <h3 v-if="title" class="card-title">{{ title }}</h3>
        </slot>
        <slot name="header-actions">
          <!-- 用于放置操作按钮 -->
        </slot>
      </div>
    </template>

    <!-- 统计信息插槽 -->
    <slot name="stats"></slot>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="data"
      stripe
      style="width: 100%"
      :row-class-name="rowClassName"
      @selection-change="$emit('selection-change', $event)"
    >
      <el-table-column
        v-if="showSelection"
        type="selection"
        width="55"
      />

      <slot name="columns">
        <!-- 默认列插槽 -->
      </slot>

      <!-- 操作列 -->
      <el-table-column
        v-if="showActions"
        label="操作"
        :width="actionWidth"
        fixed="right"
      >
        <template #default="scope">
          <slot name="actions" :row="scope.row">
            <el-button
              type="primary"
              size="small"
              @click="$emit('edit', scope.row)"
            >
              <el-icon class="mr-1"><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="$emit('delete', scope.row)"
            >
              <el-icon class="mr-1"><Delete /></el-icon>
              删除
            </el-button>
          </slot>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="showPagination" class="pagination-container">
      <div v-if="showInfo" class="pagination-info">
        共 {{ total }} 条
      </div>
      <el-pagination
        v-model:current-page="modelCurrentPage"
        v-model:page-size="modelPageSize"
        :page-sizes="[10, 20, 50, 100]"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="$emit('size-change', $event)"
        @current-change="$emit('page-change', $event)"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  // 表格数据
  data: {
    type: Array,
    default: () => []
  },
  // 加载状态
  loading: {
    type: Boolean,
    default: false
  },
  // 标题
  title: {
    type: String,
    default: ''
  },
  // 是否显示选择框
  showSelection: {
    type: Boolean,
    default: false
  },
  // 是否显示操作列
  showActions: {
    type: Boolean,
    default: true
  },
  // 操作列宽度
  actionWidth: {
    type: [String, Number],
    default: 200
  },
  // 是否显示分页
  showPagination: {
    type: Boolean,
    default: true
  },
  // 是否显示分页信息
  showInfo: {
    type: Boolean,
    default: true
  },
  // 当前页码
  currentPage: {
    type: Number,
    default: 1
  },
  // 每页数量
  pageSize: {
    type: Number,
    default: 10
  },
  // 总数
  total: {
    type: Number,
    default: 0
  },
  // 行样式
  rowClassName: {
    type: [Function, String],
    default: ''
  }
})

const modelCurrentPage = ref(props.currentPage)
const modelPageSize = ref(props.pageSize)

watch(() => props.currentPage, (val) => {
  modelCurrentPage.value = val
})

watch(() => props.pageSize, (val) => {
  modelPageSize.value = val
})

defineEmits([
  'edit',
  'delete',
  'selection-change',
  'size-change',
  'page-change'
])
</script>

<style scoped>
.data-table-card {
  border-radius: 8px;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.data-table-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
  flex-wrap: wrap;
  gap: 12px;
}

.pagination-info {
  color: var(--text-secondary);
  font-size: 14px;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-container {
    justify-content: center;
  }
}
</style>
