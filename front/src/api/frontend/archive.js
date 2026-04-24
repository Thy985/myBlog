import request from '@/axios'

export function getArchives(params) {
  return request.get('/article/archive', { params })
}
