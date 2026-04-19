/**
 * v-mermaid 指令
 * 用于渲染 Mermaid 图表
 * 使用方式：<pre v-mermaid="code"><code class="language-mermaid">code</code></pre>
 */
import logger from '@/utils/logger'

let mermaidInstance = null

async function getMermaid() {
  if (!mermaidInstance) {
    const module = await import('mermaid')
    mermaidInstance = module.default
    mermaidInstance.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'strict',
      fontFamily: 'Arial, sans-serif'
    })
  }
  return mermaidInstance
}

export const mermaidDirective = {
  async mounted(el, binding) {
    const code = binding.value || el.querySelector('code')?.textContent?.trim()
    if (!code) {return}

    try {
      const mm = await getMermaid()
      const id = `mermaid-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
      const { svg } = await mm.render(id, code)

      // 用 SVG 替换 code 元素，但保留 pre 元素的结构
      const codeEl = el.querySelector('code')
      if (codeEl) {
        // 创建一个容器来包裹 SVG
        const wrapper = document.createElement('div')
        wrapper.className = 'mermaid-rendered'
        wrapper.innerHTML = svg
        el.replaceChild(wrapper, codeEl)
      }
    } catch (e) {
      logger.error('Mermaid render error:', e)
    }
  }
}

export default mermaidDirective
