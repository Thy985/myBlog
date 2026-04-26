import request from '@/axios'
import { transformCategory, transformArticleList } from '@/utils/transform'
import type { ApiResponse } from '@/types/api'
import type { Category } from '@/types/article'
import type { ArticleListItem } from '@/types/article'

export interface CategoryArticlesParams {
  current?: number
  size?: number
  categoryId?: number
  keyword?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 获取分类列表
export function getCategories(): Promise<ApiResponse<Category[]>> {
  return request.get('/category/discover').then(res => {
    if (res?.data && Array.isArray(res.data)) {
      res.data = res.data.map(transformCategory)
    }
    return res as ApiResponse<Category[]>
  })
}

// 获取分类下的文章列表
export function getCategoryArticles(params: CategoryArticlesParams): Promise<ApiResponse<{ list: ArticleListItem[]; total: number; page: number; size: number }>> {
  return request.get('/article/list', { params: { ...params, page: params.current, size: params.size } }).then(res => {
    if (res?.data?.list) {
      res.data.list = transformArticleList(res.data.list)
    }
    return res
  })
}