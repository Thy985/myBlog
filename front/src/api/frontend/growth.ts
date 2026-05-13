import request from '@/axios'
import type { ApiResponse } from '@/types/api'

// ============================================================================
// Growth Cycle API
// ============================================================================

/** 获取可用增长周期 */
export function getGrowthCycles(): Promise<ApiResponse<string[]>> {
  return request.get('/growth/cycles')
}

// ============================================================================
// Growth Task API
// ============================================================================

export interface ScheduleGrowthTaskData {
  cycle: string
  delayMs?: number
  options?: Record<string, any>
}

/** 调度增长任务 */
export function scheduleGrowthTask(data: ScheduleGrowthTaskData): Promise<ApiResponse<any>> {
  return request.post('/growth/task/schedule', data)
}

export interface ExecuteGrowthTaskData {
  cycle: string
}

/** 立即执行增长任务 */
export function executeGrowthTask(data: ExecuteGrowthTaskData): Promise<ApiResponse<any>> {
  return request.post('/growth/task/execute', data)
}

/** 获取用户任务列表 */
export function getGrowthTaskList(): Promise<ApiResponse<any[]>> {
  return request.get('/growth/task/list')
}

/** 获取任务结果 */
export function getTaskResult(taskId: string): Promise<ApiResponse<any>> {
  return request.get(`/growth/task/${taskId}`)
}

/** 取消任务 */
export function cancelTask(taskId: string): Promise<ApiResponse<null>> {
  return request.delete(`/growth/task/${taskId}`)
}

// ============================================================================
// Growth Report API
// ============================================================================

/** 获取增长报告 */
export function getGrowthReport(period: string = 'daily'): Promise<ApiResponse<any>> {
  return request.get('/growth/report', { params: { period } })
}

/** 发现机会 */
export function discoverOpportunities(): Promise<ApiResponse<any>> {
  return request.get('/growth/opportunities')
}

// ============================================================================
// RAG API
// ============================================================================

export interface RagSearchData {
  query: string
  topK?: number
}

/** RAG 搜索 */
export function ragSearch(data: RagSearchData): Promise<ApiResponse<any>> {
  return request.get('/growth/rag/search', { params: data })
}

export interface RagGenerateContentData {
  topic: string
  keywords?: string[]
}

/** RAG 内容生成 */
export function ragGenerateContent(data: RagGenerateContentData): Promise<ApiResponse<any>> {
  return request.post('/growth/rag/content/generate', data)
}

export interface RagOptimizeContentData {
  originalContent: string
  optimizationGoal?: string
}

/** RAG 内容优化 */
export function ragOptimizeContent(data: RagOptimizeContentData): Promise<ApiResponse<any>> {
  return request.post('/growth/rag/content/optimize', data)
}

export interface RagSuggestTopicsData {
  topic: string
  limit?: number
}

/** RAG 主题推荐 */
export function ragSuggestTopics(data: RagSuggestTopicsData): Promise<ApiResponse<any>> {
  return request.get('/growth/rag/topics/suggest', { params: data })
}

// ============================================================================
// Content API
// ============================================================================

export interface GenerateContentData {
  topic: string
  tags?: string[]
  category?: string
}

/** 生成内容 */
export function generateContent(data: GenerateContentData): Promise<ApiResponse<any>> {
  return request.post('/growth/content/generate', data)
}

/** 优化文章 */
export function optimizeArticle(articleId: number, data: Record<string, any>): Promise<ApiResponse<any>> {
  return request.post(`/growth/article/${articleId}/optimize`, data)
}
