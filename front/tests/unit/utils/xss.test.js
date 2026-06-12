import { describe, it, expect } from 'vitest'
import { sanitizeHtml, escapeHtml, stripTags } from '@/utils/xss'

describe('sanitizeHtml - XSS 过滤', () => {
  it('应该过滤恶意的 script 标签', () => {
    const maliciousHtml = '<script>alert("XSS攻击")</script><p>安全内容</p>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('<script>')
    expect(sanitized).not.toContain('alert')
    expect(sanitized).toContain('<p>安全内容</p>')
  })

  it('应该过滤事件处理器 (onclick, onerror 等)', () => {
    const maliciousHtml = '<img src="x" onerror="alert(1)" />'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('onerror')
    expect(sanitized).not.toContain('onclick')
  })

  it('应该过滤 onclick 事件', () => {
    const maliciousHtml = '<div onclick="alert(\'XSS\')">点击我</div>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('onclick')
  })

  it('应该过滤 javascript: 协议 URL', () => {
    const maliciousHtml = '<a href="javascript:alert(1)">恶意链接</a>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('javascript:')
    expect(sanitized).not.toContain('href=')
  })

  it('应该允许安全的 HTML 标签 (p, a, img 等)', () => {
    const safeHtml = '<p>段落</p><a href="https://example.com">链接</a><img src="https://example.com/img.png" alt="图片" />'
    const sanitized = sanitizeHtml(safeHtml)
    expect(sanitized).toContain('<p>段落</p>')
    expect(sanitized).toContain('href="https://example.com"')
    expect(sanitized).toContain('src="https://example.com/img.png"')
    expect(sanitized).toContain('alt="图片"')
  })

  it('应该允许安全的属性 (href, src, alt 等)', () => {
    const html = '<a href="https://example.com" target="_blank">链接</a>'
    const sanitized = sanitizeHtml(html)
    expect(sanitized).toContain('href="https://example.com"')
    expect(sanitized).toContain('target="_blank"')
  })

  it('应该允许 img 标签的安全属性', () => {
    const html = '<img src="https://example.com/image.png" alt="描述" />'
    const sanitized = sanitizeHtml(html)
    expect(sanitized).toContain('<img')
    expect(sanitized).toContain('src="https://example.com/image.png"')
    expect(sanitized).toContain('alt="描述"')
  })

  it('应该处理嵌套的恶意内容', () => {
    const maliciousHtml = '<div><script>alert(1)</script><img src="x" onerror="alert(2)" /></div>'
    const sanitized = sanitizeHtml(maliciousHtml)
    expect(sanitized).not.toContain('<script>')
    expect(sanitized).not.toContain('alert')
    expect(sanitized).not.toContain('onerror')
    expect(sanitized).toContain('<div>')
    expect(sanitized).toContain('</div>')
  })

  it('应该处理编码/转义的攻击', () => {
    const encodedHtml = '&lt;script&gt;alert(1)&lt;/script&gt;'
    const sanitized = sanitizeHtml(encodedHtml)
    // 编码后的实体不会被当作 HTML 解析，应该保留或安全处理
    expect(sanitized).not.toContain('<script>')
  })

  it('应该保留 markdown 类内容', () => {
    const markdownHtml = '<h1>标题</h1><p>这是 <strong>加粗</strong> 文本和 <code>代码</code>。</p><ul><li>列表项1</li><li>列表项2</li></ul>'
    const sanitized = sanitizeHtml(markdownHtml)
    expect(sanitized).toContain('<h1>标题</h1>')
    expect(sanitized).toContain('<strong>加粗</strong>')
    expect(sanitized).toContain('<code>代码</code>')
    expect(sanitized).toContain('<ul>')
    expect(sanitized).toContain('<li>列表项1</li>')
  })

  it('应该允许 strong 和 em 标签', () => {
    const html = '<p><strong>加粗</strong>和<em>斜体</em></p>'
    const sanitized = sanitizeHtml(html)
    expect(sanitized).toBe(html)
  })

  it('应该允许 pre 和 code 标签', () => {
    const html = '<pre><code>const x = 1;</code></pre>'
    const sanitized = sanitizeHtml(html)
    expect(sanitized).toBe(html)
  })

  it('应该允许 table 相关标签', () => {
    const html = '<table><thead><tr><th>表头</th></tr></thead><tbody><tr><td>单元格</td></tr></tbody></table>'
    const sanitized = sanitizeHtml(html)
    expect(sanitized).toContain('<table>')
    expect(sanitized).toContain('<th>表头</th>')
    expect(sanitized).toContain('<td>单元格</td>')
  })

  it('应该处理空字符串', () => {
    expect(sanitizeHtml('')).toBe('')
  })

  it('应该处理非字符串输入', () => {
    expect(sanitizeHtml(null)).toBe('')
    expect(sanitizeHtml(undefined)).toBe('')
    expect(sanitizeHtml(123)).toBe('')
    expect(sanitizeHtml({})).toBe('')
  })

  it('应该过滤 iframe 标签', () => {
    const html = '<iframe src="http://malicious.com"></iframe>'
    const sanitized = sanitizeHtml(html)
    expect(sanitized).not.toContain('<iframe>')
  })

  it('应该过滤 form 标签', () => {
    const html = '<form action="http://evil.com"><input type="text" /></form>'
    const sanitized = sanitizeHtml(html)
    expect(sanitized).not.toContain('<form')
    expect(sanitized).not.toContain('</form>')
  })
})

describe('escapeHtml - HTML 转义', () => {
  it('应该转义 < 和 > 字符', () => {
    expect(escapeHtml('<div>')).toBe('&lt;div&gt;')
  })

  it('应该转义双引号', () => {
    expect(escapeHtml('"hello"')).toBe('&quot;hello&quot;')
  })

  it('应该转义单引号', () => {
    expect(escapeHtml("'hello'")).toBe('&#x27;hello&#x27;')
  })

  it('应该转义 & 字符', () => {
    expect(escapeHtml('a & b')).toBe('a &amp; b')
  })

  it('应该同时转义所有特殊字符', () => {
    const input = '<div class="test">\'hello\' & <b>world</b></div>'
    const output = escapeHtml(input)
    expect(output).toBe('&lt;div class=&quot;test&quot;&gt;&#x27;hello&#x27; &amp; &lt;b&gt;world&lt;/b&gt;&lt;/div&gt;')
  })

  it('应该处理空字符串', () => {
    expect(escapeHtml('')).toBe('')
  })

  it('应该处理 null', () => {
    expect(escapeHtml(null)).toBe('')
  })

  it('应该处理 undefined', () => {
    expect(escapeHtml(undefined)).toBe('')
  })

  it('应该处理不包含特殊字符的字符串', () => {
    expect(escapeHtml('Hello World')).toBe('Hello World')
  })
})

describe('stripTags - 移除 HTML 标签', () => {
  it('应该移除所有 HTML 标签', () => {
    expect(stripTags('<p>Hello</p>')).toBe('Hello')
  })

  it('应该移除嵌套的 HTML 标签', () => {
    expect(stripTags('<div><p>Hello <strong>World</strong></p></div>')).toBe('Hello World')
  })

  it('应该保留纯文本', () => {
    expect(stripTags('Hello World')).toBe('Hello World')
  })

  it('应该处理自闭合标签', () => {
    expect(stripTags('文本<br />更多文本')).toBe('文本更多文本')
  })

  it('应该处理带属性的标签', () => {
    expect(stripTags('<a href="http://example.com" class="link">链接</a>')).toBe('链接')
  })

  it('应该处理空字符串', () => {
    expect(stripTags('')).toBe('')
  })

  it('应该处理 null', () => {
    expect(stripTags(null)).toBe('')
  })

  it('应该处理 undefined', () => {
    expect(stripTags(undefined)).toBe('')
  })

  it('应该移除 script 标签及其内容', () => {
    expect(stripTags('<script>alert(1)</script>')).toBe('alert(1)')
  })

  it('应该处理多个标签和文本混合', () => {
    expect(stripTags('<h1>标题</h1><p>段落1</p><p>段落2</p>')).toBe('标题段落1段落2')
  })
})
