import request from '@/axios'
import { transformArticleList } from '@/utils/transform'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ArticleListItem, ArticleDetail, ArticleForm, ArticleQuery } from '@/types/article'

export interface ArticleListParams {
  current?: number
  size?: number
  keyword?: string
  categoryId?: number
  tagId?: number
  authorId?: number
  status?: string
  sortBy?: 'createdTime' | 'readNum' | 'likeNum'
  sortOrder?: 'asc' | 'desc'
}

export interface HotArticleParams {
  limit?: number
}

export interface RelatedArticleParams {
  articleId: number
  limit?: number
}

// 获取文章列表
export function getArticles(params: ArticleListParams): Promise<ApiResponse<PageResponse<ArticleListItem>>> {
  return request.get('/article/list', { params })
}

// 获取热门文章
export function getHotArticles(limit = 10): Promise<ApiResponse<ArticleListItem[]>> {
  return request.get('/article/hot', { params: { limit } }).then(res => {
    if (res?.data) {
      res.data = transformArticleList(res.data)
    }
    return res as ApiResponse<ArticleListItem[]>
  })
}

// 获取推荐文章列表（用于轮播图）
export function getRecommendedArticles(params: HotArticleParams = {}): Promise<ApiResponse<ArticleListItem[]>> {
  return request.get('/article/hot', { params: { limit: params.limit || 5 } }).then(res => {
    if (res?.data) {
      res.data = transformArticleList(res.data)
    }
    return res as ApiResponse<ArticleListItem[]>
  })
}

// 获取相关文章推荐
export function getRelatedArticles(articleId: number, limit = 5): Promise<ApiResponse<ArticleListItem[]>> {
  return request.get('/article/related', { params: { articleId, limit } }).then(res => {
    if (res?.data) {
      res.data = transformArticleList(res.data)
    }
    return res as ApiResponse<ArticleListItem[]>
  })
}

// 获取文章详情
export function getArticle(id: number): Promise<ApiResponse<ArticleDetail>> {
  return request.get(`/article/${id}`)
}

// 搜索文章
export function searchArticles(params: ArticleQuery): Promise<ApiResponse<PageResponse<ArticleListItem>>> {
  return request.get('/article/search', { params })
}

// 创建文章
export function createArticle(data: ArticleForm): Promise<ApiResponse<{ id: number }>> {
  return request.post('/article', data)
}

// 更新文章
export function updateArticle(id: number, data: Partial<ArticleForm>): Promise<ApiResponse<ArticleDetail>> {
  return request.put(`/article/${id}`, data)
}

// 删除文章
export function deleteArticle(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/article/${id}`)
}

// 发布文章（将草稿变为正式文章）
export function publishArticleById(id: number): Promise<ApiResponse<{ id: number }>> {
  return request.post(`/article/${id}/publish`)
}

// 下线文章
export function offlineArticle(id: number): Promise<ApiResponse<null>> {
  return request.post(`/article/${id}/offline`)
}

// 置顶/取消置顶文章
export function topArticle(id: number, isTop: boolean): Promise<ApiResponse<null>> {
  return request.post(`/article/${id}/top`, null, { params: { isTop } })
}

// 点赞文章
export function likeArticle(id: number): Promise<ApiResponse<null>> {
  return request.post(`/article/${id}/like`)
}

// 取消点赞文章
export function unlikeArticle(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/article/${id}/like`)
}

// 收藏文章
export function collectArticle(id: number): Promise<ApiResponse<null>> {
  return request.post(`/article/${id}/collect`)
}

// 取消收藏文章
export function uncollectArticle(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/article/${id}/collect`)
}

// 获取用户收藏的文章列表
export function getUserCollects(params: { page?: number; size?: number } = {}): Promise<ApiResponse<PageResponse<ArticleListItem>>> {
  return request.get('/article/user/collects', { params })
}

// 获取文章归档
export function getArticleArchive(params: { page?: number; size?: number } = {}): Promise<ApiResponse<{ list: ArticleListItem[]; total: number }>> {
  return request.get('/article/archive', { params })
}

// 获取用户文章归档
export function getUserArticleArchive(params: { page?: number; size?: number } = {}): Promise<ApiResponse<{ list: ArticleListItem[]; total: number }>> {
  return request.get('/article/user/archive', { params })
}

// 更新文章阅读量
export function updateReadNum(id: number): Promise<ApiResponse<null>> {
  return request.post(`/article/${id}/read`)
}

// 获取文章阅读统计
export function getArticleReadStats(id: number): Promise<ApiResponse<{ readNum: number; likeNum: number; commentNum: number }>> {
  return request.get(`/article/${id}/stats`)
}

// 获取用户的文章列表
export function getUserArticles(userId: number, params: ArticleListParams = {}): Promise<ApiResponse<PageResponse<ArticleListItem>>> {
  return request.get(`/article/user/${userId}`, { params })
}