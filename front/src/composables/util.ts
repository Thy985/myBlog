import { ElNotification, ElMessageBox, ElMessage } from 'element-plus'
import nprogress from 'nprogress'

export type NotificationType = 'success' | 'warning' | 'info' | 'error'
export type MessageType = 'success' | 'warning' | 'info' | 'error'

export function notification(message: string, type: NotificationType = 'success', dangerouslyUseHTMLString = false): void {
  ElNotification({
    message,
    type,
    dangerouslyUseHTMLString,
    duration: 3000
  })
}

export function showModel(content = '提示内容', type: MessageType = 'warning', title = ''): Promise<'confirm' | 'cancel'> {
  return ElMessageBox.confirm(
    content,
    title,
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type
    }
  ) as Promise<'confirm' | 'cancel'>
}

export function showMessage(message = '提示内容', type: MessageType = 'success', customClass = ''): void {
  ElMessage({
    type,
    message,
    customClass
  })
}

export function showPageLoading(): void {
  nprogress.start()
}

export function hidePageLoading(): void {
  nprogress.done()
}