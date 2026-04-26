import request from '@/axios'
import type { ApiResponse } from '@/types/api'

// 获取博客设置详情
export function getBlogSettingDetail(): Promise<ApiResponse<any>> {
  return request.get('/blog/setting/detail')
}

// 获取管理员设置列表
export function getAdminSettings(): Promise<ApiResponse<any>> {
  return request.get('/admin/settings')
}

// 更新博客设置
export function updateBlogSetting(data: Record<string, any>): Promise<ApiResponse<null>> {
  if (data.key && data.value !== undefined) {
    return request.put(`/admin/settings/${data.key}`, null, {
      params: { value: data.value }
    })
  }
  const promises = Object.entries(data).map(([key, value]) => {
    return request.put(`/admin/settings/${key}`, null, {
      params: { value: String(value) }
    })
  })
  return Promise.all(promises).then(() => ({
    code: 200,
    message: 'success'
  } as ApiResponse<null>))
}

// 更新单个设置项
export function updateSettingItem(key: string, value: string): Promise<ApiResponse<null>> {
  return request.put(`/admin/settings/${key}`, null, {
    params: { value }
  })
}