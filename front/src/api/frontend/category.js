import request from '@/axios'
import { transformCategory, transformArticleList } from '@/utils/transform'

export function getCategories() {
  return request.get('/category/discover').then(res => {
    if (res && res.data && Array.isArray(res.data)) {
      res.data = res.data.map(transformCategory)
    }
    return res
  })
}

export function getCategoryArticles(params) {
  return request.get('/article/list', { params: { ...params, page: params.current, size: params.size } }).then(res => {
    if (res && res.data && res.data.list) {
      res.data.list = transformArticleList(res.data.list)
    }
    return res
  })
}
