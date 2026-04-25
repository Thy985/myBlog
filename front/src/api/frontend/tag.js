import request from '@/axios'
import { transformTag } from '@/utils/transform'

export function getTags() {
  return request.get('/tag/discover').then(res => {
    if (res && res.data && Array.isArray(res.data)) {
      res.data = res.data.map(transformTag)
    }
    return res
  })
}

export function getTagArticles(params) {
  return request.get('/article/list', { params: { ...params, page: params.current, size: params.size } })
}
