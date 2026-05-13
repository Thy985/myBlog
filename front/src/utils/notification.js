import { ElNotification, ElMessageBox, ElMessage, ElLoading } from 'element-plus'

export function notification(message, type = 'success', dangerouslyUseHTMLString = false) {
  ElNotification({
    message,
    type,
    dangerouslyUseHTMLString,
    duration: 3000
  })
}

export function showModel(content = '提示内容', type = 'warning', title = '') {
  return ElMessageBox.confirm(
    content,
    title,
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type
    }
  )
}

export function showMessage(message = '提示内容', type = 'success', customClass = '') {
  return ElMessage({
    type: type,
    message,
    customClass
  })
}

export async function confirmDelete(itemName, itemType = '数据') {
  return ElMessageBox.confirm(
    `确定要删除这个${itemType}"${itemName}"吗？此操作不可恢复。`,
    '确认删除',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  )
}

let loadingInstance = null

export function showLoading(text = '加载中...') {
  if (loadingInstance) {
    loadingInstance.close()
  }
  loadingInstance = ElLoading.service({
    lock: true,
    text,
    background: 'rgba(0, 0, 0, 0.7)'
  })
  return loadingInstance
}

export function hideLoading() {
  if (loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

export function showSuccess(message = '操作成功') {
  return ElMessage.success({
    message,
    duration: 3000
  })
}

export function showError(message = '操作失败') {
  return ElMessage.error({
    message,
    duration: 3000
  })
}

export function showWarning(message = '警告') {
  return ElMessage.warning({
    message,
    duration: 3000
  })
}
