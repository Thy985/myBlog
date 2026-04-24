<template>
  <el-row :gutter="20" class="mb-6">
    <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :offset="0">
      <el-card shadow="never" class="chart-card border-1 rounded-xl">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-semibold">文章发布趋势</span>
            <el-select v-model="localTimeRange" size="small" class="w-32" @change="handleTimeRangeChange">
              <el-option label="近7天" value="7"></el-option>
              <el-option label="近30天" value="30"></el-option>
              <el-option label="近90天" value="90"></el-option>
            </el-select>
          </div>
        </template>
        <div class="chart-container">
          <ArticlePublishChart ref="articleChart" :time-range="localTimeRange"></ArticlePublishChart>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :offset="0">
      <el-card shadow="never" class="chart-card border-1 rounded-xl">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-semibold">访问统计</span>
            <el-select v-model="localTimeRange" size="small" class="w-32" @change="handleTimeRangeChange">
              <el-option label="近7天" value="7"></el-option>
              <el-option label="近30天" value="30"></el-option>
              <el-option label="近90天" value="90"></el-option>
            </el-select>
          </div>
        </template>
        <div class="chart-container">
          <PVChart ref="pvChart" :time-range="localTimeRange"></PVChart>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, watch } from 'vue'
import ArticlePublishChart from '@/components/business/ArticlePublishChart.vue'
import PVChart from '@/components/business/PVChart.vue'

const props = defineProps({
  timeRange: {
    type: String,
    default: '7'
  }
})

const emit = defineEmits(['update:timeRange', 'change'])

const localTimeRange = ref(props.timeRange)
const articleChart = ref(null)
const pvChart = ref(null)

watch(() => props.timeRange, (val) => {
  localTimeRange.value = val
})

const handleTimeRangeChange = (val) => {
  emit('update:timeRange', val)
  emit('change', val)
}

const updateCharts = () => {
  if (articleChart.value) {
    articleChart.value.updateChart(localTimeRange.value)
  }
  if (pvChart.value) {
    pvChart.value.updateChart(localTimeRange.value)
  }
}

defineExpose({
  updateChart: handleTimeRangeChange,
  updateCharts
})
</script>

<style scoped>
.chart-card {
  border-radius: 12px;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  background-color: white;
}

.chart-container {
  height: 300px;
  padding: 10px 0;
}

@media (max-width: 1024px) {
  .chart-container {
    height: 250px;
  }
}

@media (max-width: 768px) {
  .chart-container {
    height: 200px;
  }
}

.dark .chart-card {
  background-color: #1e293b;
  border-color: #334155;
}
</style>
