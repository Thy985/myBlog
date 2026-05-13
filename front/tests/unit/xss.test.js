import { describe, it, expect } from 'vitest'
import { sanitizeHtml } from '@/utils/xss'

describe('XSS过滤功能测试', () => {
  it('应该过滤恶意的script标签', () => {
    const maliciousHtml = '<script>alert("XSS攻击")</script>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('<script>')
    expect(sanitized).not.toContain('alert("XSS攻击")')
  })

  it('应该过滤恶意的onclick事件', () => {
    const maliciousHtml = '<div onclick="alert(\"XSS攻击\")">点击我</div>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('onclick=')
    expect(sanitized).toContain('<div>点击我</div>')
  })

  it('应该保留安全的HTML标签', () => {
    const safeHtml = '<div><p>安全的HTML内容</p><br><strong>加粗文本</strong></div>'
    const sanitized = sanitizeHtml(safeHtml)
    expect(sanitized).toBe(safeHtml)
  })

  it('应该处理空字符串', () => {
    const sanitized = sanitizeHtml('')
    expect(sanitized).toBe('')
  })

  it('应该处理非字符串输入', () => {
    expect(sanitizeHtml(null)).toBe('')
    expect(sanitizeHtml(undefined)).toBe('')
    expect(sanitizeHtml(123)).toBe('')
    expect(sanitizeHtml({})).toBe('')
  })

  it('应该过滤iframe标签', () => {
    const maliciousHtml = '<iframe src="http://malicious.com"></iframe>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('<iframe>')
  })

  it('应该过滤svg中的脚本', () => {
    const maliciousHtml = '<svg><script>alert("XSS攻击")</script></svg>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('<script>')
  })
})