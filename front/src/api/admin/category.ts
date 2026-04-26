import request from '@/axios'
import type { ApiResponse } from '@/types/api'

export interface CategoryData {
  name: string
  description?: string
}

export interface CategoryPageParams {
  page?: number
  size?: number
  keyword?: string
}

// 添加分类
export function addCategory(data: CategoryData): Promise<ApiResponse<{ id: number }>> {
  return request.post('/category', data)
}

// 获取分类列表（分页）
export function getCategoryPageList(params: CategoryPageParams = {}): Promise<ApiResponse<{ list: any[]; total: number }>> {
  return request.get('/category/list', { params })
}

// 获取所有分类列表（不分页）
export function getCategoryList(): Promise<ApiResponse<any[]>> {
  return request.get('/category/list')
}

// 获取发现页分类列表
export function getDiscoverCategoryList(): Promise<ApiResponse<any[]>> {
  return request.get('/category/discover')
}

// 获取当前用户分类列表
export function getUserCategoryList(): Promise<ApiResponse<any[]>> {
  return request.get('/category/user')
}

// 获取分类详情
export function getCategoryById(id: number): Promise<ApiResponse<any>> {
  return request.get(`/category/${id}`)
}

// 删除分类
export function deleteCategory(categoryId: number): Promise<ApiResponse<null>> {
  return request.delete(`/category/${categoryId}`)
}

// 获取分类下拉选择列表
export function getCategorySelect(): Promise<ApiResponse<any[]>> {
  return request.get('/category/list')
}

// 更新分类
export function updateCategory(id: number, data: CategoryData): Promise<ApiResponse<null>> {
  return request.put(`/category/${id}`, data)
}

export const getCategoryPageListLegacy = getCategoryPageList