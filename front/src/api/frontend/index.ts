import request from '@/axios'
import { transformPageResponse } from '@/utils/transform'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ArticleListItem } from '@/types/article'

export interface IndexArticlesParams {
  current?: number
  page?: number
  size?: number
}

export interface SearchParams {
  keyword: string
  page?: number
  size?: number
}

// 获取首页文章列表
export function getIndexArticles(params: IndexArticlesParams): Promise<ApiResponse<PageResponse<ArticleListItem>>> {
  return request.get('/article/list', {
    params: {
      page: params.current || params.page || 1,
      size: params.size || 10
    }
  }).then(res => {
    if (res?.data) {
      res.data = transformPageResponse(res.data)
    }
    return res as ApiResponse<PageResponse<ArticleListItem>>
  })
}

// 搜索文章
export function searchArticles(keyword: string, page = 1, size = 10): Promise<ApiResponse<PageResponse<ArticleListItem>>> {
  return request.get('/article/search', { params: { keyword, page, size } }).then(res => {
    if (res?.data) {
      res.data = transformPageResponse(res.data)
    }
    return res as ApiResponse<PageResponse<ArticleListItem>>
  })
}