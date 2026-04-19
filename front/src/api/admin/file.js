import request from '@/axios'

/**
 * 上传文件
 * @param {File} file - 文件对象
 * @param {number} [categoryId] - 分类ID
 * @returns {Promise}
 */
export function uploadFile(file, categoryId = null) {
  // 创建 FormData 对象
  const formData = new FormData()
  formData.append('file', file)
  if (categoryId) {
    formData.append('categoryId', categoryId)
  }

  // 后端接口: POST /api/file/upload
  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 上传图片
 * @param {File} file - 图片文件
 * @returns {Promise}
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)

  // 后端接口: POST /api/file/upload/image
  return request.post('/file/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除文件
 * @param {number} id - 文件ID
 * @returns {Promise}
 */
export function deleteFile(id) {
  // 后端接口: DELETE /api/file/{id}
  return request.delete(`/file/${id}`)
}

/**
 * 获取文件列表
 * @param {Object} params - 查询参数
 * @param {number} [params.categoryId] - 分类ID
 * @param {string} [params.fileType] - 文件类型
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @returns {Promise}
 */
export function getFileList(params = {}) {
  // 后端接口: GET /api/file/list
  return request.get('/file/list', { params })
}

/**
 * 获取图片列表
 * @param {Object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @returns {Promise}
 */
export function getImageList(params = {}) {
  // 后端接口: GET /api/file/images
  return request.get('/file/images', { params })
}

/**
 * 获取文件URL
 * @param {number} id - 文件ID
 * @returns {Promise}
 */
export function getFileUrl(id) {
  // 后端接口: GET /api/file/{id}/url
  return request.get(`/file/${id}/url`)
}

/**
 * 记录文件下载
 * @param {number} id - 文件ID
 * @returns {Promise}
 */
export function recordDownload(id) {
  // 后端接口: POST /api/file/{id}/download
  return request.post(`/file/${id}/download`)
}

/**
 * 创建文件分类
 * @param {Object} data - 分类数据
 * @param {string} data.name - 分类名称
 * @param {string} [data.description] - 分类描述
 * @returns {Promise}
 */
export function createFileCategory(data) {
  // 后端接口: POST /api/file/category
  return request.post('/file/category', null, {
    params: {
      name: data.name,
      description: data.description
    }
  })
}

/**
 * 删除文件分类
 * @param {number} id - 分类ID
 * @returns {Promise}
 */
export function deleteFileCategory(id) {
  // 后端接口: DELETE /api/file/category/{id}
  return request.delete(`/file/category/${id}`)
}

/**
 * 获取文件分类列表
 * @returns {Promise}
 */
export function getFileCategoryList() {
  // 后端接口: GET /api/file/category/list
  return request.get('/file/category/list')
}
