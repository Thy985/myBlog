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