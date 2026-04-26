import request from '@/axios'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'
import type { ApiResponse } from '@/types/api'

export interface TagData {
  name: string
  color?: string
}

export interface TagPageParams {
  page?: number
  size?: number
  keyword?: string
}

// 添加标签
export function addTags(data: TagData): Promise<ApiResponse<{ id: number }>> {
  return request.post('/tag', null, {
    params: {
      name: data.name,
      color: data.color
    }
  })
}

// 获取标签列表（分页）
export function getTagPageList(params: TagPageParams = {}): Promise<ApiResponse<{ list: any[]; total: number }>> {
  return request.get('/tag/list', { params })
}

// 获取所有标签列表（不分页）
export function getTagList(): Promise<ApiResponse<any[]>> {
  return request.get('/tag/list')
}

// 获取发现页标签列表
export function getDiscoverTagList(): Promise<ApiResponse<any[]>> {
  return request.get('/tag/discover')
}

// 获取当前用户标签列表
export function getUserTagList(): Promise<ApiResponse<any[]>> {
  return request.get('/tag/user')
}

// 获取热门标签
export function getHotTags(limit = 20): Promise<ApiResponse<any[]>> {
  return request.get('/tag/hot', { params: { limit } })
}

// 获取标签详情
export function getTagById(id: number): Promise<ApiResponse<any>> {
  return request.get(`/tag/${id}`)
}

// 删除标签
export function deleteTag(tagId: number): Promise<ApiResponse<null>> {
  return request.delete(`/tag/${tagId}`)
}

// 搜索标签
export function selectTags(key: string): Promise<ApiResponse<any[]>> {
  logger.warn('selectTags: 后端搜索接口暂未实现，使用前端过滤')
  return request.get('/tag/list').then(res => {
    if (res.code === API_STATUS.SUCCESS && res.data) {
      const list = Array.isArray(res.data) ? res.data : res.data.list || []
      const filtered = list.filter((tag: any) =>
        tag.name && tag.name.toLowerCase().includes(key.toLowerCase())
      )
      return {
        code: 200,
        data: filtered,
        message: 'success'
      } as ApiResponse<any[]>
    }
    return res
  })
}

// 获取标签下拉选择列表
export function getTagSelect(): Promise<ApiResponse<any[]>> {
  return request.get('/tag/list')
}

// 更新标签
export function updateTag(id: number, data: TagData): Promise<ApiResponse<null>> {
  return request.put(`/tag/${id}`, null, {
    params: {
      name: data.name,
      color: data.color
    }
  })
}