/**
 * ⚠️ 警告：此文件中的接口后端暂未实现
 * 请使用以下替代方案：
 *
 * - 评论相关：使用 src/api/frontend/comment.js
 *   - createComment -> publishComment
 *   - getComments -> getCommentList
 *
 * - 点赞/收藏：使用 src/api/frontend/article.js
 *   - likeArticle -> 后端: POST /api/article/{id}/like
 *   - collectArticle -> 后端: POST /api/article/{id}/collect
 *
 * - 分享功能：后端暂未实现
 */
import logger from '@/utils/logger'

/**
 * @deprecated 后端没有 /comments 接口，请使用 publishComment (src/api/frontend/comment.js)
 */
// eslint-disable-next-line no-unused-vars
export function createComment(_data) {
  // eslint-disable-next-line no-console
  logger.warn('createComment: 后端接口 /comments 不存在，请使用 publishComment')
  return Promise.resolve({ code: 200, message: 'success' })
}

/**
 * @deprecated 后端没有 /comments 接口，请使用 getCommentList (src/api/frontend/comment.js)
 */
// eslint-disable-next-line no-unused-vars
export function getComments(_params) {
  // eslint-disable-next-line no-console
  logger.warn('getComments: 后端接口 /comments 不存在，请使用 getCommentList')
  return Promise.resolve({ code: 200, data: [], message: 'success' })
}

/**
 * @deprecated 后端没有 /likes 接口，请使用 ArticleController 的 /article/{id}/like
 */
// eslint-disable-next-line no-unused-vars
export function likeArticle(_data) {
  // eslint-disable-next-line no-console
  logger.warn('likeArticle: 后端接口 /likes 不存在，请使用 /article/{id}/like')
  return Promise.resolve({ code: 200, message: 'success' })
}

/**
 * @deprecated 后端没有 /collects 接口，请使用 ArticleController 的 /article/{id}/collect
 */
// eslint-disable-next-line no-unused-vars
export function collectArticle(_data) {
  // eslint-disable-next-line no-console
  logger.warn('collectArticle: 后端接口 /collects 不存在，请使用 /article/{id}/collect')
  return Promise.resolve({ code: 200, message: 'success' })
}

/**
 * @deprecated 后端没有 /likes/status 接口
 */
// eslint-disable-next-line no-unused-vars
export function getArticleInteractionStatus(_params) {
  // eslint-disable-next-line no-console
  logger.warn('getArticleInteractionStatus: 后端接口 /likes/status 不存在')
  return Promise.resolve({ code: 200, data: { liked: false, collected: false }, message: 'success' })
}

/**
 * @deprecated 后端没有 /shares 接口
 */
// eslint-disable-next-line no-unused-vars
export function shareArticle(_data) {
  // eslint-disable-next-line no-console
  logger.warn('shareArticle: 后端接口 /shares 不存在')
  return Promise.resolve({ code: 200, message: 'success' })
}

/**
 * @deprecated 后端没有 /shares/generate-qrcode 接口
 */
// eslint-disable-next-line no-unused-vars
export function generateShareQrCode(_articleId) {
  // eslint-disable-next-line no-console
  logger.warn('generateShareQrCode: 后端接口 /shares/generate-qrcode 不存在')
  return Promise.resolve({ code: 200, data: null, message: 'success' })
}
