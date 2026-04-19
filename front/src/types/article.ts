/**
 * 文章相关类型定义
 *
 * 字段命名规范：
 * - description: 文章摘要 (后端: summary / description)
 * - readCount: 阅读数量 (后端: readNum / viewCount)
 * - likeCount: 点赞数量 (后端: likeNum)
 * - createdAt: 创建时间 (后端: createdTime)
 * - updatedAt: 更新时间 (后端: updatedTime)
 */

// 文章状态
export type ArticleStatus = 'draft' | 'published' | 'archived' | string

// 文章分类
export interface Category {
  id: number
  name: string
  description?: string
  articleCount?: number
  createdTime?: string
  updatedTime?: string
}

// 文章标签
export interface Tag {
  id: number
  name: string
  color?: string
  articleCount?: number
  createdTime?: string
}

// 文章作者
export interface Author {
  id: number
  username: string
  nickname?: string
  avatar?: string
  email?: string
  role?: 'user' | 'admin'
}

// 文章基础信息
export interface Article {
  id: number
  title: string
  description: string
  content: string
  titleImage: string
  category: Category
  categoryId: number
  categoryName: string
  tags: Tag[]
  author?: Author
  authorId?: number
  createdAt: string
  updatedAt: string
  publishTime?: string
  readCount: number
  likeCount?: number
  commentCount?: number
  collectCount?: number
  shareCount?: number
  status?: ArticleStatus
  isTop?: boolean
  isRecommend?: boolean
  // 当前用户互动状态
  isLiked?: boolean
  isCollected?: boolean
  // 上下篇
  preArticle?: { id: number; title: string }
  nextArticle?: { id: number; title: string }
}

// 文章列表项(简化版)
export interface ArticleListItem {
  id: number
  title: string
  description: string        // 前端统一使用 description
  titleImage: string
  category: {
    id: number
    name: string
  }
  tags: Array<{
    id: number
    name: string
  }>
  createdAt: string         // 前端统一使用 createdAt
  readCount: number         // 前端统一使用 readCount
}

// 文章详情
export interface ArticleDetail extends Article {
  prevArticle?: {
    id: number
    title: string
  }
  nextArticle?: {
    id: number
    title: string
  }
  relatedArticles?: ArticleListItem[]
}

// 文章查询参数
export interface ArticleQuery {
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

// 文章创建/更新参数
export interface ArticleForm {
  title: string
  summary: string           // 后端仍使用 summary
  content: string
  titleImage: string
  categoryId: number
  tagIds: number[]
  status?: 'draft' | 'published'
  isTop?: boolean
  isRecommend?: boolean
}

// 分页响应 - 与后端 PageResult 对应
export interface PageResponse<T> {
  list: T[]
  page: number
  size: number
  total: number
  pages: number
}

// 文章分页响应
export type ArticlePageResponse = PageResponse<ArticleListItem>
