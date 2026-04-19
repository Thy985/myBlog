import request from '@/axios'

/**
 * 获取博客设置详情
 * @returns {Promise}
 */
export function getBlogSettingDetail() {
  // 使用 BlogSettingController 的接口
  return request.get('/blog/setting/detail')
}

/**
 * 获取管理员设置列表
 * @returns {Promise}
 */
export function getAdminSettings() {
  // 使用 AdminController 的接口
  return request.get('/admin/settings')
}

/**
 * 更新博客设置
 * @param {Object} data - 设置数据
 * @returns {Promise}
 */
export function updateBlogSetting(data) {
  // 如果 data 包含 key 和 value，调用 AdminController 的单个设置更新接口
  if (data.key && data.value !== undefined) {
    return request.put(`/admin/settings/${data.key}`, null, {
      params: { value: data.value }
    })
  }
  // 批量更新设置（如果后端支持）
  // 目前后端 AdminController 只支持单个更新，需要逐个调用
  const promises = Object.entries(data).map(([key, value]) => {
    return request.put(`/admin/settings/${key}`, null, {
      params: { value: String(value) }
    })
  })
  return Promise.all(promises).then(() => ({
    code: 200,
    message: 'success'
  }))
}

/**
 * 更新单个设置项
 * @param {string} key - 设置键名
 * @param {string} value - 设置值
 * @returns {Promise}
 */
export function updateSettingItem(key, value) {
  return request.put(`/admin/settings/${key}`, null, {
    params: { value: String(value) }
  })
}
