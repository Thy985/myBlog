<template>
  <el-row :gutter="20" class="mb-6">
    <el-col v-for="(stat, index) in stats" :key="index" :xs="12" :sm="8" :md="6" :lg="4" :xl="4" :offset="0">
      <el-card shadow="never" :class="['stat-card border-1 hover:shadow-md transition-all duration-300', index === 0 && 'stat-card--primary']">
        <div class="flex items-center">
          <div :class="['mr-4 stat-icon', stat.iconBg, stat.iconColor] + ' rounded-xl w-12 h-12 flex items-center justify-center'">
            <el-icon class="text-xl"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="flex-1">
            <div class="text-gray-500 text-sm mb-1">{{ stat.label }}</div>
            <div class="flex items-end justify-between">
              <CountTo :value="stat.value" :class="['text-xl font-bold', index === 0 && 'text-2xl']"></CountTo>
              <span v-if="stat.change !== undefined" :class="['text-xs font-medium', stat.change > 0 ? 'text-green-500' : 'text-red-500']">
                {{ stat.change > 0 ? '+' : '' }}{{ stat.change }}%
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import CountTo from '@/components/ui/CountTo.vue'

defineProps({
  stats: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.stat-card {
  border-radius: 12px;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
  padding: 20px;
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-card--primary {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(var(--color-primary-rgb), 0.04) 0%, transparent 100%);
}

.stat-card--primary .stat-icon {
  transform: scale(1.05);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  margin-right: 16px;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.stat-card:hover .stat-icon {
  transform: scale(1.1);
}

@media (max-width: 768px) {
  .stat-card {
    padding: 16px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    margin-right: 12px;
  }
}
</style>
