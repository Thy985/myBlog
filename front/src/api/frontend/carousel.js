/**
 * ⚠️ 警告：后端没有对应的 CarouselController 接口
 * 当前轮播图功能使用 /article/hot 接口获取热门文章作为轮播数据
 * 参考：src/api/frontend/article.js 中的 getRecommendedArticles 函数
 */
import logger from '@/utils/logger'

/**
 * @deprecated 后端没有 /carousel/list 接口，请勿使用
 * 请使用 getRecommendedArticles() 代替
 */
export function getCarouselList() {
  logger.warn('getCarouselList: 后端接口 /carousel/list 不存在，请使用 getRecommendedArticles')
  return Promise.resolve({ code: 200, data: [], message: 'success' })
}

/**
 * @deprecated 后端没有 /carousel/{id} 接口，请勿使用
 */
// eslint-disable-next-line no-unused-vars
export function getCarouselDetail(_id) {
  logger.warn('getCarouselDetail: 后端接口 /carousel/{id} 不存在')
  return Promise.resolve({ code: 200, data: null, message: 'success' })
}
