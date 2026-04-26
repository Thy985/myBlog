import request from '@/axios'
import { transformComment, transformCommentList } from '@/utils/transform'
import type { ApiResponse } from '@/types/api'
import type { CommentVO } from '@/types/comment'

export interface CommentListParams {
  articleId: number
  page?: number
  pageSize?: number
}

export interface PublishCommentData {
  articleId: number
  content: string
  parentId?: number
  rootId?: number
}

// 获取文章评论树（GET /api/comment/list）
export function getCommentList(articleId: number, page = 1, pageSize = 10): Promise<ApiResponse<CommentVO[]>> {
  return request.get('/comment/list', { params: { articleId, page, pageSize } }).then(res => {
    if (res?.data) {
      res.data = transformCommentList(res.data)
    }
    return res as ApiResponse<CommentVO[]>
  })
}

// 获取回复列表（GET /api/comment/{rootId}/replies）
export function getReplyList(rootId: number): Promise<ApiResponse<CommentVO[]>> {
  return request.get(`/comment/${rootId}/replies`).then(res => {
    if (res?.data) {
      res.data = transformCommentList(res.data)
    }
    return res as ApiResponse<CommentVO[]>
  })
}

// 发布评论（POST /api/comment）
export function publishComment(data: PublishCommentData): Promise<ApiResponse<CommentVO>> {
  return request.post('/comment', data).then(res => {
    if (res?.data) {
      res.data = transformComment(res.data)
    }
    return res as ApiResponse<CommentVO>
  })
}

// 删除评论（DELETE /api/comment/{id}）
export function deleteComment(commentId: number): Promise<ApiResponse<null>> {
  return request.delete(`/comment/${commentId}`)
}

// 评论点赞（POST /api/comment/{id}/like）
export function likeComment(commentId: number): Promise<ApiResponse<null>> {
  return request.post(`/comment/${commentId}/like`)
}

// 取消评论点赞（DELETE /api/comment/{id}/like）
export function unlikeComment(commentId: number): Promise<ApiResponse<null>> {
  return request.delete(`/comment/${commentId}/like`)
}