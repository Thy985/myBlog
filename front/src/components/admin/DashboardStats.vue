<template>
  <el-row :gutter="20" class="mb-6">
    <el-col v-for="(stat, index) in stats" :key="index" :xs="12" :sm="8" :md="6" :lg="4" :xl="4" :offset="0">
      <el-card shadow="never" class="stat-card border-1 hover:shadow-md transition-all duration-300">
        <div class="flex items-center">
          <div :class="['mr-4 stat-icon', stat.iconBg, stat.iconColor] + ' rounded-xl w-12 h-12 flex items-center justify-center'">
            <el-icon class="text-xl"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="flex-1">
            <div class="text-gray-500 text-sm mb-1">{{ stat.label }}</div>
            <div class="flex items-end justify-between">
              <CountTo :value="stat.value" class="text-xl font-bold"></CountTo>
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
  transition: all 0.3s ease;
  padding: 20px;
  background-color: white;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  margin-right: 16px;
  transition: all 0.3s ease;
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

.dark .stat-card {
  background-color: #1e293b;
  border-color: #334155;
}

.dark .stat-card:hover {
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.3);
}
</style>
