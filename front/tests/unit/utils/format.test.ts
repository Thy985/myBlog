import { describe, it, expect, vi } from 'vitest'
import { formatDate, formatNumber, formatFileSize, formatRelativeTime, truncateText } from '@/utils/format'

describe('formatDate - 日期格式化', () => {
  it('应该将日期对象格式化为 YYYY-MM-DD 格式', () => {
    const date = new Date('2026-06-10T10:30:00')
    expect(formatDate(date)).toBe('2026-06-10')
  })

  it('应该支持 YYYY-MM-DD HH:mm:ss 格式', () => {
    const date = new Date('2026-06-10T14:05:30')
    expect(formatDate(date, 'YYYY-MM-DD HH:mm:ss')).toBe('2026-06-10 14:05:30')
  })

  it('应该支持 MM/DD/YYYY 格式', () => {
    const date = new Date('2026-06-10')
    expect(formatDate(date, 'MM/DD/YYYY')).toBe('06/10/2026')
  })

  it('应该支持 YYYY/MM/DD 格式', () => {
    const date = new Date('2026-06-10')
    expect(formatDate(date, 'YYYY/MM/DD')).toBe('2026/06/10')
  })

  it('应该支持 MM-DD 格式', () => {
    const date = new Date('2026-06-10')
    expect(formatDate(date, 'MM-DD')).toBe('06-10')
  })

  it('应该支持 YYYY年MM月DD日 格式', () => {
    const date = new Date('2026-06-10')
    expect(formatDate(date, 'YYYY年MM月DD日')).toBe('2026年06月10日')
  })

  it('应该处理时间戳输入', () => {
    const timestamp = 1718010600000
    expect(formatDate(timestamp)).toBe('2024-06-10')
  })

  it('应该处理日期字符串输入', () => {
    expect(formatDate('2026-01-15T08:00:00Z')).toBe('2026-01-15')
  })

  it('应该处理 null 输入', () => {
    expect(formatDate(null)).toBe('')
  })

  it('应该处理 undefined 输入', () => {
    expect(formatDate(undefined)).toBe('')
  })

  it('应该处理空字符串输入', () => {
    expect(formatDate('')).toBe('')
  })

  it('应该处理无效日期字符串', () => {
    expect(formatDate('not-a-date')).toBe('')
  })
})

describe('formatNumber - 数字格式化', () => {
  it('应该添加千分位分隔符', () => {
    expect(formatNumber(1234567)).toBe('1,234,567.00')
  })

  it('应该处理小数', () => {
    expect(formatNumber(1234.567)).toBe('1,234.57')
  })

  it('应该处理负数', () => {
    expect(formatNumber(-1234567)).toBe('-1,234,567.00')
  })

  it('应该支持自定义小数位数', () => {
    expect(formatNumber(1234567, 0)).toBe('1,234,567')
    expect(formatNumber(1234.56789, 4)).toBe('1,234.5679')
  })

  it('应该处理数字字符串', () => {
    expect(formatNumber('1234567')).toBe('1,234,567.00')
  })

  it('应该处理 null', () => {
    expect(formatNumber(null)).toBe('0')
  })

  it('应该处理 undefined', () => {
    expect(formatNumber(undefined)).toBe('0')
  })

  it('应该处理空字符串', () => {
    expect(formatNumber('')).toBe('0')
  })

  it('应该处理非数字字符串', () => {
    expect(formatNumber('abc')).toBe('0')
  })
})

describe('formatFileSize - 文件大小格式化', () => {
  it('应该格式化字节', () => {
    expect(formatFileSize(500)).toBe('500 B')
  })

  it('应该转换为 KB', () => {
    expect(formatFileSize(1024)).toBe('1 KB')
    expect(formatFileSize(2048)).toBe('2 KB')
  })

  it('应该转换为 MB', () => {
    expect(formatFileSize(1024 * 1024)).toBe('1 MB')
  })

  it('应该转换为 GB', () => {
    expect(formatFileSize(1024 * 1024 * 1024)).toBe('1 GB')
  })

  it('应该处理小数部分', () => {
    expect(formatFileSize(1536)).toBe('1.50 KB')
  })

  it('应该处理 null', () => {
    expect(formatFileSize(null)).toBe('0 B')
  })

  it('应该处理 undefined', () => {
    expect(formatFileSize(undefined)).toBe('0 B')
  })

  it('应该处理负数', () => {
    expect(formatFileSize(-100)).toBe('0 B')
  })

  it('应该处理 0', () => {
    expect(formatFileSize(0)).toBe('0 B')
  })
})

describe('formatRelativeTime - 相对时间格式化', () => {
  it('应该显示"刚刚"', () => {
    const now = new Date()
    expect(formatRelativeTime(now)).toBe('刚刚')
  })

  it('应该显示"X分钟前"', () => {
    const fiveMinAgo = new Date(Date.now() - 5 * 60 * 1000)
    expect(formatRelativeTime(fiveMinAgo)).toBe('5分钟前')

    const thirtyMinAgo = new Date(Date.now() - 30 * 60 * 1000)
    expect(formatRelativeTime(thirtyMinAgo)).toBe('30分钟前')
  })

  it('应该显示"X小时前"', () => {
    const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000)
    expect(formatRelativeTime(threeHoursAgo)).toBe('3小时前')

    const twelveHoursAgo = new Date(Date.now() - 12 * 60 * 60 * 1000)
    expect(formatRelativeTime(twelveHoursAgo)).toBe('12小时前')
  })

  it('应该显示"X天前"', () => {
    const fiveDaysAgo = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000)
    expect(formatRelativeTime(fiveDaysAgo)).toBe('5天前')

    const twentyDaysAgo = new Date(Date.now() - 20 * 24 * 60 * 60 * 1000)
    expect(formatRelativeTime(twentyDaysAgo)).toBe('20天前')
  })

  it('应该显示"X个月前"', () => {
    const threeMonthsAgo = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000)
    expect(formatRelativeTime(threeMonthsAgo)).toBe('3个月前')
  })

  it('应该显示"X年前"', () => {
    const twoYearsAgo = new Date(Date.now() - 2 * 365 * 24 * 60 * 60 * 1000)
    expect(formatRelativeTime(twoYearsAgo)).toBe('2年前')
  })

  it('应该处理 null', () => {
    expect(formatRelativeTime(null)).toBe('')
  })

  it('应该处理 undefined', () => {
    expect(formatRelativeTime(undefined)).toBe('')
  })

  it('应该处理无效日期', () => {
    expect(formatRelativeTime('invalid-date')).toBe('')
  })
})

describe('truncateText - 文本截断', () => {
  it('应该截断超出长度的文本', () => {
    expect(truncateText('这是一段很长的文本内容', 5)).toBe('这是一段很...')
  })

  it('应该保留不超出长度的文本', () => {
    expect(truncateText('短文', 10)).toBe('短文')
  })

  it('应该支持自定义后缀', () => {
    expect(truncateText('这是一段很长的文本', 5, '…')).toBe('这是一段很…')
  })

  it('应该处理空字符串', () => {
    expect(truncateText('', 10)).toBe('')
  })

  it('应该处理空值', () => {
    expect(truncateText(null as unknown as string, 10)).toBe('')
  })

  it('应该正确处理长度等于 maxLength 的文本', () => {
    expect(truncateText('abcde', 5)).toBe('abcde')
  })

  it('应该正确处理长度比 maxLength 多 1 的文本', () => {
    expect(truncateText('abcdef', 5)).toBe('abcde...')
  })
})
