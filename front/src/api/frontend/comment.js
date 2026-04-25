import request from '@/axios'
import { transformComment, transformCommentList } from '@/utils/transform'

/**
 * 获取文章评论树（GET /api/comment/list）
 * @param {number} articleId - 文章ID
 * @param {number} [page=1] - 页码
 * @param {number} [pageSize=10] - 每页数量
 */
export function getCommentList(articleId, page = 1, pageSize = 10) {
  return request.get('/comment/list', { params: { articleId, page, pageSize } }).then(res => {
    if (res && res.data) {
      res.data = transformCommentList(res.data)
    }
    return res
  })
}

/**
 * 获取回复列表（GET /api/comment/{rootId}/replies）
 * @param {number} rootId - 根评论ID
 */
export function getReplyList(rootId) {
  return request.get(`/comment/${rootId}/replies`).then(res => {
    if (res && res.data) {
      res.data = transformCommentList(res.data)
    }
    return res
  })
}

/**
 * 发布评论（POST /api/comment）
 * @param {Object} data - 评论数据
 * @param {number} data.articleId - 文章ID
 * @param {string} data.content - 评论内容
 * @param {number} [data.parentId] - 父评论ID，0表示顶级评论
 * @param {number} [data.rootId] - 根评论ID，0表示顶级评论
 */
export function publishComment(data) {
  return request.post('/comment', data).then(res => {
    if (res && res.data) {
      res.data = transformComment(res.data)
    }
    return res
  })
}

/**
 * 删除评论（DELETE /api/comment/{id}）
 * @param {number} commentId - 评论ID
 */
export function deleteComment(commentId) {
  return request.delete(`/comment/${commentId}`)
}

/**
 * 评论点赞（POST /api/comment/{id}/like）
 * @param {number} commentId - 评论ID
 */
export function likeComment(commentId) {
  return request.post(`/comment/${commentId}/like`)
}

/**
 * 取消评论点赞（DELETE /api/comment/{id}/like）
 * @param {number} commentId - 评论ID
 */
export function unlikeComment(commentId) {
  return request.delete(`/comment/${commentId}/like`)
}
