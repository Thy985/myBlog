/**
 * 数据转换工具
 *
 * 统一处理后端字段名到前端字段名的映射
 * 在 API 响应拦截层处理，全局生效
 *
 * 后端 → 前端字段映射规则：
 * - readNum/viewCount  → readCount
 * - likeNum             → likeCount
 * - commentNum          → commentCount
 * - collectNum          → collectCount
 * - createdTime         → createdAt
 * - updatedTime         → updatedAt
 * - titleImage/coverImage → titleImage
 * - summary             → description (兜底)
 *
 * 枚举值转换：
 * - Article.status:    Integer(0/1)  → 'draft'/'published'
 * - Comment.status:    String(0/1/2/3) → 'pending'/'approved'/'deleted'/'blocked'
 * - User.status:       Integer(0/1)  → 'active'/'inactive'
 * - User.gender:       Integer(0/1/2) → 'other'/'male'/'female'
 */

import logger from '@/utils/logger'

// ============================================================
// 枚举值转换
// ============================================================

/** 文章状态: 后端 Integer → 前端 String */
export function transformArticleStatus(raw: any): 'draft' | 'published' | 'archived' | string {
  if (typeof raw === 'string') return raw
  if (raw === 0) return 'draft'
  if (raw === 1) return 'published'
  if (raw === 2) return 'archived'
  return String(raw)
}

/** 用户状态: 后端 Integer → 前端 String */
export function transformUserStatus(raw: any): 'active' | 'inactive' | 'banned' | string {
  if (typeof raw === 'string') return raw
  if (raw === 0) return 'active'
  if (raw === 1) return 'inactive'
  return 'banned'
}

/** 用户性别: 后端 Integer → 前端 String */
export function transformGender(raw: any): 'male' | 'female' | 'other' | string {
  if (typeof raw === 'string') return raw
  if (raw === 1) return 'male'
  if (raw === 2) return 'female'
  return 'other'
}

/** 评论状态: 后端 String('0'-'3') → 前端 String */
export function transformCommentStatus(raw: any): 'pending' | 'approved' | 'deleted' | 'blocked' | string {
  if (typeof raw === 'string' && !isNaN(Number(raw))) {
    raw = Number(raw)
  }
  if (raw === 0 || raw === '0') return 'pending'
  if (raw === 1 || raw === '1') return 'approved'
  if (raw === 2 || raw === '2') return 'deleted'
  if (raw === 3 || raw === '3') return 'blocked'
  return String(raw)
}

// ============================================================
// 基础工具
// ============================================================

/** 安全取数字，默认 0 */
function toNum(val: any, fallback = 0): number {
  const n = Number(val)
  return isNaN(n) ? fallback : n
}

// ============================================================
// 转换函数
// ============================================================

/**
 * 转换分类数据
 * 修复: categoryName/name 双字段冗余，只取 name
 */
export function transformCategory(cat: any): any {
  if (!cat) return null
  return {
    id: cat.id,
    name: cat.name || cat.categoryName || '',
    description: cat.description || '',
    articleCount: toNum(cat.articleCount),
    createdTime: cat.createdTime || cat.createTime || null,
    updatedTime: cat.updatedTime || cat.updateTime || null,
    status: cat.status,
    parentId: cat.parentId,
    sortOrder: cat.sortOrder
  }
}

/**
 * 转换标签数据
 * 修复: tagName/name 双字段冗余，只取 name
 */
export function transformTag(tag: any): any {
  if (!tag) return null
  return {
    id: tag.id,
    name: tag.name || tag.tagName || '',
    color: tag.color || '',
    articleCount: toNum(tag.articleCount),
    createdTime: tag.createdTime || tag.createTime || null
  }
}

/**
 * 转换评论作者信息
 * 修复: 后端平铺字段 → 前端嵌套 author 对象
 */
export function transformCommentAuthor(author: any): any {
  if (!author) return null
  // 如果已经是嵌套 author 对象（前端期望格式），直接返回
  if (author.username !== undefined && author.nickname === undefined) {
    return author
  }
  return {
    id: author.id,
    username: author.username || '',
    nickname: author.nickname || '',
    avatar: author.avatar || ''
  }
}

/**
 * 转换评论回复对象
 * 修复:
 *   - 添加最大递归深度防止栈溢出
 */
export function transformReply(reply: any, depth: number = 0): any {
  if (!reply) return null

  // 防止无限递归，最大深度为 10
  if (depth > 10) {
    logger.warn('transformReply: 达到最大递归深度，可能存在循环引用')
    return {
      id: reply.id,
      content: reply.content,
      authorId: reply.userId || reply.authorId,
      articleId: reply.articleId,
      articleTitle: reply.articleTitle,
      parentId: reply.parentId,
      rootId: reply.rootId,
      level: reply.level,
      likeCount: toNum(reply.likeCount || reply.like_num),
      replyCount: toNum(reply.replyCount || reply.reply_num),
      replyNum: toNum(reply.replyNum || reply.reply_count),
      status: transformCommentStatus(reply.status),
      device: reply.device || reply.deviceType || '',
      isLiked: !!reply.isLiked,
      createdAt: reply.createdAt || reply.createTime,
      updatedAt: reply.updatedAt || reply.updateTime,
      author: transformCommentAuthor(reply),
      replyTo: null,
      children: []
    }
  }

  return {
    id: reply.id,
    content: reply.content,
    authorId: reply.userId || reply.authorId,
    articleId: reply.articleId,
    articleTitle: reply.articleTitle,
    parentId: reply.parentId,
    rootId: reply.rootId,
    level: reply.level,
    likeCount: toNum(reply.likeCount || reply.like_num),
    replyCount: toNum(reply.replyCount || reply.reply_num),
    replyNum: toNum(reply.replyNum || reply.reply_count),
    status: transformCommentStatus(reply.status),
    device: reply.device || reply.deviceType || '',
    isLiked: !!reply.isLiked,
    createdAt: reply.createdAt || reply.createTime,
    updatedAt: reply.updatedAt || reply.updateTime,
    author: transformCommentAuthor(reply),
    replyTo: reply.replyToUsername
      ? {
          id: reply.replyToUserId,
          username: reply.replyToUsername,
          nickname: reply.replyToNickname
        }
      : null,
    children: Array.isArray(reply.replies)
      ? reply.replies.map((r: any) => transformReply(r, depth + 1))
      : []
  }
}

/**
 * 转换评论数据（后端 CommentVO → 前端 Comment）
 * 修复:
 *   - status Integer→String
 *   - likeCount 字段名
 *   - replyNum vs replyCount
 *   - 平铺 author → 嵌套 author
 */
export function transformComment(comment: any): any {
  if (!comment) return null

  // 提取嵌套 author（后端 CommentVO 平铺输出）
  const authorData = {
    id: comment.userId || comment.authorId || comment.id,
    username: comment.username || '',
    nickname: comment.nickname || '',
    avatar: comment.avatar || ''
  }

  return {
    id: comment.id,
    content: comment.content,
    authorId: comment.userId || comment.authorId,
    articleId: comment.articleId,
    articleTitle: comment.articleTitle,
    parentId: comment.parentId,
    rootId: comment.rootId,
    level: comment.level,
    // 兼容 likeCount / likeNum / like_num
    likeCount: toNum(comment.likeCount || comment.likeNum || comment.like_num),
    // 兼容 replyCount / replyNum / reply_count
    replyCount: toNum(comment.replyCount || comment.replyNum || comment.reply_count),
    // 状态转换
    status: transformCommentStatus(comment.status),
    device: comment.device || comment.deviceType || '',
    isLiked: !!comment.isLiked,
    createdAt: comment.createdAt || comment.createTime,
    updatedAt: comment.updatedAt || comment.updateTime,
    // 嵌套 author 对象
    author: authorData,
    // 回复目标用户
    replyTo: comment.replyToUsername
      ? {
          id: comment.replyToUserId,
          username: comment.replyToUsername,
          nickname: comment.replyToNickname
        }
      : null,
    // 子评论（后端用 replies，前端用 children）
    children: Array.isArray(comment.replies)
      ? comment.replies.map(transformReply)
      : Array.isArray(comment.children)
        ? comment.children
        : []
  }
}

/**
 * 转换评论列表
 */
export function transformCommentList(list: any[]): any[] {
  if (!Array.isArray(list)) return []
  return list.map(transformComment)
}

/**
 * 转换文章作者信息
 */
export function transformArticleAuthor(author: any): any {
  if (!author) return null
  // 如果已经是完整对象直接返回
  if (author.id !== undefined) return author
  return {
    id: author.id,
    username: author.username || '',
    nickname: author.nickname || author.authorName || '',
    avatar: author.avatar || author.authorAvatar || '',
    email: author.email
  }
}

/**
 * 转换文章数据（后端 → 前端）
 * 修复:
 *   - status Integer→String
 *   - readCount/readNum/viewCount 统一
 *   - summary/description 统一
 *   - titleImage/thumbnail/coverImage 统一
 *   - 时间字段统一
 */
export function transformArticle(article: any): any {
  if (!article) return null

  return {
    id: article.id,
    title: article.title || '',
    // description: 优先 description，其次 summary（ArticleVO 用 summary，ArticleListVO 用 description）
    description: article.description || article.summary || '',
    content: article.content || '',
    // titleImage: 优先 titleImage，其次 thumbnail/coverImage
    titleImage: article.titleImage || article.thumbnail || article.coverImage || '',
    // 分类
    category: article.category ? transformCategory(article.category) : null,
    categoryId: article.categoryId || article.category?.id,
    categoryName: article.categoryName || article.category?.name || '',
    // 标签：tagNames 是字符串数组如 ["AI","Python"]，需要转换为对象数组
    tags: Array.isArray(article.tagNames)
      ? article.tagNames.map((name: string, index: number) => ({ id: index || 0, name }))
      : Array.isArray(article.tags)
        ? article.tags.map(transformTag)
        : [],
    // 作者
    author: article.author
      ? transformArticleAuthor(article.author)
      : article.authorId
        ? { id: article.authorId }
        : null,
    authorId: article.authorId,
    authorName: article.authorName || '',
    authorAvatar: article.authorAvatar || '',
    // 时间
    createdAt: article.createdAt || article.createdTime || article.createTime,
    updatedAt: article.updatedAt || article.updatedTime || article.updateTime,
    publishTime: article.publishTime || article.publish_time,
    // 数量字段: 优先 readCount，其次 readNum，最后 viewCount
    readCount: toNum(
      article.readCount ?? article.readNum ?? article.viewCount
    ),
    likeCount: toNum(
      article.likeCount ?? article.likeNum ?? article.like_num
    ),
    commentCount: toNum(
      article.commentCount ?? article.commentNum ?? article.comment_num
    ),
    collectCount: toNum(
      article.collectCount ?? article.collectNum ?? article.collect_num
    ),
    shareCount: toNum(article.shareCount ?? article.shareNum),
    // 状态转换: Integer(0/1) → 'draft'/'published'
    status: transformArticleStatus(article.status),
    isTop: !!article.isTop || article.topStatus === 1,
    isRecommend: !!article.isRecommend,
    // 当前用户互动状态
    isLiked: !!article.isLiked,
    isCollected: !!article.isCollected,
    // 上下篇
    preArticle: article.preArticle || null,
    nextArticle: article.nextArticle || null
  }
}

/**
 * 转换文章列表数据
 */
export function transformArticleList(list: any[]): any[] {
  if (!Array.isArray(list)) return []
  return list.map(transformArticle)
}

/**
 * 转换用户数据
 * 修复:
 *   - status Integer→String
 *   - gender Integer→String
 *   - birthday 格式
 *   - 排除敏感字段 (password, mfaSecret, backupCodes)
 */
export function transformUser(user: any): any {
  if (!user) return null

  return {
    id: user.id,
    username: user.username || '',
    email: user.email || '',
    phone: user.phone || '',
    avatar: user.avatar || '',
    nickname: user.nickname || user.username || '',
    intro: user.intro || user.bio || '',
    bio: user.bio || user.intro || '',
    website: user.website || '',
    location: user.location || '',
    github: user.github || '',
    gitee: user.gitee || '',
    csdn: user.csdn || '',
    zhihu: user.zhihu || '',
    // 枚举值转换
    gender: transformGender(user.gender),
    birthday: user.birthday || null,
    status: transformUserStatus(user.status),
    mfaEnabled: !!user.mfaEnabled || user.mfa_enabled === 1,
    mfaType: user.mfaType || user.mfa_type || '',
    // 时间
    lastLoginTime: user.lastLoginTime || user.last_login_time,
    createdAt: user.createdAt || user.createdTime || user.createTime,
    updatedAt: user.updatedAt || user.updatedTime || user.updateTime,
    // 不包含密码等敏感信息
    _raw: undefined
  }
}

/**
 * 转换分页响应
 * 兼容后端不同分页格式: { list, total, page, size, pages }
 *                         { records, total, current, size, pages }
 *
 * ⚠️ 重要: 同时对 list 内的文章数据进行字段转换
 */
export function transformPageResponse(response: any): any {
  if (!response) return { list: [], total: 0, page: 1, size: 10, pages: 0 }

  const rawList = response.list || response.records || []
  const total = Number(response.total || 0)
  const page = Number(response.page || response.current || 1)
  const size = Number(response.size || response.pageSize || 10)
  const pages = Math.ceil(total / size) || 1

  // 自动推断 list 元素类型并转换
  let list = rawList
  if (rawList.length > 0) {
    const first = rawList[0]
    if (first?.title !== undefined) {
      list = transformArticleList(rawList)
    } else if (first?.content !== undefined && first?.articleId !== undefined) {
      list = transformCommentList(rawList)
    }
  }

  return {
    list,
    total,
    page,
    size,
    pages
  }
}

/**
 * 统一入口: 转换任意 API 响应数据
 * 根据数据结构自动选择转换策略
 */
export function autoTransform(data: any, type?: 'article' | 'articleList' | 'comment' | 'user' | 'category' | 'tag'): any {
  if (!data) return data

  // 单个对象
  if (!Array.isArray(data) && typeof data === 'object') {
    if (type === 'article') return transformArticle(data)
    if (type === 'comment') return transformComment(data)
    if (type === 'user') return transformUser(data)
    if (type === 'category') return transformCategory(data)
    if (type === 'tag') return transformTag(data)
    // 自动推断
    if (data.content !== undefined && data.title !== undefined) return transformArticle(data)
    if (data.username !== undefined && data.email !== undefined) return transformUser(data)
    if (data.content !== undefined && data.articleId !== undefined) return transformComment(data)
  }

  // 数组
  if (Array.isArray(data)) {
    if (type === 'article' || type === 'articleList') return transformArticleList(data)
    if (type === 'comment') return transformCommentList(data)
    if (type === 'category') return data.map(transformCategory)
    if (type === 'tag') return data.map(transformTag)
    // 默认当作文章列表
    if (data.length > 0 && data[0]?.title !== undefined) return transformArticleList(data)
    if (data.length > 0 && data[0]?.content !== undefined) return transformCommentList(data)
  }

  // 分页响应
  if (data.list !== undefined || data.records !== undefined) {
    const result = transformPageResponse(data)
    if (result.list.length > 0) {
      const first = result.list[0]
      if (first?.title !== undefined) {
        result.list = transformArticleList(result.list)
      } else if (first?.content !== undefined) {
        result.list = transformCommentList(result.list)
      }
    }
    return result
  }

  return data
}
