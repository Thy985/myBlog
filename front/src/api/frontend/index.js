import request from '@/axios'
import { transformPageResponse } from '@/utils/transform'

export function getIndexArticles(params) {
  // 后端使用 GET /article/list，接受 page 和 size 参数
  return request.get('/article/list', {
    params: {
      page: params.current || params.page || 1,
      size: params.size || 10
    }
  }).then(res => {
    if (res && res.data) {
      res.data = transformPageResponse(res.data)
    }
    return res
  })
}

export function searchArticles(keyword, page = 1, size = 10) {
  return request.get('/article/search', { params: { keyword, page, size } }).then(res => {
    if (res && res.data) {
      res.data = transformPageResponse(res.data)
    }
    return res
  })
}
