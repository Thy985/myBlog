<template>
    <div>
        <el-card shadow="never" class="border-1">
            <template #header>
                <div class="flex justify-between">
                    <span class="text-sm"><!-- card title -->PV 访问量统计</span>
                    <el-select v-model="selectedRange" size="small" class="w-32" @change="onRangeChange">
                        <el-option label="7天" value="7" />
                        <el-option label="30天" value="30" />
                        <el-option label="90天" value="90" />
                    </el-select>
                </div>
            </template>
            <div ref="chartRef" class="w-full" style="height: 300px;"></div>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboardPVStatisticsInfo } from '@/api/admin/dashboard'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
    timeRange: {
        type: String,
        default: '7'
    }
})

const chartRef = ref(null)
const selectedRange = ref(props.timeRange)
let myChart = null

const getChartThemeColors = () => {
    const style = getComputedStyle(document.documentElement)
    /* eslint-disable no-restricted-syntax */
    return {
        primary: style.getPropertyValue('--color-primary').trim() || '#6366F1',
        primarySubtle: style.getPropertyValue('--color-primary-subtle').trim() || 'rgba(99, 102, 241, 0.15)',
        textPrimary: style.getPropertyValue('--text-primary').trim() || '#111827',
        textSecondary: style.getPropertyValue('--text-secondary').trim() || '#374151',
        textMuted: style.getPropertyValue('--text-muted').trim() || '#9ca3af',
        borderColor: style.getPropertyValue('--border-color').trim() || '#e5e7eb',
        borderSubtle: style.getPropertyValue('--border-subtle').trim() || '#f3f4f6',
        bgCard: style.getPropertyValue('--bg-card').trim() || '#ffffff'
    }
    /* eslint-enable no-restricted-syntax */
}

const updateChartOption = (colors) => ({
    tooltip: {
        trigger: 'axis',
        backgroundColor: colors.bgCard,
        borderColor: colors.borderColor,
        borderWidth: 1,
        textStyle: { color: colors.textPrimary }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
        type: 'category',
        boundaryGap: false,
        axisLine: { lineStyle: { color: colors.borderColor } },
        axisLabel: { color: colors.textMuted, fontSize: 12 }
    },
    yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: colors.borderSubtle } },
        axisLabel: { color: colors.textMuted, fontSize: 12 }
    },
    series: [{
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: colors.primary },
        itemStyle: {
            color: colors.primary,
            borderColor: colors.bgCard,
            borderWidth: 2
        },
        areaStyle: {
            color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                    { offset: 0, color: colors.primarySubtle },
                    { offset: 1, color: 'rgba(99, 102, 241, 0.02)' }
                ]
            }
        }
    }]
})

const fetchDataAndRender = async () => {
    if (!myChart) {return}
    try {
        const res = await getDashboardPVStatisticsInfo()
        if (res.code === API_STATUS.SUCCESS) {
            const colors = getChartThemeColors()
            const option = updateChartOption(colors)
            option.xAxis.data = res.data.pvDates || []
            option.series[0].data = res.data.pvCounts || []
            myChart.setOption(option)
        }
    } catch (error) {
        logger.error('获取 PV 数据失败:', error)
    }
}

const initChart = () => {
    if (!chartRef.value) {return}
    myChart = echarts.init(chartRef.value)
    fetchDataAndRender(selectedRange.value)
}

const onRangeChange = () => {
    fetchDataAndRender()
}

watch(() => props.timeRange, () => {
    fetchDataAndRender()
})

const handleResize = () => {
    myChart?.resize()
}

const handleThemeChange = () => {
    if (!myChart) {return}
    const colors = getChartThemeColors()
    myChart.setOption(updateChartOption(colors))
}

onMounted(() => {
    nextTick(() => initChart())
    window.addEventListener('resize', handleResize)
    window.addEventListener('themeChange', handleThemeChange)
})

onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    window.removeEventListener('themeChange', handleThemeChange)
    if (myChart) {
        myChart.dispose()
        myChart = null
    }
})
</script>
