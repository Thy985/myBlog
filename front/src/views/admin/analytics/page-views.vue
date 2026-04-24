<template>
    <div>
        <el-card shadow="never" class="border-1">
            <template #header>
                <div class="flex justify-between items-center">
                    <span class="font-medium">访问统计</span>
                    <div class="flex gap-2">
                        <el-select v-model="timeRange" size="small" style="width: 120px">
                            <el-option label="近7天" value="7"></el-option>
                            <el-option label="近30天" value="30"></el-option>
                            <el-option label="近90天" value="90"></el-option>
                            <el-option label="近1年" value="365"></el-option>
                        </el-select>
                        <el-button size="small" type="primary" @click="refreshData">刷新数据</el-button>
                    </div>
                </div>
            </template>
            
            <!-- 统计概览 -->
            <el-row :gutter="16" class="mb-6">
                <el-col :span="4" :offset="0">
                    <el-card shadow="never" class="border-1">
                        <div class="text-center">
                            <div class="text-gray-500 mb-1">总访问量(PV)</div>
                            <div class="text-2xl font-bold">{{ totalPV }}</div>
                            <div class="text-sm" :class="pvTrendClass">
                                <el-icon>{{ pvTrendIcon }}</el-icon>
                                {{ pvTrend }}%
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="4" :offset="0">
                    <el-card shadow="never" class="border-1">
                        <div class="text-center">
                            <div class="text-gray-500 mb-1">独立访客(UV)</div>
                            <div class="text-2xl font-bold">{{ totalUV }}</div>
                            <div class="text-sm" :class="uvTrendClass">
                                <el-icon>{{ uvTrendIcon }}</el-icon>
                                {{ uvTrend }}%
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="4" :offset="0">
                    <el-card shadow="never" class="border-1">
                        <div class="text-center">
                            <div class="text-gray-500 mb-1">平均停留时间</div>
                            <div class="text-2xl font-bold">{{ avgTime }}</div>
                            <div class="text-sm" :class="timeTrendClass">
                                <el-icon>{{ timeTrendIcon }}</el-icon>
                                {{ timeTrend }}%
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="4" :offset="0">
                    <el-card shadow="never" class="border-1">
                        <div class="text-center">
                            <div class="text-gray-500 mb-1">跳出率</div>
                            <div class="text-2xl font-bold">{{ bounceRate }}%</div>
                            <div class="text-sm" :class="bounceTrendClass">
                                <el-icon>{{ bounceTrendIcon }}</el-icon>
                                {{ bounceTrend }}%
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="4" :offset="0">
                    <el-card shadow="never" class="border-1">
                        <div class="text-center">
                            <div class="text-gray-500 mb-1">平均访问深度</div>
                            <div class="text-2xl font-bold">{{ avgDepth }}</div>
                            <div class="text-sm" :class="depthTrendClass">
                                <el-icon>{{ depthTrendIcon }}</el-icon>
                                {{ depthTrend }}%
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="4" :offset="0">
                    <el-card shadow="never" class="border-1">
                        <div class="text-center">
                            <div class="text-gray-500 mb-1">今日访问量</div>
                            <div class="text-2xl font-bold">{{ todayPV }}</div>
                            <div class="text-sm" :class="todayTrendClass">
                                <el-icon>{{ todayTrendIcon }}</el-icon>
                                {{ todayTrend }}%
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
            
            <!-- 访问趋势图表 -->
            <el-row :gutter="20" class="mb-6">
                <el-col :span="24">
                    <el-card shadow="never" class="border-1">
                        <template #header>
                            <span class="font-medium">访问趋势</span>
                        </template>
                        <div id="pvChart" style="height: 400px;"></div>
                    </el-card>
                </el-col>
            </el-row>
            
            <!-- 来源分析和页面分析 -->
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-card shadow="never" class="border-1">
                        <template #header>
                            <span class="font-medium">来源分析</span>
                        </template>
                        <div id="sourceChart" style="height: 300px;"></div>
                    </el-card>
                </el-col>
                <el-col :span="12">
                    <el-card shadow="never" class="border-1">
                        <template #header>
                            <span class="font-medium">页面分析</span>
                        </template>
                        <el-table :data="pageStats" stripe style="width: 100%" size="small">
                            <el-table-column prop="page" label="页面" min-width="150"></el-table-column>
                            <el-table-column prop="pv" label="访问量" width="100"></el-table-column>
                            <el-table-column prop="uv" label="访客数" width="100"></el-table-column>
                            <el-table-column prop="avgTime" label="平均停留" width="120"></el-table-column>
                            <el-table-column prop="bounceRate" label="跳出率" width="100">
                                <template #default="scope">
                                    {{ scope.row.bounceRate }}%
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-card>
                </el-col>
            </el-row>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

import logger from '@/utils/logger'
// 时间范围
const timeRange = ref('7')

// 统计数据
const totalPV = ref(12345)
const totalUV = ref(6789)
const avgTime = ref('2:34')
const bounceRate = ref(45.6)
const avgDepth = ref(3.2)
const todayPV = ref(1234)

// 趋势数据
const pvTrend = ref(12.5)
const pvTrendClass = ref('text-green-500')
const pvTrendIcon = ref(ArrowUp)

const uvTrend = ref(8.2)
const uvTrendClass = ref('text-green-500')
const uvTrendIcon = ref(ArrowUp)

const timeTrend = ref(-2.1)
const timeTrendClass = ref('text-red-500')
const timeTrendIcon = ref(ArrowDown)

const bounceTrend = ref(3.5)
const bounceTrendClass = ref('text-red-500')
const bounceTrendIcon = ref(ArrowUp)

const depthTrend = ref(5.8)
const depthTrendClass = ref('text-green-500')
const depthTrendIcon = ref(ArrowUp)

const todayTrend = ref(15.3)
const todayTrendClass = ref('text-green-500')
const todayTrendIcon = ref(ArrowUp)

// 页面统计数据
const pageStats = ref([
    { page: '/', pv: 3456, uv: 1234, avgTime: '3:45', bounceRate: 32.5 },
    { page: '/article/1', pv: 2345, uv: 987, avgTime: '5:23', bounceRate: 28.3 },
    { page: '/article/2', pv: 1987, uv: 876, avgTime: '4:56', bounceRate: 31.2 },
    { page: '/category/1', pv: 1234, uv: 567, avgTime: '2:34', bounceRate: 45.6 },
    { page: '/tag/1', pv: 987, uv: 432, avgTime: '2:12', bounceRate: 52.3 }
])

// 图表实例
let pvChart = null
let sourceChart = null

// 初始化访问趋势图表
const initPVChart = () => {
    const chartDom = document.getElementById('pvChart')
    if (chartDom) {
        pvChart = echarts.init(chartDom)
        
        const option = {
            tooltip: {
                trigger: 'axis'
            },
            legend: {
                data: ['PV', 'UV']
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: ['1月1日', '1月2日', '1月3日', '1月4日', '1月5日', '1月6日', '1月7日']
            },
            yAxis: {
                type: 'value'
            },
            series: [
                {
                    name: 'PV',
                    type: 'line',
                    stack: 'Total',
                    data: [1200, 1900, 1500, 2100, 1800, 2300, 2500]
                },
                {
                    name: 'UV',
                    type: 'line',
                    stack: 'Total',
                    data: [800, 1200, 900, 1500, 1200, 1600, 1800]
                }
            ]
        }
        
        pvChart.setOption(option)
    }
}

// 初始化来源分析图表
const initSourceChart = () => {
    const chartDom = document.getElementById('sourceChart')
    if (chartDom) {
        sourceChart = echarts.init(chartDom)
        
        const option = {
            tooltip: {
                trigger: 'item'
            },
            legend: {
                orient: 'vertical',
                left: 'left'
            },
            series: [
                {
                    name: '来源',
                    type: 'pie',
                    radius: '50%',
                    data: [
                        { value: 35, name: '直接访问' },
                        { value: 25, name: '搜索引擎' },
                        { value: 20, name: '社交媒体' },
                        { value: 15, name: '外部链接' },
                        { value: 5, name: '其他' }
                    ],
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowOffsetX: 0,
                            shadowColor: 'rgba(0, 0, 0, 0.5)'
                        }
                    }
                }
            ]
        }
        
        sourceChart.setOption(option)
    }
}

// 刷新数据
const refreshData = () => {
    // 调用API获取最新数据
    logger.debug('刷新数据，时间范围:', timeRange.value)
    // 模拟数据更新
    totalPV.value += 100
    totalUV.value += 50
    todayPV.value += 10
}

// 监听时间范围变化
watch(timeRange, () => {
    refreshData()
})

// 窗口大小变化时重新调整图表
const handleResize = () => {
    pvChart?.resize()
    sourceChart?.resize()
}

// 组件挂载时初始化
onMounted(() => {
    initPVChart()
    initSourceChart()
    window.addEventListener('resize', handleResize)
})

// 组件卸载时清理
onUnmounted(() => {
    pvChart?.dispose()
    sourceChart?.dispose()
    window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.font-medium {
    font-weight: 500!important;
}

.justify-between {
    justify-content: space-between!important;
}

.items-center {
    align-items: center!important;
}

.gap-2 {
    gap: 0.5rem!important;
}

.mb-6 {
    margin-bottom: 1.5rem!important;
}

.text-center {
    text-align: center!important;
}

.text-gray-500 {
    color: var(--text-secondary)!important;
}

.mb-1 {
    margin-bottom: 0.25rem!important;
}

.text-2xl {
    font-size: 1.5rem!important;
}

.font-bold {
    font-weight: 700!important;
}

.text-sm {
    font-size: 0.875rem!important;
}

.text-green-500 {
    color: var(--color-success)!important;
}

.text-red-500 {
    color: var(--color-error)!important;
}

.mb-4 {
    margin-bottom: 1rem!important;
}

.justify-end {
    justify-content: flex-end!important;
}
</style>
