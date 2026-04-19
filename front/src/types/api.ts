/**
 * API响应相关类型定义
 */

// API响应基础结构
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

// 分页参数
export interface PageParams {
  current?: number
  size?: number
}

// 分页响应 - 与后端 PageResult 对应
export interface PageResponse<T> {
  list: T[]
  page: number
  size: number
  total: number
  pages: number
}

// 排序参数
export interface SortParams {
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 查询参数基础接口
export interface QueryParams extends PageParams, SortParams {
  keyword?: string
  [key: string]: any
}

// 上传文件响应
export interface UploadResponse {
  url: string
  filename: string
  size: number
  mimeType: string
}

// 错误响应
export interface ErrorResponse {
  code: number
  message: string
  errors?: Array<{
    field: string
    message: string
  }>
}

// 请求配置选项
export interface RequestOptions {
  showError?: boolean
  showSuccess?: boolean
  successMessage?: string
  showLoading?: boolean
  loadingMessage?: string
  showPageLoading?: boolean
}
