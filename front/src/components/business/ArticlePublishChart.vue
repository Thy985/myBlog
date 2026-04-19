<template>
    <div>
        <el-card shadow="never" class="border-1">
            <template #header>
                <div class="flex justify-between">
                    <span class="text-sm">文章发布热点图</span>
                </div>
            </template>
            <!-- card body -->
            <div id="publishArticleChart" style="width: 100%; height: 300px;">

            </div>
        </el-card>

    </div>
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts/core'
import { HeatmapChart } from 'echarts/charts'
import { CalendarComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboardPublishArticleStatisticsInfo } from '@/api/admin/dashboard'

echarts.use([HeatmapChart, CalendarComponent, VisualMapComponent, CanvasRenderer])

// 接收时间范围属性
const props = defineProps({
    timeRange: {
        type: String,
        default: '7'
    }
})

/*
* origin：将整个年度的活跃图都显示出来，这就导致因屏幕尺寸不足难以显示后面月数的热图信息
* repair：只显示已当前日期为基准，前四后二总共六个月的热图信息
* */
const currentDate = new Date()
const currentYear = currentDate.getFullYear()
const currentMonth = currentDate.getMonth() + 1
const startYear = currentYear
const startMonth = currentMonth - 4 // 获取最近三个月的数据
const endYear = currentYear
const endMonth = currentMonth + 2

let myChart = null

// 初始化图表
const initChart = () => {
    const chartDom = document.getElementById('publishArticleChart')
    if (chartDom) {
        myChart = echarts.init(chartDom)
        updateChart(props.timeRange)
    }
}

// 更新图表
const updateChart = () => {
    if (!myChart) {return}
    
    getDashboardPublishArticleStatisticsInfo().then((e) => {
        if (e.code === 200) {
            const map = e.data
            const chartData = []
            for (const key in map) {
                chartData.push([
                    key,
                    map[key]
                ])
            }

            const option = {
                visualMap: {
                    show: true,
                    min: 0,
                    max: 10,
                    orient: 'horizontal',
                    left: 'center',
                    bottom: 10,
                    inRange: {
                        // eslint-disable-next-line no-restricted-syntax
                        color: ['#fff', '#40c463', '#30a14e', '#216e39']
                    }
                },
                calendar: {
                    range: [startYear + '-' + startMonth, endYear + '-' + endMonth],
                    cellSize: ['auto', 13],
                    top: 20,
                    left: 30,
                    right: 30,
                    itemStyle: {
                        borderWidth: 0.5
                    }
                },
                series: {
                    type: 'heatmap',
                    coordinateSystem: 'calendar',
                    data: chartData,
                    label: {
                        show: false
                    },
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowColor: 'rgba(0, 0, 0, 0.5)'
                        }
                    }
                }
            }

            myChart.setOption(option)
        }
    })
}

// 监听时间范围变化
watch(() => props.timeRange, () => {
    updateChart()
})

// 窗口大小变化处理函数
const handleResize = () => {
    if (myChart) {
        myChart.resize()
    }
}

// 添加窗口大小变化监听
window.addEventListener('resize', handleResize)

onMounted(() => {
    initChart()
})

// 组件卸载时清理
onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    if (myChart) {
        myChart.dispose()
        myChart = null
    }
})

// 暴露更新方法
defineExpose({
    updateChart
})
</script>
