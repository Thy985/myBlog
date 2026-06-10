import type { GrowthReport, RagResult } from '@/types/growth'

export interface FormatResult {
  title: string
  content: string
}

const EXECUTION_HOURS = {
  daily: 2,
  weekly: 3,
  monthly: 4
} as const

export function calculateNextExecutionTime(cycle: string): string {
  const now = new Date()
  const hour = EXECUTION_HOURS[cycle as keyof typeof EXECUTION_HOURS] || 2
  let next = new Date(now)

  switch (cycle) {
    case 'daily':
      next.setDate(next.getDate() + 1)
      break
    case 'weekly':
      const daysUntilMonday = (8 - next.getDay()) % 7 || 7
      next.setDate(next.getDate() + daysUntilMonday)
      break
    case 'monthly':
      next = new Date(next.getFullYear(), next.getMonth() + 1, 1)
      break
    default:
      next.setDate(next.getDate() + 1)
  }

  next.setHours(hour, 0, 0, 0)

  if (next < now) {
    const increment = cycle === 'daily' ? 1 : cycle === 'weekly' ? 7 : 30
    next.setDate(next.getDate() + increment)
  }

  const timeStr = next.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })

  return `明天凌晨 ${hour}:00` // 简化显示
}

// ============================================================
// 通用格式化工具
// ============================================================

/**
 * 格式化日期
 * @param date - Date 对象、时间戳或日期字符串
 * @param format - 格式化模板: 'YYYY-MM-DD', 'YYYY-MM-DD HH:mm:ss', 'MM/DD/YYYY', 'YYYY/MM/DD', 'MM-DD', 'YYYY年MM月DD日'
 * @returns 格式化后的日期字符串
 */
export function formatDate(
  date: Date | string | number | null | undefined,
  format: string = 'YYYY-MM-DD'
): string {
  if (!date) return ''

  const d = typeof date === 'string' ? new Date(date) : new Date(date)
  if (isNaN(d.getTime())) return ''

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化数字，添加千分位分隔符
 * @param num - 数字或数字字符串
 * @param decimals - 小数位数
 * @returns 格式化后的数字字符串
 */
export function formatNumber(num: number | string | null | undefined, decimals: number = 2): string {
  if (num === null || num === undefined || num === '') return '0'

  const n = typeof num === 'string' ? parseFloat(num) : num
  if (isNaN(n)) return '0'

  const fixed = n.toFixed(decimals)
  const parts = fixed.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')

  return parts.join('.')
}

/**
 * 格式化文件大小
 * @param bytes - 字节数
 * @returns 格式化后的大小字符串 (B/KB/MB/GB)
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || bytes < 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }

  return `${size % 1 === 0 ? size.toFixed(0) : size.toFixed(2)} ${units[unitIndex]}`
}

/**
 * 格式化相对时间
 * @param date - Date 对象、时间戳或日期字符串
 * @returns 相对时间描述字符串
 */
export function formatRelativeTime(
  date: Date | string | number | null | undefined
): string {
  if (!date) return ''

  const now = new Date()
  const d = typeof date === 'string' ? new Date(date) : new Date(date)
  if (isNaN(d.getTime())) return ''

  const diffMs = now.getTime() - d.getTime()
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)
  const diffHour = Math.floor(diffMin / 60)
  const diffDay = Math.floor(diffHour / 24)

  if (diffSec < 60) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  if (diffHour < 24) return `${diffHour}小时前`
  if (diffDay < 30) return `${diffDay}天前`
  if (diffDay < 365) return `${Math.floor(diffDay / 30)}个月前`
  return `${Math.floor(diffDay / 365)}年前`
}

/**
 * 截断文本，超出部分用省略号替代
 * @param text - 原始文本
 * @param maxLength - 最大长度
 * @param suffix - 省略号后缀
 * @returns 截断后的文本
 */
export function truncateText(
  text: string,
  maxLength: number,
  suffix: string = '...'
): string {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + suffix
}

// ============================================================
// Growth 报告格式化
// ============================================================

export function formatTaskResult(data: any): string {
  if (!data) return '无返回数据'
  if (typeof data === 'string') return data
  return JSON.stringify(data, null, 2)
}

export function formatReport(data: GrowthReport | any): string {
  if (!data) return '无报告数据'
  if (typeof data === 'string') return data

  const lines: string[] = []

  if (data.summary) {
    lines.push(`总文章数：${data.summary.totalArticles ?? 0}`)
    lines.push(`新增文章：${data.summary.newArticles ?? 0}`)
    lines.push(`优化文章：${data.summary.optimizedArticles ?? 0}`)
    lines.push(`发现问题：${data.summary.issuesFound ?? 0}`)
  }

  if (data.topics?.length) {
    lines.push('\n推荐主题：')
    data.topics.forEach((t: any, i: number) => {
      lines.push(`${i + 1}. ${extractText(t)}`)
    })
  }

  return lines.length ? lines.join('\n') : JSON.stringify(data, null, 2)
}

export function formatOpportunities(data: any): string {
  if (!data) return '无数据'
  if (typeof data === 'string') return data
  if (Array.isArray(data)) {
    if (!data.length) return '未发现新机会'
    return data.map((item, i) => `${i + 1}. ${extractText(item)}`).join('\n')
  }
  return JSON.stringify(data, null, 2)
}

export function formatTopics(data: any): string {
  if (!data) return '无推荐'
  if (typeof data === 'string') return data
  if (Array.isArray(data)) {
    if (!data.length) return '暂无推荐'
    return data.map((item, i) => `${i + 1}. ${extractText(item)}`).join('\n')
  }
  return JSON.stringify(data, null, 2)
}

export function extractText(item: any): string {
  if (typeof item === 'string') return item
  return item?.opportunity || item?.title || item?.topic || item?.name || JSON.stringify(item)
}

export function extractResults(data: any): RagResult[] {
  if (!data) return []
  if (Array.isArray(data)) return data
  if (data.results) return data.results
  if (data.contents) return data.contents
  return []
}

export function formatActionResult(type: string, data: any): FormatResult {
  switch (type) {
    case 'execute':
      return { title: '执行结果', content: formatTaskResult(data) }
    case 'report':
      return { title: 'Growth 报告', content: formatReport(data) }
    case 'opportunities':
      return { title: '发现的机会', content: formatOpportunities(data) }
    case 'topics':
      return { title: '推荐创作主题', content: formatTopics(data) }
    default:
      return { title: '结果', content: formatTaskResult(data) }
  }
}