/**
 * 评论相关类型定义
 */

// 评论作者
export interface CommentAuthor {
  id: number
  username: string
  nickname?: string
  avatar?: string
}

// 评论状态
export type CommentStatus = 'pending' | 'approved' | 'deleted' | 'blocked' | string

// 评论基础信息
export interface Comment {
  id: number
  content: string
  author: CommentAuthor
  authorId: number
  articleId: number
  articleTitle?: string
  parentId?: number
  rootId?: number          // 根评论ID
  replyTo?: CommentAuthor // 被回复的用户
  createTime: string
  updateTime?: string
  likeCount: number
  replyCount?: number     // 回复数量
  status?: CommentStatus // 'pending'|'approved'|'deleted'|'blocked'
  isLiked?: boolean
  children?: Comment[]
  level?: number          // 评论层级
}

// 评论表单
export interface CommentForm {
  content: string
  articleId: number
  parentId?: number
  replyToId?: number
}

// 评论查询参数
export interface CommentQuery {
  current?: number
  size?: number
  articleId?: number
  authorId?: number
  parentId?: number
  sortBy?: 'createTime' | 'likeCount'
  sortOrder?: 'asc' | 'desc'
}
