import request from '@/axios'
import type { ApiResponse } from '@/types/api'

// 获取仪表盘概览统计数据
export function getDashboardOverview(): Promise<ApiResponse<any>> {
  return request.get('/admin/dashboard/stats/overview')
}

// 获取文章发布趋势
export function getArticleTrend(days = 7): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/article-trend', { params: { days } })
}

// 获取用户增长趋势
export function getUserTrend(days = 7): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/user-trend', { params: { days } })
}

// 获取访问量趋势
export function getVisitTrend(days = 7): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/visit-trend', { params: { days } })
}

// 获取分类分布统计
export function getCategoryDistribution(): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/category-distribution')
}

// 获取标签分布统计
export function getTagDistribution(): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/tag-distribution')
}

// 获取热门文章排行
export function getHotArticles(limit = 10): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/hot-articles', { params: { limit } })
}

// 获取活跃用户排行
export function getActiveUsers(limit = 10): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/active-users', { params: { limit } })
}

// 获取待审核内容统计
export function getPendingReviewStats(): Promise<ApiResponse<any>> {
  return request.get('/admin/dashboard/stats/pending-review')
}

// 获取系统运行状态
export function getSystemStatus(): Promise<ApiResponse<any>> {
  return request.get('/admin/dashboard/stats/system-status')
}

// 获取评论趋势
export function getCommentTrend(days = 7): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/comment-trend', { params: { days } })
}

// 获取用户角色分布
export function getUserRoleDistribution(): Promise<ApiResponse<any[]>> {
  return request.get('/admin/dashboard/stats/user-role-distribution')
}

export const getDashboardArticleStatisticsInfo = getArticleTrend
export const getDashboardPublishArticleStatisticsInfo = getArticleTrend
export const getDashboardPVStatisticsInfo = getVisitTrend