export interface GrowthFormData {
  growthCycle: 'daily' | 'weekly' | 'monthly'
  autoExecute: boolean
  contentAudit: boolean
  seoOptimization: boolean
  ragEnabled: boolean
}

export interface RagSearchParams {
  query: string
  topK: number
}

export interface RagResult {
  title?: string
  snippet?: string
  content?: string
  text?: string
  score?: number
}

export type ActionType = 'execute' | 'report' | 'opportunities' | 'topics' | null

export interface ActionResult {
  title: string
  content: string
}

export interface GrowthReport {
  summary?: {
    totalArticles?: number
    newArticles?: number
    optimizedArticles?: number
    issuesFound?: number
  }
  topics?: Array<{ title?: string; topic?: string; name?: string; [key: string]: any }>
}

export const GROWTH_CYCLE_OPTIONS = [
  { value: 'daily', label: '每日增长' },
  { value: 'weekly', label: '每周增长' },
  { value: 'monthly', label: '每月增长' }
] as const

export const TOP_K_OPTIONS = [
  { value: 3, label: 'Top 3' },
  { value: 5, label: 'Top 5' },
  { value: 10, label: 'Top 10' }
] as const

export interface ToggleOption {
  key: keyof GrowthFormData
  label: string
  description: string
  importance: 'high' | 'medium' | 'low'
}

export const TOGGLE_OPTIONS: ToggleOption[] = [
  {
    key: 'autoExecute',
    label: '自动执行',
    description: '按周期自动运行 Growth 任务',
    importance: 'high'
  },
  {
    key: 'contentAudit',
    label: '内容审核',
    description: '自动审核生成的内容',
    importance: 'medium'
  },
  {
    key: 'seoOptimization',
    label: 'SEO 优化',
    description: '自动优化文章 SEO 结构',
    importance: 'low'
  },
  {
    key: 'ragEnabled',
    label: '启用 RAG',
    description: '检索相关知识辅助内容生成',
    importance: 'medium'
  }
]