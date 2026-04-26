import request from '@/axios'
import { transformTag } from '@/utils/transform'
import type { ApiResponse } from '@/types/api'
import type { Tag } from '@/types/article'
import type { ArticleListItem } from '@/types/article'

export interface TagArticlesParams {
  current?: number
  size?: number
  tagId?: number
  keyword?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 获取标签列表
export function getTags(): Promise<ApiResponse<Tag[]>> {
  return request.get('/tag/discover').then(res => {
    if (res?.data && Array.isArray(res.data)) {
      res.data = res.data.map(transformTag)
    }
    return res as ApiResponse<Tag[]>
  })
}

// 获取标签下的文章列表
export function getTagArticles(params: TagArticlesParams): Promise<ApiResponse<{ list: ArticleListItem[]; total: number; page: number; size: number }>> {
  return request.get('/article/list', { params: { ...params, page: params.current, size: params.size } })
}