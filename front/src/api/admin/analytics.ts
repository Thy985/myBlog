import request from '@/axios'
import logger from '@/utils/logger'
import type { ApiResponse } from '@/types/api'

// 获取统计概览
export function getAnalyticsOverview(): Promise<ApiResponse<any>> {
  return request.get('/analytics/overview')
}

// 获取趋势数据
export function getTrendData(startDate: string, endDate: string): Promise<ApiResponse<any[]>> {
  return request.get('/analytics/trend', { params: { startDate, endDate } })
}

// 获取访问趋势（兼容旧接口，自动计算日期范围）
export function getVisitTrend(days = 7): Promise<ApiResponse<any[]>> {
  const endDate = new Date().toISOString().split('T')[0]
  const startDate = new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  return getTrendData(startDate, endDate)
}

// 获取热门文章
export function getHotArticles(limit = 10): Promise<ApiResponse<any[]>> {
  return request.get('/analytics/hot-articles', { params: { limit } })
}

// 获取文章统计
export function getArticleStats(articleId: number, _days = 30): Promise<ApiResponse<{ articleId: number; views: number; likes: number; comments: number }>> {
  logger.warn('getArticleStats: 后端接口暂未实现')
  return Promise.resolve({
    code: 200,
    data: {
      articleId,
      views: 0,
      likes: 0,
      comments: 0
    },
    message: 'success'
  } as ApiResponse<{ articleId: number; views: number; likes: number; comments: number }>)
}

// 获取用户活动统计
export function getUserActivity(_days = 7): Promise<ApiResponse<any[]>> {
  logger.warn('getUserActivity: 后端接口暂未实现')
  return Promise.resolve({
    code: 200,
    data: [],
    message: 'success'
  } as ApiResponse<any[]>)
}

// 获取来源分析
export function getSourceAnalysis(_days = 7): Promise<ApiResponse<any>> {
  return request.get('/analytics/source')
}

// 获取设备分析
export function getDeviceAnalysis(_days = 7): Promise<ApiResponse<any>> {
  return request.get('/analytics/device')
}

// 记录页面访问
export function recordPageView(pageUrl: string): Promise<ApiResponse<null>> {
  return request.post('/analytics/pv', null, { params: { pageUrl } })
}

// 上报性能指标
export function recordPerformance(data: any): Promise<ApiResponse<null>> {
  return request.post('/analytics/performance', data)
}

// 上报性能错误
export function recordPerformanceErrors(data: any): Promise<ApiResponse<null>> {
  return request.post('/analytics/performance/errors', data)
}

// 获取实时统计
export function getRealtimeStats(): Promise<ApiResponse<any>> {
  return request.get('/analytics/realtime')
}