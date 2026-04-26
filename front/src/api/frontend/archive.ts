import request from '@/axios'
import type { ApiResponse } from '@/types/api'

export interface ArchiveParams {
  page?: number
  size?: number
}

// 获取文章归档
export function getArchives(params: ArchiveParams = {}): Promise<ApiResponse<{ list: any[]; total: number }>> {
  return request.get('/article/archive', { params })
}