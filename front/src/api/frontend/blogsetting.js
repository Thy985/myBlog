import request from '@/axios'

export function getBlogSettingDetail() {
  return request.get('/blog/setting/detail')
}
