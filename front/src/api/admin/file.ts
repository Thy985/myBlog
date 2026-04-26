import request from '@/axios'
import type { ApiResponse } from '@/types/api'

export interface FileListParams {
  categoryId?: number
  fileType?: string
  page?: number
  size?: number
}

export interface FileCategoryData {
  name: string
  description?: string
}

// 上传文件
export function uploadFile(file: File, categoryId?: number): Promise<ApiResponse<{ url: string; filename: string; size: number }>> {
  const formData = new FormData()
  formData.append('file', file)
  if (categoryId) {
    formData.append('categoryId', categoryId.toString())
  }
  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传图片
export function uploadImage(file: File): Promise<ApiResponse<{ url: string; filename: string; size: number }>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除文件
export function deleteFile(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/file/${id}`)
}

// 获取文件列表
export function getFileList(params: FileListParams = {}): Promise<ApiResponse<{ list: any[]; total: number }>> {
  return request.get('/file/list', { params })
}

// 获取图片列表
export function getImageList(params: { page?: number; size?: number } = {}): Promise<ApiResponse<{ list: any[]; total: number }>> {
  return request.get('/file/images', { params })
}

// 获取文件URL
export function getFileUrl(id: number): Promise<ApiResponse<{ url: string }>> {
  return request.get(`/file/${id}/url`)
}

// 记录文件下载
export function recordDownload(id: number): Promise<ApiResponse<null>> {
  return request.post(`/file/${id}/download`)
}

// 创建文件分类
export function createFileCategory(data: FileCategoryData): Promise<ApiResponse<{ id: number }>> {
  return request.post('/file/category', null, {
    params: {
      name: data.name,
      description: data.description
    }
  })
}

// 删除文件分类
export function deleteFileCategory(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/file/category/${id}`)
}

// 获取文件分类列表
export function getFileCategoryList(): Promise<ApiResponse<any[]>> {
  return request.get('/file/category/list')
}