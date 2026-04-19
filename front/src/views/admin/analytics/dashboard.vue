<template>
    <div class="analytics-dashboard">
        <!-- 概览卡片 -->
        <div class="overview-cards">
            <el-row :gutter="20">
                <el-col v-for="(item, index) in overviewData" :key="index" :xs="12" :sm="6">
                    <el-card shadow="hover" class="overview-card">
                        <div class="card-content">
                            <div class="card-icon" :style="{ backgroundColor: item.color }">
                                <el-icon :size="24">
                                    <component :is="item.icon"></component>
                                </el-icon>
                            </div>
                            <div class="card-info">
                                <div class="card-value">{{ item.value }}</div>
                                <div class="card-label">{{ item.label }}</div>
                                <div class="card-trend" :class="item.trend > 0 ? 'up' : 'down'">
                                    <el-icon><CaretTop v-if="item.trend > 0" /><CaretBottom v-else /></el-icon>
                                    {{ Math.abs(item.trend) }}%
                                </div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- 访问趋势图表 -->
        <el-row :gutter="20" class="mt-4">
            <el-col :span="24">
                <el-card shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span class="card-title">访问趋势</span>
                            <el-radio-group v-model="trendDays" size="small" @change="loadTrendData">
                                <el-radio-button :label="7">近7天</el-radio-button>
                                <el-radio-button :label="30">近30天</el-radio-button>
                                <el-radio-button :label="90">近90天</el-radio-button>
                            </el-radio-group>
                        </div>
                    </template>
                    <div ref="trendChartRef" style="width: 100%; height: 400px;"></div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 热门文章和设备分析 -->
        <el-row :gutter="20" class="mt-4">
            <!-- 热门文章 -->
            <el-col :xs="24" :md="12">
                <el-card shadow="never">
                    <template #header>
                        <span class="card-title">热门文章 TOP 10</span>
                    </template>
                    <el-table :data="hotArticles" style="width: 100%" max-height="400">
                        <el-table-column type="index" label="#" width="50" />
                        <el-table-column prop="title" label="文章标题" show-overflow-tooltip />
                        <el-table-column prop="pv" label="浏览量" width="100" align="center" />
                        <el-table-column prop="uv" label="访客数" width="100" align="center" />
                    </el-table>
                </el-card>
            </el-col>

            <!-- 设备分析 -->
            <el-col :xs="24" :md="12">
                <el-card shadow="never">
                    <template #header>
                        <span class="card-title">设备分析</span>
                    </template>
                    <div ref="deviceChartRef" style="width: 100%; height: 400px;"></div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 来源分析 -->
        <el-row :gutter="20" class="mt-4">
            <el-col :span="24">
                <el-card shadow="never">
                    <template #header>
                        <span class="card-title">流量来源</span>
                    </template>
                    <div ref="sourceChartRef" style="width: 100%; height: 300px;"></div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import {
    View, User, Document, ChatDotRound,
    CaretTop, CaretBottom
} from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { graphic } from 'echarts/core'
import { getAnalyticsOverview, getVisitTrend, getHotArticles, getDeviceAnalysis, getSourceAnalysis } from '@/api/admin/analytics'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

echarts.use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// 数据
const overviewData = ref([
    { label: '总访问量', value: '0', trend: 0, icon: View, color: '#409EFF' },
    { label: '总访客数', value: '0', trend: 0, icon: User, color: '#67C23A' },
    { label: '文章总数', value: '0', trend: 0, icon: Document, color: '#E6A23C' },
    { label: '评论总数', value: '0', trend: 0, icon: ChatDotRound, color: '#F56C6C' }
])

const trendDays = ref(7)
const hotArticles = ref([])

// 图表引用
const trendChartRef = ref(null)
const deviceChartRef = ref(null)
const sourceChartRef = ref(null)

let trendChart = null
let deviceChart = null
let sourceChart = null

// 加载概览数据
const loadOverview = async () => {
    try {
        const res = await getAnalyticsOverview()
        if (res.code === API_STATUS.SUCCESS) {
            const data = res.data
            overviewData.value[0].value = data.totalPV || '0'
            overviewData.value[0].trend = data.pvTrend || 0
            overviewData.value[1].value = data.totalUV || '0'
            overviewData.value[1].trend = data.uvTrend || 0
            overviewData.value[2].value = data.totalArticles || '0'
            overviewData.value[2].trend = data.articleTrend || 0
            overviewData.value[3].value = data.totalComments || '0'
            overviewData.value[3].trend = data.commentTrend || 0
        }
    } catch (error) {
        logger.error('加载概览数据失败:', error)
    }
}

// 加载访问趋势数据
const loadTrendData = async () => {
    try {
        const res = await getVisitTrend(trendDays.value)
        if (res.code === API_STATUS.SUCCESS) {
            const data = res.data
            initTrendChart(data.dates, data.pvData, data.uvData)
        }
    } catch (error) {
        logger.error('加载趋势数据失败:', error)
    }
}

// 初始化趋势图表
const initTrendChart = (dates, pvData, uvData) => {
    if (!trendChart) {
        trendChart = echarts.init(trendChartRef.value)
    }

    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'cross'
            }
        },
        legend: {
            data: ['浏览量(PV)', '访客数(UV)']
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
            data: dates
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                name: '浏览量(PV)',
                type: 'line',
                smooth: true,
                data: pvData,
                itemStyle: {
                    color: '#409EFF'
                },
                areaStyle: {
                    color: new graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                        { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
                    ])
                }
            },
            {
                name: '访客数(UV)',
                type: 'line',
                smooth: true,
                data: uvData,
                itemStyle: {
                    color: '#67C23A'
                },
                areaStyle: {
                    color: new graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
                        { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
                    ])
                }
            }
        ]
    }

    trendChart.setOption(option)
}

// 加载热门文章
const loadHotArticles = async () => {
    try {
        const res = await getHotArticles(10)
        if (res.code === API_STATUS.SUCCESS) {
            hotArticles.value = res.data
        }
    } catch (error) {
        logger.error('加载热门文章失败:', error)
    }
}

// 加载设备分析
const loadDeviceAnalysis = async () => {
    try {
        const res = await getDeviceAnalysis(7)
        if (res.code === API_STATUS.SUCCESS) {
            initDeviceChart(res.data)
        }
    } catch (error) {
        logger.error('加载设备分析失败:', error)
    }
}

// 初始化设备图表
const initDeviceChart = (data) => {
    if (!deviceChart) {
        deviceChart = echarts.init(deviceChartRef.value)
    }

    const option = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
        },
        legend: {
            orient: 'vertical',
            left: 'left'
        },
        series: [
            {
                type: 'pie',
                radius: '60%',
                data: data,
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

    deviceChart.setOption(option)
}

// 加载来源分析
const loadSourceAnalysis = async () => {
    try {
        const res = await getSourceAnalysis(7)
        if (res.code === API_STATUS.SUCCESS) {
            initSourceChart(res.data)
        }
    } catch (error) {
        logger.error('加载来源分析失败:', error)
    }
}

// 初始化来源图表
const initSourceChart = (data) => {
    if (!sourceChart) {
        sourceChart = echarts.init(sourceChartRef.value)
    }

    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: data.map(item => item.name)
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                type: 'bar',
                data: data.map(item => item.value),
                itemStyle: {
                    color: new graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: '#409EFF' },
                        { offset: 1, color: '#67C23A' }
                    ])
                },
                barWidth: '60%'
            }
        ]
    }

    sourceChart.setOption(option)
}

// 窗口大小改变时调整图表
const handleResize = () => {
    trendChart?.resize()
    deviceChart?.resize()
    sourceChart?.resize()
}

// 组件挂载
onMounted(() => {
    loadOverview()
    loadTrendData()
    loadHotArticles()
    loadDeviceAnalysis()
    loadSourceAnalysis()

    window.addEventListener('resize', handleResize)
})

// 组件卸载
onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    trendChart?.dispose()
    deviceChart?.dispose()
    sourceChart?.dispose()
})
</script>

<style scoped>
.analytics-dashboard {
    padding: 20px;
}

.overview-card {
    margin-bottom: 20px;
}

.card-content {
    display: flex;
    align-items: center;
    gap: 16px;
}

.card-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
}

.card-info {
    flex: 1;
}

.card-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
}

.card-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
}

.card-trend {
    font-size: 12px;
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
}

.card-trend.up {
    color: #67C23A;
}

.card-trend.down {
    color: #F56C6C;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.card-title {
    font-size: 16px;
    font-weight: bold;
}

.mt-4 {
    margin-top: 20px;
}
</style>
